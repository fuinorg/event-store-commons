/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. 
 * http://www.fuin.org/
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
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package org.fuin.esc.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fuin.utils4j.Utils4J.deserialize;
import static org.fuin.utils4j.Utils4J.serialize;

import java.util.UUID;

import org.fuin.objects4j.common.ContractViolationException;
import org.junit.Test;

// CHECKSTYLE:OFF Test code
public final class EventIdTest {

    @Test
    public final void testSerialize() {
        final UUID uuid = UUID.randomUUID();
        final EventId original = new EventId(uuid);
        final EventId copy = deserialize(serialize(original));
        assertThat(original).isEqualTo(copy);
    }

    @Test
    public final void testConstructValid() {
        final UUID uuid = UUID.randomUUID();
        assertThat(new EventId(uuid).asBaseType()).isEqualTo(uuid);
    }

    @Test(expected = ContractViolationException.class)
    public final void testConstructNullString() {
        new EventId((String) null);
    }

    @Test(expected = ContractViolationException.class)
    public final void testConstructNullUUID() {
        new EventId((UUID) null);
    }

    @Test(expected = ContractViolationException.class)
    public final void testConstructInvalidString() {
        new EventId("x");
    }

}
// CHECKSTYLE:ON
