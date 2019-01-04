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
import static org.fuin.utils4j.JaxbUtils.marshal;
import static org.fuin.utils4j.JaxbUtils.unmarshal;

import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.activation.MimeTypeParseException;
import javax.json.Json;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbConfig;

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

        final SimpleSerializedDataTypeRegistry registry = new SimpleSerializedDataTypeRegistry();
        registry.add(MyEvent.SER_TYPE, MyEvent.class);
        registry.add(MyMeta.SER_TYPE, MyMeta.class);

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

        final JsonbConfig config = new JsonbConfig().withSerializers(EscSpiUtils.createEscJsonbSerializers(registry))
                .withPropertyVisibilityStrategy(new FieldAccessStrategy());
        try (final Jsonb jsonb = new EscJsonb(config)) {

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

        final EnhancedMimeType dataContentType = new EnhancedMimeType("application", "json", Charset.forName("utf-8"), "1");
        final EnhancedMimeType metaContentType = new EnhancedMimeType("application", "json", Charset.forName("utf-8"), "1");

        final SimpleSerializedDataTypeRegistry registry = new SimpleSerializedDataTypeRegistry();
        registry.add(MyEvent.SER_TYPE, MyEvent.class);
        registry.add(MyMeta.SER_TYPE, MyMeta.class);

        final JsonbConfig config = new JsonbConfig()
                .withSerializers(EscSpiUtils.createEscJsonbSerializers(registry))
                .withDeserializers(EscSpiUtils.createEscJsonbDeserializers(registry))
                .withPropertyVisibilityStrategy(new FieldAccessStrategy());
        try (final Jsonb jsonb = new EscJsonb(config)) {
            
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
            assertThat(escMeta.getDataContentType()).isEqualTo(dataContentType);
            assertThat(escMeta.getMetaType()).isEqualTo("MyMeta");
            assertThat(escMeta.getMetaContentType()).isEqualTo(metaContentType);
            assertThat(escMeta.getMeta()).isInstanceOf(MyMeta.class);
            final MyMeta myMeta = (MyMeta) escMeta.getMeta();
            assertThat(myMeta.getUser()).isEqualTo("abc");

        }
    }

    @Test
    public void testMarshalJsonBBase64() throws Exception {

        // PREPARE
        final String expectedJson = IOUtils.toString(this.getClass().getResourceAsStream("/esc-event-base64.json"));

        final SimpleSerializedDataTypeRegistry registry = new SimpleSerializedDataTypeRegistry();
        registry.add(MyEvent.SER_TYPE, MyEvent.class);
        registry.add(MyMeta.SER_TYPE, MyMeta.class);

        final EscEvent event = createEventBase64();

        final JsonbConfig config = new JsonbConfig().withSerializers(EscSpiUtils.createEscJsonbSerializers(registry))
                .withPropertyVisibilityStrategy(new FieldAccessStrategy());
        try (final Jsonb jsonb = new EscJsonb(config)) {

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

        final SimpleSerializedDataTypeRegistry registry = new SimpleSerializedDataTypeRegistry();
        registry.add(MyEvent.SER_TYPE, MyEvent.class);
        registry.add(MyMeta.SER_TYPE, MyMeta.class);

        final JsonbConfig config = new JsonbConfig().withSerializers(EscSpiUtils.createEscJsonbSerializers(registry))
                .withDeserializers(EscSpiUtils.createEscJsonbDeserializers(registry))
                .withPropertyVisibilityStrategy(new FieldAccessStrategy());
        try (final Jsonb jsonb = new EscJsonb(config)) {

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

}
// CHECKSTYLE:ON
