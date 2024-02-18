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


public final class SubscriptionTest {

    @Test
    public final void testConstruction() {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("stream1");
        final Long lastEventNumber = 1L;

        // TEST
        final Subscription testee = new Subscription(streamId, lastEventNumber) {
            private static final long serialVersionUID = 1L;
        };

        // VERIFY
        assertThat(testee.getStreamId()).isEqualTo(streamId);
        assertThat(testee.getLastEventNumber()).isEqualTo(lastEventNumber);

    }


}

