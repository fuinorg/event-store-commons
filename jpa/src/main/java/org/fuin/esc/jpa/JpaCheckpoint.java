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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.NotThreadSafe;

/**
 * Durable catch-up position (next event number) for a stream. The primary key is the stream's
 * {@link org.fuin.esc.api.StreamId#asString()}.
 */
@NotThreadSafe
@Table(name = "ESC_CHECKPOINT")
@Entity
public class JpaCheckpoint {

    @Id
    @NotNull
    @Column(name = "STREAM_ID", nullable = false, updatable = false, length = 250)
    private String streamId;

    @Column(name = "NEXT_POS", nullable = false)
    private long nextPos;

    /**
     * Protected default constructor for JPA.
     */
    @SuppressWarnings("NullAway.Init") // Fields are populated by JPA
    protected JpaCheckpoint() { //NOSONAR Ignore uninitialized fields
        super();
    }

    /**
     * Constructor with all mandatory data.
     *
     * @param streamId Unique stream id (the {@code asString()} form).
     * @param nextPos  Number of the next event to read.
     */
    public JpaCheckpoint(final String streamId, final long nextPos) {
        super();
        Contract.requireArgNotNull("streamId", streamId);
        this.streamId = streamId;
        this.nextPos = nextPos;
    }

    /**
     * Returns the unique stream id.
     *
     * @return Stream id.
     */
    public String getStreamId() {
        return streamId;
    }

    /**
     * Returns the number of the next event to read.
     *
     * @return Next event number.
     */
    public long getNextPos() {
        return nextPos;
    }

    /**
     * Sets the number of the next event to read.
     *
     * @param nextPos Next event number.
     */
    public void setNextPos(final long nextPos) {
        this.nextPos = nextPos;
    }

}
