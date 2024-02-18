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

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * Result of subscribing to a stream. The subclasses will contain
 * implementation specific data that is required to handle unsubscribe requests.
 */
public abstract class Subscription implements Serializable {

    private static final long serialVersionUID = 1000L;

    private final StreamId streamId;

    private final Long lastEventNumber;

    /**
     * Constructor with all mandatory data.
     *
     * @param streamId
     *            Unique stream identifier.
     * @param lastEventNumber
     *            Number of the last event written to the stream.
     */
    public Subscription(@NotNull final StreamId streamId,
                        @Nullable final Long lastEventNumber) {
        this.streamId = streamId;
        this.lastEventNumber = lastEventNumber;
    }

    /**
     * Returns the unique stream identifier.
     *
     * @return Stream ID.
     */
    @NotNull
    public final StreamId getStreamId() {
        return streamId;
    }

    /**
     * Returns the number of the last event written to the stream.
     *
     * @return Event number.
     */
    @Nullable
    public final Long getLastEventNumber() {
        return lastEventNumber;
    }

}
