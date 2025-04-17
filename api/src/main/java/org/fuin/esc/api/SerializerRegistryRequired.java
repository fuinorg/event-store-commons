package org.fuin.esc.api;

/**
 * Marker for instances that need to have a {@link SerializerRegistry} to function correctly. Allows late configuration in some rare edge
 * cases where the configuration cannot be provided at construction time.
 */
public interface SerializerRegistryRequired {

    /**
     * Sets the registry to use.
     *
     * @param registry Actual registry.
     */
    void setRegistry(SerializerRegistry registry);

}
