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
package org.fuin.esc.esgrpc;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.Charset;

import jakarta.json.Json;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.spi.EscMeta;
import org.fuin.esc.api.JsonDeSerializer;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.SimpleSerializerDeserializerRegistry;
import org.junit.jupiter.api.Test;

import com.eventstore.dbclient.EventData;

/**
 * Test for {@link CommonEvent2EventDataConverter} class.
 */
// CHECKSTYLE:OFF Test code
public final class CommonEvent2EventDataConverterTest {

    @Test
    public final void testConvert() {

        // PREPARE
        final EnhancedMimeType targetContentType = EnhancedMimeType.create("application", "json",
                Charset.forName("utf-8"));
        final SimpleSerializerDeserializerRegistry serRegistry = new SimpleSerializerDeserializerRegistry();
        final JsonDeSerializer deserializer = new JsonDeSerializer();
        serRegistry.addSerializer(new SerializedDataType("MyData"), deserializer);
        serRegistry.addSerializer(new SerializedDataType("MyMeta"), deserializer);
        serRegistry.addSerializer(new SerializedDataType(EscMeta.TYPE.asBaseType()), deserializer);

        final EventId id = new EventId();
        final TypeName dataType = new TypeName("MyData");
        final Object data = Json.createObjectBuilder().add("id", 1).add("name", "Peter").build();
        final TypeName metaType = new TypeName("MyMeta");
        final Object meta = Json.createObjectBuilder().add("ip", "127.0.0.1").build();
        final CommonEvent commonEvent = new SimpleCommonEvent(id, dataType, data, metaType, meta);

        final CommonEvent2EventDataConverter testee = new CommonEvent2EventDataConverter(serRegistry,
                targetContentType);

        // TEST
        final EventData eventData = testee.convert(commonEvent);

        // VERIFY
        assertThat(eventData.getEventId()).isEqualTo(commonEvent.getId().asBaseType());
        assertThat(eventData.getEventType()).isEqualTo(commonEvent.getDataType().asBaseType());
        assertThat(new String(eventData.getEventData(), deserializer.getMimeType().getEncoding()))
                .isEqualTo("{\"id\":1,\"name\":\"Peter\"}");
        assertThat(new String(eventData.getUserMetadata(), deserializer.getMimeType().getEncoding())).isEqualTo(
                "{\"data-type\":\"MyData\"," + "\"data-content-type\":\"application/json; encoding=UTF-8\","
                        + "\"meta-type\":\"MyMeta\",\"meta-content-type\":\"application/json; encoding=UTF-8\","
                        + "\"MyMeta\":{\"ip\":\"127.0.0.1\"}}");

    }

}
// CHECKSTYLE:ON
