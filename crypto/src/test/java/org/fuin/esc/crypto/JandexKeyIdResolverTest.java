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

import org.fuin.esc.api.*;
import org.fuin.objects4j.crypto.EncryptedData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link JandexKeyIdResolver}. The resolver scans the compiled test classes ({@code target/test-classes}) for
 * implementors of the local {@link TestRequiresEncryptionAtRest} marker.
 */
public class JandexKeyIdResolverTest {

    private static final File TEST_CLASSES = new File("target/test-classes");

    private FakeEncryptedDataService service;

    private JandexKeyIdResolver testee;

    @BeforeEach
    public void setup() {
        service = new FakeEncryptedDataService();
        testee = new JandexKeyIdResolver(TestRequiresEncryptionAtRest.class.getName(), service, TEST_CLASSES);
    }

    @Test
    public void testEncryptedTypeWithExistingKey() throws Exception {
        // PREPARE - the per-stream key already exists
        final StreamId streamId = new SimpleStreamId("Vendor-1");
        service.createKey(streamId.asString());

        // TEST + VERIFY - the event type requires encryption, so the stream's keyId is returned
        assertThat(testee.getKeyId(streamId, null, new TypeName(EncryptedTestEvent.TYPE)))
                .contains(streamId.asString());
    }

    @Test
    public void testEncryptedTypeMissingKeyFails() {
        // PREPARE - no key created for the stream
        final StreamId streamId = new SimpleStreamId("Vendor-2");

        // TEST + VERIFY - encryption is required but the key is missing -> fail (and do not create it)
        assertThatThrownBy(() -> testee.getKeyId(streamId, null, new TypeName(EncryptedTestEvent.TYPE)))
                .isInstanceOf(EscEncryptionException.class);
        assertThat(service.keyExists(streamId.asString())).isFalse();
    }

    @Test
    public void testTenantIncludedInKey() throws Exception {
        // PREPARE - the key is "<tenant>/<stream>" when the event belongs to a tenant
        final StreamId streamId = new SimpleStreamId("Vendor-1");
        final TenantId tenantId = new SimpleTenantId("acme");
        final String expectedKeyId = tenantId.asString() + "/" + streamId.asString();
        service.createKey(expectedKeyId);

        // TEST + VERIFY - the tenant is part of the derived keyId
        assertThat(testee.getKeyId(streamId, tenantId, new TypeName(EncryptedTestEvent.TYPE)))
                .contains(expectedKeyId);
    }

    @Test
    public void testPlainTypeReturnsEmpty() {
        // A type that does NOT implement the marker is stored in plain text (no key, no service interaction)
        assertThat(testee.getKeyId(new SimpleStreamId("Vendor-3"), null, new TypeName(PlainTestEvent.TYPE)))
                .isEmpty();
    }

    @Test
    public void testUnknownTypeReturnsEmpty() {
        // A type that was not scanned at all is treated as "does not require encryption"
        assertThat(testee.getKeyId(new SimpleStreamId("Vendor-4"), null, new TypeName("CompletelyUnknownEvent")))
                .isEmpty();
    }

    @Test
    public void testEndToEndSelectiveEncryption() throws Exception {
        // PREPARE
        final TestRegistry registry = new TestRegistry();
        final TestEventStore delegate = new TestEventStore();
        final EncryptingEventStore es = new EncryptingEventStore.Builder()
                .delegate(delegate)
                .serRegistry(registry)
                .desRegistry(registry)
                .encryptionService(service)
                .keyIdResolver(testee)
                .encryptedDataFactory(new TestEncryptedDataFactory())
                .build();

        final StreamId encStream = new SimpleStreamId("Vendor-1");
        service.createKey(encStream.asString());
        final StreamId plainStream = new SimpleStreamId("Vendor-9");

        // TEST - append an event whose type requires encryption and one that does not
        es.appendToStream(encStream,
                new SimpleCommonEvent(new EventId(), new TypeName(EncryptedTestEvent.TYPE), new TestData("secret"), null));
        es.appendToStream(plainStream,
                new SimpleCommonEvent(new EventId(), new TypeName(PlainTestEvent.TYPE), new TestData("public"), null));

        // VERIFY at rest: only the flagged type is encrypted
        assertThat(delegate.events(encStream).get(0).getData()).isInstanceOf(EncryptedData.class);
        assertThat(delegate.events(plainStream).get(0).getData()).isInstanceOf(TestData.class);

        // VERIFY both read back to the original
        assertThat(es.readEvent(encStream, 0).getData()).isEqualTo(new TestData("secret"));
        assertThat(es.readEvent(plainStream, 0).getData()).isEqualTo(new TestData("public"));
    }

}
