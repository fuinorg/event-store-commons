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

import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.activation.MimeTypeParseException;
import javax.json.Json;
import javax.json.bind.Jsonb;

import org.apache.commons.io.IOUtils;
import org.eclipse.yasson.FieldAccessStrategy;
import org.junit.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

/**
 * Test for {@link EscEvent} class.
 */
// CHECKSTYLE:OFF Test
public class EscEventTest {

    @Test
    public final void testMarshalUnmarshalJaxb() throws Exception {

        // PREPARE
        final String expectedXml = IOUtils.toString(this.getClass().getResourceAsStream("/esc-event.xml"));

        // TEST
        final EscEvent testee = unmarshal(expectedXml, EscEvent.class, EscMeta.class, MyMeta.class, MyEvent.class, Base64Data.class);

        // VERIFY
        assertThat(testee).isNotNull();
        assertThat(testee.getEventId()).isEqualTo("68616d90-cf72-4c2a-b913-32bf6e6506ed");
        assertThat(testee.getEventType()).isEqualTo("MyEvent");
        assertThat(testee.getData()).isNotNull();
        assertThat(testee.getData().getObj()).isInstanceOf(MyEvent.class);
        assertThat(testee.getMeta()).isNotNull();
        assertThat(testee.getMeta().getObj()).isNotNull();
        assertThat(testee.getMeta().getObj()).isInstanceOf(EscMeta.class);

        // TEST
        final String xml = marshal(testee, EscEvent.class, EscMeta.class, MyMeta.class, MyEvent.class, Base64Data.class);

        // VERIFY
        final Diff documentDiff = DiffBuilder.compare(expectedXml).withTest(xml).ignoreWhitespace().build();
        assertThat(documentDiff.hasDifferences()).describedAs(documentDiff.toString()).isFalse();

    }

    @Test
    public final void testToJson() throws Exception {

        // PREPARE
        final String expectedJson = IOUtils.toString(this.getClass().getResourceAsStream("/esc-event.json"));
        final UUID eventId = UUID.fromString("b2a936ce-d479-414f-b67f-3df4da383d47");
        final MyEvent myEvent = new MyEvent(UUID.fromString("b2a936ce-d479-414f-b67f-3df4da383d47"), "Hello, JSON!");
        final MyMeta myMeta = new MyMeta("abc");

        final EnhancedMimeType dataContentType = new EnhancedMimeType("application", "json", Charset.forName("utf-8"), "1");
        final EnhancedMimeType metaContentType = new EnhancedMimeType("application", "json", Charset.forName("utf-8"), "1");
        final EscMeta escMeta = new EscMeta(MyEvent.SER_TYPE.asBaseType(), dataContentType, MyMeta.SER_TYPE.asBaseType(), metaContentType,
                myMeta);
        final DataWrapper dataWrapper = new DataWrapper(myEvent);
        final DataWrapper metaWrapper = new DataWrapper(escMeta);
        final EscEvent event = new EscEvent(eventId, MyEvent.TYPE.asBaseType(), dataWrapper, metaWrapper);

        // TEST
        final StringWriter sw = new StringWriter();
        Json.createWriter(sw).writeObject(event.toJson());
        final String actualJson = sw.toString();

        // VERIFY
        assertThatJson(expectedJson).isEqualTo(actualJson);

    }

    @Test
    public void testMarshalJsonB() throws Exception {

        // PREPARE
        final String expectedJson = IOUtils.toString(this.getClass().getResourceAsStream("/esc-event.json"));

        final SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();
        typeRegistry.add(MyEvent.SER_TYPE, MyEvent.class);
        typeRegistry.add(MyMeta.SER_TYPE, MyMeta.class);

        final UUID eventId = UUID.fromString("b2a936ce-d479-414f-b67f-3df4da383d47");
        final MyEvent myEvent = new MyEvent(UUID.fromString("b2a936ce-d479-414f-b67f-3df4da383d47"), "Hello, JSON!");
        final MyMeta myMeta = new MyMeta("abc");

        final EnhancedMimeType dataContentType = EnhancedMimeType.create("application/json; version=1; encoding=UTF-8");
        final EnhancedMimeType metaContentType = EnhancedMimeType.create("application/json; version=1; encoding=UTF-8");
        final EscMeta escMeta = new EscMeta(MyEvent.SER_TYPE.asBaseType(), dataContentType, MyMeta.SER_TYPE.asBaseType(), metaContentType,
                myMeta);
        final DataWrapper dataWrapper = new DataWrapper(myEvent);
        final DataWrapper metaWrapper = new DataWrapper(escMeta);
        final EscEvent event = new EscEvent(eventId, MyEvent.TYPE.asBaseType(), dataWrapper, metaWrapper);

        final JsonbDeSerializer jsonbDeSer = JsonbDeSerializer.builder()
                .withSerializers(EscSpiUtils.createEscJsonbSerializers())
                .withPropertyVisibilityStrategy(new FieldAccessStrategy())
                .withEncoding(Charset.forName("utf-8"))
                .build();
        jsonbDeSer.init(typeRegistry);
        
        try (final Jsonb jsonb = jsonbDeSer.getJsonb()) {

            // TEST
            final String currentJson = jsonb.toJson(event);

            // VERIFY
            assertThatJson(currentJson).isEqualTo(expectedJson);

        }

    }

    @Test
    public final void testUnmarshalJsonB() throws Exception {

        // PREPARE
        final String expectedJson = IOUtils.toString(this.getClass().getResourceAsStream("/esc-event.json"));

        final SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();
        typeRegistry.add(MyEvent.SER_TYPE, MyEvent.class);
        typeRegistry.add(MyMeta.SER_TYPE, MyMeta.class);

        final JsonbDeSerializer jsonbDeSer = JsonbDeSerializer.builder()
                .withSerializers(EscSpiUtils.createEscJsonbSerializers())
                .withDeserializers(EscSpiUtils.createEscJsonbDeserializers())
                .withPropertyVisibilityStrategy(new FieldAccessStrategy())
                .withEncoding(Charset.forName("utf-8"))
                .build();
        initSerDeserializerRegistry(typeRegistry, jsonbDeSer);
        
        try (final Jsonb jsonb = jsonbDeSer.getJsonb()) {
            
            // TEST
            final EscEvent testee = jsonb.fromJson(expectedJson, EscEvent.class);

            // VERIFY
            assertThat(testee.getEventId()).isEqualTo("b2a936ce-d479-414f-b67f-3df4da383d47");
            assertThat(testee.getEventType()).isEqualTo("MyEvent");
            assertThat(testee.getData().getObj()).isInstanceOf(MyEvent.class);
            final MyEvent myEvent = (MyEvent) testee.getData().getObj();
            assertThat(myEvent.getId()).isEqualTo("b2a936ce-d479-414f-b67f-3df4da383d47");
            assertThat(myEvent.getDescription()).isEqualTo("Hello, JSON!");
            assertThat(testee.getMeta().getObj()).isInstanceOf(EscMeta.class);
            final EscMeta escMeta = (EscMeta) testee.getMeta().getObj();
            assertThat(escMeta.getDataType()).isEqualTo("MyEvent");
            assertThat(escMeta.getDataContentType()).isEqualTo(EnhancedMimeType.create("application/json; version=1; encoding=UTF-8"));
            assertThat(escMeta.getMetaType()).isEqualTo("MyMeta");
            assertThat(escMeta.getMetaContentType()).isEqualTo(EnhancedMimeType.create("application/json; version=1; encoding=UTF-8"));
            assertThat(escMeta.getMeta()).isInstanceOf(MyMeta.class);
            final MyMeta myMeta = (MyMeta) escMeta.getMeta();
            assertThat(myMeta.getUser()).isEqualTo("abc");

        }
    }

    @Test
    public void testMarshalJsonBBase64() throws Exception {

        // PREPARE
        final String expectedJson = IOUtils.toString(this.getClass().getResourceAsStream("/esc-event-base64.json"));

        final SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();
        typeRegistry.add(MyEvent.SER_TYPE, MyEvent.class);
        typeRegistry.add(MyMeta.SER_TYPE, MyMeta.class);

        final EscEvent event = createEventBase64();

        final JsonbDeSerializer jsonbDeSer = JsonbDeSerializer.builder()
                .withSerializers(EscSpiUtils.createEscJsonbSerializers())
                .withDeserializers(EscSpiUtils.createEscJsonbDeserializers())
                .withPropertyVisibilityStrategy(new FieldAccessStrategy())
                .withEncoding(Charset.forName("utf-8"))
                .build();
        initSerDeserializerRegistry(typeRegistry, jsonbDeSer);
        
        try (final Jsonb jsonb = jsonbDeSer.getJsonb()) {

            // TEST
            final String currentJson = jsonb.toJson(event);

            // VERIFY
            assertThatJson(currentJson).isEqualTo(expectedJson);

        }

    }

    @Test
    public final void testUnmarshalJsonBBase64() throws Exception {

        // PREPARE
        final String expectedJson = IOUtils.toString(this.getClass().getResourceAsStream("/esc-event-base64.json"));

        final EscEvent expectedEvent = createEventBase64();

        final SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();
        typeRegistry.add(MyEvent.SER_TYPE, MyEvent.class);
        typeRegistry.add(MyMeta.SER_TYPE, MyMeta.class);

        final JsonbDeSerializer jsonbDeSer = JsonbDeSerializer.builder()
                .withSerializers(EscSpiUtils.createEscJsonbSerializers())
                .withDeserializers(EscSpiUtils.createEscJsonbDeserializers())
                .withPropertyVisibilityStrategy(new FieldAccessStrategy())
                .withEncoding(Charset.forName("utf-8"))
                .build();
        initSerDeserializerRegistry(typeRegistry, jsonbDeSer);
        
        try (final Jsonb jsonb = jsonbDeSer.getJsonb()) {

            // TEST
            final EscEvent testee = jsonb.fromJson(expectedJson, EscEvent.class);

            // VERIFY
            assertThat(testee.getEventId()).isEqualTo(expectedEvent.getEventId());
            assertThat(testee.getEventType()).isEqualTo(expectedEvent.getEventType());
            assertThat(testee.getData().getObj()).isInstanceOf(Base64Data.class);
            final Base64Data base64Data = (Base64Data) testee.getData().getObj();
            assertThat(base64Data.getEncoded()).isEqualTo(((Base64Data) expectedEvent.getData().getObj()).getEncoded());
            assertThat(testee.getMeta().getObj()).isInstanceOf(EscMeta.class);
            final EscMeta actualEscMeta = (EscMeta) testee.getMeta().getObj();
            final EscMeta expectedEscMeta = (EscMeta) expectedEvent.getMeta().getObj();
            assertThat(actualEscMeta.getDataType()).isEqualTo(expectedEscMeta.getDataType());
            assertThat(actualEscMeta.getDataContentType()).isEqualTo(expectedEscMeta.getDataContentType());
            assertThat(actualEscMeta.getMetaType()).isEqualTo(expectedEscMeta.getMetaType());
            assertThat(actualEscMeta.getMetaContentType()).isEqualTo(expectedEscMeta.getMetaContentType());
            assertThat(actualEscMeta.getMeta()).isInstanceOf(MyMeta.class);
            final MyMeta actualMyMeta = (MyMeta) actualEscMeta.getMeta();
            final MyMeta expectedMyMeta = (MyMeta) expectedEscMeta.getMeta();
            assertThat(actualMyMeta.getUser()).isEqualTo(expectedMyMeta.getUser());

        }

    }

    private EscEvent createEventBase64() throws MimeTypeParseException {
        final UUID eventId = UUID.fromString("68616d90-cf72-4c2a-b913-32bf6e6506ed");
        final Base64Data data = new Base64Data(
                "eyAibXktZXZlbnQiOiB7ICJpZCI6ICAiNjg2MTZkOTAtY2Y3Mi00YzJhLWI5MTMtMzJiZjZlNjUwNmVkIiwgImRlc2NyaXB0aW9uIjogIkhlbGxvLCBKU09OISIgfSB9");

        final MyMeta myMeta = new MyMeta("abc");

        final Map<String, String> params = new HashMap<>();
        params.put("transfer-encoding", "base64");
        final EnhancedMimeType dataContentType = new EnhancedMimeType("application", "json", Charset.forName("utf-8"), "1", params);
        final EnhancedMimeType metaContentType = new EnhancedMimeType("application", "json", Charset.forName("utf-8"), "1");
        final EscMeta escMeta = new EscMeta(MyEvent.SER_TYPE.asBaseType(), dataContentType, MyMeta.SER_TYPE.asBaseType(), metaContentType,
                myMeta);
        return new EscEvent(eventId, MyEvent.TYPE.asBaseType(), new DataWrapper(data), new DataWrapper(escMeta));
    }

    /**
     * Creates a registry that connects the type with the appropriate serializer and de-serializer.
     * 
     * @param typeRegistry
     *            Type registry (Mapping from type name to class).
     * @param jsonbDeSer 
     *            JSON-B serializer/deserializer to use.
     */
    private static void initSerDeserializerRegistry(SerializedDataTypeRegistry typeRegistry, 
            JsonbDeSerializer jsonbDeSer) {
        
        SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();

        // Base types always needed
        registry.add(EscEvents.SER_TYPE, "application/json", jsonbDeSer);
        registry.add(EscEvent.SER_TYPE, "application/json", jsonbDeSer);
        registry.add(EscMeta.SER_TYPE, "application/json", jsonbDeSer);
        
        // User defined types
        registry.add(MyMeta.SER_TYPE, "application/json", jsonbDeSer);
        registry.add(MyEvent.SER_TYPE, "application/json", jsonbDeSer);
        
        jsonbDeSer.init(typeRegistry, registry, registry);
        
    }
    
}
// CHECKSTYLE:ON
