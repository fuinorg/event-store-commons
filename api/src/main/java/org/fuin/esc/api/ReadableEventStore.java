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
 * Interface for reading events from an event store synchronously. Calling any
 * method on a non-open event store will implicitly {@link #open()} it.
 */
public interface ReadableEventStore extends EventStoreBasics {

    /**
     * Reads count Events from an Event Stream forwards (e.g. oldest to newest)
     * starting from position start. traing to read from a non-open
     * 
     * @param streamId
     *            The stream to read from.
     * @param start
     *            The starting point to read from.
     * @param count
     *            The count of items to read.
     * 
     * @return A slice containing the results of the read operation. Never
     *         <code>null</code>, but may be an empty list.
     * 
     * @throws StreamNotFoundException
     *             A stream with the given name does not exist in the
     *             repository.
     * @throws StreamDeletedException
     *             A stream with the given name previously existed but was
     *             deleted.
     */
    @NotNull
    public StreamEventsSlice readEventsForward(@NotNull StreamId streamId,
            int start, int count);

    /**
     * Reads count Events from an Event Stream backwards (e.g. newest to oldest)
     * starting from position start.
     * 
     * @param streamId
     *            The stream to read from.
     * @param start
     *            The starting point to read from.
     * @param count
     *            The count of items to read.
     * 
     * @return A slice containing the results of the read operation. Never
     *         <code>null</code>, but may be an empty list.
     * 
     * @throws StreamNotFoundException
     *             A stream with the given name does not exist in the
     *             repository.
     * @throws StreamDeletedException
     *             A stream with the given name previously existed but was
     *             deleted.
     */
    @NotNull
    public StreamEventsSlice readEventsBackward(@NotNull StreamId streamId,
            int start, int count);

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
     *             A stream with the given name does not exist in the
     *             repository.
     * @throws StreamDeletedException
     *             A stream with the given name previously existed but was
     *             deleted.
     */
    @NotNull
    public CommonEvent readEvent(@NotNull StreamId streamId, int eventNumber);

    /**
     * Determines if a stream exists.
     * 
     * @param streamId
     *            Unique identifier of the stream.
     * 
     * @return TRUE if the stream exists, else FALSE.
     */
    public boolean streamExists(@NotNull StreamId streamId);

    /**
     * Returns the state of the stream.
     * 
     * @param streamId
     *            Unique identifier of the stream.
     * 
     * @return State.
     * 
     * @throws StreamNotFoundException
     *             A stream with the given name does not exist in the
     *             repository.
     */
    @NotNull
    public StreamState streamState(@NotNull StreamId streamId);

    /**
     * Reads all events until the end of the stream. A stream that does not
     * exist will be ignored without any exception. CAUTION: Internally a
     * {@link StreamNotFoundException} may be thrown. There might be cases in an
     * EJB environment that require to configure this runtime exception as an
     * application exceptions using the "ejb-jar.xml" (If you don't know what
     * this is, just google for "ejb-jar.xml assembly-descriptor
     * application-exception").
     * 
     * @param streamId
     *            Unique identifier of the stream.
     * @param startingAtEventNumber
     *            First event number to read.
     * @param chunkSize
     *            Number of events to read in a single operation.
     * @param handler
     *            Handler to pass a read chunk to.
     * 
     * @throws StreamDeletedException
     *             A stream with the given name previously existed but was
     *             deleted.
     */
    public void readAllEventsForward(StreamId streamId,
            int startingAtEventNumber, int chunkSize,
            ChunkEventHandler handler);

    /**
     * Handles a number of events.
     */
    public interface ChunkEventHandler {

        /**
         * List of events to handle.
         * 
         * @param currentSlice
         *            Slice with events to handle.
         */
        public void handle(StreamEventsSlice currentSlice);

    }

}
