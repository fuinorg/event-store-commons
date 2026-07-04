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

/**
 * Durably stores the position (next event number) a catch-up consumer has processed for a stream, keyed by
 * {@link StreamId#asString()}. This is the backend-neutral primitive that lets any backend (relational,
 * in-memory, gRPC) drive read models by polling from the persisted position instead of relying on an
 * EventStoreDB-specific durable subscription.
 * <p>
 * All implementations are expected to be thread safe.
 */
@ThreadSafe
public interface CheckpointStore {

    /**
     * Returns the number of the next event to read for the given stream, or {@code 0} if no checkpoint has
     * been stored yet (start from the beginning).
     *
     * @param streamId Unique id of the stream.
     * @return Next event number (never negative).
     */
    long readCheckpoint(StreamId streamId);

    /**
     * Stores the number of the next event to read for the given stream. Expected to be called within the same
     * transaction that applies the just-processed chunk so the position advances atomically with the work.
     *
     * @param streamId        Unique id of the stream.
     * @param nextEventNumber Number of the next event to read.
     */
    void updateCheckpoint(StreamId streamId, long nextEventNumber);

    /**
     * Removes any stored checkpoint for the given stream, so a subsequent read starts from the beginning.
     *
     * @param streamId Unique id of the stream.
     */
    void resetCheckpoint(StreamId streamId);

}
