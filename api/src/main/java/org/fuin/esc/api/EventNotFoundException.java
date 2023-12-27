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

import jakarta.validation.constraints.NotNull;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.Immutable;

/**
 * Signals that an event with the given number was not found.
 */
@Immutable
public final class EventNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final StreamId streamId;

    private final long version;

    /**
     * Constructor with all data.
     * 
     * @param streamId
     *            Unique identifier of the stream.
     * @param eventNumber
     *            Number of the event that was not found.
     */
    public EventNotFoundException(@NotNull final StreamId streamId,
            final long eventNumber) {
        super("Version " + eventNumber + " does not exist on stream '"
                + streamId + "'");

        Contract.requireArgNotNull("streamId", streamId);

        this.streamId = streamId;
        this.version = eventNumber;
    }

    /**
     * Returns the unique ID of the stream.
     * 
     * @return Stream with version that was not found.
     */
    @NotNull
    public final StreamId getStreamId() {
        return streamId;
    }

    /**
     * Returns the number of the event.
     * 
     * @return Number that was not found.
     */
    public final long getVersion() {
        return version;
    }

}
