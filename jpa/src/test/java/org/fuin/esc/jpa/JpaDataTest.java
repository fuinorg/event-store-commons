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
package org.fuin.esc.jpa;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.api.EnhancedMimeType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public final class JpaDataTest extends AbstractPersistenceTest {

    @Test
    public void testEqualsHashCode() {
        EqualsVerifier.forClass(JpaData.class)
                .suppress(Warning.NONFINAL_FIELDS, Warning.STRICT_INHERITANCE).verify();
    }

    @Test
    public void testGetter() {

        // PREPARE
        final TypeName typeName = new TypeName("AnyName");
        final EnhancedMimeType mimeType = EnhancedMimeType.create("application", "xml");
        final byte[] raw = new byte[]{1, 2, 3};
        final JpaData testee = new JpaData(typeName, mimeType, raw);

        // TEST
        assertThat(testee.getTypeName()).isEqualTo(typeName);
        assertThat(testee.getMimeType()).isEqualTo(mimeType);
        assertThat(testee.getRaw()).isEqualTo(raw);

    }

}

