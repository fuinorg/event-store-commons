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

import lt.emasina.esj.message.ClientMessageDtos;
import lt.emasina.esj.message.ClientMessageDtos.ReadEventCompleted.ReadEventResult;
import lt.emasina.esj.message.ReadEventCompleted;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventNotFoundException;
import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamNotFoundException;

/**
 * Handles the asynchronous result from the ESJ interface.
 */
public final class ReadEventHandler extends AbstractCompletedHandler {

    private final StreamId streamId;

    private final int eventNumber;

    /**
     * Constructor with all mandatory data.
     * 
     * @param streamId
     *            Stream identifier.
     * @param eventNumber
     *            Number of the event to read.
     */
    public ReadEventHandler(@NotNull final StreamId streamId,
            final int eventNumber) {
        super();
        this.streamId = streamId;
        this.eventNumber = eventNumber;
    }

    /**
     * Waits for the result and returns it.
     * 
     * @return Result.
     * 
     * @throws EventNotFoundException
     *             An event with the given number was not found in the stream.
     * @throws StreamNotFoundException
     *             A stream with the given name does not exist in the
     *             repository.
     * @throws StreamDeletedException
     *             A stream with the given name previously existed but was
     *             deleted.
     */
    public final ReadEventCompleted getResult() throws StreamDeletedException,
            EventNotFoundException, StreamNotFoundException {
        waitForResult();
        if (getException() != null) {
            throw new RuntimeException(getException());
        } else {
            final ReadEventCompleted completed = (ReadEventCompleted) getMessage();
            verify(completed.getResult());
            return completed;
        }

    }

    private void verify(
            final ClientMessageDtos.ReadEventCompleted.ReadEventResult operationResult)
            throws StreamDeletedException, EventNotFoundException,
            StreamNotFoundException {

        final int result = operationResult.getNumber();
        switch (result) {
        case ReadEventResult.Success_VALUE:
            return;
        case ReadEventResult.NoStream_VALUE:
            throw new StreamNotFoundException(streamId);
        case ReadEventResult.NotFound_VALUE:
            throw new EventNotFoundException(streamId, eventNumber);
        case ReadEventResult.StreamDeleted_VALUE:
            throw new StreamDeletedException(streamId);
        default:
            throw new RuntimeException(operationResult.toString());
        }
    }

}
