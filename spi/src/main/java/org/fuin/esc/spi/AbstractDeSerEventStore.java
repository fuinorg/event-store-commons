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

import javax.validation.constraints.NotNull;

import org.fuin.objects4j.common.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for event stores implementations that require serialization and
 * deserialization of the data and meta data.
 */
public abstract class AbstractDeSerEventStore {

    private static final Logger LOG = LoggerFactory
            .getLogger(AbstractDeSerEventStore.class);

    private final SerializerRegistry serRegistry;

    private final DeserializerRegistry desRegistry;

    /**
     * Constructor with all mandatory data.
     * 
     * @param serRegistry
     *            Registry used to locate serializers.
     * @param desRegistry
     *            Registry used to locate deserializers.
     */
    protected AbstractDeSerEventStore(
            @NotNull final SerializerRegistry serRegistry,
            @NotNull final DeserializerRegistry desRegistry) {
        super();

        Contract.requireArgNotNull("serRegistry", serRegistry);
        Contract.requireArgNotNull("desRegistry", desRegistry);

        this.serRegistry = serRegistry;
        this.desRegistry = desRegistry;
    }

    /**
     * Tries to find a serializer for the given type of object and converts it
     * into a storable data block.
     * 
     * @param type
     *            Type of event.
     * @param data
     *            Event of the given type.
     * 
     * @return Event ready to persist.
     */
    protected final SerializedData serialize(final SerializedDataType type,
            final Object data) {
        if (data == null) {
            return null;
        }
        final Serializer serializer = getSerializerRegistry().getSerializer(
                type);
        if (serializer == null) {
            throw new IllegalStateException("Couldn't get a serializer for: "
                    + type);
        }
        return new SerializedData(type, serializer.getMimeType(),
                serializer.marshal(data));
    }

    /**
     * Tries to find a deserializer for the given data block.
     * 
     * @param data
     *            Persisted data.
     * 
     * @return Unmarshalled event.
     * 
     * @param <T>
     *            Expected type of event.
     */
    protected final <T> T deserialize(final SerializedData data) {
        LOG.debug("Deserialize: type={}, mimeType={}", data.getType(),
                data.getMimeType());
        final Deserializer deserializer = getDeserializerRegistry()
                .getDeserializer(data.getType(), data.getMimeType());
        if (deserializer == null) {
            throw new IllegalStateException("Couldn't get a deserializer for: "
                    + data.getType() + " / " + data.getMimeType());
        }
        return deserializer.unmarshal(data.getRaw(), data.getMimeType());

    }

    /**
     * Returns a registry of serializers.
     * 
     * @return Registry with known serializers.
     */
    @NotNull
    protected final SerializerRegistry getSerializerRegistry() {
        return serRegistry;
    }

    /**
     * Returns a registry of deserializers.
     * 
     * @return Registry with known deserializers.
     */
    @NotNull
    protected final DeserializerRegistry getDeserializerRegistry() {
        return desRegistry;
    }

}
