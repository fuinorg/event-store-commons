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
package org.fuin.esc.jsonb;

import jakarta.json.bind.Jsonb;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.IBase64Data;
import org.fuin.esc.api.IEscMeta;
import org.fuin.esc.api.SimpleSerializedDataTypeRegistry;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link IEscMeta} class.
 */
public class EscMetaTest {

    @Test
    public final void testMarshalJsonB() throws Exception {

        // PREPARE
        final String expectedJson = """
                {
                    "data-type": "MyEvent",
                    "data-content-type":"application/xml; version=1; transfer-encoding=base64; encoding=UTF-8",
                    "meta-type": "MyMeta",
                    "meta-content-type":"application/json; version=1; encoding=UTF-8",
                    "MyMeta":{
                        "user":"abc"
                    }
                }
                """;

        final MyMeta myMeta = new MyMeta("abc");

        final Map<String, String> params = new HashMap<>();
        params.put("transfer-encoding", "base64");
        final EnhancedMimeType dataContentType = new EnhancedMimeType("application", "xml", StandardCharsets.UTF_8, "1", params);
        final EnhancedMimeType metaContentType = new EnhancedMimeType("application", "json", StandardCharsets.UTF_8, "1");
        final IEscMeta escMeta = new EscMeta(MyEvent.SER_TYPE.asBaseType(), dataContentType, MyMeta.SER_TYPE.asBaseType(), metaContentType,
                myMeta);

        final SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();
        typeRegistry.add(EscMeta.SER_TYPE, IEscMeta.class);

        try (final JsonbDeSerializer jsonbDeSer = TestUtils.createJsonbDeSerializer()) {
            TestUtils.initSerDeserializerRegistry(typeRegistry, jsonbDeSer);
            try (final Jsonb jsonb = jsonbDeSer.getJsonb()) {

                // TEST
                final String currentJson = jsonb.toJson(escMeta);

                // VERIFY
                assertThatJson(currentJson).isEqualTo(expectedJson);

            }

        }

    }

    @Test
    public final void testUnmarshalJsonB() throws Exception {

        // PREPARE
        final String expectedJson = """
                {
                    "data-type": "MyEvent",
                    "data-content-type":"application/xml; version=1; transfer-encoding=base64; encoding=UTF-8",
                    "meta-type": "MyMeta",
                    "meta-content-type":"application/json; version=1; encoding=UTF-8",
                    "MyMeta":{
                        "user":"abc"
                    }
                }
                """;

        final SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();
        typeRegistry.add(MyMeta.SER_TYPE, MyMeta.class);

        final Map<String, String> params = new HashMap<>();
        params.put("transfer-encoding", "base64");
        final EnhancedMimeType dataContentType = new EnhancedMimeType("application", "xml", StandardCharsets.UTF_8, "1", params);
        final EnhancedMimeType metaContentType = new EnhancedMimeType("application", "json", StandardCharsets.UTF_8, "1");

        try (final JsonbDeSerializer jsonbDeSer = TestUtils.createJsonbDeSerializer()) {
            TestUtils.initSerDeserializerRegistry(typeRegistry, jsonbDeSer);

            try (final Jsonb jsonb = jsonbDeSer.getJsonb()) {

                // TEST
                final IEscMeta testee = jsonb.fromJson(expectedJson, EscMeta.class);

                // VERIFY
                assertThat(testee.getDataType()).isEqualTo("MyEvent");
                assertThat(testee.getDataContentType()).isEqualTo(dataContentType);
                assertThat(testee.getMetaType()).isEqualTo("MyMeta");
                assertThat(testee.getMetaContentType()).isEqualTo(metaContentType);
                assertThat(testee.getMeta()).isInstanceOf(MyMeta.class);
                final MyMeta myMeta = (MyMeta) testee.getMeta();
                assertThat(myMeta.getUser()).isEqualTo("abc");

            }

        }

    }

    @Test
    public final void testMarshalJsonBBase64() throws Exception {

        // PREPARE
        final String expectedJson = """
                {
                	"data-type": "MyEvent",
                	"data-content-type": "application/xml; encoding=UTF-8",
                	"meta-type": "MyMeta",
                	"meta-content-type": "application/xml; transfer-encoding=base64; encoding=UTF-8",
                	"Base64": "PG15LW1ldGE+PHVzZXI+bWljaGFlbDwvdXNlcj48L215LW1ldGE+"
                }
                                
                """;

        final IBase64Data base64Data = new Base64Data("PG15LW1ldGE+PHVzZXI+bWljaGFlbDwvdXNlcj48L215LW1ldGE+");

        final EnhancedMimeType dataContentType = EnhancedMimeType.create("application/xml; encoding=UTF-8");
        final EnhancedMimeType metaContentType = EnhancedMimeType.create("application/xml; transfer-encoding=base64; encoding=UTF-8");
        final IEscMeta escMeta = new EscMeta(MyEvent.SER_TYPE.asBaseType(), dataContentType, MyMeta.SER_TYPE.asBaseType(), metaContentType,
                base64Data);

        final SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();
        typeRegistry.add(EscMeta.SER_TYPE, IEscMeta.class);

        try (final JsonbDeSerializer jsonbDeSer = TestUtils.createJsonbDeSerializer()) {
            TestUtils.initSerDeserializerRegistry(typeRegistry, jsonbDeSer);

            try (final Jsonb jsonb = jsonbDeSer.getJsonb()) {

                // TEST
                final String currentJson = jsonb.toJson(escMeta);

                // VERIFY
                assertThatJson(currentJson).isEqualTo(expectedJson);

            }

        }

    }

    @Test
    public final void testUnmarshalJsonBBase64() throws Exception {

        // PREPARE
        final String expectedJson = """
                {
                	"data-type": "MyEvent",
                	"data-content-type": "application/xml; encoding=UTF-8",
                	"meta-type": "MyMeta",
                	"meta-content-type": "application/xml; transfer-encoding=base64; encoding=UTF-8",
                	"Base64": "PG15LW1ldGE+PHVzZXI+bWljaGFlbDwvdXNlcj48L215LW1ldGE+"
                }
                                
                """;

        final SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();
        typeRegistry.add(MyMeta.SER_TYPE, MyMeta.class);

        final EnhancedMimeType dataContentType = EnhancedMimeType.create("application/xml; encoding=UTF-8");
        final EnhancedMimeType metaContentType = EnhancedMimeType.create("application/xml; transfer-encoding=base64; encoding=UTF-8");

        try (final JsonbDeSerializer jsonbDeSer = TestUtils.createJsonbDeSerializer()) {
            TestUtils.initSerDeserializerRegistry(typeRegistry, jsonbDeSer);

            try (final Jsonb jsonb = jsonbDeSer.getJsonb()) {

                // TEST
                final IEscMeta testee = jsonb.fromJson(expectedJson, EscMeta.class);

                // VERIFY
                assertThat(testee.getDataType()).isEqualTo("MyEvent");
                assertThat(testee.getDataContentType()).isEqualTo(dataContentType);
                assertThat(testee.getMetaType()).isEqualTo("MyMeta");
                assertThat(testee.getMetaContentType()).isEqualTo(metaContentType);
                assertThat(testee.getMeta()).isInstanceOf(IBase64Data.class);
                final IBase64Data base64Data = (IBase64Data) testee.getMeta();
                assertThat(base64Data.getEncoded()).isEqualTo("PG15LW1ldGE+PHVzZXI+bWljaGFlbDwvdXNlcj48L215LW1ldGE+");

            }

        }

    }

}

