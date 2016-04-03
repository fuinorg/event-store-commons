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

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.Nullable;

/**
 * A structure that contains the user's meta data and the system's meta information.
 */
@XmlRootElement(name = "esc-meta")
public final class EscMeta {

    @XmlElement(name = "data-type")
    private String dataType;

    @XmlElement(name = "data-content-type")
    private String dataContentTypeStr;

    @XmlElement(name = "meta-type")
    private String metaType;

    @XmlElement(name = "meta-content-type")
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
    public EscMeta(@NotNull final String dataType, @NotNull final EnhancedMimeType dataContentType,
            @Nullable final String metaType, @Nullable final EnhancedMimeType metaContentType,
            @Nullable final Object meta) {
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
        builder.add("data-type", dataType);
        builder.add("data-content-type", dataContentTypeStr);
        if (meta == null) {
            return builder.build();
        }
        builder.add("meta-type", metaType);
        builder.add("meta-content-type", metaContentTypeStr);
        if (meta instanceof JsonObject) {
            return builder.add(metaType, (JsonObject) meta).build();
        }
        if (meta instanceof Base64Data) {
            final Base64Data base64data = (Base64Data) meta;
            return builder.add(Base64Data.TYPE, base64data.getEncoded()).build();
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
        final String dataType = jsonObj.getString("data-type");
        final EnhancedMimeType dataContentType = EnhancedMimeType.create(jsonObj
                .getString("data-content-type"));
        if (!jsonObj.containsKey("meta-type")) {
            return new EscMeta(jsonObj.getString("data-type"), dataContentType);
        }
        final String metaType = jsonObj.getString("meta-type");
        final EnhancedMimeType metaContentType = EnhancedMimeType.create(jsonObj
                .getString("meta-content-type"));
        if (metaType.equals(Base64Data.TYPE)) {
            return new EscMeta(dataType, dataContentType, metaType, metaContentType, new Base64Data(
                    jsonObj.getString(metaType)));
        }
        return new EscMeta(dataType, dataContentType, metaType, metaContentType, jsonObj.get(metaType));
    }

}
