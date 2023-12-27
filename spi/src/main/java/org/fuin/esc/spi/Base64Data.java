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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlValue;
import org.apache.commons.codec.binary.Base64;
import org.fuin.esc.api.TypeName;
import org.fuin.objects4j.common.Contract;

import java.nio.charset.Charset;

/**
 * Contains some Base64 encoded data.
 */
@XmlRootElement(name = Base64Data.EL_ROOT_NAME)
public final class Base64Data implements ToJsonCapable {

    /** Unique XML/JSON root element name of the type. */
    public static final String EL_ROOT_NAME = "Base64";

    /** Unique name of the type. */
    public static final TypeName TYPE = new TypeName(EL_ROOT_NAME);

    /** Unique name of the serialized type. */
    public static final SerializedDataType SER_TYPE = new SerializedDataType(TYPE.asBaseType());

    @XmlValue
    private String base64Str;

    @XmlTransient
    private byte[] binaryData;

    /**
     * Default constructor for JAXB.
     */
    protected Base64Data() {
        super();
    }

    /**
     * Constructor with Base64 encoded string.
     * 
     * @param base64Str
     *            Base64 encoded data.
     */
    public Base64Data(@NotNull final String base64Str) {
        super();
        Contract.requireArgNotNull("base64Str", base64Str);
        this.base64Str = base64Str;
        this.binaryData = Base64.decodeBase64(base64Str);
    }

    /**
     * Constructor with binary data that will be Base64 encoded.
     * 
     * @param binaryData
     *            Binary data.
     */
    public Base64Data(@NotNull final byte[] binaryData) {
        super();
        Contract.requireArgNotNull("binaryData", binaryData);
        this.base64Str = Base64.encodeBase64String(binaryData);
        this.binaryData = binaryData;
    }

    /**
     * Returns the Base64 encoded data.
     * 
     * @return Base64 string.
     */
    public final String getEncoded() {
        return base64Str;
    }

    /**
     * Returns the decoded data.
     * 
     * @return Binary data.
     */
    public final byte[] getDecoded() {
        if (binaryData == null) {
            binaryData = Base64.decodeBase64(base64Str);
        }
        return binaryData;
    }

    @Override
    public final JsonObject toJson() {
        return Json.createObjectBuilder().add(EL_ROOT_NAME, base64Str).build();
    }

    /**
     * Creates in instance from the given JSON object.
     * 
     * @param jsonObj
     *            Object to read values from.
     * 
     * @return New instance.
     */
    public static Base64Data create(final JsonObject jsonObj) {
        final String base64Str = jsonObj.getString(EL_ROOT_NAME);
        return new Base64Data(base64Str);
    }

    /**
     * Serializes and deserializes a {@link Base64Data} object as JSON. The
     * content type for serialization is always "application/json".
     */
    public static class Base64DataJsonDeSerializer implements SerDeserializer {

        private final JsonDeSerializer jsonDeSer;

        /**
         * Default constructor.
         */
        public Base64DataJsonDeSerializer() {
            super();
            this.jsonDeSer = new JsonDeSerializer();
        }

        /**
         * Constructor with encoding.
         * 
         * @param encoding
         *            Default encoding to use.
         */
        public Base64DataJsonDeSerializer(final Charset encoding) {
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
        public final Base64Data unmarshal(final Object data, final SerializedDataType type, final EnhancedMimeType mimeType) {
            final JsonObject jsonObj = jsonDeSer.unmarshal(data, type, mimeType);
            return Base64Data.create(jsonObj);
        }

    }

}
