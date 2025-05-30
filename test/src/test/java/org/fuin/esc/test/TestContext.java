package org.fuin.esc.test;

import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.DeserializerRegistry;
import org.fuin.esc.api.EventStore;

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
}
