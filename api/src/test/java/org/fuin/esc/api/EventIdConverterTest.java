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
package org.fuin.esc.api;

import static org.fest.assertions.Assertions.assertThat;
import static org.fuin.units4j.Units4JUtils.XML_PREFIX;
import static org.fuin.units4j.Units4JUtils.assertCauseCauseMessage;
import static org.fuin.units4j.Units4JUtils.marshal;
import static org.fuin.units4j.Units4JUtils.unmarshal;
import static org.junit.Assert.fail;

import java.util.UUID;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Test;

// CHECKSTYLE:OFF Test code
public final class EventIdConverterTest {

    @Test
    public final void testMarshal() throws JAXBException {
        final Event event = new Event();
        final UUID uuid = UUID.randomUUID();
        event.id = new EventId(uuid);
        assertThat(marshal(event, Event.class)).isEqualTo(
                XML_PREFIX + "<event id=\"" + uuid + "\"/>");

    }

    @Test
    public final void testMarshalUnmarshal() throws JAXBException {
        final UUID uuid = UUID.randomUUID();
        final Event original = new Event();
        original.id = new EventId(uuid);
        final Event copy = unmarshal(marshal(original, Event.class),
                Event.class);
        assertThat(copy).isEqualTo(original);
    }

    @Test
    public final void testUnmarshalError() {
        final String uuid = "x";
        final String invalidEmailInXmlEvent = XML_PREFIX + "<event id=\""
                + uuid + "\"/>";
        try {
            unmarshal(invalidEmailInXmlEvent, Event.class);
            fail("Expected an exception");
        } catch (final RuntimeException ex) {
            assertCauseCauseMessage(ex,
                    "The argument 'value' is not valid: 'x'");
        }
    }

    @XmlRootElement(name = "event")
    public static class Event {

        @XmlAttribute(name = "id")
        private EventId id;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (!(obj instanceof Event))
                return false;
            Event other = (Event) obj;
            if (id == null) {
                if (other.id != null)
                    return false;
            } else if (!id.equals(other.id))
                return false;
            return true;
        }

    }

}
// CHECKSTYLE:ON
