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

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.fuin.esc.api.TypeName;
import org.fuin.objects4j.common.Contract;

/**
 * A list of events.
 */
@XmlRootElement(name = EscEvents.EL_ROOT_NAME)
public final class EscEvents implements ToJsonCapable {

    /** Unique root element name of the type. */
    protected static final String EL_ROOT_NAME = "Events";

    private static final String EL_EVENT = "Event";

    /** Unique name of the type. */
    public static final TypeName TYPE = new TypeName(EL_ROOT_NAME);

    /** Unique name of the serialized type. */
    public static final SerializedDataType SER_TYPE = new SerializedDataType(TYPE.asBaseType());

    @XmlElement(name = EL_EVENT)
    private List<EscEvent> list;

    /**
     * Default constructor for JAXB.
     */
    protected EscEvents() {
        super();
    }

    /**
     * Constructor with array.
     * 
     * @param events
     *            Event array.
     */
    public EscEvents(@NotNull final EscEvent... events) {
        this(Arrays.asList(events));
    }

    /**
     * Constructor with list.
     * 
     * @param events
     *            Event list.
     */
    public EscEvents(@NotNull final List<EscEvent> events) {
        super();
        Contract.requireArgNotNull("events", events);
        this.list = events;
    }

    /**
     * Returns an immutable event list.
     * 
     * @return Unmodifiable list of events.
     */
    public final List<EscEvent> getList() {
        return Collections.unmodifiableList(list);
    }

    @Override
    public final JsonArray toJson() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        for (final EscEvent event : list) {
            builder.add(event.toJson());
        }
        return builder.build();
    }

    /**
     * Creates in instance from the given JSON array.
     * 
     * @param jsonArray
     *            Array to read values from.
     * 
     * @return New instance.
     */
    public static EscEvents create(final JsonArray jsonArray) {
        final List<EscEvent> events = new ArrayList<>();
        for (final JsonValue jsonValue : jsonArray) {
            if (jsonValue.getValueType() != JsonValue.ValueType.OBJECT) {
                throw new IllegalArgumentException(
                        "All elements in the JSON array must be an JsonObject, but was: " + jsonValue.getValueType());
            }
            events.add(EscEvent.create((JsonObject) jsonValue));
        }
        return new EscEvents(events);
    }

    /**
     * Serializes and deserializes a {@link EscEvents} object as JSON. The content type for serialization is always "application/json".
     */
    public static class EscEventsJsonDeSerializer implements SerDeserializer {

        private JsonDeSerializer jsonDeSer;

        /**
         * Constructor with UTF-8 encoding.
         */
        public EscEventsJsonDeSerializer() {
            super();
            this.jsonDeSer = new JsonDeSerializer();
        }

        /**
         * Constructor with type and encoding.
         * 
         * @param encoding
         *            Default encoding to use.
         */
        public EscEventsJsonDeSerializer(final Charset encoding) {
            super();
            this.jsonDeSer = new JsonDeSerializer(encoding);
        }

        @Override
        public final EnhancedMimeType getMimeType() {
            return jsonDeSer.getMimeType();
        }

        @Override
        public final <T> byte[] marshal(final T obj, final SerializedDataType type) {
            return jsonDeSer.marshal(obj, type);
        }

        @SuppressWarnings("unchecked")
        @Override
        public final EscEvents unmarshal(final Object data, final SerializedDataType type, final EnhancedMimeType mimeType) {
            final JsonArray jsonArray = jsonDeSer.unmarshal(data, type, mimeType);
            return EscEvents.create(jsonArray);
        }

    }

    /**
     * Adapter to use for JSON-B.
     */
    public static final class JsonbDeSer implements JsonbSerializer<EscEvents>, JsonbDeserializer<EscEvents> {

        @Override
        public EscEvents deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            final List<EscEvent> events = new ArrayList<>();
            while (parser.hasNext()) {
                final JsonParser.Event event = parser.next();
                if (event == JsonParser.Event.START_OBJECT) {
                    events.add(ctx.deserialize(EscEvent.class, parser));
                }
            }
            return new EscEvents(events);
        }

        @Override
        public void serialize(EscEvents obj, JsonGenerator generator, SerializationContext ctx) {
            if (obj == null) {
                return;
            }
            generator.writeStartArray();
            if (obj.list != null) {
                for (final EscEvent event : obj.list) {
                    ctx.serialize(event, generator);
                }
            }
            generator.writeEnd();
        }

    }

}
