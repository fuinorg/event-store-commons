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
package org.fuin.esc.eshttp;

import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.commons.codec.binary.Base64;
import org.fuin.esc.spi.Deserializer;
import org.fuin.esc.spi.DeserializerRegistry;
import org.fuin.esc.spi.EnhancedMimeType;
import org.fuin.esc.spi.SerializedDataType;

/**
 * Unmarshals data in JSON format after reading it from the event store.
 */
public final class ESHttpJsonUnmarshaller implements ESHttpUnmarshaller {

    @Override
    public final Object unmarshal(DeserializerRegistry registry, SerializedDataType dataType,
            EnhancedMimeType mimeType, Object data) {

        if (data == null) {
            return null;
        }
        if (!(data instanceof JsonValue)) {
            throw new IllegalArgumentException("Can only unmarshal JsonValue, but was: " + data);
        }
        final JsonValue jsonValue = (JsonValue) data;

        final String transferEncodingData = mimeType.getParameter("transfer-encoding");
        if (transferEncodingData == null) {
            // JSON Object or Array
            final Deserializer deSer = getDeserializer(registry, dataType, mimeType);
            return deSer.unmarshal(jsonValue, mimeType);
        }        
        if (jsonValue.getValueType() != JsonValue.ValueType.STRING) {
            throw new IllegalStateException("Got transferEncodingData='" + transferEncodingData
                    + "' and expected JSON String, but was: " + jsonValue);
        }
        final JsonString jsonStr = (JsonString) jsonValue;
        final String base64str = jsonStr.getString();
        final byte[] bytes = Base64.decodeBase64(base64str);
        final Deserializer deSer = getDeserializer(registry, dataType, mimeType);
        return deSer.unmarshal(bytes, mimeType);

    }

    private Deserializer getDeserializer(DeserializerRegistry registry, SerializedDataType dataType,
            EnhancedMimeType mimeType) {
        final Deserializer deSer = registry.getDeserializer(dataType, mimeType);
        if (deSer == null) {
            throw new IllegalStateException("Couldn't find a deserializer for: " + dataType + " + "
                    + mimeType);
        }
        return deSer;
    }

}
