/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. 
 * http://www.fuin.org/
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
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package org.fuin.esc.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamState;
import org.junit.Test;

// CHECKSTYLE:OFF
public final class NoParamsStreamTest {

    
    @Test
    public void testCreate() {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("AnyName");
        
        // TEST
        final NoParamsStream testee = new NoParamsStream(streamId);
        
        // VERIFY
        assertThat(testee.toString()).isEqualTo(streamId.toString());
        assertThat(testee.getState()).isEqualTo(StreamState.ACTIVE);
        assertThat(testee.getVersion()).isEqualTo(-1);
        assertThat(testee.isDeleted()).isFalse();
        
    }
    
    @Test
    public void testSoftDelete() {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("AnyName");
        final NoParamsStream testee = new NoParamsStream(streamId);
        
        // TEST
        testee.delete(false);
        
        // VERIFY
        assertThat(testee.getState()).isEqualTo(StreamState.SOFT_DELETED);
        assertThat(testee.isDeleted()).isTrue();
        
    }

    @Test
    public void testHardDelete() {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("AnyName");
        final NoParamsStream testee = new NoParamsStream(streamId);
        
        // TEST
        testee.delete(true);
        
        // VERIFY
        assertThat(testee.getState()).isEqualTo(StreamState.HARD_DELETED);
        assertThat(testee.isDeleted()).isTrue();
        
    }

    @Test
    public void testIncVersion() {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("AnyName");
        final NoParamsStream testee = new NoParamsStream(streamId);
        
        // TEST & VERIFY
        assertThat(testee.incVersion()).isEqualTo(0);
        assertThat(testee.getVersion()).isEqualTo(0);
        
        assertThat(testee.incVersion()).isEqualTo(1);
        assertThat(testee.getVersion()).isEqualTo(1);
        
    }

    @Test
    public void testCreateEvent() {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("AnyName");
        final NoParamsStream testee = new NoParamsStream(streamId);
        final JpaEvent eventEntry = new JpaEvent();
        final int version = testee.getVersion();
        
        // TEST
        final JpaStreamEvent result = testee.createEvent(streamId, eventEntry);
        
        // VERIFY
        assertThat(result).isNotNull();
        assertThat(result.getEvent()).isSameAs(eventEntry);
        assertThat(testee.getVersion()).isEqualTo(version + 1);
        
    }
    
}
// CHECKSTYLE:ON
