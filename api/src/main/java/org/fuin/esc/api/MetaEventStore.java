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
 * Interface for determining capabilities of an event store.
 */
public interface MetaEventStore {

    /**
     * Returns the information if the event store implementation supports creating a stream without appending
     * events to it. If the event store does not support a create operation, a call to
     * {@link EventStore#createStream(StreamId)} will do nothing, but it will not fail.
     * 
     * @return TRUE if it's possible to create a stream without appending events to it or FALSE if only
     *         appending events implicitly creates a stream.
     */
    public boolean isSupportsCreate();

}
