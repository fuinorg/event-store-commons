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


import org.fuin.objects4j.common.ThreadSafe;

import java.util.List;

/**
 * Interface for writing events to an event store synchronously. Calling any
 * method on a non-open event store will implicitly {@link #open()} it.
 * <p>
 * Implementations are expected to be thread-safe.
 * <p>
 * <b>Retrying an append.</b> An {@link EscConnectionException} means the outcome is unknown - the failure
 * can happen before the store ever saw the request, but also after it accepted and persisted the events and
 * only the acknowledgement was lost. Repeating the call with {@link ExpectedVersion#ANY} therefore appends
 * the events a second time. Pass the concrete version the stream is expected to have instead; the
 * repetition of an append that did get through is then not applied again.
 * <p>
 * How that is reported differs between backends and a retrying caller must cope with both: an
 * implementation may answer with a {@link WrongExpectedVersionException} (the version no longer matches), or
 * it may recognize that the stream already ends with exactly these events and answer with the current
 * version as if the call had just succeeded. Either way the stream stays free of duplicates. A caller that
 * cannot know the expected version needs a deduplication mechanism of its own; there is no other way to make
 * a retry safe.
 */
@ThreadSafe
public interface WritableEventStore extends EventStoreBasics {

    /**
     * Returns the information if the event store implementation supports
     * creating a stream without appending events to it. If the event store does
     * not support a create operation, a call to {@link #createStream(StreamId)}
     * will do nothing, but it will not fail.
     *
     * @return TRUE if it's possible to create a stream without appending events
     * to it or FALSE if only appending events implicitly creates a
     * stream.
     */
    boolean isSupportsCreateStream();

    /**
     * Creates a new stream. Some implementations may do nothing, because they
     * create streams when the first event is appended. If
     * {@link #isSupportsCreateStream()} returns FALSE, this method does
     * nothing, but is expected not fail.
     *
     * @param streamId The unique identifier of the stream to create.
     * @throws StreamAlreadyExistsException The stream already exists.
     */
    void createStream(StreamId streamId) throws StreamAlreadyExistsException;

    /**
     * Appends one or more events to a stream. If the stream does not exist, the
     * implementation may create it on the fly.
     *
     * @param streamId        The unique identifier of the stream to append the events to.
     * @param expectedVersion The version the stream should have.
     * @param events          Array of events to write to the stream
     * @return The next expected version for the stream.
     * @throws StreamNotFoundException       The stream does not exist in the repository and the
     *                                       implementation cannot create it on-the-fly.
     * @throws StreamDeletedException        A stream with the given name previously existed but was
     *                                       deleted.
     * @throws WrongExpectedVersionException The expected version didn't match the actual version.
     * @throws StreamReadOnlyException       The given stream identifier points to a projection.
     */
    long appendToStream(StreamId streamId, long expectedVersion, CommonEvent... events)
            throws StreamNotFoundException, StreamDeletedException, WrongExpectedVersionException,
            StreamReadOnlyException;

    /**
     * Appends one or more events to a stream. If the stream does not exist, the
     * implementation may create it on the fly.
     *
     * @param streamId The unique identifier of the stream to append the events to.
     * @param events   Array of events to write to the stream
     * @return The next expected version for the stream.
     * @throws StreamNotFoundException The stream does not exist in the repository and the
     *                                 implementation cannot create it on-the-fly.
     * @throws StreamDeletedException  A stream with the given name previously existed but was
     *                                 deleted.
     * @throws StreamReadOnlyException The given stream identifier points to a projection.
     */
    long appendToStream(StreamId streamId, CommonEvent... events)
            throws StreamNotFoundException, StreamDeletedException, StreamReadOnlyException;

    /**
     * Appends a list of events to a stream. If the stream does not exist, the
     * implementation may create it on the fly.
     *
     * @param streamId        The unique identifier of the stream to append the events to.
     * @param expectedVersion The version the stream should have.
     * @param events          List of events to write to the stream
     * @return The next expected version for the stream.
     * @throws StreamNotFoundException       The stream does not exist in the repository and the
     *                                       implementation cannot create it on-the-fly.
     * @throws StreamDeletedException        The stream previously existed but was deleted.
     * @throws WrongExpectedVersionException The expected version didn't match the actual version.
     * @throws StreamReadOnlyException       The given stream identifier points to a projection.
     */
    long appendToStream(StreamId streamId, long expectedVersion,
                        List<CommonEvent> events) throws StreamNotFoundException, StreamDeletedException,
            WrongExpectedVersionException, StreamReadOnlyException;

    /**
     * Appends a list of events to a stream. If the stream does not exist, the
     * implementation may create it on the fly.
     *
     * @param streamId The unique identifier of the stream to append the events to.
     * @param events   List of events to write to the stream
     * @return The next expected version for the stream.
     * @throws StreamNotFoundException The stream does not exist in the repository and the
     *                                 implementation cannot create it on-the-fly.
     * @throws StreamDeletedException  The stream previously existed but was deleted.
     * @throws StreamReadOnlyException The given stream identifier points to a projection.
     */
    long appendToStream(StreamId streamId, List<CommonEvent> events)
            throws StreamNotFoundException, StreamDeletedException, StreamReadOnlyException;

    /**
     * Deletes a stream from the event store if it has a given version. Deleting
     * a previously soft-deleted stream again does NOT throw an exception.
     * Deleting a non-existing stream with an expected version of
     * {@link ExpectedVersion#ANY} or {@link ExpectedVersion#NO_OR_EMPTY_STREAM}
     * does also NOT throw an exception.
     *
     * @param streamId        The unique identifier of the stream to be deleted
     * @param expectedVersion The version the stream should have when being deleted.
     * @param hardDelete      TRUE if it should be impossible to recreate the stream. FALSE
     *                        (soft delete) if appending to it will recreate it. Please note
     *                        that in this case the version numbers do not start at zero but
     *                        at where you previously soft deleted the stream from.
     * @throws StreamDeletedException        A stream with the given name previously existed but was hard
     *                                       deleted.
     * @throws WrongExpectedVersionException The expected version didn't match the actual version.
     * @throws StreamReadOnlyException       The given stream identifier points to a projection.
     */
    void deleteStream(StreamId streamId, long expectedVersion, boolean hardDelete)
            throws StreamDeletedException, WrongExpectedVersionException, StreamReadOnlyException;

    /**
     * Deletes a stream from the event store not matter what the current version
     * is.
     *
     * @param streamId   The unique identifier of the stream to be deleted
     * @param hardDelete TRUE if it should be impossible to recreate the stream. FALSE
     *                   (soft delete) if appending to it will recreate it. Please note
     *                   that in this case the version numbers do not start at zero but
     *                   at where you previously soft deleted the stream from.
     * @throws StreamNotFoundException A stream with the given name does not exist in the
     *                                 repository.
     * @throws StreamDeletedException  A stream with the given name previously existed but was
     *                                 deleted.
     * @throws StreamReadOnlyException The given stream identifier points to a projection.
     */
    void deleteStream(StreamId streamId, boolean hardDelete)
            throws StreamNotFoundException, StreamDeletedException, StreamReadOnlyException;

}
