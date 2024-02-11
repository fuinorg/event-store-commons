package org.fuin.esc.test;

import java.util.Objects;

/**
 * Helper methods for the package.
 */
public final class TestUtils {

    private TestUtils() {
    }

    /**
     * Determines if an object has an expected type in a null-safe way.
     *
     * @param expectedClass Expected type.
     * @param obj           Object to test.
     * @return TRUE if the object is exactly of the same class, else FALSE.
     */
    public static boolean isExpectedType(final Class<?> expectedClass, final Object obj) {
        final Class<?> actualClass;
        if (obj == null) {
            actualClass = null;
        } else {
            actualClass = obj.getClass();
        }
        return Objects.equals(expectedClass, actualClass);
    }

    /**
     * Determines if an exception has an expected type and message in a null-safe way.
     *
     * @param expectedClass
     *            Expected exception type.
     * @param expectedMessage
     *            Expected message.
     * @param ex
     *            Exception to test.
     *
     * @return TRUE if the object is exactly of the same class and has the same message, else FALSE.
     */
    public static boolean isExpectedException(final Class<? extends Exception> expectedClass,
                                              final String expectedMessage, final Exception ex) {
        if (!isExpectedType(expectedClass, ex)) {
            return false;
        }
        if ((expectedClass != null) && (expectedMessage != null) && (ex != null)) {
            return Objects.equals(expectedMessage, ex.getMessage());
        }
        return true;
    }

}
