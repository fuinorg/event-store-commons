/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. <http://www.fuin.org/>
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.fuin.esc.test;

import java.util.Objects;

import org.fuin.esc.api.EscApiUtils;
import org.fuin.esc.api.StreamId;
import org.fuin.objects4j.common.Nullable;

/**
 * Utilities for the package.
 */
final class EscTestUtils {

    private EscTestUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Determines if an object has an expected type.
     * 
     * @param expectedClass
     *            Expected type.
     * @param obj
     *            Object to test.
     * 
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
     * Determines if an exception has an expected type and message.
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

    /**
     * Creates a failure message from an expected type and an exception.
     * 
     * @param streamId
     *            Unique stream identifier this failure relates to.
     * @param expectedExceptionClass
     *            Expected exception type.
     * @param exception
     *            Current exception.
     * 
     * @return Message.
     */
    public static String createExceptionFailureMessage(final StreamId streamId,
            final Class<? extends Exception> expectedExceptionClass, final Exception exception) {
        return createExceptionFailureMessage(streamId, expectedExceptionClass, null, exception);
    }

    /**
     * Creates a failure message from an expected type, message and an exception.
     * 
     * @param streamId
     *            Unique stream identifier this failure relates to.
     * @param expectedExceptionClass
     *            Expected exception type.
     * @param expectedExceptionMessage
     *            Expected exception message.
     * @param exception
     *            Current exception.
     * 
     * @return Message.
     */
    public static String createExceptionFailureMessage(final StreamId streamId,
            final Class<? extends Exception> expectedExceptionClass, final String expectedExceptionMessage,
            final Exception exception) {
        if (expectedExceptionClass == null) {
            if (exception == null) {
                return "[" + streamId + "] OK";
            }
            return "[" + streamId + "] expected no exception, but was: " + nameAndMessage(exception);
        }
        if (exception == null) {
            return "[" + streamId + "] expected "
                    + nameAndMessage(expectedExceptionClass, expectedExceptionMessage)
                    + ", but no exception was thrown";
        }
        return "[" + streamId + "] expected "
                + nameAndMessage(expectedExceptionClass, expectedExceptionMessage) + ", but was: "
                + nameAndMessage(exception);
    }

    private static String nameAndMessage(final Class<? extends Exception> expectedExceptionClass,
            final String expectedExceptionMessage) {
        if (expectedExceptionMessage == null) {
            return expectedExceptionClass.getName();
        }
        return expectedExceptionClass.getName() + "('" + expectedExceptionMessage + "')";
    }

    private static String nameAndMessage(final Exception ex) {
        return ex.getClass().getName() + "('" + ex.getMessage() + "')";
    }

    /**
     * Converts an empty string into <code>null</code>.
     * 
     * @param str
     *            String to return.
     * 
     * @return String or <code>null</code> if the input string was "" or "-".
     */
    public static String emptyAsNull(@Nullable final String str) {
        if (str == null) {
            return null;
        }
        final String name = str.trim();
        if (name.length() == 0 || name.equals("-")) {
            return null;
        }
        return str;
    }

    /**
     * Creates an exception class from an exception name. If the name is NOT fully qualified (has no '.' in
     * the name) it's assumed that it's an API exception from package 'org.fuin.esc.api'.
     * 
     * @param exceptionName
     *            Fully qualified name or a simple name of a class located in the "org.fuin.esc.api" package.
     * 
     * @return Class.
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends Exception> exceptionForName(final String exceptionName) {
        String name = emptyAsNull(exceptionName);
        if (exceptionName == null) {
            return null;
        }
        if (name.indexOf('.') == -1) {
            name = EscApiUtils.class.getPackage().getName() + "." + name;
        }
        try {
            return (Class<? extends Exception>) Class.forName(name);
        } catch (final ClassNotFoundException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

}
