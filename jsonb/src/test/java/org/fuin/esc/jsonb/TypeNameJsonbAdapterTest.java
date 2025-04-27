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
package org.fuin.esc.jsonb;

import org.fuin.esc.api.TypeName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for the {@link TypeNameJsonbAdapter} class.
 */
public class TypeNameJsonbAdapterTest {

    @Test
    public void testUnmarshal() throws Exception {

        // PREPARE
        final TypeName original = new TypeName("Foo");
        final TypeNameJsonbAdapter testee = new TypeNameJsonbAdapter();

        // TEST
        final TypeName copy = testee.adaptFromJson(original.asBaseType());

        // VERIFY
        assertThat(copy).isEqualTo(original);
        assertThat(copy.asBaseType()).isEqualTo(original.asBaseType());
        assertThat(copy.asString()).isEqualTo(original.asString());

        // TEST & VERIFY
        assertThat(testee.adaptFromJson(null)).isNull();

    }

    @Test
    public void testMarshal() throws Exception {

        // PREPARE
        final TypeName original = new TypeName("Foo");
        final TypeNameJsonbAdapter testee = new TypeNameJsonbAdapter();

        // TEST & VERIFY
        assertThat(testee.adaptToJson(original)).isEqualTo(original.asString());
        assertThat(testee.adaptToJson(null)).isNull();

    }

}
