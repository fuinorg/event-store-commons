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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the {@link VersionNegotiator} class. The registry models a down-cast chain {@code v3 → v2 → v1}, the
 * direction content negotiation with older consumers needs (a registry is a single directed graph, so up-cast and
 * down-cast chains live in separate registries).
 */
public class VersionNegotiatorTest {

    private static final SerializedDataType TYPE = new SerializedDataType("MyEvent");

    /** Registry that can down-cast a v3 value to v2 and a v2 value to v1. */
    private static ConverterRegistry downCastChain() {
        return new SimpleConverterRegistry.Builder()
                .add(TYPE, "3", "2", replace("v3", "v2"))
                .add(TYPE, "2", "1", replace("v2", "v1"))
                .build();
    }

    @Test
    public void testToDownCastAndSkip() {

        // PREPARE
        final VersionNegotiator testee = new VersionNegotiator(downCastChain());

        // TEST & VERIFY
        assertThat((Object) testee.to(TYPE, "3", "2", "data-v3")).isEqualTo("data-v2"); // one step down
        assertThat((Object) testee.to(TYPE, "3", "1", "data-v3")).isEqualTo("data-v1"); // two steps down
        assertThat((Object) testee.to(TYPE, "2", "2", "data-v2")).isEqualTo("data-v2"); // skip (same version)

    }

    @Test
    public void testToNoChainThrows() {

        // PREPARE
        final VersionNegotiator testee = new VersionNegotiator(downCastChain());

        // TEST & VERIFY
        assertThatThrownBy(() -> testee.to(TYPE, "3", "9", "data-v3"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No converter chain");

    }

    @Test
    public void testNegotiateDownCastForOlderClient() {

        // PREPARE: the current event is v3, the client only accepts v1
        final VersionNegotiator testee = new VersionNegotiator(downCastChain());

        // TEST
        final Optional<String> result = testee.negotiate(TYPE, "3", List.of("1"), "data-v3");

        // VERIFY: it was down-cast to v1
        assertThat(result).contains("data-v1");

    }

    @Test
    public void testNegotiateServesCurrentVersionUnchanged() {

        // PREPARE: the client accepts the current version among others
        final VersionNegotiator testee = new VersionNegotiator(downCastChain());

        // TEST
        final Optional<String> result = testee.negotiate(TYPE, "3", List.of("3", "1"), "data-v3");

        // VERIFY: served as-is, no conversion applied
        assertThat(result).contains("data-v3");

    }

    @Test
    public void testNegotiatePrefersFirstReachableVersion() {

        // PREPARE: client accepts an unreachable version first, then a reachable one
        final VersionNegotiator testee = new VersionNegotiator(downCastChain());

        // TEST
        final Optional<String> result = testee.negotiate(TYPE, "3", List.of("9", "2"), "data-v3");

        // VERIFY: skipped the unreachable "9" and down-cast to "2"
        assertThat(result).contains("data-v2");

    }

    @Test
    public void testNegotiateEmptyWhenNoAcceptedVersionReachable() {

        // PREPARE: client only accepts a version there is no chain to
        final VersionNegotiator testee = new VersionNegotiator(downCastChain());

        // TEST
        final Optional<String> result = testee.negotiate(TYPE, "3", List.of("9"), "data-v3");

        // VERIFY
        assertThat(result).isEmpty();

    }

    /** Converter that rewrites one token into another (stands in for a real version transform). */
    private static Converter<String, String> replace(final String from, final String to) {
        return new Converter<>() {
            @Override
            public Class<String> getSourceType() {
                return String.class;
            }

            @Override
            public Class<String> getTargetType() {
                return String.class;
            }

            @Override
            public String convert(final String source) {
                return source.replace(from, to);
            }
        };
    }

}
