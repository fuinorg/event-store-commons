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
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;

import org.apache.commons.codec.binary.Base64;
import org.fuin.esc.api.TypeName;
import org.fuin.objects4j.common.Contract;

/**
 * Contains some Base64 encoded data.
 */
@XmlRootElement(name = Base64Data.EL_ROOT_NAME)
public final class Base64Data {

    /** Unique XML/JSON root element name of the type. */
    public static final String EL_ROOT_NAME = "Base64"; 

    /** Unique name of the type. */
    public static final TypeName TYPE = new TypeName(EL_ROOT_NAME);
    
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

    /**
     * Converts the object into a JSON object.
     * 
     * @return JSON object.
     */
    public JsonObject toJson() {
        return Json.createObjectBuilder().add("Base64", base64Str).build();
    }

}
