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

import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParser;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.fuin.esc.api.TypeName;
import org.fuin.objects4j.common.Contract;

/**
 * A structure that contains the user's meta data and the system's meta information.
 */
@XmlRootElement(name = EscMeta.EL_ROOT_NAME)
public final class EscMeta implements ToJsonCapable {

    /** Unique XML/JSON root element name of the type. */
    public static final String EL_ROOT_NAME = "esc-meta";

    /** Unique name of the type. */
    public static final TypeName TYPE = new TypeName(EscMeta.class.getSimpleName());

    /** Unique name of the serialized type. */
    public static final SerializedDataType SER_TYPE = new SerializedDataType(TYPE.asBaseType());

    private static final String EL_DATA_TYPE = "data-type";

    private static final String EL_DATA_CONTENT_TYPE = "data-content-type";

    private static final String EL_META_TYPE = "meta-type";

    private static final String EL_META_CONTENT_TYPE = "meta-content-type";

    @XmlElement(name = EL_DATA_TYPE)
    private String dataType;

    @XmlElement(name = EL_DATA_CONTENT_TYPE)
    private String dataContentTypeStr;

    @XmlElement(name = EL_META_TYPE)
    private String metaType;

    @XmlElement(name = EL_META_CONTENT_TYPE)
    private String metaContentTypeStr;

    @XmlAnyElement(lax = true)
    private Object meta;

    @XmlTransient
    private EnhancedMimeType dataContentType;

    @XmlTransient
    private EnhancedMimeType metaContentType;

    /**
     * Default constructor for JAXB.
     */
    protected EscMeta() {
        super();
    }

    /**
     * Constructor with all mandatory data.
     * 
     * @param dataType
     *            Type of the data.
     * @param dataContentType
     *            Content type of the data.
     */
    public EscMeta(@NotNull final String dataType, @NotNull final EnhancedMimeType dataContentType) {
        this(dataType, dataContentType, null, null, null);
    }

    /**
     * Constructor with all data.
     * 
     * @param dataType
     *            Type of the data.
     * @param dataContentType
     *            Type of the data.
     * @param metaType
     *            Unique name of the meta data type if available.
     * @param metaContentType
     *            Type of the meta data if meta data is available.
     * @param meta
     *            Meta data object if available.
     */
    public EscMeta(@NotNull final String dataType, @NotNull final EnhancedMimeType dataContentType, @Nullable final String metaType,
            @Nullable final EnhancedMimeType metaContentType, @Nullable final Object meta) {
        super();
        Contract.requireArgNotNull("dataType", dataType);
        Contract.requireArgNotNull("dataContentType", dataContentType);

        this.dataType = dataType;
        this.dataContentType = dataContentType;
        this.dataContentTypeStr = dataContentType.toString();
        this.metaType = metaType;
        this.metaContentType = metaContentType;
        if (metaContentType != null) {
            this.metaContentTypeStr = metaContentType.toString();
        }
        this.meta = meta;
    }

    /**
     * Returns the unique name of the data type.
     * 
     * @return Data type.
     */
    @Nullable
    public final String getDataType() {
        return dataType;
    }

    /**
     * Returns the type of the data.
     * 
     * @return Data type.
     */
    @NotNull
    public final EnhancedMimeType getDataContentType() {
        if (dataContentType == null) {
            dataContentType = EnhancedMimeType.create(dataContentTypeStr);
        }
        return dataContentType;
    }

    /**
     * Returns the unique name of the meta data type if available.
     * 
     * @return Meta type.
     */
    @Nullable
    public final String getMetaType() {
        return metaType;
    }

    /**
     * Returns the type of the meta data if meta data is available.
     * 
     * @return Meta type.
     */
    @Nullable
    public final EnhancedMimeType getMetaContentType() {
        if ((metaContentType == null) && (metaContentTypeStr != null)) {
            metaContentType = EnhancedMimeType.create(metaContentTypeStr);
        }
        return metaContentType;
    }

    /**
     * Returns the meta data object.
     * 
     * @return Meta data object.
     */
    @NotNull
    public final Object getMeta() {
        return meta;
    }

    /**
     * Converts the object into a JSON object.
     * 
     * @return JSON object.
     */
    public JsonObject toJson() {
        final JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(EL_DATA_TYPE, dataType);
        builder.add(EL_DATA_CONTENT_TYPE, dataContentTypeStr);
        if (meta == null) {
            return builder.build();
        }
        builder.add(EL_META_TYPE, metaType);
        builder.add(EL_META_CONTENT_TYPE, metaContentTypeStr);
        if (meta instanceof JsonObject) {
            final JsonObject jo = (JsonObject) meta;
            return builder.add(metaType, jo).build();
        }
        if (meta instanceof ToJsonCapable) {
            final ToJsonCapable tjc = (ToJsonCapable) meta;
            return builder.add(metaType, tjc.toJson()).build();
        }
        if (meta instanceof Base64Data) {
            final Base64Data base64data = (Base64Data) meta;
            return builder.add(metaType, base64data.getEncoded()).build();
        }
        throw new IllegalStateException("Unknown meta object type: " + meta.getClass());
    }

    /**
     * Creates in instance from the given JSON object.
     * 
     * @param jsonObj
     *            Object to read values from.
     * 
     * @return New instance.
     */
    public static EscMeta create(final JsonObject jsonObj) {
        final String dataType = jsonObj.getString(EL_DATA_TYPE);
        final EnhancedMimeType dataContentType = EnhancedMimeType.create(jsonObj.getString(EL_DATA_CONTENT_TYPE));
        if (!jsonObj.containsKey(EL_META_TYPE)) {
            return new EscMeta(jsonObj.getString(EL_DATA_TYPE), dataContentType);
        }
        final String metaType = jsonObj.getString(EL_META_TYPE);
        final EnhancedMimeType metaContentType = EnhancedMimeType.create(jsonObj.getString(EL_META_CONTENT_TYPE));
        final String transferEncoding = metaContentType.getParameter("transfer-encoding");
        if (transferEncoding == null) {
            return new EscMeta(dataType, dataContentType, metaType, metaContentType, jsonObj.get(metaType));
        }
        final JsonObject base64obj = jsonObj.getJsonObject(metaType);
        return new EscMeta(dataType, dataContentType, metaType, metaContentType,
                new Base64Data(base64obj.getString(Base64Data.EL_ROOT_NAME)));
    }

    /**
     * Adapter to use for JSON-B.
     */
    public static final class JsonbDeSer implements JsonbSerializer<EscMeta>, JsonbDeserializer<EscMeta> {

        private final SerializedDataTypeRegistry registry;

        /**
         * Constructor with mandatory data.
         * 
         * @param registry
         *            Registry for type lookup.
         */
        public JsonbDeSer(final SerializedDataTypeRegistry registry) {
            super();
            this.registry = registry;
        }

        @Override
        public EscMeta deserialize(final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
            final EscMeta escMeta = new EscMeta();
            while (parser.hasNext()) {
                final JsonParser.Event event = parser.next();
                if (event == JsonParser.Event.KEY_NAME) {
                    final String field = parser.getString();
                    switch (field) {
                    case EL_DATA_TYPE:
                        escMeta.dataType = ctx.deserialize(String.class, parser);
                        break;
                    case EL_DATA_CONTENT_TYPE:
                        escMeta.dataContentType = EnhancedMimeType.create(ctx.deserialize(String.class, parser));
                        break;
                    case EL_META_TYPE:
                        escMeta.metaType = ctx.deserialize(String.class, parser);
                        break;
                    case EL_META_CONTENT_TYPE:
                        escMeta.metaContentType = EnhancedMimeType.create(ctx.deserialize(String.class, parser));
                        break;
                    default:
                        // meta
                        final Class<?> clasz = registry.findClass(new SerializedDataType(escMeta.metaType));
                        escMeta.meta = ctx.deserialize(clasz, parser);
                        break;
                    }
                }
            }
            return escMeta;
        }

        @Override
        public void serialize(EscMeta obj, JsonGenerator generator, SerializationContext ctx) {

            generator.writeStartObject();
            generator.write(EL_DATA_TYPE, obj.dataType);
            generator.write(EL_DATA_CONTENT_TYPE, obj.dataContentTypeStr);
            if (obj.meta != null) {
                generator.write(EL_META_TYPE, obj.metaType);
                generator.write(EL_META_CONTENT_TYPE, obj.metaContentTypeStr);
                if (obj.meta instanceof Base64Data) {
                    final Base64Data base64data = (Base64Data) obj.meta;
                    generator.write(obj.metaType, base64data.getEncoded());
                } else {
                    ctx.serialize(obj.metaType, obj.meta, generator);
                }
            }
            generator.writeEnd();

        }

    }

}
