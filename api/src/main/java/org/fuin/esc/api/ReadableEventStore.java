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

import javax.validation.constraints.NotNull;

/**
 * Interface for reading events from an event store synchronously.
 */
public interface ReadableEventStore extends AutoCloseable {

    /**
     * Opens a connection to the repository.
     */
    public void open();

    /**
     * Closes the connection to the repository.
     */
    public void close();

    /**
     * Reads count Events from an Event Stream forwards (e.g. oldest to newest) starting from position start.
     * 
     * @param streamId
     *            The stream to read from.
     * @param start
     *            The starting point to read from.
     * @param count
     *            The count of items to read.
     * 
     * @return A slice containing the results of the read operation. Never <code>null</code>, but may be an
     *         empty list.
     * 
     * @throws StreamNotFoundException
     *             A stream with the given name does not exist in the repository.
     * @throws StreamDeletedException
     *             A stream with the given name previously existed but was deleted.
     */
    @NotNull
    public StreamEventsSlice readEventsForward(@NotNull StreamId streamId, int start, int count);

    /**
     * Reads count Events from an Event Stream backwards (e.g. newest to oldest) starting from position start.
     * 
     * @param streamId
     *            The stream to read from.
     * @param start
     *            The starting point to read from.
     * @param count
     *            The count of items to read.
     * 
     * @return A slice containing the results of the read operation. Never <code>null</code>, but may be an
     *         empty list.
     * 
     * @throws StreamNotFoundException
     *             A stream with the given name does not exist in the repository.
     * @throws StreamDeletedException
     *             A stream with the given name previously existed but was deleted.
     */
    @NotNull
    public StreamEventsSlice readEventsBackward(@NotNull StreamId streamId, int start, int count);

    /**
     * Reads a single event from a stream.
     * 
     * @param streamId
     *            The stream to read from.
     * @param eventNumber
     *            The event number to read.
     * 
     * @return A result containing the results of the read operation.
     * 
     * @throws EventNotFoundException
     *             An event with the given number was not found in the stream.
     * @throws StreamNotFoundException
     *             A stream with the given name does not exist in the repository.
     * @throws StreamDeletedException
     *             A stream with the given name previously existed but was deleted.
     */
    @NotNull
    public CommonEvent readEvent(@NotNull StreamId streamId, int eventNumber);

    /**
     * Determines if a stream exists.
     * 
     * @param streamId Unique identifier of the stream.
     * 
     * @return TRUE if the stream exists, else FALSE.
     */
    public boolean streamExists(@NotNull StreamId streamId);
    
    /**
     * Returns the state of the stream.
     * 
     * @param streamId Unique identifier of the stream.
     * 
     * @return State.
     * 
     * @throws StreamNotFoundException
     *             A stream with the given name does not exist in the repository.
     */
    public StreamState streamState(@NotNull StreamId streamId);
    
}
