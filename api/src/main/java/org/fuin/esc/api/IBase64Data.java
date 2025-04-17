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
package org.fuin.esc.api;

/**
 * Contains some Base64 encoded data.
 */
public interface IBase64Data extends IBaseType {

    /**
     * Unique XML/JSON root element name of the type.
     */
    String EL_ROOT_NAME = "Base64";

    /**
     * Unique name of the type.
     */
    TypeName TYPE = new TypeName(EL_ROOT_NAME);

    /**
     * Unique name of the serialized type.
     */
    SerializedDataType SER_TYPE = new SerializedDataType(TYPE.asBaseType());

    /**
     * Returns the Base64 encoded data.
     *
     * @return Base64 string.
     */
    String getEncoded();

    /**
     * Returns the decoded data.
     *
     * @return Binary data.
     */
    byte[] getDecoded();

}
