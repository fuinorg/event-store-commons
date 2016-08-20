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
package org.fuin.esc.esjc;

import static com.github.msemys.esjc.util.EmptyArrays.EMPTY_BYTES;
import static com.github.msemys.esjc.util.UUIDConverter.toUUID;
import static java.time.Instant.ofEpochMilli;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.spi.EnhancedMimeType;
import org.fuin.esc.spi.EscMeta;
import org.fuin.esc.spi.EscMetaJsonDeSerializer;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.esc.spi.JsonDeSerializer;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.SimpleSerializerDeserializerRegistry;
import org.junit.Test;

import com.github.msemys.esjc.EventData;
import com.github.msemys.esjc.RecordedEvent;
import com.github.msemys.esjc.proto.EventStoreClientMessages.EventRecord;
import com.github.msemys.esjc.util.UUIDConverter;
import com.google.protobuf.ByteString;

/**
 * Test for {@link RecordedEvent2CommonEventConverter} class.
 */
// CHECKSTYLE:OFF Test code
public class RecordedEvent2CommonEventConverterTest {

    @Test
    public final void testConvert() throws UnsupportedEncodingException {

        // PREPARE
        final EnhancedMimeType targetContentType = EnhancedMimeType.create("application", "json",
                Charset.forName("utf-8"));
        final SimpleSerializerDeserializerRegistry serRegistry = new SimpleSerializerDeserializerRegistry();
        final JsonDeSerializer deserializer = new JsonDeSerializer();
        serRegistry.add(new SerializedDataType("MyData"), "application/json", deserializer);
        serRegistry.add(new SerializedDataType("MyMeta"), "application/json", deserializer);
        serRegistry.add(new SerializedDataType(EscMeta.TYPE.asBaseType()), "application/json", new EscMetaJsonDeSerializer());

        final EventId id = new EventId();
        final TypeName dataType = new TypeName("MyData");
        final JsonObject data = Json.createObjectBuilder().add("id", 1).add("name", "Peter").build();
        final TypeName metaType = new TypeName("MyMeta");
        final JsonObject meta = Json.createObjectBuilder().add("ip", "127.0.0.1").build();
        final CommonEvent commonEvent = new SimpleCommonEvent(id, dataType, data, metaType, meta);
        
        final EscMeta escMeta = EscSpiUtils.createEscMeta(serRegistry, targetContentType, commonEvent);
        
        // TODO Remove after https://github.com/msemys/esjc/pull/4 is available
        final EventRecord eventRecord = EventRecord.newBuilder()
                .setEventId(ByteString.copyFrom(UUIDConverter.toBytes(id.asBaseType())))
                .setEventStreamId("mystream")
                .setEventNumber(1)
                .setEventType(dataType.asBaseType())
                .setDataContentType(1)
                .setData(ByteString.copyFrom(marshal(data) , "utf-8"))
                .setMetadataContentType(1)
                .setMetadata(ByteString.copyFrom(marshal(escMeta.toJson()) , "utf-8"))
                .setCreated(System.currentTimeMillis())
                .build();
        
        final RecordedEvent recordedEvent = new RecordedEvent(eventRecord);
        
        final RecordedEvent2CommonEventConverter testee = new RecordedEvent2CommonEventConverter(serRegistry);
        
        // TEST
        final CommonEvent result = testee.convert(recordedEvent);
        
        // VERIFY
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getDataType()).isEqualTo(dataType);
        assertThat(result.getMetaType()).isEqualTo(metaType);
        
        assertThat(result.getData()).isInstanceOf(JsonObject.class);
        final JsonObject jsonData = (JsonObject) result.getData();
        assertThat(jsonData.getInt("id")).isEqualTo(1);
        assertThat(jsonData.getString("name")).isEqualTo("Peter");

        assertThat(result.getMeta()).isInstanceOf(JsonObject.class);
        final JsonObject jsonMeta = (JsonObject) result.getMeta();
        assertThat(jsonMeta.getString("ip")).isEqualTo("127.0.0.1");


    }
    
    
    private String marshal(final JsonObject jsonObj) {
        final StringWriter writer = new StringWriter();
        final JsonWriter jsonWriter = Json.createWriter(writer);
        try {
            jsonWriter.write(jsonObj);
        } finally {
            jsonWriter.close();
        }
        return writer.toString();
    }
    
}
//CHECKSTYLE:ON
