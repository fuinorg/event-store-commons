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

import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.fuin.utils4j.jaxb.MarshallerBuilder;
import org.fuin.utils4j.jaxb.UnmarshallerBuilder;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

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
        final Unmarshaller unmarshaller = new UnmarshallerBuilder().addClassesToBeBound(EscEvents.class, MyMeta.class, MyEvent.class, Base64Data.class).build();
        final EscEvents testee = unmarshal(unmarshaller, expectedXml);

        // VERIFY
        assertThat(testee).isNotNull();
        assertThat(testee.getList()).isNotNull();
        assertThat(testee.getList()).hasSize(2);
        assertThat(testee.getList().get(0).getEventId()).isEqualTo("68616d90-cf72-4c2a-b913-32bf6e6506ed");
        assertThat(testee.getList().get(1).getEventId()).isEqualTo("c198a02e-126e-4fbb-910c-918abf39a4a6");

        // TEST
        final Marshaller marshaller = new MarshallerBuilder().addClassesToBeBound(EscEvents.class, MyMeta.class, MyEvent.class, Base64Data.class).build();
        final String xml = marshal(marshaller, testee);

        // VERIFY
        final Diff documentDiff = DiffBuilder.compare(expectedXml).withTest(xml).ignoreWhitespace().build();
        assertThat(documentDiff.hasDifferences()).describedAs(documentDiff.toString()).isFalse();

    }

}
