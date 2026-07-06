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
package org.fuin.esc.mem;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.mem.InMemoryProjections.InMemoryProjection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link InMemoryProjections} registry and its {@link InMemoryProjection} predicate.
 */
public class InMemoryProjectionsTest {

    @Test
    public void testRegistryAddGetContainsRemove() {
        final InMemoryProjections registry = new InMemoryProjections();
        assertThat(registry.contains("p")).isFalse();
        assertThat(registry.get("p")).isNull();

        final InMemoryProjection projection = new InMemoryProjection(true, List.of(new TypeName("TypeA")), List.of());
        registry.add("p", projection);
        assertThat(registry.contains("p")).isTrue();
        assertThat(registry.get("p")).isSameAs(projection);

        registry.remove("p");
        assertThat(registry.contains("p")).isFalse();
        assertThat(registry.get("p")).isNull();
    }

    @Test
    public void testEnableDisable() {
        final InMemoryProjection projection = new InMemoryProjection(false, List.of(), List.of("Created"));
        assertThat(projection.isEnabled()).isFalse();
        projection.enable();
        assertThat(projection.isEnabled()).isTrue();
        projection.disable();
        assertThat(projection.isEnabled()).isFalse();
    }

    @Test
    public void testSelectsNothing() {
        assertThat(new InMemoryProjection(true, List.of(), List.of()).selectsNothing()).isTrue();
        assertThat(new InMemoryProjection(true, List.of(new TypeName("A")), List.of()).selectsNothing()).isFalse();
        assertThat(new InMemoryProjection(true, List.of(), List.of("C")).selectsNothing()).isFalse();
    }

    @Test
    public void testMatchesByType() {
        final InMemoryProjection projection = new InMemoryProjection(true, List.of(new TypeName("TypeA")), List.of());
        assertThat(projection.matches(event("TypeA", List.of()))).isTrue();
        assertThat(projection.matches(event("TypeB", List.of("TypeA")))).isFalse();
    }

    @Test
    public void testMatchesByCategory() {
        final InMemoryProjection projection = new InMemoryProjection(true, List.of(), List.of("Created", "Archived"));
        assertThat(projection.matches(event("TypeA", List.of("Created")))).isTrue();
        assertThat(projection.matches(event("TypeA", List.of("Other")))).isFalse();
        assertThat(projection.matches(event("TypeA", List.of()))).isFalse();
    }

    @Test
    public void testMatchesByTypeOrCategory() {
        final InMemoryProjection projection = new InMemoryProjection(true, List.of(new TypeName("TypeA")), List.of("Created"));
        assertThat(projection.matches(event("TypeA", List.of()))).isTrue();
        assertThat(projection.matches(event("TypeB", List.of("Created")))).isTrue();
        assertThat(projection.matches(event("TypeB", List.of("Other")))).isFalse();
    }

    private static CommonEvent event(final String type, final List<String> categories) {
        return new SimpleCommonEvent(new EventId(), new TypeName(type), new MyEvent("x"), null, null, null, categories);
    }

}
