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
package org.fuin.esc.spi;

import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link InMemoryCheckpointStore} class.
 */
public class InMemoryCheckpointStoreTest {

    private static final StreamId STREAM_A = new SimpleStreamId("A");

    private static final StreamId STREAM_B = new SimpleStreamId("B");

    @Test
    public void testDefaultsToZero() {
        assertThat(new InMemoryCheckpointStore().readCheckpoint(STREAM_A)).isZero();
    }

    @Test
    public void testUpdateAndRead() {

        // PREPARE
        final InMemoryCheckpointStore testee = new InMemoryCheckpointStore();

        // TEST
        testee.updateCheckpoint(STREAM_A, 5);

        // VERIFY: independent per stream
        assertThat(testee.readCheckpoint(STREAM_A)).isEqualTo(5);
        assertThat(testee.readCheckpoint(STREAM_B)).isZero();

    }

    @Test
    public void testReset() {

        // PREPARE
        final InMemoryCheckpointStore testee = new InMemoryCheckpointStore();
        testee.updateCheckpoint(STREAM_A, 7);

        // TEST
        testee.resetCheckpoint(STREAM_A);

        // VERIFY
        assertThat(testee.readCheckpoint(STREAM_A)).isZero();

    }

}
