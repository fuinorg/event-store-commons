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

import org.fuin.esc.api.EventId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public final class EventIdXmlAdapterTest {

    private static final String UUID = "f73422c8-2ed9-4613-865d-fa82adf43767";

    @Test
    public final void testMarshalNull() throws Exception {
        assertThat(new EventIdXmlAdapter().marshal(null)).isNull();
    }

    @Test
    public final void testMarshal() throws Exception {
        final EventId eventId = new EventId(UUID);
        assertThat(new EventIdXmlAdapter().marshal(eventId)).isEqualTo(UUID);
    }

    @Test
    public final void testUnmarshalNull() throws Exception {
        assertThat(new EventIdXmlAdapter().unmarshal(null)).isNull();
    }

    @Test
    public final void testUnmarshal() throws Exception {
        final EventId eventId = new EventId(UUID);
        assertThat(new EventIdXmlAdapter().unmarshal(UUID)).isEqualTo(eventId);
    }

}

