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
import java.util.UUID;

import org.fuin.esc.api.TypeName;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.Nullable;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * An event structure.
 */
@XmlRootElement(name = EscEvent.EL_ROOT_NAME)
public final class EscEvent implements ToJsonCapable {

    /** Unique XML/JSON root element name of the type. */
    public static final String EL_ROOT_NAME = "Event";

    private static final String EL_EVENT_ID = "EventId";

    private static final String EL_EVENT_TYPE = "EventType";

    private static final String EL_DATA = "Data";

    private static final String EL_META_DATA = "MetaData";

    /** Unique name of the type. */
    public static final TypeName TYPE = new TypeName(EL_ROOT_NAME);

    /** Unique name of the serialized type. */
    public static final SerializedDataType SER_TYPE = new SerializedDataType(TYPE.asBaseType());

    @XmlElement(name = EL_EVENT_ID)
    private String eventId;

    @XmlElement(name = EL_EVENT_TYPE)
    private String eventType;

    @XmlElement(name = EL_DATA)
    private DataWrapper data;

    @XmlElement(name = EL_META_DATA)
    private DataWrapper meta;

    /**
     * Default constructor for JAXB.
     */
    protected EscEvent() {
        super();
    }

    /**
     * Constructor with mandatory data.
     * 
     * @param eventId
     *            Unique event identifier.
     * @param eventType
     *            Unique type name of the event.
     * @param data
     *            The data.
     */
    public EscEvent(@NotNull final UUID eventId, @NotNull final String eventType, @NotNull final DataWrapper data) {
        this(eventId, eventType, data, null);
    }

    /**
     * Constructor with all data.
     * 
     * @param eventId
     *            Unique event identifier.
     * @param eventType
     *            Unique type name of the event.
     * @param data
     *            The data.
     * @param meta
     *            The meta data if available.
     */
    public EscEvent(@NotNull final UUID eventId, @NotNull final String eventType, @NotNull final DataWrapper data,
            @Nullable final DataWrapper meta) {
        super();
        Contract.requireArgNotNull("eventId", eventId);
        Contract.requireArgNotNull("eventType", eventType);
        Contract.requireArgNotNull("data", data);
        this.eventId = eventId.toString();
        this.eventType = eventType;
        this.data = data;
        this.meta = meta;
    }

    /**
     * Returns the unique event identifier.
     * 
     * @return Event ID.
     */
    public final String getEventId() {
        return eventId;
    }

    /**
     * Returns the unique type name of the event.
     * 
     * @return Event type.
     */
    public final String getEventType() {
        return eventType;
    }

    /**
     * Returns the data.
     * 
     * @return Data.
     */
    public final DataWrapper getData() {
        return data;
    }

    /**
     * Returns the meta data.
     * 
     * @return Meta data.
     */
    public final DataWrapper getMeta() {
        return meta;
    }

    @Override
    public final JsonObject toJson() {
        return Json.createObjectBuilder().add(EL_EVENT_ID, eventId).add(EL_EVENT_TYPE, eventType).add(EL_DATA, data.toJson())
                .add(EL_META_DATA, meta.toJson()).build();
    }

    /**
     * Creates in instance from the given JSON object.
     * 
     * @param jsonObj
     *            Object to read values from.
     * 
     * @return New instance.
     */
    public static EscEvent create(final JsonObject jsonObj) {
        final String eventId = jsonObj.getString(EL_EVENT_ID);
        final String eventType = jsonObj.getString(EL_EVENT_TYPE);
        final JsonObject jsonData = jsonObj.getJsonObject(EL_DATA);
        final JsonObject jsonMeta = jsonObj.getJsonObject(EL_META_DATA);
        return new EscEvent(UUID.fromString(eventId), eventType, new DataWrapper(jsonData), new DataWrapper(jsonMeta));
    }

    /**
     * Serializes and deserializes a {@link EscEvent} object as JSON. The content type for serialization is always "application/json".
     */
    public static class EscEventJsonDeSerializer implements SerDeserializer {

        private JsonDeSerializer jsonDeSer;

        /**
         * Constructor with UTF-8 encoding.
         */
        public EscEventJsonDeSerializer() {
            super();
            this.jsonDeSer = new JsonDeSerializer();
        }

        /**
         * Constructor with type and encoding.
         * 
         * @param encoding
         *            Default encoding to use.
         */
        public EscEventJsonDeSerializer(final Charset encoding) {
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
        public final EscEvent unmarshal(final Object data, final SerializedDataType type, final EnhancedMimeType mimeType) {
            final JsonObject jsonObj = jsonDeSer.unmarshal(data, type, mimeType);
            return EscEvent.create(jsonObj);
        }

    }

    /**
     * Adapter to use for JSON-B.
     */
    public static final class JsonbDeSer implements JsonbSerializer<EscEvent>, JsonbDeserializer<EscEvent>, DeserializerRegistryRequired {

        private DeserializerRegistry registry;

        @Override
        public EscEvent deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            final EscEvent escEvent = new EscEvent();
            JsonObject content = null;
            while (parser.hasNext()) {
                final JsonParser.Event event = parser.next();
                if (event == JsonParser.Event.KEY_NAME) {
                    final String field = parser.getString();
                    switch (field) {
                    case EL_EVENT_ID:
                        escEvent.eventId = ctx.deserialize(String.class, parser);
                        break;
                    case EL_EVENT_TYPE:
                        escEvent.eventType = ctx.deserialize(String.class, parser);
                        break;
                    case EL_META_DATA:
                    	parser.next(); // Skip key and deserialize object
                        escEvent.meta = new DataWrapper(ctx.deserialize(EscMeta.class, parser));
                        break;
                    case EL_DATA:
                    	parser.next(); // Skip key and deserialize object
                        content = ctx.deserialize(JsonObject.class, parser);
                        break;
                    default:
                        // ignore
                        break;
                    }
                }
            }

            // Handle data at the end, because meta data is only safely available at the end of the process
            if (content == null) {
                throw new IllegalStateException("Expected content to be set, but was never processed during parse process");
            }
            if (content.containsKey(Base64Data.EL_ROOT_NAME)) {
                escEvent.data = new DataWrapper(new Base64Data(content.getString(Base64Data.EL_ROOT_NAME)));
            } else {
                if (!(escEvent.meta.getObj() instanceof EscMeta)) {
                    throw new IllegalStateException("Expected 'meta.object' to be of type 'EscMeta', but was: " + escEvent.meta.getObj());
                }
                final EscMeta escMeta = (EscMeta) escEvent.meta.getObj();
                final Deserializer deserializer = registry.getDeserializer(new SerializedDataType(escEvent.eventType),
                        escMeta.getDataContentType());
                final Object obj = deserializer.unmarshal(content, new SerializedDataType(escEvent.eventType),
                        escMeta.getDataContentType());
                escEvent.data = new DataWrapper(obj);
            }

            return escEvent;
        }

        @Override
        public void serialize(EscEvent obj, JsonGenerator generator, SerializationContext ctx) {
            generator.writeStartObject();
            if (obj != null) {
                generator.write(EL_EVENT_ID, obj.eventId);
                generator.write(EL_EVENT_TYPE, obj.eventType);
                if (obj.getData().getObj() instanceof Base64Data) {
                    final Base64Data base64Data = (Base64Data) obj.getData().getObj();
                    generator.writeStartObject(EL_DATA);
                    generator.write(Base64Data.EL_ROOT_NAME, base64Data.getEncoded());
                    generator.writeEnd();
                } else {
                    ctx.serialize(EL_DATA, obj.getData().getObj(), generator);
                }
                ctx.serialize(EL_META_DATA, obj.getMeta().getObj(), generator);
            }
            generator.writeEnd();
        }

        @Override
        public void setRegistry(final DeserializerRegistry registry) {
            this.registry = registry;

        }

    }

}
