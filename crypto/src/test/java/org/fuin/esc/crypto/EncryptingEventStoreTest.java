/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved.
 * http://www.fuin.org/
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package org.fuin.esc.crypto;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.EventStore;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.TypeName;
import org.fuin.objects4j.crypto.EncryptedData;
import org.fuin.objects4j.crypto.EncryptionKeyIdUnknownException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link EncryptingEventStore}.
 */
public class EncryptingEventStoreTest {

    private static final TypeName DATA_TYPE = new TypeName("MyEvent");

    private static final TypeName META_TYPE = new TypeName("MyMeta");

    private static final String KEY_ID = "key-1";

    private TestEventStore delegate;

    private FakeEncryptedDataService service;

    private TestRegistry registry;

    @BeforeEach
    public void setup() throws Exception {
        delegate = new TestEventStore();
        service = new FakeEncryptedDataService();
        service.createKey(KEY_ID);
        registry = new TestRegistry();
    }

    private EncryptingEventStore.Builder builder() {
        return new EncryptingEventStore.Builder()
                .delegate(delegate)
                .serRegistry(registry)
                .desRegistry(registry)
                .encryptionService(service)
                .keyIdResolver(new FixedKeyIdResolver(KEY_ID))
                .encryptedDataFactory(new TestEncryptedDataFactory());
    }

    @Test
    public void testRoundTripDataEncrypted() {
        // PREPARE
        final EncryptingEventStore es = builder().build();
        final StreamId streamId = new SimpleStreamId("stream-1");
        final CommonEvent original = new SimpleCommonEvent(new EventId(), DATA_TYPE,
                new TestData("secret-payload"), META_TYPE, new TestData("plain-meta"), null);

        // TEST
        es.appendToStream(streamId, original);

        // VERIFY at rest: data is encrypted, meta stays plain
        final CommonEvent stored = delegate.events(streamId).get(0);
        assertThat(stored.getData()).isInstanceOf(EncryptedData.class);
        assertThat(stored.getDataType()).isEqualTo(new TypeName("EscEncryptedData"));
        assertThat(stored.getMeta()).isInstanceOf(TestData.class);
        assertThat(stored.getMetaType()).isEqualTo(META_TYPE);

        // VERIFY read back restores the original
        final CommonEvent read = es.readEvent(streamId, 0);
        assertThat(read.getId()).isEqualTo(original.getId());
        assertThat(read.getDataType()).isEqualTo(DATA_TYPE);
        assertThat(read.getData()).isEqualTo(new TestData("secret-payload"));
        assertThat(read.getMetaType()).isEqualTo(META_TYPE);
        assertThat(read.getMeta()).isEqualTo(new TestData("plain-meta"));
    }

    @Test
    public void testReadForwardBackwardAndAll() {
        // PREPARE
        final EncryptingEventStore es = builder().build();
        final StreamId streamId = new SimpleStreamId("stream-1");
        final CommonEvent ev0 = event("a");
        final CommonEvent ev1 = event("b");
        es.appendToStream(streamId, ev0, ev1);

        // forward
        final StreamEventsSlice forward = es.readEventsForward(streamId, 0, 10);
        assertThat(forward.getEvents()).extracting(CommonEvent::getData)
                .containsExactly(new TestData("a"), new TestData("b"));

        // backward
        final StreamEventsSlice backward = es.readEventsBackward(streamId, 1, 10);
        assertThat(backward.getEvents()).extracting(CommonEvent::getData)
                .containsExactly(new TestData("b"), new TestData("a"));

        // all
        final List<CommonEvent> all = new ArrayList<>();
        es.readAllEventsForward(streamId, 0, 1, slice -> all.addAll(slice.getEvents()));
        assertThat(all).extracting(CommonEvent::getData)
                .containsExactly(new TestData("a"), new TestData("b"));
    }

    @Test
    public void testEncryptMeta() {
        // PREPARE
        final EncryptingEventStore es = builder().encryptMeta(true).build();
        final StreamId streamId = new SimpleStreamId("stream-1");
        final CommonEvent original = new SimpleCommonEvent(new EventId(), DATA_TYPE,
                new TestData("d"), META_TYPE, new TestData("m"), null);

        // TEST
        es.appendToStream(streamId, original);

        // VERIFY meta encrypted at rest
        final CommonEvent stored = delegate.events(streamId).get(0);
        assertThat(stored.getMeta()).isInstanceOf(EncryptedData.class);
        assertThat(stored.getMetaType()).isEqualTo(new TypeName("EscEncryptedData"));

        // VERIFY round-trip
        final CommonEvent read = es.readEvent(streamId, 0);
        assertThat(read.getMeta()).isEqualTo(new TestData("m"));
        assertThat(read.getMetaType()).isEqualTo(META_TYPE);
    }

    @Test
    public void testNullKeyStoresPlaintext() {
        // PREPARE - resolver returns null => no encryption
        final EncryptingEventStore es = builder()
                .keyIdResolver((streamId, tenantId, dataType) -> java.util.Optional.empty()).build();
        final StreamId streamId = new SimpleStreamId("stream-1");
        final CommonEvent original = event("plain");

        // TEST
        es.appendToStream(streamId, original);

        // VERIFY stored in plain text
        final CommonEvent stored = delegate.events(streamId).get(0);
        assertThat(stored.getData()).isInstanceOf(TestData.class);
        assertThat(stored.getDataType()).isEqualTo(DATA_TYPE);

        // VERIFY read returns the original unchanged
        final CommonEvent read = es.readEvent(streamId, 0);
        assertThat(read.getData()).isEqualTo(new TestData("plain"));
    }

    @Test
    public void testIdempotentNoDoubleEncryption() {
        // PREPARE - feed an event whose data is already an EncryptedData
        final EncryptingEventStore es = builder().build();
        final StreamId streamId = new SimpleStreamId("stream-1");
        final EncryptedData already = new TestEncryptedData(KEY_ID, "1", "X", "application/octet-stream", new byte[]{1, 2, 3});
        final CommonEvent event = new SimpleCommonEvent(new EventId(), new TypeName("EscEncryptedData"), already, null);

        // TEST
        es.appendToStream(streamId, event);

        // VERIFY stored unchanged (same wrapper, not re-wrapped)
        final CommonEvent stored = delegate.events(streamId).get(0);
        assertThat(stored.getData()).isSameAs(already);
    }

    @Test
    public void testKeyRotation() throws Exception {
        // PREPARE
        final EncryptingEventStore es = builder().build();
        final StreamId streamId = new SimpleStreamId("stream-1");

        // append with key version 1
        es.appendToStream(streamId, event("v1"));

        // rotate key -> version 2
        service.rotateKey(KEY_ID);
        es.appendToStream(streamId, event("v2"));

        // VERIFY both decrypt using their stamped version
        assertThat(((EncryptedData) delegate.events(streamId).get(0).getData()).getKeyVersion()).isEqualTo("1");
        assertThat(((EncryptedData) delegate.events(streamId).get(1).getData()).getKeyVersion()).isEqualTo("2");
        assertThat(es.readEvent(streamId, 0).getData()).isEqualTo(new TestData("v1"));
        assertThat(es.readEvent(streamId, 1).getData()).isEqualTo(new TestData("v2"));
    }

    @Test
    public void testMissingKeyFailsByDefault() {
        // PREPARE - store encrypted, then read with a service that does not know the key
        final EncryptingEventStore writeEs = builder().build();
        final StreamId streamId = new SimpleStreamId("stream-1");
        writeEs.appendToStream(streamId, event("x"));

        final FakeEncryptedDataService emptyService = new FakeEncryptedDataService();
        final EncryptingEventStore readEs = builder().encryptionService(emptyService).build();

        // TEST + VERIFY
        assertThatThrownBy(() -> readEs.readEvent(streamId, 0))
                .isInstanceOf(EscEncryptionException.class);
    }

    @Test
    public void testMissingKeyReturnsCiphertextWhenConfigured() {
        // PREPARE
        final EncryptingEventStore writeEs = builder().build();
        final StreamId streamId = new SimpleStreamId("stream-1");
        writeEs.appendToStream(streamId, event("x"));

        final FakeEncryptedDataService emptyService = new FakeEncryptedDataService();
        final EncryptingEventStore readEs = builder()
                .encryptionService(emptyService).failOnUndecryptable(false).build();

        // TEST
        final CommonEvent read = readEs.readEvent(streamId, 0);

        // VERIFY ciphertext wrapper returned as-is
        assertThat(read.getData()).isInstanceOf(EncryptedData.class);
    }

    @Test
    public void testPassThrough() {
        // PREPARE
        final EventStore mockDelegate = mock(EventStore.class);
        final StreamId streamId = new SimpleStreamId("stream-1");
        final EncryptingEventStore es = builder().delegate(mockDelegate).build();

        // TEST + VERIFY pass-through methods
        es.createStream(streamId);
        verify(mockDelegate).createStream(streamId);

        es.deleteStream(streamId, true);
        verify(mockDelegate).deleteStream(streamId, true);

        es.deleteStream(streamId, 5, false);
        verify(mockDelegate).deleteStream(streamId, 5, false);

        when(mockDelegate.streamExists(streamId)).thenReturn(true);
        assertThat(es.streamExists(streamId)).isTrue();

        when(mockDelegate.isSupportsCreateStream()).thenReturn(true);
        assertThat(es.isSupportsCreateStream()).isTrue();

        assertThat(es.open()).isSameAs(es);
        verify(mockDelegate).open();

        es.close();
        verify(mockDelegate).close();
    }

    private static CommonEvent event(final String payload) {
        return new SimpleCommonEvent(new EventId(), DATA_TYPE, new TestData(payload), null);
    }

}
