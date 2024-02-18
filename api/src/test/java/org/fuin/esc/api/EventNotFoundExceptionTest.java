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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fuin.utils4j.Utils4J.deserialize;
import static org.fuin.utils4j.Utils4J.serialize;

/**
 * Tests the {@link EventNotFoundException} class.
 */
public class EventNotFoundExceptionTest {

    private static final StreamId STREAM_ID = new SimpleStreamId("MyStream");

    private static final int VERSION = 1;

    private EventNotFoundException testee;

    @BeforeEach
    public void setup() {
        testee = new EventNotFoundException(STREAM_ID, VERSION);
    }

    @AfterEach
    public void teardown() {
        testee = null;
    }

    @Test
    public void testGetter() {
        assertThat(testee.getStreamId()).isEqualTo(STREAM_ID);
        assertThat(testee.getVersion()).isEqualTo(VERSION);
    }

    @Test
    public void testSerializeDeserialize() {

        // PREPARE
        final EventNotFoundException original = testee;

        // TEST
        final byte[] data = serialize(original);
        final EventNotFoundException copy = deserialize(data);

        // VERIFY
        assertThat(copy.getMessage()).isEqualTo(original.getMessage());
        assertThat(copy.getStreamId()).isEqualTo(original.getStreamId());
        assertThat(copy.getVersion()).isEqualTo(original.getVersion());

    }

}

