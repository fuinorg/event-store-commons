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

import jakarta.annotation.Nullable;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.CommonEvent;
import org.fuin.objects4j.common.Contract;
import org.w3c.dom.Node;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.ArrayList;
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
    public static <T> List<T> asList(final T[] array) {
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
        if ((eventsA == null) && (eventsB != null)) {
            return false;
        }
        if ((eventsA != null) && (eventsB == null)) {
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
     * @param targetContentType
     *            Content type that will later be used to serialize the created result.
     * @param commonEvent
     *            Event to create meta information for.
     * 
     * @return New meta instance.
     */
    public static EscMeta createEscMeta(@NotNull final SerializerRegistry registry, @NotNull final EnhancedMimeType targetContentType,
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
            return new EscMeta(dataType, dataContentType);
        }

        final String metaType = commonEvent.getMetaType().asBaseType();
        final SerializedDataType serDataType = new SerializedDataType(metaType);
        final Serializer metaSerializer = registry.getSerializer(serDataType);
        if (metaSerializer.getMimeType().matchEncoding(targetContentType)) {
            return new EscMeta(dataType, dataContentType, metaType, metaSerializer.getMimeType(), commonEvent.getMeta());
        }

        final byte[] serMeta = metaSerializer.marshal(commonEvent.getMeta(), serDataType);
        final EnhancedMimeType metaContentType = contentType(metaSerializer.getMimeType(), targetContentType);
        return new EscMeta(dataType, dataContentType, metaType, metaContentType, new Base64Data(serMeta));

    }

    private static EnhancedMimeType contentType(final EnhancedMimeType sourceContentType, final EnhancedMimeType targetContentType) {
        if (sourceContentType.matchEncoding(targetContentType)) {
            return sourceContentType;
        }
        return EnhancedMimeType.create(sourceContentType.toString() + "; transfer-encoding=base64");
    }

    /**
     * Render a node as string.
     * 
     * @param node
     *            Node to render.
     * 
     * @return XML.
     */
    public static String nodeToString(final Node node) {
        try {
            final Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            final StringWriter sw = new StringWriter();
            t.transform(new DOMSource(node), new StreamResult(sw));
            return sw.toString();
        } catch (final TransformerException ex) {
            throw new RuntimeException("Failed to render node", ex);
        }
    }

    /**
     * Creates all available JSON-B serializers necessary for the ESC implementation.
     * 
     * @return New array with serializers.
     */
    public static JsonbSerializer<?>[] createEscJsonbSerializers() {
        return new JsonbSerializer[] { new EscEvents.JsonbDeSer(), new EscEvent.JsonbDeSer(), new EscMeta.JsonbDeSer() };
    }

    /**
     * Creates all available JSON-B serializers necessary for the ESC implementation.
     * 
     * @return New array with serializers.
     */
    public static JsonbSerializer<?>[] joinJsonbSerializers(final JsonbSerializer<?>[] serializersA,
            final JsonbSerializer<?>... serializersB) {
        return joinJsonbSerializerArrays(serializersA, serializersB);
    }

    /**
     * Creates all available JSON-B serializers necessary for the ESC implementation.
     * 
     * @return New array with serializers.
     */
    public static JsonbSerializer<?>[] joinJsonbSerializerArrays(final JsonbSerializer<?>[]... serializerArrays) {
        final List<JsonbSerializer<?>> all = joinArrays(serializerArrays);
        return all.toArray(new JsonbSerializer<?>[all.size()]);
    }

    /**
     * Creates all available JSON-B deserializers necessary for the ESC implementation.
     * 
     * @return New array with deserializers.
     */
    public static JsonbDeserializer<?>[] createEscJsonbDeserializers() {
        return new JsonbDeserializer[] { new EscEvents.JsonbDeSer(), new EscEvent.JsonbDeSer(), new EscMeta.JsonbDeSer() };
    }

    /**
     * Creates all available JSON-B deserializers necessary for the ESC implementation.
     * 
     * @return New array with deserializers.
     */
    public static JsonbDeserializer<?>[] joinJsonbDeserializers(final JsonbDeserializer<?>[] deserializersA,
            final JsonbDeserializer<?>... deserializersB) {
        return joinJsonbDeserializerArrays(deserializersA, deserializersB);
    }

    /**
     * Creates all available JSON-B deserializers necessary for the ESC implementation.
     * 
     * @return New array with deserializers.
     */
    public static JsonbDeserializer<?>[] joinJsonbDeserializerArrays(final JsonbDeserializer<?>[]... deserializerArrays) {
        final List<JsonbDeserializer<?>> all = joinArrays(deserializerArrays);
        return all.toArray(new JsonbDeserializer<?>[all.size()]);
    }

    /**
     * Creates all available JSON-B serializers necessary for the ESC implementation.
     * 
     * @return New array with serializers.
     */
    @SafeVarargs
    static <T> List<T> joinArrays(final T[]... arrays) {
        final List<T> all = new ArrayList<>();
        for (final T[] array : arrays) {
            for (int j = 0; j < array.length; j++) {
                all.add(array[j]);
            }
        }
        return all;
    }

}
