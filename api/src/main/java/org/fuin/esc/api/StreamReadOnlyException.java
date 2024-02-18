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

import jakarta.validation.constraints.NotNull;
import org.fuin.objects4j.common.Contract;

import javax.annotation.concurrent.Immutable;

/**
 * Signals that a attempt was made to write to a stream that is read only.
 */
@Immutable
public final class StreamReadOnlyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final StreamId streamId;

    /**
     * Constructor with all data.
     *
     * @param streamId
     *            Unique name of the stream.
     */
    public StreamReadOnlyException(@NotNull final StreamId streamId) {
        super("Stream '" + streamId + "' is read only");
        Contract.requireArgNotNull("streamId", streamId);
        this.streamId = streamId;
    }

    /**
     * Returns the unique identifier of the stream.
     *
     * @return Stream that was not found.
     */
    @NotNull
    public StreamId getStreamId() {
        return streamId;
    }

}
