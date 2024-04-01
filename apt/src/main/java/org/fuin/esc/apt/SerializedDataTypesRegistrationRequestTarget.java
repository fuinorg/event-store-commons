package org.fuin.esc.apt;

/**
 * Defines the data necessary to create a source file.
 *
 * @param packageName         Name of the package where to create the class.
 * @param simpleClassName     Name of the class to create.
 * @param simpleInterfaceName Interface the class should implement.
 */
public record SerializedDataTypesRegistrationRequestTarget(String packageName, String simpleClassName, String simpleInterfaceName) {

    public SerializedDataTypesRegistrationRequestTarget {
        if (packageName == null || packageName.isEmpty()) {
            throw new IllegalArgumentException("packageName cannot be null or empty: " + packageName);
        }
        if (simpleClassName == null || simpleClassName.isEmpty()) {
            throw new IllegalArgumentException("simpleClassName cannot be null or empty: " + simpleClassName);
        }
        if (simpleInterfaceName == null || simpleInterfaceName.isEmpty()) {
            throw new IllegalArgumentException("simpleInterfacefName cannot be null or empty: " + simpleInterfaceName);
        }
    }

    @Override
    public String toString() {
        return packageName + "." + simpleClassName;
    }

}
