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

import java.util.List;

import javax.validation.constraints.NotNull;

/**
 * Interface for writing events to an event store synchronously..
 */
public interface WritableEventStoreSync {

    /**
     * Opens a connection to the repository.
     */
    public void open();

    /**
     * Closes the connection to the repository.
     */
    public void close();

    /**
     * Appends one or more events to a stream. If the stream does not exist, the
     * implementation may create it on the fly.
     * 
     * @param streamId
     *            The unique identifier of the stream to append the events to.
     * @param expectedVersion
     *            The version the stream should have.
     * @param events
     *            Array of events to write to the stream
     * 
     * @return The next expected version for the stream.
     * 
     * @throws StreamNotFoundException
     *             The stream does not exist in the repository and the
     *             implementation cannot create it on-the-fly.
     * @throws StreamDeletedException
     *             A stream with the given name previously existed but was
     *             deleted.
     * @throws StreamVersionConflictException
     *             The expected version didn't match the actual version.
     * @throws StreamReadOnlyException
     *             The given stream identifier points to a projection.
     */
    public int appendToStream(@NotNull StreamId streamId, int expectedVersion,
            @NotNull CommonEvent... events) throws StreamNotFoundException,
            StreamDeletedException, StreamVersionConflictException,
            StreamReadOnlyException;

    /**
     * Appends one or more events to a stream. If the stream does not exist, the
     * implementation may create it on the fly.
     * 
     * @param streamId
     *            The unique identifier of the stream to append the events to.
     * @param events
     *            Array of events to write to the stream
     * 
     * @return The next expected version for the stream.
     * 
     * @throws StreamNotFoundException
     *             The stream does not exist in the repository and the
     *             implementation cannot create it on-the-fly.
     * @throws StreamDeletedException
     *             A stream with the given name previously existed but was
     *             deleted.
     * @throws StreamReadOnlyException
     *             The given stream identifier points to a projection.
     */
    public int appendToStream(@NotNull StreamId streamId,
            @NotNull CommonEvent... events) throws StreamNotFoundException,
            StreamDeletedException, StreamReadOnlyException;

    /**
     * Appends a list of events to a stream. If the stream does not exist, the
     * implementation may create it on the fly.
     * 
     * @param streamId
     *            The unique identifier of the stream to append the events to.
     * @param expectedVersion
     *            The version the stream should have.
     * @param events
     *            List of events to write to the stream
     * 
     * @return The next expected version for the stream.
     * 
     * @throws StreamNotFoundException
     *             The stream does not exist in the repository and the
     *             implementation cannot create it on-the-fly.
     * @throws StreamDeletedException
     *             The stream previously existed but was deleted.
     * @throws StreamVersionConflictException
     *             The expected version didn't match the actual version.
     * @throws StreamReadOnlyException
     *             The given stream identifier points to a projection.
     */
    public int appendToStream(@NotNull StreamId streamId, int expectedVersion,
            @NotNull List<CommonEvent> events) throws StreamNotFoundException,
            StreamDeletedException, StreamVersionConflictException,
            StreamReadOnlyException;

    /**
     * Appends a list of events to a stream. If the stream does not exist, the
     * implementation may create it on the fly.
     * 
     * @param streamId
     *            The unique identifier of the stream to append the events to.
     * @param events
     *            List of events to write to the stream
     * 
     * @return The next expected version for the stream.
     * 
     * @throws StreamNotFoundException
     *             The stream does not exist in the repository and the
     *             implementation cannot create it on-the-fly.
     * @throws StreamDeletedException
     *             The stream previously existed but was deleted.
     * @throws StreamReadOnlyException
     *             The given stream identifier points to a projection.
     */
    public int appendToStream(@NotNull StreamId streamId,
            @NotNull List<CommonEvent> events) throws StreamNotFoundException,
            StreamDeletedException, StreamReadOnlyException;

    /**
     * Deletes a stream from the event store if it has a given version.
     * 
     * @param streamId
     *            The unique identifier of the stream to be deleted
     * @param expectedVersion
     *            The version the stream should have when being deleted.
     * 
     * @throws StreamNotFoundException
     *             A stream with the given name does not exist in the
     *             repository.
     * @throws StreamDeletedException
     *             A stream with the given name previously existed but was
     *             deleted.
     * @throws StreamVersionConflictException
     *             The expected version didn't match the actual version.
     */
    public void deleteStream(@NotNull StreamId streamId, int expectedVersion)
            throws StreamNotFoundException, StreamDeletedException,
            StreamVersionConflictException;

    /**
     * Deletes a stream from the event store not matter what the current version
     * is.
     * 
     * @param streamId
     *            The unique identifier of the stream to be deleted
     * 
     * @throws StreamNotFoundException
     *             A stream with the given name does not exist in the
     *             repository.
     * @throws StreamDeletedException
     *             A stream with the given name previously existed but was
     *             deleted.
     */
    public void deleteStream(@NotNull StreamId streamId)
            throws StreamNotFoundException, StreamDeletedException;

}
