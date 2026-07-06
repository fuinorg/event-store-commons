package org.fuin.esc.test;

import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.DeserializerRegistry;
import org.fuin.esc.api.EventStore;
import org.fuin.esc.api.ProjectionAdminEventStore;
import org.jspecify.annotations.Nullable;

/**
 * Container used to pass additional information to commands.
 */
public final class TestContext {

    @NotNull
    private final String currentEventStoreImplType;

    @NotNull
    private final EventStore eventStore;

    @NotNull
    private DeserializerRegistry deserializerRegistry;

    /** Projection admin store for the current backend, or {@literal null} if it does not support projections. */
    @Nullable
    private ProjectionAdminEventStore projectionAdmin;

    /**
     * Constructor with mandatory data.
     *
     * @param currentEventStoreImplType Type name of the currently tested event store implementation. Will be used to prefix the stream names to avoid name
     *                                  clashes for multiple implementations for the same backend store.
     * @param eventStore                Event store to use.
     */
    public TestContext(@NotNull String currentEventStoreImplType,
                       @NotNull EventStore eventStore,
                       @NotNull DeserializerRegistry deserializerRegistry) {
        super();
        this.currentEventStoreImplType = currentEventStoreImplType;
        this.eventStore = eventStore;
        this.deserializerRegistry = deserializerRegistry;
    }

    /**
     * Returns the type name of the currently tested event store implementation. Will be used to prefix the stream names to avoid name
     * clashes for multiple implementations for the same backend store.
     *
     * @return Type name.
     */
    public final String getCurrentEventStoreImplType() {
        return currentEventStoreImplType;
    }

    /**
     * Returns the event store to use.
     *
     * @return Event store.
     */
    public final EventStore getEventStore() {
        return eventStore;
    }


    /**
     * Returns the deserializer registry.
     *
     * @return Registry.
     */
    public DeserializerRegistry getDeserializerRegistry() {
        return deserializerRegistry;
    }

    /**
     * Returns the projection admin store for the current backend.
     *
     * @return Projection admin store, or {@literal null} if the current backend does not support projections.
     */
    @Nullable
    public ProjectionAdminEventStore getProjectionAdmin() {
        return projectionAdmin;
    }

    /**
     * Sets the projection admin store for the current backend.
     *
     * @param projectionAdmin Projection admin store (may be {@literal null}).
     */
    public void setProjectionAdmin(@Nullable final ProjectionAdminEventStore projectionAdmin) {
        this.projectionAdmin = projectionAdmin;
    }
}
