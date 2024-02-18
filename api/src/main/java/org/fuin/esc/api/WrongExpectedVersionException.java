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
import org.fuin.objects4j.common.Contract;

import javax.annotation.concurrent.Immutable;

/**
 * Signals a conflict between an expected and an actual version.
 */
@Immutable
public final class WrongExpectedVersionException extends RuntimeException {

    private static final long serialVersionUID = 1000L;

    private final StreamId streamId;

    private final Long expected;

    private final Long actual;

    /**
     * Constructor with all data.
     *
     * @param streamId
     *            Unique name of the stream.
     * @param expected
     *            Expected version.
     * @param actual
     *            Actual version.
     */
    public WrongExpectedVersionException(@NotNull final StreamId streamId,
                                         @NotNull final Long expected, @Nullable final Long actual) {
        super("Expected version " + expected + " for stream '" + streamId
                + (actual == null ? "'" : "', but was " + actual));

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgNotNull("expected", streamId);
        this.streamId = streamId;
        this.expected = expected;
        this.actual = actual;
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

    /**
     * Returns the expected version.
     *
     * @return Expected version.
     */
    @NotNull
    public Long getExpected() {
        return expected;
    }

    /**
     * Returns the actual version.
     *
     * @return Actual version or <code>null</code> if the event store didn't
     *         tell us what version it expected.
     */
    @Nullable
    public Long getActual() {
        return actual;
    }

}
