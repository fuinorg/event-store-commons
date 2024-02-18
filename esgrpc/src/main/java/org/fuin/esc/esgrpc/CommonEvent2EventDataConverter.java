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

import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.EventDataBuilder;
import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.Converter;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.IBase64Data;
import org.fuin.esc.api.IBaseTypeFactory;
import org.fuin.esc.api.IEscMeta;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.Serializer;
import org.fuin.esc.api.SerializerRegistry;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.objects4j.common.Contract;

import java.nio.charset.StandardCharsets;

/**
 * Converts a {@link CommonEvent} into {@link EventData}.
 */
public final class CommonEvent2EventDataConverter implements Converter<CommonEvent, EventData> {

    private static final EnhancedMimeType XML_UTF8 = EnhancedMimeType.create("application", "xml",
            StandardCharsets.UTF_8);

    private static final EnhancedMimeType JSON_UTF8 = EnhancedMimeType.create("application", "json",
            StandardCharsets.UTF_8);

    private final SerializerRegistry serRegistry;

    private final IBaseTypeFactory baseTypeFactory;

    private final EnhancedMimeType targetContentType;

    /**
     * Constructor with all mandatory data.
     *
     * @param serRegistry       Registry used to locate serializers.
     * @param baseTypeFactory   Factory used to create basic types.
     * @param targetContentType Target content type (Allows only 'application/xml'
     *                          or 'application/json' with 'utf-8' encoding).
     */
    public CommonEvent2EventDataConverter(@NotNull final SerializerRegistry serRegistry,
                                          @NotNull final IBaseTypeFactory baseTypeFactory,
                                          final EnhancedMimeType targetContentType) {
        super();
        Contract.requireArgNotNull("serRegistry", serRegistry);
        Contract.requireArgNotNull("baseTypeFactory", baseTypeFactory);
        Contract.requireArgNotNull("targetContentType", targetContentType);
        if (!(targetContentType.matchEncoding(JSON_UTF8) || targetContentType.matchEncoding(XML_UTF8))) {
            throw new IllegalArgumentException(
                    "Only 'application/xml' or 'application/json' with 'utf-8' encoding is allowed, but was: "
                            + targetContentType);
        }
        this.serRegistry = serRegistry;
        this.baseTypeFactory = baseTypeFactory;
        this.targetContentType = targetContentType;
    }

    /**
     * Converts a common event into event data.
     *
     * @param commonEvent Event to convert.
     * @return Converted event as event data.
     */
    @Override
    public EventData convert(final CommonEvent commonEvent) {

        // User's data
        final String dataType = commonEvent.getDataType().asBaseType();
        final SerializedDataType serUserDataType = new SerializedDataType(dataType);
        final Serializer userDataSerializer = serRegistry.getSerializer(serUserDataType);
        final byte[] serUserData = userDataSerializer.marshal(commonEvent.getData(), serUserDataType);
        final byte[] serData;
        if (userDataSerializer.getMimeType().matchEncoding(targetContentType)) {
            serData = serUserData;
        } else {
            final IBase64Data base64data = baseTypeFactory.createBase64Data(serUserData);
            final Serializer base64Serializer = serRegistry.getSerializer(IBase64Data.SER_TYPE);
            serData = base64Serializer.marshal(base64data, IBase64Data.SER_TYPE);
        }

        // EscMeta
        final IEscMeta escMeta = EscSpiUtils.createEscMeta(serRegistry, baseTypeFactory, targetContentType, commonEvent);
        final SerializedDataType escMetaType = new SerializedDataType(IEscMeta.TYPE.asBaseType());
        final Serializer escMetaSerializer = getSerializer(escMetaType);
        final byte[] escSerMeta = escMetaSerializer.marshal(escMeta, escMetaType);

        // Create event data
        if (targetContentType.isJson()) {
            return EventDataBuilder.json(commonEvent.getId().asBaseType(), dataType, serData)
                    .metadataAsBytes(escSerMeta).build();
        }
        return EventDataBuilder.binary(commonEvent.getId().asBaseType(), dataType, serData)
                .metadataAsBytes(escSerMeta).build();

    }

    private Serializer getSerializer(final SerializedDataType serDataType) {
        final Serializer serializer = serRegistry.getSerializer(serDataType);
        if (!serializer.getMimeType().matchEncoding(targetContentType)) {
            throw new IllegalArgumentException("Target content type is '" + targetContentType
                    + "', but serializer returned for " + serDataType + "' was: " + serializer.getMimeType());
        }
        return serializer;
    }

    @Override
    public Class<CommonEvent> getSourceType() {
        return CommonEvent.class;
    }

    @Override
    public Class<EventData> getTargetType() {
        return EventData.class;
    }

}
