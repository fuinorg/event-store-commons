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
package org.fuin.esc.spi;

import org.fuin.esc.api.CheckpointStore;
import org.fuin.esc.api.StreamId;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.ThreadSafe;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Volatile {@link CheckpointStore} backed by a concurrent map. Positions are lost on restart, so this is meant
 * for tests, the in-memory backend, and as the portable default; use a durable implementation (e.g. the JPA
 * one) in production.
 * <p>
 * It lives in {@code spi} (rather than {@code mem}, where the other {@code InMemory*} classes reside) on
 * purpose: it is the default store for the {@link CatchupSubscription} driver that lives here, and {@code spi}
 * cannot depend on {@code mem}.
 */
@ThreadSafe
public final class InMemoryCheckpointStore implements CheckpointStore {

    private final Map<String, Long> positions = new ConcurrentHashMap<>();

    @Override
    public long readCheckpoint(final StreamId streamId) {
        Contract.requireArgNotNull("streamId", streamId);
        return positions.getOrDefault(streamId.asString(), 0L);
    }

    @Override
    public void updateCheckpoint(final StreamId streamId, final long nextEventNumber) {
        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("nextEventNumber", nextEventNumber, 0);
        positions.put(streamId.asString(), nextEventNumber);
    }

    @Override
    public void resetCheckpoint(final StreamId streamId) {
        Contract.requireArgNotNull("streamId", streamId);
        positions.remove(streamId.asString());
    }

}
