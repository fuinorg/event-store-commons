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
import org.fuin.esc.api.SerDeserializerRegistry;
import org.fuin.esc.api.SerializedDataTypeRegistry;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleSerializedDataTypeRegistry;
import org.fuin.esc.api.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.jaxb.EscJaxbUtils;
import org.fuin.esc.jaxb.XmlDeSerializer;
import org.fuin.esc.jsonb.EscJsonbUtils;
import org.fuin.esc.jsonb.JsonbSerDeserializer;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link CommonEvent2EventDataConverter} class.
 */

public final class CommonEvent2EventDataConverterTest extends AbstractTest {

    private static final EnhancedMimeType JSON_UTF8 = EnhancedMimeType.create("application", "json", StandardCharsets.UTF_8);

    private static final EnhancedMimeType XML_UTF8 = EnhancedMimeType.create("application", "xml", StandardCharsets.UTF_8);

    @Test
    public final void testConvertXmlEnvelope() throws IOException {

        // PREPARE

        // Envelope XML
        final SerializedDataTypeRegistry jsonbTypeRegistry = new SimpleSerializedDataTypeRegistry.Builder()
                .add(MyEvent.SER_TYPE, MyEvent.class)
                .add(MyMeta.SER_TYPE, MyMeta.class)
                .build();
        final JsonbSerDeserializer jsonbSerDeser = new JsonbSerDeserializer(getJsonbProvider(), jsonbTypeRegistry, StandardCharsets.UTF_8);
        final XmlDeSerializer xmlSerDeser = EscJaxbUtils.xmlDeSerializerBuilder().build();
        final SerDeserializerRegistry serDeserRegistry =
                EscJaxbUtils.addEscSerDeserializer(new SimpleSerializerDeserializerRegistry.Builder(XML_UTF8), xmlSerDeser)
                        // Meta JSON
                        .add(MyMeta.SER_TYPE, jsonbSerDeser, jsonbSerDeser.getMimeType())
                        // Data JSON
                        .add(MyEvent.SER_TYPE, jsonbSerDeser, jsonbSerDeser.getMimeType())
                        .build();
        TestUtils.register(getJsonbConfig(), serDeserRegistry, serDeserRegistry);

        final MyEvent myEvent = new MyEvent(UUID.fromString("52faeb52-3933-422e-a1f4-4393a6517678"), "Hello, JSON!");
        final MyMeta myMeta = new MyMeta("michael");
        final CommonEvent commonEvent = new SimpleCommonEvent(new EventId(myEvent.getId()), MyEvent.TYPE, myEvent, MyMeta.TYPE, myMeta, null);
        final CommonEvent2EventDataConverter testee = new CommonEvent2EventDataConverter(serDeserRegistry, new org.fuin.esc.jaxb.BaseTypeFactory(), XML_UTF8);

        // TEST
        final EventData eventData = testee.convert(commonEvent);

        // VERIFY
        assertThat(eventData.getEventId()).isEqualTo(commonEvent.getId().asBaseType());
        assertThat(eventData.getEventType()).isEqualTo(commonEvent.getDataType().asBaseType());

        final String actualBase64Xml = new String(eventData.getEventData(), StandardCharsets.UTF_8);
        final String expectedBase64Xml = "<Base64>eyJkZXNjcmlwdGlvbiI6IkhlbGxvLCBKU09OISIsImlkIjoiNTJmYWViNTItMzkzMy00MjJlLWExZjQtNDM5M2E2NTE3Njc4In0=</Base64>";
        final Diff base64Diff = DiffBuilder.compare(expectedBase64Xml).withTest(actualBase64Xml).ignoreWhitespace().build();
        assertThat(base64Diff.hasDifferences()).describedAs(base64Diff.toString()).isFalse();

        final org.fuin.esc.jaxb.Base64Data base64Data = xmlSerDeser.unmarshal(actualBase64Xml.getBytes(StandardCharsets.UTF_8), org.fuin.esc.jaxb.Base64Data.SER_TYPE, XML_UTF8);
        final String expectedDataJson = """
                {
                   "id" : "52faeb52-3933-422e-a1f4-4393a6517678",
                   "description" : "Hello, JSON!"
                }
                """;
        assertThatJson(new String(base64Data.getDecoded(), StandardCharsets.UTF_8)).isEqualTo(expectedDataJson);

        final String expectedEscMetaXml = """
                <esc-meta>
                    <data-type>MyEvent</data-type>
                    <data-content-type>application/json; transfer-encoding=base64; encoding=UTF-8</data-content-type>
                    <meta-type>MyMeta</meta-type>
                    <meta-content-type>application/json; transfer-encoding=base64; encoding=UTF-8</meta-content-type>
                    <Base64>eyJ1c2VyIjoibWljaGFlbCJ9</Base64>
                </esc-meta>
                """;
        final String actualEscMetaXml = new String(eventData.getUserMetadata(), StandardCharsets.UTF_8);
        final Diff escMetaDiff = DiffBuilder.compare(expectedEscMetaXml).withTest(actualEscMetaXml).ignoreWhitespace().build();
        assertThat(escMetaDiff.hasDifferences()).describedAs(escMetaDiff.toString()).isFalse();

        final org.fuin.esc.jaxb.EscMeta escMeta = xmlSerDeser.unmarshal(actualEscMetaXml.getBytes(StandardCharsets.UTF_8), org.fuin.esc.jaxb.EscMeta.SER_TYPE, XML_UTF8);
        assertThat(escMeta.getMeta()).isInstanceOf(org.fuin.esc.jaxb.Base64Data.class);

        final String expectedMyMetaJson = """
                {"user":"michael"}
                """;
        final String actualMyMetaJson = new String(((org.fuin.esc.jaxb.Base64Data) escMeta.getMeta()).getDecoded(), StandardCharsets.UTF_8);
        assertThatJson(actualMyMetaJson).isEqualTo(expectedMyMetaJson);
    }

    @Test
    public final void testConvertJsonEnvelope() throws IOException {

        // PREPARE

        // Envelope JSON
        final SerializedDataTypeRegistry jsonbTypeRegistry = EscJsonbUtils.addEscTypes(new SimpleSerializedDataTypeRegistry.Builder()).build();
        final JsonbSerDeserializer jsonbSerDeser = new JsonbSerDeserializer(getJsonbProvider(), jsonbTypeRegistry, StandardCharsets.UTF_8);
        final XmlDeSerializer xmlSerDeser = XmlDeSerializer.builder().add(MyEvent.class).add(MyMeta.class).build();
        final SerDeserializerRegistry serDeserRegistry =
                EscJsonbUtils.addEscSerDeserializer(new SimpleSerializerDeserializerRegistry.Builder(JSON_UTF8), jsonbSerDeser)
                        // Meta XML
                        .add(MyMeta.SER_TYPE, xmlSerDeser, xmlSerDeser.getMimeType())
                        // Data XML
                        .add(MyEvent.SER_TYPE, xmlSerDeser, xmlSerDeser.getMimeType())
                        .build();
        TestUtils.register(getJsonbConfig(), serDeserRegistry, serDeserRegistry);

        final MyEvent myEvent = new MyEvent(UUID.fromString("52faeb52-3933-422e-a1f4-4393a6517678"), "Hello, JSON!");
        final MyMeta myMeta = new MyMeta("michael");
        final CommonEvent commonEvent = new SimpleCommonEvent(new EventId(myEvent.getId()), MyEvent.TYPE, myEvent, MyMeta.TYPE, myMeta, null);
        final CommonEvent2EventDataConverter testee = new CommonEvent2EventDataConverter(serDeserRegistry, new org.fuin.esc.jsonb.BaseTypeFactory(), JSON_UTF8);

        // TEST
        final EventData eventData = testee.convert(commonEvent);

        // VERIFY
        assertThat(eventData.getEventId()).isEqualTo(commonEvent.getId().asBaseType());
        assertThat(eventData.getEventType()).isEqualTo(commonEvent.getDataType().asBaseType());

        final String expectedBase64Json = """
                {"Base64":"PE15RXZlbnQ+PGlkPjUyZmFlYjUyLTM5MzMtNDIyZS1hMWY0LTQzOTNhNjUxNzY3ODwvaWQ+PGRlc2NyaXB0aW9uPkhlbGxvLCBKU09OITwvZGVzY3JpcHRpb24+PC9NeUV2ZW50Pg=="}
                """;
        final String actualBase64Json = new String(eventData.getEventData(), StandardCharsets.UTF_8);
        assertThatJson(actualBase64Json).isEqualTo(expectedBase64Json);

        final String expectedDataXml = """
                <MyEvent>
                   <id>52faeb52-3933-422e-a1f4-4393a6517678</id>
                   <description>Hello, JSON!</description>
                </MyEvent>
                """;
        final org.fuin.esc.jsonb.Base64Data base64Data = jsonbSerDeser.unmarshal(actualBase64Json.getBytes(StandardCharsets.UTF_8), org.fuin.esc.jsonb.Base64Data.SER_TYPE, JSON_UTF8);
        final String actualDataXml = new String(base64Data.getDecoded(), StandardCharsets.UTF_8);
        final Diff dataDiff = DiffBuilder.compare(expectedDataXml).withTest(actualDataXml).ignoreWhitespace().build();
        assertThat(dataDiff.hasDifferences()).describedAs(dataDiff.toString()).isFalse();

        final String expectedEscMetaJson = """
                {
                  "data-type": "MyEvent",
                  "data-content-type" : "application/xml; transfer-encoding=base64; encoding=UTF-8",
                  "meta-type" : "MyMeta",
                  "meta-content-type" : "application/xml; transfer-encoding=base64; encoding=UTF-8",
                  "Base64" : "PE15TWV0YT48dXNlcj5taWNoYWVsPC91c2VyPjwvTXlNZXRhPg=="
                }
                """;
        final String actualEscMetaJson = new String(eventData.getUserMetadata(), StandardCharsets.UTF_8);
        assertThatJson(actualEscMetaJson).isEqualTo(expectedEscMetaJson);

        final org.fuin.esc.jsonb.EscMeta escMeta = jsonbSerDeser.unmarshal(actualEscMetaJson.getBytes(StandardCharsets.UTF_8), org.fuin.esc.jsonb.EscMeta.SER_TYPE, JSON_UTF8);
        assertThat(escMeta.getMeta()).isInstanceOf(org.fuin.esc.jsonb.Base64Data.class);

        final String expectedMyMetaXml = """
                <MyMeta>
                   <user>michael</user>
                </MyMeta>
                """;
        final String actualMyMetaXml = new String(((org.fuin.esc.jsonb.Base64Data) escMeta.getMeta()).getDecoded(), StandardCharsets.UTF_8);
        final Diff myMetaDiff = DiffBuilder.compare(expectedMyMetaXml).withTest(actualMyMetaXml).ignoreWhitespace().build();
        assertThat(myMetaDiff.hasDifferences()).describedAs(dataDiff.toString()).isFalse();

    }

}

