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
import static org.fuin.units4j.Units4JUtils.deserialize;
import static org.fuin.units4j.Units4JUtils.serialize;

import org.fuin.objects4j.common.ContractViolationException;
import org.junit.Test;

// CHECKSTYLE:OFF Test code
public final class EventTypeTest {

    @Test
    public final void testSerialize() {
        final String name = "MyUniqueTypeName";
        final EventType original = new EventType(name);
        final EventType copy = deserialize(serialize(original));
        assertThat(original).isEqualTo(copy);
    }

    @Test
    public final void testConstructValid() {
        final String name = "MyType";
        assertThat(new EventType(name).toString()).isEqualTo(name);
        assertThat(new EventType(name).length()).isEqualTo(name.length());
    }

    @Test(expected = ContractViolationException.class)
    public final void testConstructNullString() {
        new EventType(null);
    }

    @Test(expected = ContractViolationException.class)
    public final void testConstructEmpty() {
        new EventType("");
    }

}
// CHECKSTYLE:ON
