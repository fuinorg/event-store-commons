/**
 * Copyright (C) 2013 Future Invent Informationsmanagement GmbH. All rights
 * reserved. <http://www.fuin.org/>
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
 * along with this library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.fuin.esc.api;

import static org.fest.assertions.Assertions.assertThat;
import static org.fuin.units4j.Units4JUtils.deserialize;
import static org.fuin.units4j.Units4JUtils.serialize;
import static org.fuin.units4j.Units4JUtils.unmarshal;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import nl.jqno.equalsverifier.EqualsVerifier;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link EventData} class.
 */
// CHECKSTYLE:OFF Test
public class EventDataTest extends AbstractXmlTest {

    private static final String ID = "5741bcf1-9292-446b-84c1-957ed53b8d88";

    private static final String DATA_TYPE = "MyEvent";

    private static final VersionedMimeType DATA_MIME_TYPE = VersionedMimeType
            .create("application/xml; encoding=utf-8; version=1");

    private static final String DATA_CONTENT = "<myEvent/>";

    private static final Data DATA = new Data(DATA_TYPE, DATA_MIME_TYPE,
            DATA_CONTENT);

    private static final String META_TYPE = "MyMeta";

    private static final VersionedMimeType META_MIME_TYPE = VersionedMimeType
            .create("application/json; encoding=utf-8");

    private static final String META_CONTENT = "{ \"a\" : \"1\" }";

    private static final Data META = new Data(META_TYPE, META_MIME_TYPE,
            META_CONTENT);

    private EventData testee;

    @Before
    public void setup() {
        testee = new EventData(ID, DATA, META);
    }

    @After
    public void teardown() {
        testee = null;
    }

    @Test
    public void testEqualsHashCode() {
        EqualsVerifier.forClass(EventData.class).verify();
    }

    @Test
    public void testGetter() {
        assertEqualsConstantValues(testee);
    }

    @Test
    public void testSerializeDeserialize() {

        // PREPARE
        final EventData original = testee;

        // TEST
        final byte[] data = serialize(original);
        final EventData copy = deserialize(data);

        // VERIFY
        assertEqualsConstantValues(copy);

    }

    @Test
    public final void testMarshalUnmarshalXML() throws Exception {

        // PREPARE
        final EventData original = testee;

        // TEST
        final String xml = marshalToStr(original, createXmlAdapter(),
                EventData.class);

        // VERIFY
        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert
                .assertXMLEqual(
                        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                                + "<event-data id=\"5741bcf1-9292-446b-84c1-957ed53b8d88\">"
                                + "<data type=\"MyEvent\" mime-type=\"application/xml; version=1; encoding=utf-8\">"
                                + "<![CDATA[<myEvent/>]]>"
                                + "</data>"
                                + "<meta type=\"MyMeta\" mime-type=\"application/json; encoding=utf-8\">"
                                + "<![CDATA[{ \"a\" : \"1\" }]]>" + "</meta>"
                                + "</event-data>", xml);

    }

    @Test
    public final void testMarshalUnmarshalEquals() throws Exception {

        // PREPARE
        final EventData original = testee;

        // TEST
        final String xml = marshalToStr(original, createXmlAdapter(), EventData.class);
        final EventData copy = unmarshal(xml, createXmlAdapter(),
                EventData.class);

        // VERIFY
        assertEqualsConstantValues(copy);

    }

    private void assertEqualsConstantValues(EventData ed) {
        assertThat(ed.getId()).isEqualTo(ID);
        assertThat(ed.getData().getType()).isEqualTo(DATA_TYPE);
        assertThat(ed.getData().getMimeType()).isEqualTo(DATA_MIME_TYPE);
        assertThat(ed.getData().getContent()).isEqualTo(DATA_CONTENT);
        assertThat(ed.getMeta().getType()).isEqualTo(META_TYPE);
        assertThat(ed.getMeta().getMimeType()).isEqualTo(META_MIME_TYPE);
        assertThat(ed.getMeta().getContent()).isEqualTo(META_CONTENT);
    }

    private XmlAdapter<?, ?>[] createXmlAdapter() {
        return new XmlAdapter[] {};
    }

}
// CHECKSTYLE:ON
