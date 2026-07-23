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
import org.fuin.esc.spi.AbstractReadableEventStore;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.esc.spi.SerializedData;
import org.fuin.objects4j.common.ThreadSafe;
import org.fuin.objects4j.crypto.*;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Event store that transparently encrypts the event data before it is passed to an underlying {@link EventStore}
 * and decrypts it again when events are read. The concrete event store implementations (memory, JPA, gRPC) stay
 * completely unaware of encryption.
 * <p>
 * On append the event data is serialized, encrypted using the {@link EncryptedDataService} and wrapped into
 * an {@link EncryptedData} representation that the underlying store persists like any other event. On read
 * the wrapper is detected, decrypted and deserialized back into the original event.
 * <p>
 * By default, only the event data is encrypted while the metadata stays in plain text so it remains usable
 * for routing and projections. Set {@link Builder#encryptMeta(boolean)} to also encrypt the metadata.
 * <p>
 * <b>A key service outage is transient and never silently degrades.</b> Failures of the external key service
 * are reported as {@link EscEncryptionConnectionException}, which extends
 * {@link org.fuin.esc.api.EscConnectionException}, so an application can apply the same retry / circuit
 * breaker / fallback policy it applies to the store itself. This is deliberately kept apart from
 * {@link Builder#failOnUndecryptable(boolean)}: that flag decides what happens to an event that is
 * <em>permanently</em> undecryptable (its key or key version is gone), and it must not turn "the vault did
 * not answer" into "here is your ciphertext" - a caller would take the wrapper for data. A transient failure
 * therefore always propagates, whatever the flag says.
 * <p>
 * Set {@link Builder#keyServiceTimeout(java.time.Duration)} to also bound how long a single key service call
 * may block the calling thread. The primary bound should be the key service client's own connect/read
 * timeout; this is the backstop for a client that has none.
 */
@ThreadSafe
public final class EncryptingEventStore extends AbstractReadableEventStore implements EventStore {

    private final EventStore delegate;

    private final SerializerRegistry serRegistry;

    private final DeserializerRegistry desRegistry;

    private final EncryptedDataService encryptionService;

    private final KeyIdResolver keyIdResolver;

    private final EncryptedDataFactory encryptedDataFactory;

    private final SerializedDataType encryptedDataType;

    private final TypeName encryptedTypeName;

    private final boolean encryptMeta;

    private final boolean failOnUndecryptable;

    @Nullable
    private final Duration keyServiceTimeout;

    @Nullable
    private final ExecutorService keyServiceExecutor;

    private final boolean ownsKeyServiceExecutor;

    private EncryptingEventStore(final Builder builder) {
        super();
        this.delegate = Objects.requireNonNull(builder.delegate, "delegate");
        this.serRegistry = Objects.requireNonNull(builder.serRegistry, "serRegistry");
        this.desRegistry = Objects.requireNonNull(builder.desRegistry, "desRegistry");
        this.encryptionService = Objects.requireNonNull(builder.encryptionService, "encryptionService");
        this.keyIdResolver = Objects.requireNonNull(builder.keyIdResolver, "keyIdResolver");
        this.encryptedDataFactory = Objects.requireNonNull(builder.encryptedDataFactory, "encryptedDataFactory");
        this.encryptedDataType = encryptedDataFactory.getType();
        this.encryptedTypeName = new TypeName(encryptedDataType.asBaseType());
        this.encryptMeta = builder.encryptMeta;
        this.failOnUndecryptable = builder.failOnUndecryptable;
        this.keyServiceTimeout = builder.keyServiceTimeout;
        if (builder.keyServiceTimeout == null) {
            this.keyServiceExecutor = null;
            this.ownsKeyServiceExecutor = false;
        } else if (builder.keyServiceExecutor == null) {
            this.keyServiceExecutor = Executors.newCachedThreadPool(runnable -> {
                final Thread thread = new Thread(runnable, "esc-crypto-key-service");
                // Daemon: a worker still stuck in an unreachable vault must not keep the JVM alive.
                thread.setDaemon(true);
                return thread;
            });
            this.ownsKeyServiceExecutor = true;
        } else {
            this.keyServiceExecutor = builder.keyServiceExecutor;
            this.ownsKeyServiceExecutor = false;
        }
    }

    // ----- EventStoreBasics -----

    @Override
    public EncryptingEventStore open() {
        delegate.open();
        return this;
    }

    @Override
    public void close() {
        try {
            delegate.close();
        } finally {
            if (ownsKeyServiceExecutor && keyServiceExecutor != null) {
                keyServiceExecutor.shutdownNow();
            }
        }
    }

    @Override
    public EventStoreCapabilities capabilities() {
        // Transparent decorator: capabilities are those of the wrapped store.
        return delegate.capabilities();
    }

    // ----- WritableEventStore -----

    @Override
    public boolean isSupportsCreateStream() {
        return delegate.isSupportsCreateStream();
    }

    @Override
    public void createStream(final StreamId streamId) {
        delegate.createStream(streamId);
    }

    @Override
    public long appendToStream(final StreamId streamId, final long expectedVersion, final CommonEvent... events) {
        return delegate.appendToStream(streamId, expectedVersion, encrypt(streamId, Arrays.asList(events)));
    }

    @Override
    public long appendToStream(final StreamId streamId, final CommonEvent... events) {
        return delegate.appendToStream(streamId, encrypt(streamId, Arrays.asList(events)));
    }

    @Override
    public long appendToStream(final StreamId streamId, final long expectedVersion, final List<CommonEvent> events) {
        return delegate.appendToStream(streamId, expectedVersion, encrypt(streamId, events));
    }

    @Override
    public long appendToStream(final StreamId streamId, final List<CommonEvent> events) {
        return delegate.appendToStream(streamId, encrypt(streamId, events));
    }

    @Override
    public void deleteStream(final StreamId streamId, final long expectedVersion, final boolean hardDelete) {
        delegate.deleteStream(streamId, expectedVersion, hardDelete);
    }

    @Override
    public void deleteStream(final StreamId streamId, final boolean hardDelete) {
        delegate.deleteStream(streamId, hardDelete);
    }

    // ----- ReadableEventStore -----

    @Override
    public StreamEventsSlice readEventsForward(final StreamId streamId, final long start, final int count) {
        return decrypt(delegate.readEventsForward(streamId, start, count));
    }

    @Override
    public StreamEventsSlice readEventsBackward(final StreamId streamId, final long start, final int count) {
        return decrypt(delegate.readEventsBackward(streamId, start, count));
    }

    @Override
    public CommonEvent readEvent(final StreamId streamId, final long eventNumber) {
        return decrypt(delegate.readEvent(streamId, eventNumber));
    }

    @Override
    public boolean streamExists(final StreamId streamId) {
        return delegate.streamExists(streamId);
    }

    @Override
    public StreamState streamState(final StreamId streamId) {
        return delegate.streamState(streamId);
    }

    // ----- Encryption -----

    private List<CommonEvent> encrypt(final StreamId streamId, final List<CommonEvent> events) {
        final List<CommonEvent> result = new ArrayList<>(events.size());
        for (final CommonEvent event : events) {
            result.add(encrypt(streamId, event));
        }
        return result;
    }

    private CommonEvent encrypt(final StreamId streamId, final CommonEvent event) {

        // Do not encrypt twice (idempotency / re-feeding already encrypted events)
        if (event.getData() instanceof EncryptedData) {
            return event;
        }

        // A custom resolver may have to ask an external service which key applies, so it gets the same
        // classification as the key service calls themselves.
        final Optional<String> keyId;
        try {
            keyId = keyIdResolver.getKeyId(streamId, event.getTenantId(), event.getDataType());
        } catch (final RuntimeException ex) {
            throw KeyServiceCalls.mapIfTransient(ex, "getKeyId");
        }
        if (keyId.isEmpty()) {
            // Selective encryption: store this event in plain text
            return event;
        }

        try {
            final EncryptedData data = encrypt(keyId.get(), event.getDataType().asBaseType(), event.getData());

            final TypeName metaType;
            final Object meta;
            if (encryptMeta && event.getMeta() != null) {
                final String metaTypeName = Objects.requireNonNull(event.getMetaType(), "metaType").asBaseType();
                meta = encrypt(keyId.get(), metaTypeName, event.getMeta());
                metaType = encryptedTypeName;
            } else {
                metaType = event.getMetaType();
                meta = event.getMeta();
            }

            return new SimpleCommonEvent(event.getId(), encryptedTypeName, data, metaType, meta, event.getTenantId());
        } catch (final EncryptionKeyIdUnknownException ex) {
            throw new EscEncryptionException("Failed to encrypt event " + event.getId(), ex);
        }
    }

    private EncryptedData encrypt(final String keyId, final String dataType, final Object obj)
            throws EncryptionKeyIdUnknownException {
        final SerializedData serialized = Objects.requireNonNull(
                EscSpiUtils.serialize(serRegistry, new SerializedDataType(dataType), obj), "serialized");
        final EncryptedData encrypted;
        try {
            encrypted = KeyServiceCalls.execute(
                    () -> encryptionService.encrypt(keyId, dataType, serialized.getMimeType().toString(),
                            serialized.getRaw()),
                    "encrypt", keyServiceTimeout, keyServiceExecutor);
        } catch (final EncryptionKeyVersionUnknownException | DecryptionFailedException ex) {
            // encrypt(..) does not declare these - a service that answers with one of them is broken.
            throw new EscEncryptionException("Unexpected failure encrypting data of type " + dataType, ex);
        }
        return encryptedDataFactory.create(encrypted);
    }

    private StreamEventsSlice decrypt(final StreamEventsSlice slice) {
        final List<CommonEvent> events = new ArrayList<>(slice.getEvents().size());
        for (final CommonEvent event : slice.getEvents()) {
            events.add(decrypt(event));
        }
        return new StreamEventsSlice(slice.getFromEventNumber(), events, slice.getNextEventNumber(), slice.isEndOfStream());
    }

    private CommonEvent decrypt(final CommonEvent event) {

        final Object rawData = event.getData();
        final Object rawMeta = event.getMeta();
        if (!(rawData instanceof EncryptedData) && !(rawMeta instanceof EncryptedData)) {
            return event;
        }

        try {
            final Object data;
            final TypeName dataType;
            if (rawData instanceof EncryptedData encrypted) {
                data = decrypt(encrypted);
                dataType = new TypeName(encrypted.getDataType());
            } else {
                data = rawData;
                dataType = event.getDataType();
            }

            final Object meta;
            final TypeName metaType;
            if (rawMeta instanceof EncryptedData encrypted) {
                meta = decrypt(encrypted);
                metaType = new TypeName(encrypted.getDataType());
            } else {
                meta = rawMeta;
                metaType = event.getMetaType();
            }

            return new SimpleCommonEvent(event.getId(), dataType, data, metaType, meta, event.getTenantId());
        } catch (final EncryptionKeyIdUnknownException | EncryptionKeyVersionUnknownException
                       | DecryptionFailedException ex) {
            // Only these three: they are definite answers from the key service, so the event is
            // permanently undecryptable and returning the wrapper is a deliberate choice. An
            // EscEncryptionConnectionException must NOT be handled here - the vault not answering says
            // nothing about the event, and passing the ciphertext wrapper off as the payload would let a
            // caller take it for data. It is unchecked and therefore propagates past this block.
            if (failOnUndecryptable) {
                throw new EscEncryptionException("Failed to decrypt event " + event.getId(), ex);
            }
            // Return the event with the encrypted wrapper still in place
            return event;
        }
    }

    private Object decrypt(final EncryptedData encrypted)
            throws EncryptionKeyIdUnknownException, EncryptionKeyVersionUnknownException, DecryptionFailedException {
        final byte[] clear = KeyServiceCalls.execute(() -> encryptionService.decrypt(encrypted), "decrypt",
                keyServiceTimeout, keyServiceExecutor);
        final EnhancedMimeType mimeType = Objects.requireNonNull(
                EnhancedMimeType.create(encrypted.getContentType()), "mimeType");
        final SerializedData serialized = new SerializedData(
                new SerializedDataType(encrypted.getDataType()), mimeType, clear);
        return EscSpiUtils.deserialize(desRegistry, serialized);
    }

    /**
     * Builds instances of {@link EncryptingEventStore}.
     */
    public static final class Builder {

        @Nullable
        private EventStore delegate;

        @Nullable
        private SerializerRegistry serRegistry;

        @Nullable
        private DeserializerRegistry desRegistry;

        @Nullable
        private ConverterRegistry converters;

        @Nullable
        private EncryptedDataService encryptionService;

        @Nullable
        private KeyIdResolver keyIdResolver;

        @Nullable
        private EncryptedDataFactory encryptedDataFactory;

        private boolean encryptMeta;

        private boolean failOnUndecryptable = true;

        @Nullable
        private Duration keyServiceTimeout;

        @Nullable
        private ExecutorService keyServiceExecutor;

        /**
         * Sets the event store to decorate.
         *
         * @param delegate Underlying event store.
         * @return This builder.
         */
        public Builder delegate(final EventStore delegate) {
            this.delegate = delegate;
            return this;
        }

        /**
         * Sets the registry used to serialize the event data before encryption.
         *
         * @param serRegistry Serializer registry.
         * @return This builder.
         */
        public Builder serRegistry(final SerializerRegistry serRegistry) {
            this.serRegistry = serRegistry;
            return this;
        }

        /**
         * Sets the registry used to deserialize the event data after decryption.
         *
         * @param desRegistry Deserializer registry.
         * @return This builder.
         */
        public Builder desRegistry(final DeserializerRegistry desRegistry) {
            this.desRegistry = desRegistry;
            return this;
        }

        /**
         * Sets the version up-caster registry. When set, events read (and decrypted) through this store are
         * up-cast from their stored version to the latest in-memory representation (the deserializer registry is
         * wrapped in an {@link UpcastingDeserializerRegistry}); {@literal null} or an empty registry leaves reads
         * unchanged.
         *
         * @param converters Registry of version up-casters applied after deserialization.
         * @return This builder.
         */
        public Builder converters(@Nullable final ConverterRegistry converters) {
            this.converters = converters;
            return this;
        }

        /**
         * Sets the service that performs the actual encryption and decryption.
         *
         * @param encryptionService Encryption service.
         * @return This builder.
         */
        public Builder encryptionService(final EncryptedDataService encryptionService) {
            this.encryptionService = encryptionService;
            return this;
        }

        /**
         * Sets the strategy that selects the key identifier for an event.
         *
         * @param keyIdResolver Key identifier resolver.
         * @return This builder.
         */
        public Builder keyIdResolver(final KeyIdResolver keyIdResolver) {
            this.keyIdResolver = keyIdResolver;
            return this;
        }

        /**
         * Sets the factory that creates the serializable encrypted data representation.
         *
         * @param encryptedDataFactory Encrypted data factory.
         * @return This builder.
         */
        public Builder encryptedDataFactory(final EncryptedDataFactory encryptedDataFactory) {
            this.encryptedDataFactory = encryptedDataFactory;
            return this;
        }

        /**
         * Determines if the event metadata should be encrypted as well. Defaults to <code>false</code> (metadata stays in plain text).
         *
         * @param encryptMeta TRUE to also encrypt the metadata.
         * @return This builder.
         */
        public Builder encryptMeta(final boolean encryptMeta) {
            this.encryptMeta = encryptMeta;
            return this;
        }

        /**
         * Determines if reading an event that cannot be decrypted fails with an exception (default) or returns the event with the
         * encrypted wrapper still in place.
         *
         * @param failOnUndecryptable TRUE to fail (default), FALSE to return the ciphertext wrapper.
         * @return This builder.
         */
        public Builder failOnUndecryptable(final boolean failOnUndecryptable) {
            this.failOnUndecryptable = failOnUndecryptable;
            return this;
        }

        /**
         * Bounds how long a single key service call may block the calling thread. Without it an append or a
         * read waits as long as the key service client does, which is forever if that client has no
         * connect/read timeout of its own - and that client timeout remains the better place to set the
         * bound, because it can actually abort the request. This one only stops the appending or reading
         * thread from waiting: the call itself runs on an executor and keeps running after the timeout, so
         * do not set it so low that healthy calls are abandoned. Defaults to no timeout.
         * <p>
         * The store creates and owns a daemon thread pool for this and shuts it down in {@link #close()};
         * use {@link #keyServiceTimeout(Duration, ExecutorService)} to supply your own.
         *
         * @param keyServiceTimeout Maximum time for a single key service call, or {@literal null} for none.
         * @return This builder.
         */
        public Builder keyServiceTimeout(@Nullable final Duration keyServiceTimeout) {
            if (keyServiceTimeout != null && (keyServiceTimeout.isNegative() || keyServiceTimeout.isZero())) {
                throw new IllegalArgumentException(
                        "keyServiceTimeout must be positive, but was: " + keyServiceTimeout);
            }
            this.keyServiceTimeout = keyServiceTimeout;
            return this;
        }

        /**
         * Bounds a key service call like {@link #keyServiceTimeout(Duration)}, but runs the calls on an
         * executor supplied and owned by the caller - {@link #close()} does not shut it down.
         *
         * @param keyServiceTimeout  Maximum time for a single key service call.
         * @param keyServiceExecutor Executor used to run the key service calls.
         * @return This builder.
         */
        public Builder keyServiceTimeout(final Duration keyServiceTimeout,
                                         final ExecutorService keyServiceExecutor) {
            keyServiceTimeout(keyServiceTimeout);
            this.keyServiceExecutor = Objects.requireNonNull(keyServiceExecutor, "keyServiceExecutor");
            return this;
        }

        /**
         * Builds a new instance.
         *
         * @return New encrypting event store.
         */
        public EncryptingEventStore build() {
            if (converters != null && desRegistry != null) {
                desRegistry = new UpcastingDeserializerRegistry(desRegistry, converters);
            }
            return new EncryptingEventStore(this);
        }

    }

}
