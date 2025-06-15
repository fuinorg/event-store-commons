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
package org.fuin.esc.api;

import jakarta.activation.MimeTypeParseException;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link StreamEventsSlice} class.
 */
public class StreamEventsSliceTest {

    private static final int FROM = 0;

    private static final int NEXT = 10;

    private static final boolean EOS = true;

    private static List<CommonEvent> events;

    private StreamEventsSlice testee;

    @BeforeAll
    public static void beforeClass() throws MimeTypeParseException {
        final TypeName dataType = new TypeName("MyEvent");
        final TypeName metaType = new TypeName("MyMeta");
        final String meta = "{ \"ip\" : \"127.0.0.1\" }";
        events = new ArrayList<CommonEvent>();
        events.add(new SimpleCommonEvent(new EventId(), dataType, new MyEvent("Peter"), metaType, meta, null));
        events.add(new SimpleCommonEvent(new EventId(), dataType, new MyEvent("Mary Jane"), metaType, meta, null));
    }

    @BeforeEach
    public void setup() {
        testee = new StreamEventsSlice(FROM, events, NEXT, EOS);
    }

    @AfterEach
    public void teardown() {
        testee = null;
    }

    @Test
    public void testNullList() {
        assertThat(new StreamEventsSlice(FROM, null, NEXT, EOS).getEvents()).isEmpty();
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

