/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved.
 * http://www.fuin.org/
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package org.fuin.esc.api;

import jakarta.annotation.Nullable;

/**
 * Stream version numbers that have a special meaning. This is used for an optimistic concurrency check on the
 * version of the stream to which events are to be written.
 */
public enum ExpectedVersion {

    /** This disables the optimistic concurrency check. */
    ANY(-2),

    /** This specifies the expectation that target stream does not yet exist or is empty. */
    NO_OR_EMPTY_STREAM(-1);

    private final long no;

    ExpectedVersion(final long no) {
        this.no = no;
    }

    /**
     * Returns the value for the enum.
     *
     * @return Version value.
     */
    public final long getNo() {
        return no;
    }

    /**
     * Determines if the given name is a valid one.
     *
     * @param name
     *            Name to test.
     *
     * @return TRUE if the name is valid, else FALSE.
     */
    public static boolean valid(@Nullable final String name) {
        if (name == null) {
            return true;
        }
        for (ExpectedVersion version : values()) {
            if (version.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the value of the given constant or if it cannot be found the string converted to an int.
     *
     * @param name
     *            Name of a constant or a valid Integer value. If the argument is <code>null</code> the enum
     *            {@link #ANY} will be returned.
     *
     * @return Value.
     */
    public static long no(@Nullable final String name) {
        if (name == null) {
            return ANY.no;
        }
        if (valid(name)) {
            return valueOf(name).getNo();
        }
        return Integer.valueOf(name);
    }

}
