package org.fuin.esc.spi;

/**
 * Marker for instances that need to have a {@link SerializedDataTypeRegistry} to function correctly. Allows late configuration in some rare
 * edge cases where the configuration cannot be provided at construction time.
 */
public interface SerializedDataTypeRegistryRequired {

    /**
     * Sets the registry to use.
     * 
     * @param registry
     *            Actual registry.
     */
    public void setRegistry(SerializedDataTypeRegistry registry);

}
