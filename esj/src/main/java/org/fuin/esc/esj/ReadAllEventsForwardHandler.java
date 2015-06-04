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
package org.fuin.esc.esj;

import javax.validation.constraints.NotNull;

import lt.emasina.esj.message.ClientMessageDtos.ReadStreamEventsCompleted.ReadStreamResult;
import lt.emasina.esj.message.ReadAllEventsForwardCompleted;

import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.objects4j.common.Contract;

/**
 * Handles the asynchronous result from the ESJ interface.
 */
public class ReadAllEventsForwardHandler extends AbstractCompletedHandler {

    private final StreamId streamId;

    /**
     * Constructor with all mandatory data.
     * 
     * @param streamId
     *            Stream identifier.
     */
    public ReadAllEventsForwardHandler(@NotNull final StreamId streamId) {
        super();
        Contract.requireArgNotNull("streamId", streamId);
        this.streamId = streamId;
    }

    /**
     * Waits for the result and returns it.
     * 
     * @return Result.
     * 
     * @throws StreamDeletedException
     *             The stream was deleted.
     * @throws StreamNotFoundException
     *             The given stream is unknown.
     */
    public final ReadAllEventsForwardCompleted getResult()
            throws StreamDeletedException, StreamNotFoundException {
        waitForResult();
        if (getException() != null) {
            throw new RuntimeException(getException());
        } else {
            final ReadAllEventsForwardCompleted completed = (ReadAllEventsForwardCompleted) getMessage();
            verify(completed.getResult());
            return completed;
        }
    }

    private void verify(final ReadStreamResult operationResult)
            throws StreamDeletedException, StreamNotFoundException {

        final int result = operationResult.getNumber();
        switch (result) {
        case ReadStreamResult.Success_VALUE:
            return;
        case ReadStreamResult.StreamDeleted_VALUE:
            throw new StreamDeletedException(streamId);
        case ReadStreamResult.NoStream_VALUE:
            throw new StreamNotFoundException(streamId);
        default:
            throw new RuntimeException(operationResult.toString());
        }
    }

}
