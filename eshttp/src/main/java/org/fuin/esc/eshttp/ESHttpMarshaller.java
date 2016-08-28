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
package org.fuin.esc.eshttp;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.spi.Base64Data;
import org.fuin.esc.spi.EnhancedMimeType;
import org.fuin.esc.spi.EscMeta;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.Serializer;
import org.fuin.esc.spi.SerializerRegistry;
import org.fuin.objects4j.common.Contract;

/**
 * Marshals data for sending it to the event store.
 */
public final class ESHttpMarshaller {

    /**
     * Creates a list of "application/vnd.eventstore.events(+json/+xml)" entries surrounded by "[]" (JSON) or
     * "&lt;Events&gt;&lt;/Events&gt;" (XML).
     * 
     * @param registry
     *            Registry with known serializers.
     * @param commonEvents
     *            Events to marshal.
     * 
     * @return Single event body.
     */
    @NotNull
    public final String marshal(@NotNull final SerializerRegistry registry,
            @NotNull final List<CommonEvent> commonEvents) {

        Contract.requireArgNotNull("registry", registry);
        Contract.requireArgNotNull("commonEvents", commonEvents);

        final Serializer serializer = registry.getSerializer(EscEvents.SER_TYPE);

        final List<EscEvent> eventList = new ArrayList<>();
        for (final CommonEvent commonEvent : commonEvents) {
            eventList.add(createEscEvent(registry, serializer.getMimeType(), commonEvent));
        }
        final EscEvents events = new EscEvents(eventList);
        final byte[] data = serializer.marshal(events);
        return new String(data, serializer.getMimeType().getEncoding());
    }

    /**
     * Creates a single "application/vnd.eventstore.events(+json/+xml)" entry surrounded by "[]" (JSON) or
     * "&lt;Events&gt;&lt;/Events&gt;" (XML).
     * 
     * @param registry
     *            Registry with known serializers.
     * @param commonEvent
     *            Event to marshal.
     * 
     * @return Single event body.
     */
    @NotNull
    public final String marshal(@NotNull final SerializerRegistry registry, 
            @NotNull final CommonEvent commonEvent) {

        Contract.requireArgNotNull("registry", registry);
        Contract.requireArgNotNull("commonEvent", commonEvent);

        final Serializer serializer = registry.getSerializer(EscEvent.SER_TYPE);

        final EscEvent event = createEscEvent(registry, serializer.getMimeType(), commonEvent);
        final byte[] data = serializer.marshal(event);
        return new String(data, serializer.getMimeType().getEncoding());
    }

    /**
     * Creates a single "application/vnd.eventstore.events+xml" entry.
     * 
     * @param registry
     *            Registry with known serializers.
     * @param targetContentType
     *            Type of content that will be created later on.
     * @param commonEvent
     *            Event to marshal.
     * 
     * @return Single event that has to be surrounded by "&lt;Events&gt;&lt;/Events&gt;".
     */
    private EscEvent createEscEvent(final SerializerRegistry registry,
            final EnhancedMimeType targetContentType, final CommonEvent commonEvent) {

        Contract.requireArgNotNull("registry", registry);
        Contract.requireArgNotNull("targetContentType", targetContentType);
        Contract.requireArgNotNull("commonEvent", commonEvent);

        final EscMeta meta = EscSpiUtils.createEscMeta(registry, targetContentType, commonEvent);

        final String dataType = commonEvent.getDataType().asBaseType();
        final SerializedDataType serDataType = new SerializedDataType(dataType);
        final Serializer dataSerializer = registry.getSerializer(serDataType);
        if (dataSerializer.getMimeType().match(targetContentType)) {
            return new EscEvent(commonEvent.getId().asBaseType(), dataType,
                    new DataWrapper(commonEvent.getData()), new DataWrapper(meta));
        }

        final byte[] serData = dataSerializer.marshal(commonEvent.getData());
        return new EscEvent(commonEvent.getId().asBaseType(), dataType,
                new DataWrapper(new Base64Data(dataType, serData)), new DataWrapper(meta));

    }

}
