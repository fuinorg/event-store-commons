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
package org.fuin.esc.intf;

import static org.fest.assertions.Assertions.assertThat;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.activation.MimeTypeParseException;

import nl.jqno.equalsverifier.EqualsVerifier;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests the {@link StreamEventsSlice} class.
 */
// CHECKSTYLE:OFF Test
public class StreamEventsSliceTest {

    private static final int FROM = 0;

    private static final int NEXT = 10;

    private static final boolean EOS = true;

    private static List<EventData> events;

    private StreamEventsSlice testee;

    @BeforeClass
    public static void beforeClass() throws MimeTypeParseException {
        events = new ArrayList<EventData>();
        events.add(new EventData("e48f35ee-de38-4d63-ae0a-a2d1db2dbc5c",
                "MyEvent", new VersionedMimeType(
                        "application/xml; encoding=utf-8; version=1.0.0"),
                "<myEvent/>".getBytes(Charset.forName("utf-8"))));
        events.add(new EventData("e48f35ee-de38-4d63-ae0a-a2d1db2dbc5c",
                "MyEvent", new VersionedMimeType(
                        "application/xml; encoding=iso646-us; version=1.1.0"),
                "<myEvent/>".getBytes(Charset.forName("iso646-us"))));
    }

    @Before
    public void setup() {
        testee = new StreamEventsSlice(FROM, events, NEXT, EOS);
    }

    @After
    public void teardown() {
        testee = null;
    }

    @Test
    public void testEqualsHashCode() {
        EqualsVerifier.forClass(StreamEventsSlice.class).verify();
    }

    @Test
    public void testGetter() {
        assertThat(testee.getFromEventNumber()).isEqualTo(FROM);
        assertThat(testee.getNextEventNumber()).isEqualTo(NEXT);
        assertThat(testee.isEndOfStream()).isEqualTo(EOS);
        assertThat(testee.getEvents()).isEqualTo(events);
        
    }

}
// CHECKSTYLE:ON
