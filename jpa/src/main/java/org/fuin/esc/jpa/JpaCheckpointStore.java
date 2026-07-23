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
package org.fuin.esc.jpa;

import jakarta.persistence.EntityManager;
import org.fuin.esc.api.CheckpointStore;
import org.fuin.esc.api.StreamId;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.NotThreadSafe;
import org.jspecify.annotations.Nullable;

/**
 * Relational {@link CheckpointStore} that persists the catch-up position in the {@code ESC_CHECKPOINT} table
 * ({@link JpaCheckpoint}). Like the JPA event store it is bound to a single {@link EntityManager} / transaction
 * and therefore not thread safe; give each thread/transaction its own instance. Advancing the checkpoint in
 * the same transaction that applies the chunk yields at-least-once, gap-free progress.
 */
@NotThreadSafe
public class JpaCheckpointStore implements CheckpointStore {

    private final EntityManager em;

    /**
     * Constructor with all mandatory data.
     *
     * @param em Entity manager bound to the current transaction.
     */
    public JpaCheckpointStore(final EntityManager em) {
        super();
        Contract.requireArgNotNull("em", em);
        this.em = em;
    }

    @Override
    public long readCheckpoint(final StreamId streamId) {
        Contract.requireArgNotNull("streamId", streamId);
        final JpaCheckpoint checkpoint = find(streamId);
        return checkpoint == null ? 0L : checkpoint.getNextPos();
    }

    @Override
    public void updateCheckpoint(final StreamId streamId, final long nextEventNumber) {
        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("nextEventNumber", nextEventNumber, 0);
        final JpaCheckpoint checkpoint = find(streamId);
        if (checkpoint == null) {
            JpaUtils.execute(() -> em.persist(new JpaCheckpoint(streamId.asString(), nextEventNumber)));
        } else {
            checkpoint.setNextPos(nextEventNumber);
        }
    }

    @Override
    public void resetCheckpoint(final StreamId streamId) {
        Contract.requireArgNotNull("streamId", streamId);
        @Nullable final JpaCheckpoint checkpoint = find(streamId);
        if (checkpoint != null) {
            JpaUtils.execute(() -> em.remove(checkpoint));
        }
    }

    /**
     * Looks up the checkpoint row, mapping a connectivity failure to an
     * {@link org.fuin.esc.api.EscConnectionException} like all other database access does.
     *
     * @param streamId Identifier of the stream to read the checkpoint for.
     * @return Checkpoint or {@literal null} if the stream has no checkpoint yet.
     */
    @Nullable
    private JpaCheckpoint find(final StreamId streamId) {
        return JpaUtils.execute(() -> em.find(JpaCheckpoint.class, streamId.asString()));
    }

}
