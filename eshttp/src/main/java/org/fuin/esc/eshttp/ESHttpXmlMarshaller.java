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
package org.fuin.esc.eshttp;

import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.spi.EnhancedMimeType;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.esc.spi.SerializedData;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.SerializerRegistry;
import org.fuin.objects4j.common.Contract;

/**
 * Marshals data in XML format for sending it to the event store.
 */
public final class ESHttpXmlMarshaller implements ESHttpMarshaller {

    private static final String CLOSE_TAG = "</Events>";

    private static final String OPEN_TAG = "<Events>";

    @Override
    public final String marshal(final SerializerRegistry registry, final List<CommonEvent> commonEvents) {

        Contract.requireArgNotNull("registry", registry);
        Contract.requireArgNotNull("commonEvents", commonEvents);

        final StringBuffer sb = new StringBuffer(OPEN_TAG);
        for (final CommonEvent commonEvent : commonEvents) {
            sb.append(marshalIntern(registry, commonEvent));
        }
        sb.append(CLOSE_TAG);
        return sb.toString();
    }

    @Override
    public final String marshal(final SerializerRegistry registry, final CommonEvent commonEvent) {

        Contract.requireArgNotNull("registry", registry);

        final StringBuffer sb = new StringBuffer(OPEN_TAG);
        sb.append(marshalIntern(registry, commonEvent));
        sb.append(CLOSE_TAG);
        return sb.toString();
    }

    /**
     * Creates a single "application/vnd.eventstore.events+xml" entry.
     * 
     * @param registry
     *            Registry with known serializers.
     * @param commonEvent
     *            Event to marshal.
     * 
     * @return Single event that has to be surrounded by "&lt;Events&gt;&lt;/Events&gt;".
     */
    protected final String marshalIntern(final SerializerRegistry registry, final CommonEvent commonEvent) {

        Contract.requireArgNotNull("registry", registry);

        if (commonEvent == null) {
            return null;
        }

        // Serialize data
        final SerializedDataType serDataType = new SerializedDataType(commonEvent.getDataType().asBaseType());
        final SerializedData serData = EscSpiUtils.serialize(registry, serDataType, commonEvent.getData());

        // Serialize meta data
        final SerializedDataType serMetaType;
        if (commonEvent.getMetaType() == null) {
            serMetaType = null;
        } else {
            serMetaType = new SerializedDataType(commonEvent.getMetaType().asBaseType());
        }
        final SerializedData serMeta = EscSpiUtils.serialize(registry, serMetaType, commonEvent.getMeta());

        // Convert into string
        return marshalIntern(commonEvent.getId(), commonEvent.getDataType(), serData,
                commonEvent.getMetaType(), serMeta);

    }

    /**
     * Converts the given data into a string representation capable of being sent to the event store using the
     * HTTP interface.
     * 
     * @param id
     *            Unique event identifier.
     * @param dataType
     *            Unique event type.
     * @param serData
     *            Serialized event data.
     * @param serMeta
     *            Serialized meta data.
     * 
     * @return String with single event.
     */
    protected final String marshalIntern(final EventId id, final TypeName dataType,
            final SerializedData serData, final TypeName metaType, final SerializedData serMeta) {

        Contract.requireArgNotNull("id", id);
        Contract.requireArgNotNull("type", dataType);
        Contract.requireArgNotNull("serData", serData);

        final String metaContentType;
        if (serMeta == null) {
            metaContentType = "";
        } else {
            metaContentType = "<meta-content-type>" + convertToStr(serMeta.getMimeType())
                    + "</meta-content-type>" + "<meta-type>" + metaType + "</meta-type>";
        }
        return "<Event>" + "<EventId>" + id + "</EventId>" + "<EventType>" + dataType + "</EventType>"
                + "<Data>" + convertToStr(serData) + "</Data>" + "<MetaData>" + "<EscUserMeta>"
                + convertToStr(serMeta) + "</EscUserMeta>" + "<EscSysMeta>" + "<data-content-type>"
                + convertToStr(serData.getMimeType()) + "</data-content-type>" + metaContentType
                + "</EscSysMeta>" + "</MetaData>" + "</Event>";
    }

    private String convertToStr(final SerializedData sd) {
        if (sd == null) {
            return "";
        }
        if (sd.getMimeType().getBaseType().equals("application/xml")) {
            return new String(sd.getRaw(), sd.getMimeType().getEncoding());
        }
        return "<Base64>" + Base64.encodeBase64String(sd.getRaw()) + "</Base64>";
    }

    private static String convertToStr(final EnhancedMimeType mimeType) {
        if (mimeType.getBaseType().equals("application/xml")) {
            return mimeType.toString();
        }
        return mimeType.toString() + "; transfer-encoding=base64";
    }

}
