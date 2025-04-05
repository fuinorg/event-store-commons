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
package org.fuin.esc.esgrpc;

import io.kurrent.dbclient.RecordedEvent;
import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.Converter;
import org.fuin.esc.api.Deserializer;
import org.fuin.esc.api.DeserializerRegistry;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.IBase64Data;
import org.fuin.esc.api.IEscMeta;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.TypeName;
import org.fuin.objects4j.common.Contract;

import java.nio.charset.StandardCharsets;

/**
 * Converts a {@link RecordedEvent} into {@link CommonEvent}.
 */
public final class RecordedEvent2CommonEventConverter implements Converter<RecordedEvent, CommonEvent> {

    private final DeserializerRegistry deserRegistry;

    /**
     * Constructor with all mandatory data.
     *
     * @param deserRegistry Registry used to locate deserializers.
     */
    public RecordedEvent2CommonEventConverter(@NotNull final DeserializerRegistry deserRegistry) {
        super();
        Contract.requireArgNotNull("deserRegistry", deserRegistry);
        this.deserRegistry = deserRegistry;
    }

    /**
     * Converts event data into a common event.
     *
     * @param eventData Data to convert.
     *
     * @return Converted data das event.
     */
    @Override
    public CommonEvent convert(final RecordedEvent eventData) {

        final EnhancedMimeType escMetaMimeType = metaMimeType(eventData.getContentType().equals("application/json"));
        final SerializedDataType escSerMetaType = new SerializedDataType(IEscMeta.TYPE.asBaseType());
        final Deserializer escMetaDeserializer = deserRegistry.getDeserializer(escSerMetaType, escMetaMimeType);
        final IEscMeta escMeta = escMetaDeserializer.unmarshal(eventData.getUserMetadata(), escSerMetaType, escMetaMimeType);
        final EnhancedMimeType metaMimeType = escMeta.getMetaContentType();
        final String metaTransferEncoding;
        if (escMeta.getMetaType() == null || escMeta.getMetaContentType() == null) {
            metaTransferEncoding = null;
        } else {
            metaTransferEncoding = escMeta.getMetaContentType().getParameter("transfer-encoding");
        }
        final EnhancedMimeType dataMimeType = escMeta.getDataContentType();
        final SerializedDataType serDataType = new SerializedDataType(escMeta.getDataType());
        final Deserializer dataDeserializer = deserRegistry.getDeserializer(serDataType, dataMimeType);
        final String dataTransferEncoding = escMeta.getDataContentType().getParameter("transfer-encoding");
        final Object data = unmarshal(dataTransferEncoding, serDataType, dataDeserializer, dataMimeType,
                eventData.getEventData(), metaMimeType, escMetaMimeType);

        final EventId eventId = new EventId(eventData.getEventId());
        final TypeName dataType = new TypeName(eventData.getEventType());
        if (escMeta.getMetaType() == null) {
            return new SimpleCommonEvent(eventId, dataType, data);
        }
        final TypeName metaType = new TypeName(escMeta.getMetaType());
        final SerializedDataType serMetaType = new SerializedDataType(escMeta.getMetaType());
        final Deserializer metaDeserializer = deserRegistry.getDeserializer(serMetaType, metaMimeType);
        final Object meta = unmarshal(metaTransferEncoding, serMetaType, metaDeserializer, metaMimeType,
                escMeta.getMeta(), metaMimeType, escMetaMimeType);
        return new SimpleCommonEvent(eventId, dataType, data, metaType, meta);
    }

    private Object unmarshal(final String transferEncoding, final SerializedDataType dataType,
                             final Deserializer dataDeserializer, final EnhancedMimeType dataMimeType, final Object data,
                             final EnhancedMimeType metaMimeType, final EnhancedMimeType escMetaMimeType) {

        if (transferEncoding == null) {
            return dataDeserializer.unmarshal(data, dataType, dataMimeType);
        }

        if (data instanceof IBase64Data) {
            final IBase64Data base64Data = (IBase64Data) data;
            return dataDeserializer.unmarshal(base64Data.getDecoded(), dataType, dataMimeType);
        }

        // Currently only 'base64' is supported
        final Deserializer base64Deserializer = deserRegistry.getDeserializer(IBase64Data.SER_TYPE, escMetaMimeType);
        final IBase64Data base64Data = base64Deserializer.unmarshal(data, IBase64Data.SER_TYPE, metaMimeType);
        return dataDeserializer.unmarshal(base64Data.getDecoded(), dataType, dataMimeType);
    }

    private EnhancedMimeType metaMimeType(final boolean json) {
        if (json) {
            return EnhancedMimeType.create("application", "json", StandardCharsets.UTF_8);
        }
        return EnhancedMimeType.create("application", "xml", StandardCharsets.UTF_8);
    }

    @Override
    public Class<RecordedEvent> getSourceType() {
        return RecordedEvent.class;
    }

    @Override
    public Class<CommonEvent> getTargetType() {
        return CommonEvent.class;
    }

}
