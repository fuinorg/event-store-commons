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
import lt.emasina.esj.message.ClientMessageDtos.OperationResult;
import lt.emasina.esj.message.DeleteStreamCompleted;

import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamVersionConflictException;
import org.fuin.objects4j.common.Contract;

/**
 * Handles the asynchronous result from the ESJ interface.
 */
public class DeleteStreamHandler extends AbstractCompletedHandler {

    private final StreamId streamId;

    private final int expectedVersion;

    /**
     * Constructor with all mandatory data.
     * 
     * @param streamId
     *            Stream identifier.
     * @param expectedVersion
     *            Expected version.
     */
    public DeleteStreamHandler(@NotNull final StreamId streamId,
            final int expectedVersion) {
        super();
        Contract.requireArgNotNull("streamId", streamId);
        this.streamId = streamId;
        this.expectedVersion = expectedVersion;
    }

    /**
     * Waits for the result and returns it.
     * 
     * @return Result.
     * 
     * @throws StreamVersionConflictException
     *             Writing failed because the next expected version doesn't fit.
     * @throws StreamDeletedException
     *             The stream was deleted.
     */
    public final boolean getResult() throws StreamVersionConflictException,
            StreamDeletedException {
        waitForResult();
        if (getException() != null) {
            throw new RuntimeException(getException());
        } else {
            final DeleteStreamCompleted completed = (DeleteStreamCompleted) getMessage();
            verify(completed.getResult());
            return completed.didSucceed();
        }
    }

    private void verify(final ClientMessageDtos.OperationResult operationResult)
            throws StreamVersionConflictException, StreamDeletedException {
        final int result = operationResult.getNumber();
        switch (result) {
        case OperationResult.Success_VALUE:
            return;
        case OperationResult.WrongExpectedVersion_VALUE:
            // TODO Is there a way to get the expected version back from the
            // event store?
            throw new StreamVersionConflictException(streamId, expectedVersion,
                    null);
        case OperationResult.StreamDeleted_VALUE:
            throw new StreamDeletedException(streamId);
        default:
            throw new RuntimeException(operationResult.toString());
        }
    }

}
