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
import org.fuin.esc.api.EscConnectionException;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.TypeName;
import org.fuin.objects4j.crypto.DecryptionFailedException;
import org.fuin.objects4j.crypto.EncryptedData;
import org.fuin.objects4j.crypto.EncryptedDataService;
import org.fuin.objects4j.crypto.EncryptionKeyIdUnknownException;
import org.fuin.objects4j.crypto.EncryptionKeyVersionUnknownException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers what happens to the {@link EncryptingEventStore} when the external key service is down rather than
 * merely unable to answer for a specific key: the failure must be typed as transient, must not be confused
 * with a permanently undecryptable event, and must not block the calling thread forever.
 */
public class EncryptingEventStoreKeyServiceOutageTest {

    private static final TypeName DATA_TYPE = new TypeName("MyEvent");

    private static final String KEY_ID = "key-1";

    private static final StreamId STREAM = new SimpleStreamId("stream-1");

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

    private EncryptingEventStore.Builder builder(final EncryptedDataService encryptionService) {
        return new EncryptingEventStore.Builder()
                .delegate(delegate)
                .serRegistry(registry)
                .desRegistry(registry)
                .encryptionService(encryptionService)
                .keyIdResolver(new FixedKeyIdResolver(KEY_ID))
                .encryptedDataFactory(new TestEncryptedDataFactory());
    }

    private static CommonEvent event() {
        return new SimpleCommonEvent(new EventId(), DATA_TYPE, new TestData("secret-payload"), null);
    }

    @Test
    public void testAppendReportsAnUnreachableKeyServiceAsTransient() {

        // PREPARE
        final UnreachableKeyService broken = new UnreachableKeyService(service);
        final EncryptingEventStore es = builder(broken).build();
        broken.breakIt();

        // TEST & VERIFY: one instanceof classifies it, like every other "not reachable" failure
        assertThatThrownBy(() -> es.appendToStream(STREAM, event()))
                .isInstanceOf(EscEncryptionConnectionException.class)
                .isInstanceOf(EscConnectionException.class)
                .hasRootCauseInstanceOf(ConnectException.class);

        // VERIFY: nothing reached the underlying store
        assertThat(delegate.events(STREAM)).isEmpty();
    }

    @Test
    public void testReadReportsAnUnreachableKeyServiceAsTransient() {

        // PREPARE: the event was written while the key service was up
        builder(service).build().appendToStream(STREAM, event());
        final UnreachableKeyService broken = new UnreachableKeyService(service);
        final EncryptingEventStore es = builder(broken).build();

        // TEST & VERIFY
        broken.breakIt();
        assertThatThrownBy(() -> es.readEvent(STREAM, 0))
                .isInstanceOf(EscEncryptionConnectionException.class);
    }

    @Test
    public void testAnOutageIsNeverPassedOffAsAnUndecryptableEvent() {

        // PREPARE: failOnUndecryptable(false) returns the ciphertext wrapper for an event whose key is gone
        builder(service).build().appendToStream(STREAM, event());
        final UnreachableKeyService broken = new UnreachableKeyService(service);
        final EncryptingEventStore es = builder(broken).failOnUndecryptable(false).build();

        // TEST: the vault goes down
        broken.breakIt();

        // VERIFY: "the vault did not answer" says nothing about the event, so handing the caller the
        // wrapper would let it take the ciphertext for data - the failure propagates instead
        assertThatThrownBy(() -> es.readEvent(STREAM, 0))
                .isInstanceOf(EscEncryptionConnectionException.class);
    }

    @Test
    public void testAnUndecryptableEventStillHonoursTheFlag() {

        // PREPARE: a definite answer from a reachable key service is unchanged by all of the above
        builder(service).build().appendToStream(STREAM, event());
        final EncryptingEventStore es = builder(new UnknownVersionKeyService(service))
                .failOnUndecryptable(false).build();

        // TEST
        final CommonEvent read = es.readEvent(STREAM, 0);

        // VERIFY: the wrapper is still in place, no exception
        assertThat(read.getData()).isInstanceOf(EncryptedData.class);
    }

    @Test
    public void testAHangingKeyServiceDoesNotBlockTheCallerForever() throws Exception {

        // PREPARE: a key service that never answers - the vault accepted the connection and went silent
        final CountDownLatch release = new CountDownLatch(1);
        final EncryptingEventStore es = builder(new HangingKeyService(service, release))
                .keyServiceTimeout(Duration.ofMillis(200)).build();

        try {
            // TEST & VERIFY: the appending thread stops waiting instead of hanging
            final long start = System.currentTimeMillis();
            assertThatThrownBy(() -> es.appendToStream(STREAM, event()))
                    .isInstanceOf(EscEncryptionConnectionException.class)
                    .hasMessageContaining("200");
            assertThat(System.currentTimeMillis() - start).isLessThan(5_000);
        } finally {
            release.countDown();
            es.close();
        }
    }

    @Test
    public void testWithoutATimeoutTheStoreWaitsAsLongAsTheClientDoes() throws Exception {

        // PREPARE: no timeout configured (the default) - behaviour is unchanged
        final CountDownLatch release = new CountDownLatch(1);
        final EncryptingEventStore es = builder(new HangingKeyService(service, release)).build();
        final AtomicBoolean returned = new AtomicBoolean();
        final Thread caller = new Thread(() -> {
            es.appendToStream(STREAM, event());
            returned.set(true);
        });

        // TEST
        caller.start();
        Thread.sleep(300);

        // VERIFY: still waiting, then completes once the key service answers
        assertThat(returned).isFalse();
        release.countDown();
        caller.join(5_000);
        assertThat(returned).isTrue();
    }

    @Test
    public void testABusinessAnswerIsNotClassifiedAsTransient() {

        // PREPARE: an unknown key is a definite answer and must never be retried
        final EncryptingEventStore es = builder(service).keyIdResolver(new FixedKeyIdResolver("no-such-key"))
                .build();

        // TEST & VERIFY
        assertThatThrownBy(() -> es.appendToStream(STREAM, event()))
                .isInstanceOf(EscEncryptionException.class)
                .isNotInstanceOf(EscConnectionException.class);
    }

    /**
     * Key service whose calls fail the way an unreachable vault does.
     */
    private static final class UnreachableKeyService extends DelegatingKeyService {

        private final AtomicBoolean broken = new AtomicBoolean();

        private UnreachableKeyService(final EncryptedDataService delegate) {
            super(delegate);
        }

        void breakIt() {
            broken.set(true);
        }

        @Override
        public EncryptedData encrypt(final String keyId, final String dataType, final String contentType,
                                     final byte[] data) throws EncryptionKeyIdUnknownException {
            if (broken.get()) {
                throw new UncheckedIOException(new ConnectException("Connection refused"));
            }
            return super.encrypt(keyId, dataType, contentType, data);
        }

        @Override
        public byte[] decrypt(final EncryptedData encryptedData) throws EncryptionKeyIdUnknownException,
                EncryptionKeyVersionUnknownException, DecryptionFailedException {
            if (broken.get()) {
                throw new UncheckedIOException(new ConnectException("Connection refused"));
            }
            return super.decrypt(encryptedData);
        }

    }

    /**
     * Key service that answers "I do not know this key version" - a definite answer, not an outage.
     */
    private static final class UnknownVersionKeyService extends DelegatingKeyService {

        private UnknownVersionKeyService(final EncryptedDataService delegate) {
            super(delegate);
        }

        @Override
        public byte[] decrypt(final EncryptedData encryptedData) throws EncryptionKeyVersionUnknownException {
            throw new EncryptionKeyVersionUnknownException("42");
        }

    }

    /**
     * Key service that accepts the call and never answers.
     */
    private static final class HangingKeyService extends DelegatingKeyService {

        private final CountDownLatch release;

        private HangingKeyService(final EncryptedDataService delegate, final CountDownLatch release) {
            super(delegate);
            this.release = release;
        }

        @Override
        public EncryptedData encrypt(final String keyId, final String dataType, final String contentType,
                                     final byte[] data) throws EncryptionKeyIdUnknownException {
            try {
                release.await(30, TimeUnit.SECONDS);
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(ex);
            }
            return super.encrypt(keyId, dataType, contentType, data);
        }

    }

    /**
     * Base for the key services above that forwards everything it does not override.
     */
    private abstract static class DelegatingKeyService implements EncryptedDataService {

        private final EncryptedDataService delegate;

        DelegatingKeyService(final EncryptedDataService delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean keyExists(final String keyId) {
            return delegate.keyExists(keyId);
        }

        @Override
        public void createKey(final String keyId) {
            throw new UnsupportedOperationException("Not used by these tests");
        }

        @Override
        public String rotateKey(final String keyId) throws EncryptionKeyIdUnknownException {
            return delegate.rotateKey(keyId);
        }

        @Override
        public String getKeyVersion(final String keyId) throws EncryptionKeyIdUnknownException {
            return delegate.getKeyVersion(keyId);
        }

        @Override
        public EncryptedData encrypt(final String keyId, final String dataType, final String contentType,
                                     final byte[] data) throws EncryptionKeyIdUnknownException {
            return delegate.encrypt(keyId, dataType, contentType, data);
        }

        @Override
        public byte[] decrypt(final EncryptedData encryptedData) throws EncryptionKeyIdUnknownException,
                EncryptionKeyVersionUnknownException, DecryptionFailedException {
            return delegate.decrypt(encryptedData);
        }

    }

}
