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
package org.fuin.esc.api;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for reading events from an event store.
 */
// CHECKSTYLE:OFF:RedundantThrows
public interface ReadableEventStore {

    /**
     * Reads count Events from an Event Stream forwards (e.g. oldest to newest)
     * starting from position start.
     * 
     * @param streamId
     *            The stream to read from
     * @param start
     *            The starting point to read from
     * @param count
     *            The count of items to read
     * 
     * @return A slice containing the results of the read operation
     * 
     * @throws StreamNotFoundException
     *             A stream with the given name does not exist in the
     *             repository.
     * @throws StreamDeletedException
     *             A stream with the given name previously existed but was
     *             deleted.
     */
    public CompletableFuture<StreamEventsSlice> readEventsForward(
            StreamId streamId, int start, int count);

    /**
     * Reads count Events from an Event Stream forwards (e.g. oldest to newest)
     * starting from position start using dedicated credentials.
     * 
     * @param streamId
     *            The stream to read from
     * @param start
     *            The starting point to read from
     * @param count
     *            The count of items to read
     * @param credentials
     *            Credentials to use for authentication.
     * 
     * @return A slice containing the results of the read operation
     * 
     * @throws StreamNotFoundException
     *             A stream with the given name does not exist in the
     *             repository.
     * @throws StreamDeletedException
     *             A stream with the given name previously existed but was
     *             deleted.
     */
    public CompletableFuture<StreamEventsSlice> readEventsForward(
            StreamId streamId, int start, int count, Credentials credentials);

    /**
     * Reads count Events from an Event Stream backwards (e.g. newest to oldest)
     * starting from position start.
     * 
     * @param streamId
     *            The stream to read from
     * @param start
     *            The starting point to read from
     * @param count
     *            The count of items to read
     * 
     * @return A slice containing the results of the read operation
     * 
     * @throws StreamNotFoundException
     *             A stream with the given name does not exist in the
     *             repository.
     * @throws StreamDeletedException
     *             A stream with the given name previously existed but was
     *             deleted.
     */
    public CompletableFuture<StreamEventsSlice> readEventsBackward(
            StreamId streamId, int start, int count);

    /**
     * Reads count Events from an Event Stream backwards (e.g. newest to oldest)
     * starting from position start using dedicated credentials.
     * 
     * @param streamId
     *            The stream to read from
     * @param start
     *            The starting point to read from
     * @param count
     *            The count of items to read
     * @param credentials
     *            Credentials to use for authentication.
     * 
     * @return A slice containing the results of the read operation
     * 
     * @throws StreamNotFoundException
     *             A stream with the given name does not exist in the
     *             repository.
     * @throws StreamDeletedException
     *             A stream with the given name previously existed but was
     *             deleted.
     */
    public CompletableFuture<StreamEventsSlice> readEventsBackward(
            StreamId streamId, int start, int count, Credentials credentials);

    /**
     * Reads a single event from a stream.
     * 
     * @param streamId
     *            The stream to read from
     * @param eventNumber
     *            The event number to read.
     * 
     * @return A result containing the results of the read operation
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
    public CompletableFuture<CommonEvent> readEvent(StreamId streamId,
            int eventNumber);

    /**
     * Reads a single event from a stream using dedicated credentials.
     * 
     * @param streamId
     *            The stream to read from
     * @param eventNumber
     *            The event number to read.
     * @param credentials
     *            Credentials to use for authentication.
     * 
     * @return A result containing the results of the read operation
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
    public CompletableFuture<CommonEvent> readEvent(StreamId streamId,
            int eventNumber, Credentials credentials);

}
// CHECKSTYLE:ON:RedundantThrows
