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

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fuin.utils4j.jaxb.JaxbUtils.marshal;
import static org.fuin.utils4j.jaxb.JaxbUtils.unmarshal;

/**
 * Test for {@link EscEvent} class.
 */
public class EscEventTest {

    @Test
    public final void testMarshalUnmarshalJaxb() throws Exception {

        // PREPARE
        final String expectedXml = """
                <Event>
                    <EventId>68616d90-cf72-4c2a-b913-32bf6e6506ed</EventId>
                    <EventType>MyEvent</EventType>
                    <Data>
                        <MyEvent>
                            <id>68616d90-cf72-4c2a-b913-32bf6e6506ed</id>
                            <description>Hello, XML!</description>
                        </MyEvent>
                    </Data>
                    <MetaData>
                        <esc-meta>
                            <data-type>MyEvent</data-type>
                            <data-content-type>application/xml; version=1; encoding=utf-8</data-content-type>
                            <meta-type>MyMeta</meta-type>
                            <meta-content-type>application/xml; version=1; encoding=utf-8</meta-content-type>
                            <MyMeta>
                                <user>abc</user>
                            </MyMeta>
                        </esc-meta>
                    </MetaData>
                </Event>
                """;

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

}
