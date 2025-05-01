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
package org.fuin.esc.test;

import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.fuin.esc.api.EventId;
import org.fuin.utils4j.jaxb.UnmarshallerBuilder;
import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fuin.utils4j.jaxb.JaxbUtils.unmarshal;

/**
 * Tests the {@link Events} class.
 */
public class EventsTest extends AbstractXmlTest {

    @Test
    public final void testUnMarshal() throws Exception {

        // PREPARE
        final String expectedXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <events>
                    <event id="7ab8e400-373b-4f65-96e1-96b78a791a42">
                        <data type="TextEvent" mime-type="text/plain; version=1; encoding=utf-8"><![CDATA[Anything goes]]></data>
                    </event>
                    <event id="35ae2b63-c820-4cea-8ad6-0d25e4519390">
                        <data type="TextEvent" mime-type="text/plain; version=1; encoding=utf-8"><![CDATA[More to come]]></data>
                    </event>
                </events>
                """;

        // TEST
        final Unmarshaller unmarshaller = new UnmarshallerBuilder().addClassesToBeBound(Events.class).build();
        final Events testee = unmarshal(unmarshaller, expectedXml);

        // VERIFY
        assertThat(testee).isNotNull();
        assertThat(testee.getEvents()).isNotNull();
        assertThat(testee.getEvents()).hasSize(2);
        assertThat(testee.getEvents().get(0).getId()).isEqualTo(new EventId("7ab8e400-373b-4f65-96e1-96b78a791a42"));
        assertThat(testee.getEvents().get(1).getId()).isEqualTo(new EventId("35ae2b63-c820-4cea-8ad6-0d25e4519390"));

        // TEST
        final String xml = AbstractXmlTest.marshalToStr(testee, createXmlAdapter(), Events.class);

        // VERIFY
        final Diff documentDiff = DiffBuilder.compare(expectedXml).withTest(xml).ignoreWhitespace().build();
        assertThat(documentDiff.hasDifferences()).describedAs(documentDiff.toString()).isFalse();

    }

    private XmlAdapter<?, ?>[] createXmlAdapter() {
        return new XmlAdapter[]{};
    }

}

