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
package org.fuin.esc.api;

/**
 * State of a stream.
 */
public enum StreamState {

    /** The stream was created and may contain events. */
    ACTIVE(0),
    
    /** The stream was deleted, but may be recreated by appending new events to it. */
    SOFT_DELETED(1),
    
    /** The stream was deleted, but cannot be recreated. */
    HARD_DELETED(2);
 
    
    private int dbValue;
    
    private StreamState(final int dbValue) {
        this.dbValue = dbValue;
    }
    
    /**
     * Returns the database value.
     * 
     * @return DB value.
     */
    public final int dbValue() {
        return dbValue;
    }
    
    /**
     * Returns the enum from a database value.
     * 
     * @param db Database value to return an enum for.
     *  
     * @return Enum.
     */
    public static StreamState fromDbValue(final int db) {
        for (final StreamState state : values()) {
            if (state.dbValue() == db) {
                return state;
            }
        }
        throw new IllegalArgumentException("The db value is unknown: " + db);
    }
    
}
