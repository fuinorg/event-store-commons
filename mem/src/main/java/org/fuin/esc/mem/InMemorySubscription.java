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
package org.fuin.esc.mem;

import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.Subscription;

/**
 * Result of subscribing to an in-memory event store. Hash code and equals are
 * based on the subscriber ID.
 */
public final class InMemorySubscription extends Subscription implements
        Comparable<InMemorySubscription> {

    private static final long serialVersionUID = 1000L;

    private final int subscriberId;

    /**
     * Creates a subscription.
     * 
     * @param subscriberId
     *            Uniquely identifies a subscriber.
     * @param streamId
     *            Unique stream identifier.
     * @param lastEventNumber
     *            Last event seen on the stream.
     */
    public InMemorySubscription(final int subscriberId,
            final StreamId streamId, final Long lastEventNumber) {
        super(streamId, lastEventNumber);
        this.subscriberId = subscriberId;
    }

    /**
     * Returns the subscriber ID.
     * 
     * @return Uniquely identifies a subscriber.
     */
    public final int getSubscriberId() {
        return subscriberId;
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + subscriberId;
        return result;
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof InMemorySubscription)) {
            return false;
        }
        final InMemorySubscription other = (InMemorySubscription) obj;
        return (subscriberId == other.subscriberId);
    }

    @Override
    public final int compareTo(final InMemorySubscription other) {
        if (subscriberId > other.subscriberId) {
            return 1;
        }
        if (subscriberId < other.subscriberId) {
            return -1;
        }
        return 0;
    }

    @Override
    public final String toString() {
        return this.getClass().getSimpleName() + "#" + subscriberId;
    }
    
}
