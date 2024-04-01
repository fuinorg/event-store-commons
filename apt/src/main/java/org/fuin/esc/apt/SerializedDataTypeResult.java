package org.fuin.esc.apt;

/**
 * Type found during annotation processing.
 *
 * @param packageName     Name of the package where the class is located.
 * @param simpleClassName Simple class name.
 * @param fieldName       Name of the static field.
 */
public record SerializedDataTypeResult(String packageName, String simpleClassName, String fieldName) {

    public SerializedDataTypeResult {
        if (packageName == null || packageName.isEmpty()) {
            throw new IllegalArgumentException("packageName cannot be null or empty: " + packageName);
        }
        if (simpleClassName == null || simpleClassName.isEmpty()) {
            throw new IllegalArgumentException("simpleClassName cannot be null or empty: " + simpleClassName);
        }
        if (fieldName == null || fieldName.isEmpty()) {
            throw new IllegalArgumentException("fieldName cannot be null or empty: " + fieldName);
        }
    }

    public String getFullClassName() {
        return packageName + "." + simpleClassName;
    }

    @Override
    public String toString() {
        return "new SerializedDataType2ClassMapping(" + simpleClassName + "." + fieldName + ", " + simpleClassName + ".class)";
    }
}
