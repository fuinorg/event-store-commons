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
package org.fuin.esc.esjc;

import java.nio.charset.Charset;

import javax.validation.constraints.NotNull;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.spi.Base64Data;
import org.fuin.esc.spi.Converter;
import org.fuin.esc.spi.Deserializer;
import org.fuin.esc.spi.DeserializerRegistry;
import org.fuin.esc.spi.EnhancedMimeType;
import org.fuin.esc.spi.EscMeta;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.objects4j.common.Contract;

import com.github.msemys.esjc.RecordedEvent;

/**
 * Converts a {@link RecordedEvent} into {@link CommonEvent}.
 */
public final class RecordedEvent2CommonEventConverter implements Converter<RecordedEvent, CommonEvent> {

    private final DeserializerRegistry deserRegistry;

    /**
     * Constructor with all mandatory data.
     * 
     * @param deserRegistry
     *            Registry used to locate deserializers.
     */
    public RecordedEvent2CommonEventConverter(@NotNull final DeserializerRegistry deserRegistry) {
        super();
        Contract.requireArgNotNull("deserRegistry", deserRegistry);
        this.deserRegistry = deserRegistry;
    }

    /**
     * Converts event data into a common event.
     * 
     * @param eventData
     *            Data to convert.
     * 
     * @return Converted data das event.
     */
    @Override
    public final CommonEvent convert(final RecordedEvent eventData) {

        final EnhancedMimeType escMetaMimeType = metaMimeType(eventData.isJson);
        final SerializedDataType escSerMetaType = new SerializedDataType(EscMeta.TYPE.asBaseType());
        final Deserializer escMetaDeserializer = deserRegistry.getDeserializer(escSerMetaType,
                escMetaMimeType);
        final EscMeta escMeta = escMetaDeserializer.unmarshal(eventData.metadata, escSerMetaType, escMetaMimeType);
        final EnhancedMimeType metaMimeType = escMeta.getMetaContentType();
        final String metaTransferEncoding;
        if (escMeta.getMetaType() == null) {
            metaTransferEncoding = null;
        } else {
            metaTransferEncoding = escMeta.getMetaContentType().getParameter("transfer-encoding");
        }
        final EnhancedMimeType dataMimeType = escMeta.getDataContentType();
        final SerializedDataType serDataType = new SerializedDataType(escMeta.getDataType());
        final Deserializer dataDeserializer = deserRegistry.getDeserializer(serDataType, dataMimeType);
        final String dataTransferEncoding = escMeta.getDataContentType().getParameter("transfer-encoding");
        final Object data = unmarshal(dataTransferEncoding, serDataType, dataDeserializer, dataMimeType, eventData.data,
                metaMimeType, escMetaMimeType);

        final EventId eventId = new EventId(eventData.eventId);
        final TypeName dataType = new TypeName(eventData.eventType);
        if (escMeta.getMetaType() == null) {
            return new SimpleCommonEvent(eventId, dataType, data);
        }
        final TypeName metaType = new TypeName(escMeta.getMetaType());
        final SerializedDataType serMetaType = new SerializedDataType(escMeta.getMetaType());
        final Deserializer metaDeserializer = deserRegistry.getDeserializer(serMetaType, metaMimeType);
        final Object meta = unmarshal(metaTransferEncoding, serMetaType, metaDeserializer, metaMimeType, escMeta.getMeta(),
                metaMimeType, escMetaMimeType);
        return new SimpleCommonEvent(eventId, dataType, data, metaType, meta);
    }

    private Object unmarshal(final String transferEncoding, final SerializedDataType dataType, final Deserializer dataDeserializer,
            final EnhancedMimeType dataMimeType, final Object data, final EnhancedMimeType metaMimeType,
            final EnhancedMimeType escMetaMimeType) {

        if (transferEncoding == null) {
            return dataDeserializer.unmarshal(data, dataType, dataMimeType);
        }

        if (data instanceof Base64Data) {
            final Base64Data base64Data = (Base64Data) data;
            return dataDeserializer.unmarshal(base64Data.getDecoded(), dataType, dataMimeType);
        }

        // Currently only 'base64' is supported
        final Deserializer base64Deserializer = deserRegistry.getDeserializer(Base64Data.SER_TYPE,
                escMetaMimeType);
        final Base64Data base64Data = base64Deserializer.unmarshal(data, Base64Data.SER_TYPE, metaMimeType);
        return dataDeserializer.unmarshal(base64Data.getDecoded(), dataType, dataMimeType);
    }

    private EnhancedMimeType metaMimeType(final boolean json) {
        if (json) {
            return EnhancedMimeType.create("application", "json", Charset.forName("utf-8"));
        }
        return EnhancedMimeType.create("application", "xml", Charset.forName("utf-8"));
    }

    @Override
    public final Class<RecordedEvent> getSourceType() {
        return RecordedEvent.class;
    }

    @Override
    public final Class<CommonEvent> getTargetType() {
        return CommonEvent.class;
    }

}
