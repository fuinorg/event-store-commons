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

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.activation.MimeTypeParseException;
import javax.json.bind.Jsonb;

import org.apache.commons.io.IOUtils;
import org.eclipse.yasson.FieldAccessStrategy;
import org.junit.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

/**
 * Test for {@link EscEvents} class.
 */
// CHECKSTYLE:OFF Test
public class EscEventsTest {

    @Test
    public final void testMarshalUnmarshalJaxb() throws Exception {

        // PREPARE
        final String expectedXml = IOUtils.toString(this.getClass().getResourceAsStream("/esc-events.xml"));

        // TEST
        final EscEvents testee = unmarshal(expectedXml, EscEvents.class, MyMeta.class, MyEvent.class, Base64Data.class);

        // VERIFY
        assertThat(testee).isNotNull();
        assertThat(testee.getList()).isNotNull();
        assertThat(testee.getList().size()).isEqualTo(2);
        assertThat(testee.getList().get(0).getEventId()).isEqualTo("68616d90-cf72-4c2a-b913-32bf6e6506ed");
        assertThat(testee.getList().get(1).getEventId()).isEqualTo("c198a02e-126e-4fbb-910c-918abf39a4a6");

        // TEST
        final String xml = marshal(testee, EscEvents.class, MyMeta.class, MyEvent.class, Base64Data.class);

        // VERIFY
        final Diff documentDiff = DiffBuilder.compare(expectedXml).withTest(xml).ignoreWhitespace().build();
        assertThat(documentDiff.hasDifferences()).describedAs(documentDiff.toString()).isFalse();

    }

    @Test
    public void testMarshalJsonB() throws Exception {

        // PREPARE
        final String expectedJson = IOUtils.toString(this.getClass().getResourceAsStream("/esc-events.json"));

        final SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();
        typeRegistry.add(MyEvent.SER_TYPE, MyEvent.class);
        typeRegistry.add(MyMeta.SER_TYPE, MyMeta.class);

        final EscEvents events = new EscEvents(createEvent1(), createEvent2());

        final JsonbDeSerializer jsonbDeSer = JsonbDeSerializer.builder()
                .withSerializers(EscSpiUtils.createEscJsonbSerializers())
                .withPropertyVisibilityStrategy(new FieldAccessStrategy())
                .withEncoding(Charset.forName("utf-8"))
                .build();
        jsonbDeSer.init(typeRegistry);
        
        try (final Jsonb jsonb = jsonbDeSer.getJsonb()) {

            // TEST
            final String currentJson = jsonb.toJson(events);
    
            // VERIFY
            assertThatJson(currentJson).isEqualTo(expectedJson);

        }
        
    }

    @Test
    public final void testUnmarshalJsonB() throws Exception {

        // PREPARE
        final String expectedJson = IOUtils.toString(this.getClass().getResourceAsStream("/esc-events.json"));

        final SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();
        typeRegistry.add(MyEvent.SER_TYPE, MyEvent.class);
        typeRegistry.add(MyMeta.SER_TYPE, MyMeta.class);

        final JsonbDeSerializer jsonbDeSer = JsonbDeSerializer.builder()
                .withSerializers(EscSpiUtils.createEscJsonbSerializers())
                .withDeserializers(EscSpiUtils.createEscJsonbDeserializers())
                .withPropertyVisibilityStrategy(new FieldAccessStrategy())
                .withEncoding(Charset.forName("utf-8"))
                .build();
        jsonbDeSer.init(typeRegistry);
        
        try (final Jsonb jsonb = jsonbDeSer.getJsonb()) {

            // TEST
            final EscEvents testee = jsonb.fromJson(expectedJson, EscEvents.class);
    
            // VERIFY
            assertThat(testee.getList()).hasSize(2);
            assertThat(testee.getList().get(0).getEventId()).isEqualTo("b2a936ce-d479-414f-b67f-3df4da383d47");
            assertThat(testee.getList().get(1).getEventId()).isEqualTo("68616d90-cf72-4c2a-b913-32bf6e6506ed");

        }
        
    }
    
    private EscEvent createEvent1() throws MimeTypeParseException {
        final UUID eventId = UUID.fromString("b2a936ce-d479-414f-b67f-3df4da383d47");
        final MyEvent myEvent = new MyEvent(UUID.fromString("b2a936ce-d479-414f-b67f-3df4da383d47"), "Hello, JSON!");
        final MyMeta myMeta = new MyMeta("abc");
        final EnhancedMimeType dataContentType = new EnhancedMimeType("application", "json", Charset.forName("utf-8"), "1");
        final EnhancedMimeType metaContentType = new EnhancedMimeType("application", "json", Charset.forName("utf-8"), "1");
        final EscMeta escMeta = new EscMeta(MyEvent.SER_TYPE.asBaseType(), dataContentType, MyMeta.SER_TYPE.asBaseType(), metaContentType,
                myMeta);
        return new EscEvent(eventId, MyEvent.TYPE.asBaseType(), new DataWrapper(myEvent), new DataWrapper(escMeta));
    }

    private EscEvent createEvent2() throws MimeTypeParseException {
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
