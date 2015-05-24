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
import java.util.List;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import lt.emasina.esj.model.Event;
import lt.emasina.esj.model.converter.ByteArrayToByteStringConverter;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.spi.MetaDataBuilder;
import org.fuin.esc.spi.Serializer;
import org.fuin.esc.spi.SerializerRegistry;
import org.fuin.objects4j.common.Contract;

/**
 * Converts one or more common events to ESJ events.
 */
public final class EventConverter {

    /**
     * Name used for querying the serializer/deserializer registry for the meta
     * data type.
     */
    public static final String META_TYPE = "MetaData";

    private final SerializerRegistry serRegistry;

    @SuppressWarnings("rawtypes")
    private final MetaDataBuilder metaDataBuilder;

    /**
     * Constructor with all mandatory data.
     * 
     * @param serRegistry
     *            Serializer registry.
     * @param metaDataBuilder
     *            Builder used to create/add meta data.
     */
    @SuppressWarnings("rawtypes")
    public EventConverter(@NotNull final SerializerRegistry serRegistry,
            @NotNull final MetaDataBuilder metaDataBuilder) {

        Contract.requireArgNotNull("serRegistry", serRegistry);
        Contract.requireArgNotNull("metaDataBuilder", metaDataBuilder);

        this.serRegistry = serRegistry;
        this.metaDataBuilder = metaDataBuilder;
    }

    /**
     * Converts the given common event into a ESJ event.
     * 
     * @param commonEvent
     *            Common event to convert.
     * 
     * @return Converted ESJ event.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public final Event convert(final CommonEvent commonEvent) {

        final UUID id = UUID.fromString(commonEvent.getId());
        final String type = commonEvent.getType();
        final Serializer dataSer = serRegistry.getSerializer(type);
        final Serializer metaSer = serRegistry.getSerializer(META_TYPE);
        final byte[] dataBytes = asDataBytes(commonEvent, dataSer);
        final byte[] metaBytes = asMetaBytes(commonEvent, dataSer, metaSer);

        // Prepare converter
        final ByteArrayToByteStringConverter dataConv = new ByteArrayToByteStringConverter(
                dataSer.getMimeType().isJson());
        final ByteArrayToByteStringConverter metaConv = new ByteArrayToByteStringConverter(
                metaSer.getMimeType().isJson());

        return new Event(id, type, dataBytes, dataConv, metaBytes, metaConv);

    }

    /**
     * Converts a list of common events to ESJ events.
     * 
     * @param commonEvents
     *            List to convert.
     * 
     * @return Converted list.
     */
    @SuppressWarnings("rawtypes")
    public final List<Event> convert(final List<CommonEvent> commonEvents) {
        final EventConverter converter = new EventConverter(serRegistry,
                metaDataBuilder);
        final List<Event> esjEvents = new ArrayList<Event>();
        for (final CommonEvent commonEvent : commonEvents) {
            esjEvents.add(converter.convert(commonEvent));
        }
        return esjEvents;
    }

    private byte[] asDataBytes(final CommonEvent commonEvent,
            final Serializer dataSer) {
        final Object data = commonEvent.getData();
        final byte[] dataBytes = dataSer.marshal(data);
        return dataBytes;
    }

    @SuppressWarnings("unchecked")
    private byte[] asMetaBytes(final CommonEvent commonEvent,
            final Serializer dataSer, final Serializer metaSer) {
        metaDataBuilder.init(commonEvent.getMeta());
        metaDataBuilder.add("content-type", dataSer.getMimeType().toString());
        final Object meta = metaDataBuilder.build();
        final byte[] metaBytes = metaSer.marshal(meta);
        return metaBytes;
    }

}
