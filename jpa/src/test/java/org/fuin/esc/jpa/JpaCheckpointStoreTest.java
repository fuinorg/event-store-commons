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

import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link JpaCheckpointStore} class against the in-memory HSQLDB.
 */
public class JpaCheckpointStoreTest extends AbstractPersistenceTest {

    private static final StreamId STREAM = new SimpleStreamId("MyView");

    @Test
    public void testUpdateReadReset() {

        // PREPARE: no checkpoint yet -> 0
        beginTransaction();
        assertThat(new JpaCheckpointStore(getEm()).readCheckpoint(STREAM)).isZero();
        // Insert
        new JpaCheckpointStore(getEm()).updateCheckpoint(STREAM, 3);
        commitTransaction();
        getEm().clear();

        // TEST: read back from the database
        beginTransaction();
        assertThat(new JpaCheckpointStore(getEm()).readCheckpoint(STREAM)).isEqualTo(3);
        // Update the existing row
        new JpaCheckpointStore(getEm()).updateCheckpoint(STREAM, 8);
        commitTransaction();
        getEm().clear();

        beginTransaction();
        assertThat(new JpaCheckpointStore(getEm()).readCheckpoint(STREAM)).isEqualTo(8);
        // Reset
        new JpaCheckpointStore(getEm()).resetCheckpoint(STREAM);
        commitTransaction();
        getEm().clear();

        // VERIFY: back to 0
        beginTransaction();
        assertThat(new JpaCheckpointStore(getEm()).readCheckpoint(STREAM)).isZero();
        commitTransaction();

    }

}
