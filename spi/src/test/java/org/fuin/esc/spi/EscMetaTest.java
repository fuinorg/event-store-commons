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

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.fuin.utils4j.jaxb.JaxbUtils.marshal;
import static org.fuin.utils4j.jaxb.JaxbUtils.unmarshal;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.bind.Jsonb;

import org.apache.commons.io.IOUtils;
import org.eclipse.yasson.FieldAccessStrategy;
import org.junit.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

/**
 * Test for {@link EscMeta} class.
 */
// CHECKSTYLE:OFF Test
public class EscMetaTest {

    @Test
    public final void testMarshalUnmarshalJaxb() throws Exception {

        // PREPARE
        final String expectedXml = IOUtils.toString(this.getClass().getResourceAsStream("/esc-meta.xml"));

        // TEST
        final EscMeta testee = unmarshal(expectedXml, EscMeta.class, MyMeta.class, Base64Data.class);
        final String actualXml = marshal(testee, EscMeta.class, MyMeta.class, Base64Data.class);

        // VERIFY
        final Diff documentDiff = DiffBuilder.compare(expectedXml).withTest(actualXml).ignoreWhitespace().build();
        assertThat(documentDiff.hasDifferences()).describedAs(documentDiff.toString()).isFalse();

    }

    @Test
    public final void testToJson() throws Exception {

        // PREPARE
        final String expectedJson = IOUtils.toString(this.getClass().getResourceAsStream("/esc-meta.json"));

        // TEST
        final EscMeta testee = EscMeta.create(Json.createReader(new StringReader(expectedJson)).readObject());
        final StringWriter sw = new StringWriter();
        Json.createWriter(sw).writeObject(testee.toJson());
        final String actualJson = sw.toString();

        // VERIFY
        assertThatJson(expectedJson).isEqualTo(actualJson);

    }

    @Test
    public final void testMarshalJsonB() throws Exception {

        // PREPARE
        final String expectedJson = IOUtils.toString(this.getClass().getResourceAsStream("/esc-meta.json"));

        final MyMeta myMeta = new MyMeta("abc");

        final Map<String, String> params = new HashMap<>();
        params.put("transfer-encoding", "base64");
        final EnhancedMimeType dataContentType = new EnhancedMimeType("application", "xml", Charset.forName("utf-8"), "1", params);
        final EnhancedMimeType metaContentType = new EnhancedMimeType("application", "json", Charset.forName("utf-8"), "1");
        final EscMeta escMeta = new EscMeta(MyEvent.SER_TYPE.asBaseType(), dataContentType, MyMeta.SER_TYPE.asBaseType(), metaContentType,
                myMeta);

        final SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();
        typeRegistry.add(EscMeta.SER_TYPE, EscMeta.class);
        
        final JsonbDeSerializer jsonbDeSer = JsonbDeSerializer.builder()
                .withSerializers(EscSpiUtils.createEscJsonbSerializers())
                .withPropertyVisibilityStrategy(new FieldAccessStrategy())
                .withEncoding(Charset.forName("utf-8"))
                .build();
        jsonbDeSer.init(typeRegistry);
        try (final Jsonb jsonb = jsonbDeSer.getJsonb()) {

            // TEST
            final String currentJson = jsonb.toJson(escMeta);

            // VERIFY
            assertThatJson(currentJson).isEqualTo(expectedJson);

        }

    }

    @Test
    public final void testUnmarshalJsonB() throws Exception {

        // PREPARE
        final String expectedJson = IOUtils.toString(this.getClass().getResourceAsStream("/esc-meta.json"));

        final SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();
        typeRegistry.add(MyMeta.SER_TYPE, MyMeta.class);

        final Map<String, String> params = new HashMap<>();
        params.put("transfer-encoding", "base64");
        final EnhancedMimeType dataContentType = new EnhancedMimeType("application", "xml", Charset.forName("utf-8"), "1", params);
        final EnhancedMimeType metaContentType = new EnhancedMimeType("application", "json", Charset.forName("utf-8"), "1");

        final JsonbDeSerializer jsonbDeSer = JsonbDeSerializer.builder()
                .withSerializers(new EscMeta.JsonbDeSer())
                .withDeserializers(new EscMeta.JsonbDeSer())
                .withPropertyVisibilityStrategy(new FieldAccessStrategy())
                .withEncoding(Charset.forName("utf-8"))
                .build();
        jsonbDeSer.init(typeRegistry);
        
        try (final Jsonb jsonb = jsonbDeSer.getJsonb()) {

            // TEST
            final EscMeta testee = jsonb.fromJson(expectedJson, EscMeta.class);

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

    @Test
    public final void testMarshalJsonBBase64() throws Exception {

        // PREPARE
        final String expectedJson = IOUtils.toString(this.getClass().getResourceAsStream("/esc-meta-base64.json"));

        final Base64Data base64Data = new Base64Data("PG15LW1ldGE+PHVzZXI+bWljaGFlbDwvdXNlcj48L215LW1ldGE+");

        final EnhancedMimeType dataContentType = EnhancedMimeType.create("application/xml; encoding=UTF-8");
        final EnhancedMimeType metaContentType = EnhancedMimeType.create("application/xml; transfer-encoding=base64; encoding=UTF-8");
        final EscMeta escMeta = new EscMeta(MyEvent.SER_TYPE.asBaseType(), dataContentType, MyMeta.SER_TYPE.asBaseType(), metaContentType,
                base64Data);

        final SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();
        typeRegistry.add(EscMeta.SER_TYPE, EscMeta.class);
        
        final JsonbDeSerializer jsonbDeSer = JsonbDeSerializer.builder()
                .withSerializers(EscSpiUtils.createEscJsonbSerializers())
                .withPropertyVisibilityStrategy(new FieldAccessStrategy())
                .withEncoding(Charset.forName("utf-8"))
                .build();
        jsonbDeSer.init(typeRegistry);
        
        try (final Jsonb jsonb = jsonbDeSer.getJsonb()) {

            // TEST
            final String currentJson = jsonb.toJson(escMeta);

            // VERIFY
            assertThatJson(currentJson).isEqualTo(expectedJson);

        }

    }

    @Test
    public final void testUnmarshalJsonBBase64() throws Exception {

        // PREPARE
        final String expectedJson = IOUtils.toString(this.getClass().getResourceAsStream("/esc-meta-base64.json"));

        final SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();
        typeRegistry.add(MyMeta.SER_TYPE, MyMeta.class);

        final EnhancedMimeType dataContentType = EnhancedMimeType.create("application/xml; encoding=UTF-8");
        final EnhancedMimeType metaContentType = EnhancedMimeType.create("application/xml; transfer-encoding=base64; encoding=UTF-8");

        final JsonbDeSerializer jsonbDeSer = JsonbDeSerializer.builder()
                .withSerializers(new EscMeta.JsonbDeSer())
                .withDeserializers(new EscMeta.JsonbDeSer())
                .withPropertyVisibilityStrategy(new FieldAccessStrategy())
                .withEncoding(Charset.forName("utf-8"))
                .build();
        jsonbDeSer.init(typeRegistry);
        
        try (final Jsonb jsonb = jsonbDeSer.getJsonb()) {

            // TEST
            final EscMeta testee = jsonb.fromJson(expectedJson, EscMeta.class);

            // VERIFY
            assertThat(testee.getDataType()).isEqualTo("MyEvent");
            assertThat(testee.getDataContentType()).isEqualTo(dataContentType);
            assertThat(testee.getMetaType()).isEqualTo("MyMeta");
            assertThat(testee.getMetaContentType()).isEqualTo(metaContentType);
            assertThat(testee.getMeta()).isInstanceOf(Base64Data.class);
            final Base64Data base64Data = (Base64Data) testee.getMeta();
            assertThat(base64Data.getEncoded()).isEqualTo("PG15LW1ldGE+PHVzZXI+bWljaGFlbDwvdXNlcj48L215LW1ldGE+");

        }

    }

}
// CHECKSTYLE:ON
