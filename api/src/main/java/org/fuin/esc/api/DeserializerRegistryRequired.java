package org.fuin.esc.api;

/**
 * Marker for instances that need to have a {@link DeserializerRegistry} to function correctly. Allows late configuration in some rare edge
 * cases where the configuration cannot be provided at construction time.
 */
public interface DeserializerRegistryRequired {

    /**
     * Sets the registry to use.
     * 
     * @param registry Actual registry.
     */
    public void setRegistry(DeserializerRegistry registry);

}
