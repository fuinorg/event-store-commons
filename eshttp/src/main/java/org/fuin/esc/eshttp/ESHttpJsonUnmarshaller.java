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
package org.fuin.esc.eshttp;

import javax.json.JsonObject;

import org.apache.commons.codec.binary.Base64;
import org.fuin.esc.spi.Base64Data;
import org.fuin.esc.spi.Deserializer;
import org.fuin.esc.spi.DeserializerRegistry;
import org.fuin.esc.spi.EnhancedMimeType;
import org.fuin.esc.spi.SerializedDataType;

/**
 * Unmarshals data in JSON format after reading it from the event store.
 */
public final class ESHttpJsonUnmarshaller implements ESHttpUnmarshaller {

    @Override
    public final Object unmarshal(final DeserializerRegistry registry, final SerializedDataType dataType,
            final EnhancedMimeType mimeType, final Object data) {

        if (data == null) {
            return null;
        }
        if (!(data instanceof JsonObject)) {
            throw new IllegalArgumentException("Can only unmarshal JsonObject, but was: " + data + " ["
                    + data.getClass().getName() + "]");
        }
        final JsonObject jsonObj = (JsonObject) data;

        final String transferEncodingData = mimeType.getParameter("transfer-encoding");
        if (transferEncodingData == null) {
            // JSON Object or Array
            final Deserializer deSer = registry.getDeserializer(dataType, mimeType);
            return deSer.unmarshal(jsonObj, mimeType);
        }
        final String base64str = jsonObj.getString(Base64Data.EL_ROOT_NAME);
        final byte[] bytes = Base64.decodeBase64(base64str);
        final Deserializer deSer = registry.getDeserializer(dataType, mimeType);
        return deSer.unmarshal(bytes, mimeType);

    }

}
