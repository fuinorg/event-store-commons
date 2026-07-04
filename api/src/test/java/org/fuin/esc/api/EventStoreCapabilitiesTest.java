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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the {@link EventStoreCapabilities} class.
 */
public class EventStoreCapabilitiesTest {

    @Test
    public void testNoneHasNoCapabilities() {

        // TEST & VERIFY
        assertThat(EventStoreCapabilities.NONE.subscriptions()).isFalse();
        assertThat(EventStoreCapabilities.NONE.persistentSubscriptions()).isFalse();
        assertThat(EventStoreCapabilities.NONE.projections()).isFalse();
        assertThat(EventStoreCapabilities.NONE.hardDelete()).isFalse();
        assertThat(EventStoreCapabilities.NONE.durablePersistence()).isFalse();

    }

    @Test
    public void testBuilderSetsAllFlags() {

        // TEST
        final EventStoreCapabilities capabilities = EventStoreCapabilities.builder()
                .subscriptions(true)
                .persistentSubscriptions(true)
                .projections(true)
                .hardDelete(true)
                .durablePersistence(true)
                .build();

        // VERIFY
        assertThat(capabilities).isEqualTo(new EventStoreCapabilities(true, true, true, true, true));
        assertThat(capabilities.subscriptions()).isTrue();
        assertThat(capabilities.persistentSubscriptions()).isTrue();
        assertThat(capabilities.projections()).isTrue();
        assertThat(capabilities.hardDelete()).isTrue();
        assertThat(capabilities.durablePersistence()).isTrue();

    }

    @Test
    public void testBuilderDefaultsToFalse() {

        // TEST
        final EventStoreCapabilities capabilities = EventStoreCapabilities.builder().build();

        // VERIFY
        assertThat(capabilities).isEqualTo(EventStoreCapabilities.NONE);

    }

    @Test
    public void testPersistentSubscriptionsRequireSubscriptions() {

        // TEST & VERIFY
        assertThatThrownBy(() -> new EventStoreCapabilities(false, true, false, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("persistentSubscriptions");

    }

}
