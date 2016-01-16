/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. <http://www.fuin.org/>
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
 * along with this library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.fuin.esc.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fuin.units4j.Units4JUtils.deserialize;
import static org.fuin.units4j.Units4JUtils.serialize;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link StreamNotFoundException} class.
 */
// CHECKSTYLE:OFF Test
public class StreamNotFoundExceptionTest {

    private static final StreamId STREAM_ID = new SimpleStreamId("MyStream");

    private StreamNotFoundException testee;

    @Before
    public void setup() {
        testee = new StreamNotFoundException(STREAM_ID);
    }

    @After
    public void teardown() {
        testee = null;
    }

    @Test
    public void testGetter() {
        assertThat(testee.getStreamId()).isEqualTo(STREAM_ID);
    }

    @Test
    public void testSerializeDeserialize() {

        // PREPARE
        final StreamNotFoundException original = testee;

        // TEST
        final byte[] data = serialize(original);
        final StreamNotFoundException copy = deserialize(data);

        // VERIFY
        assertThat(copy.getMessage()).isEqualTo(original.getMessage());
        assertThat(copy.getStreamId()).isEqualTo(original.getStreamId());

    }

}
// CHECKSTYLE:ON
