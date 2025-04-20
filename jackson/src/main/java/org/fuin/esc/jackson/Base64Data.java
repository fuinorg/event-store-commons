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
package org.fuin.esc.jackson;

import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.HasSerializedDataTypeConstant;
import org.fuin.esc.api.IBase64Data;
import org.fuin.objects4j.common.Contract;

import java.util.Base64;

/**
 * Contains some Base64 encoded data.
 */
@HasSerializedDataTypeConstant
public final class Base64Data implements IBase64Data {

    private String base64Str;

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
     * @param base64Str Base64 encoded data.
     */
    public Base64Data(@NotNull final String base64Str) {
        super();
        Contract.requireArgNotNull("base64Str", base64Str);
        this.base64Str = base64Str;
        this.binaryData = Base64.getDecoder().decode(base64Str);
    }

    /**
     * Constructor with binary data that will be Base64 encoded.
     *
     * @param binaryData Binary data.
     */
    public Base64Data(@NotNull final byte[] binaryData) {
        super();
        Contract.requireArgNotNull("binaryData", binaryData);
        this.base64Str = Base64.getEncoder().encodeToString(binaryData);
        this.binaryData = binaryData;
    }

    /**
     * Returns the Base64 encoded data.
     *
     * @return Base64 string.
     */
    public String getEncoded() {
        return base64Str;
    }

    /**
     * Returns the decoded data.
     *
     * @return Binary data.
     */
    public byte[] getDecoded() {
        if (binaryData == null) {
            binaryData = Base64.getDecoder().decode(base64Str);
        }
        return binaryData;
    }

}
