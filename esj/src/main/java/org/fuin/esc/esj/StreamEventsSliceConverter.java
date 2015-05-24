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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import lt.emasina.esj.message.ClientMessageDtos.EventRecord;
import lt.emasina.esj.message.ClientMessageDtos.ResolvedIndexedEvent;
import lt.emasina.esj.message.ReadAllEventsForwardCompleted;
import lt.emasina.esj.util.Bytes;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.spi.DeserializerRegistry;
import org.fuin.esc.spi.MetaDataAccessor;
import org.fuin.objects4j.common.Contract;

import com.google.protobuf.ByteString;

/**
 * Converts an ESJ {@link ReadAllEventsForwardCompleted} into an events slice.
 */
public class StreamEventsSliceConverter {

    private final DeserializerRegistry deserRegistry;

    private final MetaDataAccessor metaDataAccessor;

    /**
     * Constructor with mandatory data.
     * 
     * @param deserRegistry
     *            Deserializer registry.
     * @param metaDataAccessor
     *            Used to access an unknown type of meta data.
     */
    @SuppressWarnings("rawtypes")
    public StreamEventsSliceConverter(
            @NotNull final DeserializerRegistry deserRegistry,
            @NotNull final MetaDataAccessor metaDataAccessor) {
        super();
        Contract.requireArgNotNull("deserRegistry", deserRegistry);
        Contract.requireArgNotNull("metaDataAccessor", metaDataAccessor);

        this.deserRegistry = deserRegistry;
        this.metaDataAccessor = metaDataAccessor;
    }

    /**
     * Converts the given completed into a events slice.
     * 
     * @param completed
     *            Completed result to convert.
     * @param fromEventNumber
     *            The starting point (represented as a sequence number) of the
     *            read.
     * 
     * @return Converted events slice.
     */
    public final StreamEventsSlice convert(
            final ReadAllEventsForwardCompleted completed,
            final int fromEventNumber) {

        Contract.requireArgNotNull("completed", completed);

        final CommonEventConverter converter = new CommonEventConverter(
                deserRegistry, metaDataAccessor);
        final List<CommonEvent> events = new ArrayList<CommonEvent>();

        final int nextEventNumber = completed.getNexteventNr();
        final boolean endOfStream = completed.isEndOfStream();
        final List<ResolvedIndexedEvent> eventList = completed.getEventList();

        final Iterator<ResolvedIndexedEvent> it = eventList.iterator();
        while (it.hasNext()) {

            final ResolvedIndexedEvent indexedEvent = it.next();
            final EventRecord event = indexedEvent.getEvent();
            // TODO What is this good for?
            // indexedEvent.getLink();

            final UUID id = Bytes.fromBytes(event.getEventId().toByteArray());
            final String type = event.getEventType();
            final ByteString data = event.getData();
            final ByteString meta = event.getMetadata();

            events.add(converter.convert(id, type, data.toByteArray(),
                    meta.toByteArray()));

        }

        return new StreamEventsSlice(fromEventNumber, events, nextEventNumber,
                endOfStream);
    }

}
