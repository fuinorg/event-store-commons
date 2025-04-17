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
package org.fuin.esc.esgrpc;

import io.kurrent.dbclient.EventData;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleSerializedDataTypeRegistry;
import org.fuin.esc.api.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.jsonb.BaseTypeFactory;
import org.fuin.esc.jsonb.EscMeta;
import org.fuin.esc.jsonb.JsonbDeSerializer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link CommonEvent2EventDataConverter} class.
 */

public final class CommonEvent2EventDataConverterTest {

    private static final EnhancedMimeType JSON_UTF8 = EnhancedMimeType.create("application", "json", StandardCharsets.UTF_8);

    @Test
    public final void testConvert() throws IOException {

        // PREPARE
        final SimpleSerializedDataTypeRegistry typeRegistry = createTypeRegistry();
        try (final JsonbDeSerializer jsonbDeSer = TestUtils.createJsonbDeSerializer()) {
            TestUtils.initSerDeserializerRegistry(typeRegistry, jsonbDeSer);

            final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
            registry.add(EscMeta.SER_TYPE, JSON_UTF8.getBaseType(), jsonbDeSer);
            registry.add(MyEvent.SER_TYPE, JSON_UTF8.getBaseType(), jsonbDeSer);
            registry.add(MyMeta.SER_TYPE, JSON_UTF8.getBaseType(), jsonbDeSer);

            final MyEvent myEvent = new MyEvent(UUID.fromString("52faeb52-3933-422e-a1f4-4393a6517678"), "Hello, JSON!");
            final MyMeta myMeta = new MyMeta("michael");
            final CommonEvent commonEvent = new SimpleCommonEvent(new EventId(myEvent.getId()), MyEvent.TYPE, myEvent,
                    MyMeta.TYPE, myMeta);

            final CommonEvent2EventDataConverter testee = new CommonEvent2EventDataConverter(registry, new BaseTypeFactory(), JSON_UTF8);

            // TEST
            final EventData eventData = testee.convert(commonEvent);

            // VERIFY
            assertThat(eventData.getEventId()).isEqualTo(commonEvent.getId().asBaseType());
            assertThat(eventData.getEventType()).isEqualTo(commonEvent.getDataType().asBaseType());
            assertThatJson(new String(eventData.getEventData(), StandardCharsets.UTF_8)).isEqualTo("""
                    {"description":"Hello, JSON!","id":"52faeb52-3933-422e-a1f4-4393a6517678"}""");
            assertThatJson(new String(eventData.getUserMetadata(), StandardCharsets.UTF_8)).isEqualTo("""
                    {"data-type":"MyEvent","data-content-type":"application/json; encoding=UTF-8","meta-type":"MyMeta","meta-content-type":"application/json; encoding=UTF-8","MyMeta":{"user":"michael"}}""");

        }

    }

    private static SimpleSerializedDataTypeRegistry createTypeRegistry() {
        final SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();
        typeRegistry.add(EscMeta.SER_TYPE, org.fuin.esc.jsonb.EscMeta.class);
        typeRegistry.add(MyEvent.SER_TYPE, MyEvent.class);
        typeRegistry.add(MyMeta.SER_TYPE, MyMeta.class);
        return typeRegistry;
    }

}

