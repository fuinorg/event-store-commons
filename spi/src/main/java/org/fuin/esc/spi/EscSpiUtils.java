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
package org.fuin.esc.spi;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.fuin.esc.api.CommonEvent;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.Nullable;

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
    public static SerializedData serialize(@NotNull final SerializerRegistry registry,
            @NotNull final SerializedDataType type, @Nullable final Object data) {
        Contract.requireArgNotNull("registry", registry);
        Contract.requireArgNotNull("type", type);
        if (data == null) {
            return null;
        }
        final Serializer serializer = registry.getSerializer(type);
        if (serializer == null) {
            throw new IllegalStateException("Couldn't get a serializer for: " + type);
        }
        return new SerializedData(type, serializer.getMimeType(), serializer.marshal(data));
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
    public static <T> T deserialize(@NotNull final DeserializerRegistry registry,
            @NotNull final SerializedData data) {
        Contract.requireArgNotNull("registry", registry);
        Contract.requireArgNotNull("data", data);
        final Deserializer deserializer = registry.getDeserializer(data.getType(), data.getMimeType());
        if (deserializer == null) {
            throw new IllegalStateException("Couldn't get a deserializer for: " + data.getType() + " / "
                    + data.getMimeType());
        }
        return deserializer.unmarshal(data.getRaw(), data.getMimeType());

    }

    /**
     * Returns the mime types shared by all events in the list.
     * 
     * @param registry
     *            Registry used to peek the mime type used to serialize the event.
     * @param commonEvents
     *            List to test.
     * 
     * @return Mime type if all events share the same type or <code>null</code> if there are events with
     *         different mime types.
     */
    public static EnhancedMimeType mimeType(@NotNull final SerializerRegistry registry,
            @NotNull final List<CommonEvent> commonEvents) {

        Contract.requireArgNotNull("registry", registry);
        Contract.requireArgNotNull("commonEvents", commonEvents);

        EnhancedMimeType mimeType = null;
        for (final CommonEvent commonEvent : commonEvents) {
            final Serializer serializer = registry.getSerializer(new SerializedDataType(commonEvent.getType()
                    .asBaseType()));
            if (serializer == null) {
                throw new IllegalStateException("Could not find a serializer for event type '"
                        + commonEvent.getType() + "': " + commonEvent);
            }
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

}
