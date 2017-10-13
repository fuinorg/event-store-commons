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
package org.fuin.esc.spi;

import org.fuin.esc.api.ReadableEventStore;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides some basic functionality for event store implementations.
 */
public abstract class AbstractReadableEventStore implements ReadableEventStore {

    private static final Logger LOG = LoggerFactory
            .getLogger(AbstractReadableEventStore.class);

    @Override
    public final void readAllEventsForward(final StreamId streamId,
            final int startingAtEventNumber, final int chunkSize,
            final ChunkEventHandler handler) {

        int sliceStart = startingAtEventNumber;
        StreamEventsSlice currentSlice;
        do {
            try {
                LOG.debug(
                        "Read slice: streamId={}, sliceStart={}, sliceCount={}",
                        streamId, sliceStart, chunkSize);
                currentSlice = readEventsForward(streamId, sliceStart,
                        chunkSize);
                LOG.debug("Result slice: {}", currentSlice);
            } catch (final StreamNotFoundException ex) {
                // Nothing to read
                LOG.debug(ex.getMessage());
                break;
            }
            if (currentSlice.getEvents().size() > 0) {
                handler.handle(currentSlice);
            }
            sliceStart = currentSlice.getNextEventNumber();
        } while (!currentSlice.isEndOfStream());

    }

}
