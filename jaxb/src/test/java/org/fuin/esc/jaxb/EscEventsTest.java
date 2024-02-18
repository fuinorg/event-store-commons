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
package org.fuin.esc.jaxb;

import jakarta.activation.MimeTypeParseException;
import org.apache.commons.io.IOUtils;
import org.fuin.esc.api.EnhancedMimeType;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fuin.utils4j.jaxb.JaxbUtils.marshal;
import static org.fuin.utils4j.jaxb.JaxbUtils.unmarshal;

/**
 * Test for {@link EscEvents} class.
 */
public class EscEventsTest {

    @Test
    public final void testMarshalUnmarshalJaxb() throws Exception {

        // PREPARE
        final String expectedXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Events>
                    <Event>
                        <EventId>68616d90-cf72-4c2a-b913-32bf6e6506ed</EventId>
                        <EventType>MyEvent</EventType>
                        <Data>
                            <Base64>
                                eyAibXktZXZlbnQiOiB7ICJpZCI6ICAiNjg2MTZkOTAtY2Y3Mi00YzJhLWI5MTMtMzJiZjZlNjUwNmVkIiwgImRlc2NyaXB0aW9uIjogIkhlbGxvLCBKU09OISIgfSB9
                            </Base64>
                        </Data>
                        <MetaData>
                            <esc-meta>
                                <data-type>MyEvent</data-type>
                                <data-content-type>application/json; version=1; encoding=utf-8;transfer-encoding=base64
                                </data-content-type>
                                <meta-type>MyMeta</meta-type>
                                <meta-content-type>application/xml; version=1; encoding=utf-8</meta-content-type>
                                <my-meta>
                                    <user>abc</user>
                                </my-meta>
                            </esc-meta>
                        </MetaData>
                    </Event>
                    <Event>
                        <EventId>c198a02e-126e-4fbb-910c-918abf39a4a6</EventId>
                        <EventType>MyEvent</EventType>
                        <Data>
                            <my-event id="68616d90-cf72-4c2a-b913-32bf6e6506ed" description="Hello, XML!"/>
                        </Data>
                        <MetaData>
                            <esc-meta>
                                <data-type>MyEvent</data-type>
                                <data-content-type>application/xml; version=1; encoding=utf-8</data-content-type>
                                <meta-type>MyMeta</meta-type>
                                <meta-content-type>application/xml; version=1; encoding=utf-8</meta-content-type>
                                <my-meta>
                                    <user>abc</user>
                                </my-meta>
                            </esc-meta>
                        </MetaData>
                    </Event>
                </Events>
                """;

        // TEST
        final EscEvents testee = unmarshal(expectedXml, EscEvents.class, MyMeta.class, MyEvent.class, Base64Data.class);

        // VERIFY
        assertThat(testee).isNotNull();
        assertThat(testee.getList()).isNotNull();
        assertThat(testee.getList()).hasSize(2);
        assertThat(testee.getList().get(0).getEventId()).isEqualTo("68616d90-cf72-4c2a-b913-32bf6e6506ed");
        assertThat(testee.getList().get(1).getEventId()).isEqualTo("c198a02e-126e-4fbb-910c-918abf39a4a6");

        // TEST
        final String xml = marshal(testee, EscEvents.class, MyMeta.class, MyEvent.class, Base64Data.class);

        // VERIFY
        final Diff documentDiff = DiffBuilder.compare(expectedXml).withTest(xml).ignoreWhitespace().build();
        assertThat(documentDiff.hasDifferences()).describedAs(documentDiff.toString()).isFalse();

    }

    private EscEvent createEvent1() throws MimeTypeParseException {
        final UUID eventId = UUID.fromString("b2a936ce-d479-414f-b67f-3df4da383d47");
        final MyEvent myEvent = new MyEvent(UUID.fromString("b2a936ce-d479-414f-b67f-3df4da383d47"), "Hello, JSON!");
        final MyMeta myMeta = new MyMeta("abc");
        final EnhancedMimeType dataContentType = new EnhancedMimeType("application", "json", StandardCharsets.UTF_8, "1");
        final EnhancedMimeType metaContentType = new EnhancedMimeType("application", "json", StandardCharsets.UTF_8, "1");
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
        final EnhancedMimeType dataContentType = new EnhancedMimeType("application", "json", StandardCharsets.UTF_8, "1", params);
        final EnhancedMimeType metaContentType = new EnhancedMimeType("application", "json", StandardCharsets.UTF_8, "1");
        final EscMeta escMeta = new EscMeta(MyEvent.SER_TYPE.asBaseType(), dataContentType, MyMeta.SER_TYPE.asBaseType(), metaContentType,
                myMeta);
        return new EscEvent(eventId, MyEvent.TYPE.asBaseType(), new DataWrapper(data), new DataWrapper(escMeta));
    }

}
