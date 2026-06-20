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

import nl.jqno.equalsverifier.EqualsVerifier;
import org.fuin.objects4j.common.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.fuin.utils4j.Utils4J.deserialize;
import static org.fuin.utils4j.Utils4J.serialize;

/**
 * Tests the {@link ProjectionId} class.
 */
public class ProjectionIdTest {

    private static final String NAME = "MyProjection";

    private ProjectionId testee;

    @BeforeEach
    public void setup() {
        testee = new ProjectionId(NAME);
    }

    @AfterEach
    public void teardown() {
        testee = null;
    }

    @Test
    public void testEqualsHashCode() {
        EqualsVerifier.forClass(ProjectionId.class).withNonnullFields("name").verify();
    }

    @Test
    public void testGetter() {
        assertThat(testee.getName()).isEqualTo(NAME);
    }

    @Test
    public void testToString() {
        assertThat(testee.toString()).isEqualTo(NAME);
    }

    @Test
    public void testConstructNullName() {
        assertThatThrownBy(() -> {
            new ProjectionId(null);
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    public void testConstructEmptyName() {
        assertThatThrownBy(() -> {
            new ProjectionId("");
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    public void testSerialize() {
        final ProjectionId original = new ProjectionId(NAME);
        final ProjectionId copy = deserialize(serialize(original));
        assertThat(original).isEqualTo(copy);
    }

}
