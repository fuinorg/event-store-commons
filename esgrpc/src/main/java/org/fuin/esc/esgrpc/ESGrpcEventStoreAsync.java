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
package org.fuin.esc.esgrpc;

import io.grpc.StatusRuntimeException;
import io.kurrent.dbclient.*;
import org.fuin.esc.api.*;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.StreamState;
import org.fuin.esc.api.Subscription;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.ThreadSafe;
import org.fuin.utils4j.TestOmitted;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static org.fuin.esc.api.ExpectedVersion.ANY;

/**
 * Asynchronous implementation that connects to the event store using the gRPC API. In contrast to
 * {@link ESGrpcEventStore} this class does not block waiting for the underlying client futures, but
 * chains on them directly. It also supports volatile subscriptions
 * ({@link SubscribableEventStoreAsync}). All the event/exception conversion is shared with the
 * synchronous store via {@link ESGrpcEventStoreSupport}.
 */
@ThreadSafe
@TestOmitted("Tested in the 'test' project")
public final class ESGrpcEventStoreAsync implements IESGrpcEventStoreAsync {

    private final KurrentDBClient es;

    private final ESGrpcEventStoreSupport support;

    /**
     * Private constructor with all data used by the builder.
     *
     * @param es                Connection that is maintained outside. Opening/Closing is up to the caller!
     * @param serRegistry       Registry used to locate serializers.
     * @param desRegistry       Registry used to locate deserializers.
     * @param baseTypeFactory   Factory used to create basic types.
     * @param targetContentType Target content type (Allows only 'application/xml'
     *                          or 'application/json' with 'utf-8' encoding).
     * @param tenantContext     Provides the current tenant.
     */
    private ESGrpcEventStoreAsync(final KurrentDBClient es,
                                  final SerializerRegistry serRegistry,
                                  final DeserializerRegistry desRegistry,
                                  final IBaseTypeFactory baseTypeFactory,
                                  final EnhancedMimeType targetContentType,
                                  final TenantContext tenantContext) {
        Contract.requireArgNotNull("es", es);
        this.es = es;
        this.support = new ESGrpcEventStoreSupport(serRegistry, desRegistry, baseTypeFactory,
                targetContentType, tenantContext);
    }

    private void ensureOpen() {
        if (es.isShutdown()) {
            throw new IllegalStateException("The event store has already been closed");
        }
    }

    @Override
    public CompletableFuture<Void> open() {
        // Nothing to do - We assume that the connection is already
        // fully initialized when passed in to constructor
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void close() {
        // Do nothing - Connection is handled outside
    }

    @Override
    public EventStoreCapabilities capabilities() {
        return ESGrpcCapabilities.INSTANCE;
    }

    @Override
    public boolean isSupportsCreateStream() {
        return false;
    }

    @Override
    public CompletableFuture<Void> createStream(final StreamId streamId) throws StreamAlreadyExistsException {
        // Nothing to do as the operation is not supported
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Long> appendToStream(final StreamId streamId, final CommonEvent... events) {
        return appendToStream(streamId, ANY.getNo(), Objects.requireNonNull(EscSpiUtils.asList(events)));
    }

    @Override
    public CompletableFuture<Long> appendToStream(final StreamId streamId, final long expectedVersion,
                                                  final CommonEvent... events) {
        return appendToStream(streamId, expectedVersion, Objects.requireNonNull(EscSpiUtils.asList(events)));
    }

    @Override
    public CompletableFuture<Long> appendToStream(final StreamId streamId, final List<CommonEvent> events) {
        return appendToStream(streamId, ANY.getNo(), events);
    }

    @Override
    public CompletableFuture<Long> appendToStream(final StreamId streamId, final long expectedVersion,
                                                  final List<CommonEvent> commonEvents) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("expectedVersion", expectedVersion, ANY.getNo());
        Contract.requireArgNotNull("commonEvents", commonEvents);
        ESGrpcEventStoreSupport.ensureStreamNoProjection(streamId);
        ensureOpen();

        final TenantStreamId sid = support.sid(streamId);
        final Iterator<EventData> eventDataIt = support.asEventData(commonEvents).iterator();
        return es.appendToStream(sid.asString(),
                        AppendToStreamOptions.get().streamState(ESGrpcEventStoreSupport.version2State(expectedVersion)),
                        eventDataIt)
                .thenApply(result -> result.getNextExpectedRevision().toRawLong())
                .exceptionallyCompose(ex ->
                        CompletableFuture.failedFuture(ESGrpcEventStoreSupport.mapException(unwrap(ex), sid, expectedVersion)));
    }

    @Override
    public CompletableFuture<Void> deleteStream(final StreamId streamId, final long expectedVersion,
                                                final boolean hardDelete) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("expectedVersion", expectedVersion, ANY.getNo());
        ESGrpcEventStoreSupport.ensureStreamNoProjection(streamId);
        ensureOpen();

        final TenantStreamId sid = support.sid(streamId);
        final DeleteStreamOptions options = DeleteStreamOptions.get()
                .streamState(ESGrpcEventStoreSupport.version2State(expectedVersion));
        final CompletableFuture<DeleteResult> future = hardDelete
                ? es.tombstoneStream(sid.asString(), options)
                : es.deleteStream(sid.asString(), options);
        return future.<Void>thenApply(result -> null)
                .exceptionallyCompose(ex ->
                        CompletableFuture.failedFuture(ESGrpcEventStoreSupport.mapException(unwrap(ex), sid, expectedVersion)));
    }

    @Override
    public CompletableFuture<Void> deleteStream(final StreamId streamId, final boolean hardDelete) {
        return deleteStream(streamId, ANY.getNo(), hardDelete);
    }

    @Override
    public CompletableFuture<StreamEventsSlice> readEventsForward(final StreamId streamId, final long start,
                                                                  final int count) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("start", start, 0);
        Contract.requireArgMin("count", count, 1);
        ensureOpen();

        final TenantStreamId sid = support.sid(streamId);
        final ReadStreamOptions options = ReadStreamOptions.get().forwards().fromRevision(start).maxCount(count)
                .resolveLinkTos();
        return es.readStream(sid.asString(), options)
                .thenApply(readResult -> {
                    final List<CommonEvent> events = support.asCommonEvents(readResult.getEvents());
                    final boolean endOfStream = count > events.size();
                    return new StreamEventsSlice(start, events, start + events.size(), endOfStream);
                })
                .exceptionallyCompose(ex ->
                        CompletableFuture.failedFuture(ESGrpcEventStoreSupport.mapException(unwrap(ex), sid, ANY.getNo())));
    }

    @Override
    public CompletableFuture<StreamEventsSlice> readEventsBackward(final StreamId streamId, final long start,
                                                                   final int count) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("start", start, 0);
        Contract.requireArgMin("count", count, 1);
        ensureOpen();

        final TenantStreamId sid = support.sid(streamId);
        final ReadStreamOptions options = ReadStreamOptions.get().backwards().fromRevision(start).maxCount(count)
                .resolveLinkTos();
        return es.readStream(sid.asString(), options)
                .thenApply(slice -> {
                    final List<CommonEvent> events = support.asCommonEvents(slice.getEvents());
                    long nextEventNumber = start - events.size();
                    final boolean endOfStream = (start - count < 0);
                    if (endOfStream) {
                        nextEventNumber = 0;
                    }
                    return new StreamEventsSlice(start, events, nextEventNumber, endOfStream);
                })
                .exceptionallyCompose(ex ->
                        CompletableFuture.failedFuture(ESGrpcEventStoreSupport.mapException(unwrap(ex), sid, ANY.getNo())));
    }

    @Override
    public CompletableFuture<CommonEvent> readEvent(final StreamId streamId, final long eventNumber) {
        return readEventsForward(streamId, eventNumber, 1).thenApply(slice -> {
            if (slice.getEvents().isEmpty()) {
                throw new CompletionException(new EventNotFoundException(streamId, eventNumber));
            }
            return slice.getEvents().get(0);
        });
    }

    @Override
    public CompletableFuture<Boolean> streamExists(final StreamId streamId) {

        Contract.requireArgNotNull("streamId", streamId);
        ensureOpen();

        final TenantStreamId sid = support.sid(streamId);
        final ReadStreamOptions options = ReadStreamOptions.get().forwards().fromRevision(0).maxCount(1);
        return es.readStream(sid.asString(), options).handle((readResult, ex) -> {
            if (ex == null) {
                return Boolean.TRUE;
            }
            final Throwable cause = unwrap(ex);
            if (cause instanceof StatusRuntimeException
                    || cause instanceof io.kurrent.dbclient.StreamNotFoundException) {
                return Boolean.FALSE;
            }
            throw new CompletionException("Error executing streamExists(..)", cause);
        });
    }

    @Override
    public CompletableFuture<StreamState> streamState(final StreamId streamId) {

        Contract.requireArgNotNull("streamId", streamId);
        ensureOpen();

        final TenantStreamId sid = support.sid(streamId);
        return es.readStream(sid.asString(), ReadStreamOptions.get().forwards().fromRevision(0))
                .<StreamState>thenApply(readResult -> StreamState.ACTIVE)
                .exceptionallyCompose(ex -> {
                    final Throwable cause = unwrap(ex);
                    if (ESGrpcEventStoreSupport.statusIsDeleted(cause)) {
                        return CompletableFuture.completedFuture(StreamState.HARD_DELETED);
                    }
                    if (cause instanceof io.kurrent.dbclient.StreamNotFoundException) {
                        return softDeleted(streamId);
                    }
                    return CompletableFuture.failedFuture(
                            new RuntimeException("Error executing streamState(..)", cause));
                });
    }

    private CompletableFuture<StreamState> softDeleted(final StreamId streamId) {
        // Workaround for reading metadata because of:
        // https://github.com/EventStore/KurrentDB-Client-Java/issues/240
        return es.readStream(ESGrpcEventStoreSupport.metaStreamName(streamId),
                        ReadStreamOptions.get().forwards().fromRevision(0))
                .<StreamState>handle((readResult, ex) -> {
                    if (ex == null) {
                        throw new CompletionException(new StreamNotFoundException(streamId));
                    }
                    final Throwable cause = unwrap(ex);
                    if (cause instanceof io.kurrent.dbclient.StreamNotFoundException) {
                        throw new CompletionException(new StreamNotFoundException(streamId));
                    }
                    throw new CompletionException(new RuntimeException("Error reading stream meta data", cause));
                });
    }

    @Override
    public CompletableFuture<Subscription> subscribeToStream(final StreamId streamId, final long eventNumber,
                                                             final BiConsumer<Subscription, CommonEvent> onEvent,
                                                             final BiConsumer<Subscription, Exception> onDrop) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgNotNull("onEvent", onEvent);
        Contract.requireArgNotNull("onDrop", onDrop);
        ensureOpen();

        final TenantStreamId sid = support.sid(streamId);
        // The esc event number is an inclusive start position (0 = first event, see mem implementation),
        // but a KurrentDB subscription's 'fromRevision(n)' is exclusive (it delivers events *after* n).
        // So 0 must map to 'fromStart()' and an inclusive start at N>=1 to 'fromRevision(N-1)'.
        SubscribeToStreamOptions options = SubscribeToStreamOptions.get().resolveLinkTos();
        if (eventNumber == EscApiUtils.SUBSCRIBE_TO_NEW_EVENTS) {
            options = options.fromEnd();
        } else if (eventNumber == 0) {
            options = options.fromStart();
        } else {
            options = options.fromRevision(eventNumber - 1);
        }

        // The esc subscription only becomes available once the native subscription future completes,
        // so the listener references it through the holder which is set below.
        final AtomicReference<ESGrpcSubscription> ref = new AtomicReference<>();
        final SubscriptionListener listener = new SubscriptionListener() {
            @Override
            public void onEvent(final io.kurrent.dbclient.Subscription subscription, final ResolvedEvent event) {
                onEvent.accept(ref.get(), support.asCommonEvent(event.getEvent()));
            }

            @Override
            public void onCancelled(final io.kurrent.dbclient.Subscription subscription,
                                    @Nullable final Throwable throwable) {
                onDrop.accept(ref.get(), asException(throwable));
            }
        };

        return es.subscribeToStream(sid.asString(), listener, options).thenApply(nativeSub -> {
            final ESGrpcSubscription subscription = new ESGrpcSubscription(streamId, null, nativeSub);
            ref.set(subscription);
            return subscription;
        });
    }

    @Override
    public CompletableFuture<Void> unsubscribeFromStream(final Subscription subscription) {
        Contract.requireArgNotNull("subscription", subscription);
        if (!(subscription instanceof ESGrpcSubscription esgrpcSubscription)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(
                    "Expected a subscription of type " + ESGrpcSubscription.class.getName()
                            + ", but was: " + subscription.getClass().getName()));
        }
        final io.kurrent.dbclient.Subscription nativeSub = esgrpcSubscription.getNativeSubscription();
        if (nativeSub != null) {
            nativeSub.stop();
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Unwraps the cause from the wrapper exceptions added by the {@link CompletableFuture} chain.
     *
     * @param throwable Throwable to unwrap.
     * @return Root cause to translate.
     */
    private static Throwable unwrap(final Throwable throwable) {
        Throwable cause = throwable;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException)
                && cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    private static Exception asException(@Nullable final Throwable throwable) {
        if (throwable == null) {
            return new RuntimeException("Subscription was dropped");
        }
        if (throwable instanceof Exception ex) {
            return ex;
        }
        return new RuntimeException(throwable);
    }

    /**
     * Builder used to create a new instance of the asynchronous event store.
     */
    @SuppressWarnings("NullAway.Init") // Required fields are populated through the builder setters
    public static final class Builder {

        private KurrentDBClient eventStore;

        private SerializerRegistry serRegistry;

        private DeserializerRegistry desRegistry;

        @Nullable
        private ConverterRegistry converters;

        private IBaseTypeFactory baseTypeFactory;

        private EnhancedMimeType targetContentType;

        @Nullable
        private TenantContext tenantContext;

        /**
         * Sets the event store to use internally.
         *
         * @param eventStore TCP/IP event store connection.
         * @return Builder.
         */
        public Builder eventStore(final KurrentDBClient eventStore) {
            this.eventStore = eventStore;
            return this;
        }

        /**
         * Sets the serializer registry.
         *
         * @param serRegistry Registry used to locate serializers.
         * @return Builder.
         */
        public Builder serRegistry(final SerializerRegistry serRegistry) {
            this.serRegistry = serRegistry;
            return this;
        }

        /**
         * Sets the deserializer registry.
         *
         * @param desRegistry Registry used to locate deserializers.
         * @return Builder.
         */
        public Builder desRegistry(final DeserializerRegistry desRegistry) {
            this.desRegistry = desRegistry;
            return this;
        }

        /**
         * Sets both types of registries in one call.
         *
         * @param registry Serializer/Deserializer registry to set.
         * @return Builder.
         */
        public Builder serDesRegistry(final SerDeserializerRegistry registry) {
            this.serRegistry = registry;
            this.desRegistry = registry;
            return this;
        }

        /**
         * Sets the version up-caster registry. When set, events read from this store are up-cast from their
         * stored version to the latest in-memory representation (the deserializer registry is wrapped in an
         * {@link UpcastingDeserializerRegistry}); {@literal null} or an empty registry leaves reads unchanged.
         *
         * @param converters Registry of version up-casters applied after deserialization.
         * @return Builder.
         */
        public Builder converters(@Nullable final ConverterRegistry converters) {
            this.converters = converters;
            return this;
        }

        /**
         * Sets the base type factory.
         *
         * @param baseTypeFactory Factory used to create base types.
         * @return Builder.
         */
        public Builder baseTypeFactory(final IBaseTypeFactory baseTypeFactory) {
            this.baseTypeFactory = baseTypeFactory;
            return this;
        }

        /**
         * Sets the target content type.
         *
         * @param targetContentType Target content type (Allows only 'application/xml'
         *                          or 'application/json' with 'utf-8' encoding).
         * @return Builder.
         */
        public Builder targetContentType(final EnhancedMimeType targetContentType) {
            this.targetContentType = targetContentType;
            return this;
        }

        /**
         * Sets the tenant identifier.
         *
         * @param tenantContext Unique tenant identifier.
         * @return Builder
         */
        public Builder tenantContext(@Nullable final TenantContext tenantContext) {
            this.tenantContext = tenantContext;
            return this;
        }

        private void verifyNotNull(final String name, @Nullable final Object value) {
            if (value == null) {
                throw new IllegalStateException(
                        "It is mandatory to set the value of '" + name + "' before calling the 'build()' method");
            }
        }

        /**
         * Creates a new instance of the event store from the attributes set via the builder.
         *
         * @return New event store instance.
         */
        public ESGrpcEventStoreAsync build() {
            verifyNotNull("eventStore", eventStore);
            verifyNotNull("serRegistry", serRegistry);
            verifyNotNull("desRegistry", desRegistry);
            verifyNotNull("baseTypeFactory", baseTypeFactory);
            verifyNotNull("targetContentType", targetContentType);
            if (tenantContext == null) {
                tenantContext = new TenantContext.NoopTenantContext();
            }
            final DeserializerRegistry effectiveDesRegistry = converters == null
                    ? desRegistry : new UpcastingDeserializerRegistry(desRegistry, converters);
            return new ESGrpcEventStoreAsync(eventStore, serRegistry, effectiveDesRegistry,
                    baseTypeFactory, targetContentType, tenantContext);
        }

    }

}
