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
package org.fuin.esc.spi;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.Deserializer;
import org.fuin.esc.api.DeserializerRegistry;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.IBaseTypeFactory;
import org.fuin.esc.api.IEscMeta;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.Serializer;
import org.fuin.esc.api.SerializerRegistry;
import org.fuin.objects4j.common.Contract;

import java.util.Arrays;
import java.util.List;

/**
 * Utilities to ease the implementation of service provider implementations.
 */
public final class EscSpiUtils {

    /**
     * Private utility constructor.
     */
    private EscSpiUtils() {
        throw new UnsupportedOperationException("Creating instances of a utility class is not allowed.");
    }

    /**
     * Tries to find a serializer for the given type of object and converts it into a storable data block.
     *
     * @param registry
     *            Registry with known serializers.
     * @param type
     *            Type of event.
     * @param data
     *            Event of the given type.
     *
     * @return Event ready to persist or <code>null</code> if the given data was <code>null</code>.
     */
    @Nullable
    public static SerializedData serialize(@NotNull final SerializerRegistry registry, @NotNull final SerializedDataType type,
                                           @Nullable final Object data) {

        if (data == null) {
            return null;
        }

        Contract.requireArgNotNull("registry", registry);
        Contract.requireArgNotNull("type", type);

        final Serializer serializer = registry.getSerializer(type);
        return new SerializedData(type, serializer.getMimeType(), serializer.marshal(data, type));
    }

    /**
     * Tries to find a deserializer for the given data block.
     *
     * @param registry
     *            Registry with known deserializers.
     * @param data
     *            Persisted data.
     *
     * @return Unmarshalled event.
     *
     * @param <T>
     *            Expected type of event.
     */
    @NotNull
    public static <T> T deserialize(@NotNull final DeserializerRegistry registry, @NotNull final SerializedData data) {
        Contract.requireArgNotNull("registry", registry);
        Contract.requireArgNotNull("data", data);
        final Deserializer deserializer = registry.getDeserializer(data.getType(), data.getMimeType());
        return deserializer.unmarshal(data.getRaw(), data.getType(), data.getMimeType());

    }

    /**
     * Returns the mime types shared by all events in the list.
     *
     * @param registry
     *            Registry used to peek the mime type used to serialize the event.
     * @param commonEvents
     *            List to test.
     *
     * @return Mime type if all events share the same type or <code>null</code> if there are events with different mime types.
     */
    public static EnhancedMimeType mimeType(@NotNull final SerializerRegistry registry, @NotNull final List<CommonEvent> commonEvents) {

        Contract.requireArgNotNull("registry", registry);
        Contract.requireArgNotNull("commonEvents", commonEvents);

        EnhancedMimeType mimeType = null;
        for (final CommonEvent commonEvent : commonEvents) {
            final Serializer serializer = registry.getSerializer(new SerializedDataType(commonEvent.getDataType().asBaseType()));
            if (mimeType == null) {
                mimeType = serializer.getMimeType();
            } else {
                if (!mimeType.equals(serializer.getMimeType())) {
                    return null;
                }
            }
        }
        return mimeType;
    }

    /**
     * Returns the array as a list in a null-safe way.
     *
     * @param array
     *            Array to convert into a list.
     *
     * @return Array list.
     *
     * @param <T>
     *            Type of the array and list.
     */
    @Nullable
    public static <T> List<T> asList(@Nullable final T[] array) {
        if (array == null) {
            return null;
        }
        return Arrays.asList(array);
    }

    /**
     * Tests if both lists contain the same events.
     *
     * @param eventsA
     *            First event list.
     * @param eventsB
     *            Second event list.
     *
     * @return TRUE if both lists have the same size and all event identifiers are equal.
     */
    public static boolean eventsEqual(@Nullable final List<CommonEvent> eventsA, @Nullable final List<CommonEvent> eventsB) {
        if ((eventsA == null) && (eventsB == null)) {
            return true;
        }
        if (eventsA == null) {
            return false;
        }
        if (eventsB == null) {
            return false;
        }
        if (eventsA.size() != eventsB.size()) {
            return false;
        }
        int currentIdx = eventsA.size() - 1;
        int appendIdx = eventsB.size() - 1;
        while (appendIdx >= 0) {
            final CommonEvent current = eventsA.get(currentIdx);
            final CommonEvent append = eventsB.get(appendIdx);
            if (!current.equals(append)) {
                return false;
            }
            currentIdx--;
            appendIdx--;
        }
        return true;
    }

    /**
     * Create meta information for a given a {@link CommonEvent}.
     *
     * @param registry
     *            Registry with serializers.
     * @param baseTypeFactory
     *            Helper class to create required implementations.
     * @param targetContentType
     *            Content type that will later be used to serialize the created result.
     * @param commonEvent
     *            Event to create meta information for.
     *
     * @return New meta instance.
     */
    public static IEscMeta createEscMeta(@NotNull final SerializerRegistry registry,
                                         @NotNull final IBaseTypeFactory baseTypeFactory,
                                         @NotNull final EnhancedMimeType targetContentType,
                                         @Nullable final CommonEvent commonEvent) {

        Contract.requireArgNotNull("registry", registry);
        Contract.requireArgNotNull("targetContentType", targetContentType);

        if (commonEvent == null) {
            return null;
        }

        final String dataType = commonEvent.getDataType().asBaseType();
        final Serializer dataSerializer = registry.getSerializer(new SerializedDataType(dataType));
        final EnhancedMimeType dataContentType = contentType(dataSerializer.getMimeType(), targetContentType);

        if (commonEvent.getMeta() == null) {
            return baseTypeFactory.createEscMeta(dataType, dataContentType, null, null, null);
        }

        final String metaType = commonEvent.getMetaType().asBaseType();
        final SerializedDataType serDataType = new SerializedDataType(metaType);
        final Serializer metaSerializer = registry.getSerializer(serDataType);
        if (metaSerializer.getMimeType().matchEncoding(targetContentType)) {
            return baseTypeFactory.createEscMeta(dataType, dataContentType, metaType, metaSerializer.getMimeType(), commonEvent.getMeta());
        }

        final byte[] serMeta = metaSerializer.marshal(commonEvent.getMeta(), serDataType);
        final EnhancedMimeType metaContentType = contentType(metaSerializer.getMimeType(), targetContentType);
        return baseTypeFactory.createEscMeta(dataType, dataContentType, metaType, metaContentType, baseTypeFactory.createBase64Data(serMeta));

    }

    private static EnhancedMimeType contentType(final EnhancedMimeType sourceContentType, final EnhancedMimeType targetContentType) {
        if (sourceContentType.matchEncoding(targetContentType)) {
            return sourceContentType;
        }
        return EnhancedMimeType.create(sourceContentType + "; transfer-encoding=base64");
    }

}
