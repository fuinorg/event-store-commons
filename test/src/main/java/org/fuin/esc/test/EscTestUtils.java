/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. 
 * http://www.fuin.org/
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
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package org.fuin.esc.test;

import javax.annotation.Nullable;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EscApiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities that help testing with the event store commons.
 */
public final class EscTestUtils {

    private static final Logger LOG = LoggerFactory.getLogger(EscTestUtils.class);

    private EscTestUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a failure message from an exception.
     * 
     * @param identifier
     *            Unique identifier this failure relates to.
     * @param exception
     *            Current exception.
     * 
     * @return Message.
     */
    public static String createExceptionFailureMessage(final String identifier, final Exception exception) {
        return createExceptionFailureMessage(identifier, null, null, exception);
    }

    /**
     * Creates a failure message from an expected type and an exception.
     * 
     * @param identifier
     *            Unique identifier this failure relates to.
     * @param expectedExceptionClass
     *            Expected exception type.
     * @param exception
     *            Current exception.
     * 
     * @return Message.
     */
    public static String createExceptionFailureMessage(final String identifier, final Class<? extends Exception> expectedExceptionClass,
            final Exception exception) {
        return createExceptionFailureMessage(identifier, expectedExceptionClass, null, exception);
    }

    /**
     * Creates a failure message from an expected type, message and an exception.
     * 
     * @param identifier
     *            Unique identifier this failure relates to.
     * @param expectedExceptionClass
     *            Expected exception type.
     * @param expectedExceptionMessage
     *            Expected exception message.
     * @param exception
     *            Current exception.
     * 
     * @return Message.
     */
    public static String createExceptionFailureMessage(final String identifier, final Class<? extends Exception> expectedExceptionClass,
            final String expectedExceptionMessage, final Exception exception) {
        if (expectedExceptionClass == null) {
            if (exception == null) {
                return "[" + identifier + "] OK";
            }
            final String msg = "[" + identifier + "] expected no exception, but was: " + nameAndMessage(exception)
                    + " (See log file for exception details)";
            LOG.error(msg, exception);
            return msg;
        }
        if (exception == null) {
            return "[" + identifier + "] expected " + nameAndMessage(expectedExceptionClass, expectedExceptionMessage)
                    + ", but no exception was thrown";
        }
        final String msg = "[" + identifier + "] expected " + nameAndMessage(expectedExceptionClass, expectedExceptionMessage) + ", but was: "
                + nameAndMessage(exception) + " (See log file for exception details)";
        LOG.error(msg, exception);
        return msg;
    }

    private static String nameAndMessage(final Class<? extends Exception> expectedExceptionClass, final String expectedExceptionMessage) {
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
     * Creates an exception class from an exception name. If the name is NOT fully qualified (has no '.' in the name) it's assumed that it's
     * an API exception from package 'org.fuin.esc.api'.
     * 
     * @param exceptionName
     *            Fully qualified name or a simple name of a class located in the "org.fuin.esc.api" package.
     * 
     * @return Class.
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends Exception> exceptionForName(final String exceptionName) {
        String name = emptyAsNull(exceptionName);
        if (name == null) {
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

    /**
     * Compares the two events based on their content.
     * 
     * @param eventA
     *            First event.
     * @param eventB
     *            Second event.
     * 
     * @return TRUE id the events have the same content.
     */
    public static boolean sameContent(@Nullable final CommonEvent eventA, @Nullable final CommonEvent eventB) {
        if (eventA == null) {
            return (eventB == null);
        }
        if (eventB == null) {
            return false;
        }

        if (!eventA.getId().equals(eventB.getId())) {
            return false;
        }

        if (!eventA.getData().equals(eventB.getData())) {
            return false;
        }

        if (eventA.getMeta() == null) {
            if (eventB.getMeta() != null) {
                return false;
            }
        } else if (!eventA.getMeta().equals(eventB.getMeta())) {
            return false;
        }

        return eventA.getDataType().equals(eventB.getDataType());
    }

    /**
     * Creates an exception message by pointing out the differences between two events.
     * 
     * @param identifier
     *            Unique identifier to use in case of a failure.
     * @param expectedEvent
     *            First event.
     * @param actualEvent
     *            Second event.
     * 
     * @return Exception message.
     */
    public static String createExceptionFailureMessage(final String identifier, final CommonEvent expectedEvent,
            final CommonEvent actualEvent) {
        if (expectedEvent == null) {
            if (actualEvent == null) {
                return "[" + identifier + "] OK";
            }
            return "[" + identifier + "] Expected no event, but got: " + actualEvent;
        }
        if (actualEvent == null) {
            return "[" + identifier + "] Got no event, but expected one: " + expectedEvent;
        }

        if (expectedEvent.getId() == null) {
            if (actualEvent.getId() != null) {
                return "[" + identifier + "] Expected no event id, but was: " + actualEvent.getId();
            }
        } else if (!expectedEvent.getId().equals(actualEvent.getId())) {
            return "[" + identifier + "] Expected event id '" + expectedEvent.getId() + "', but was: " + actualEvent.getId();
        }

        if (expectedEvent.getData() == null) {
            if (actualEvent.getData() != null) {
                return "[" + identifier + "] Expected no event data, but was: " + actualEvent.getData();
            }
        } else if (!expectedEvent.getData().equals(actualEvent.getData())) {
            return "[" + identifier + "] Expected event data '" + expectedEvent.getData() + "', but was: " + actualEvent.getData();
        }

        if (expectedEvent.getMeta() == null) {
            if (actualEvent.getMeta() != null) {
                return "[" + identifier + "] Expected no event meta, but was: " + actualEvent.getMeta();
            }
        } else if (!expectedEvent.getMeta().equals(actualEvent.getMeta())) {
            return "[" + identifier + "] Expected event meta '" + expectedEvent.getMeta() + "', but was: " + actualEvent.getMeta();
        }

        if (expectedEvent.getDataType() == null) {
            if (actualEvent.getDataType() != null) {
                return "[" + identifier + "] Expected no event type, but was: " + actualEvent.getDataType();
            }
        } else if (!expectedEvent.getDataType().equals(actualEvent.getDataType())) {
            return "[" + identifier + "] Expected event type '" + expectedEvent.getDataType() + "', but was: " + actualEvent.getDataType();
        }

        return "[" + identifier + "] OK";
    }

}
