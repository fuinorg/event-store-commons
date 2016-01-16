/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. <http://www.fuin.org/>
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
package org.fuin.esc.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fuin.units4j.Units4JUtils.deserialize;
import static org.fuin.units4j.Units4JUtils.serialize;
import static org.fuin.units4j.Units4JUtils.unmarshal;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import nl.jqno.equalsverifier.EqualsVerifier;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.fuin.esc.api.EventId;
import org.fuin.esc.spi.EnhancedMimeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link Slice} class.
 */
// CHECKSTYLE:OFF Test
public class SliceTest extends AbstractXmlTest {

    private static final EventId ID = new EventId();
    
    private static final int FROM = 0;

    private static final int NEXT = 1;

    private static final boolean EOS = true;

    private List<Event> events;

    private Slice testee;

    @Before
    public void setup() throws Exception {
        events = new ArrayList<Event>();
        events.add(new Event(ID, new Data(
                "MyEvent", new EnhancedMimeType(
                        "application/xml; version=2; encoding=utf-8"),
                "<my-event/>"), null));
        testee = new Slice(FROM, events, NEXT, EOS);
    }

    @After
    public void teardown() {
        events = null;
        testee = null;
    }

    @Test
    public void testEqualsHashCode() {
        EqualsVerifier.forClass(Slice.class).verify();
    }

    @Test
    public void testGetter() {
        assertThat(testee.getFromEventNumber()).isEqualTo(FROM);
        assertThat(testee.getNextEventNumber()).isEqualTo(NEXT);
        assertThat(testee.isEndOfStream()).isEqualTo(EOS);
        assertThat(testee.getEvents()).isEqualTo(events);
    }

    @Test
    public void testSerializeDeserialize() {

        // PREPARE
        final Slice original = testee;

        // TEST
        final byte[] data = serialize(original);
        final Slice copy = deserialize(data);

        // VERIFY
        assertThat(copy).isEqualTo(original);

    }

    @Test
    public final void testMarshalUnmarshalXML() throws Exception {

        // PREPARE
        final Slice original = testee;

        // TEST
        final String xml = marshalToStr(original, createXmlAdapter(),
                Slice.class);

        // VERIFY
        XMLUnit.setIgnoreWhitespace(true);
        XMLAssert
                .assertXMLEqual(
                // @formatter:off
                        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                                + "<slice from-stream-no=\"0\" next-stream-no=\"1\" end-of-stream=\"true\">"
                                + "<event id=\"" + ID + "\">"
                                + "    <data type=\"MyEvent\" mime-type=\"application/xml; version=2; encoding=utf-8\">"
                                + "        <![CDATA[<my-event/>]]>"
                                + "    </data>" + "</event>" + "</slice>"
                        // @formatter:on
                        , xml);

    }

    @Test
    public final void testMarshalUnmarshalEquals() throws Exception {

        // PREPARE
        final Slice original = testee;

        // TEST
        final String xml = marshalToStr(original, createXmlAdapter(),
                Slice.class);
        final Slice copy = unmarshal(xml, createXmlAdapter(), Slice.class);

        // VERIFY
        assertThat(copy).isEqualTo(original);

    }

    private XmlAdapter<?, ?>[] createXmlAdapter() {
        return new XmlAdapter[] {};
    }

}
// CHECKSTYLE:ON
