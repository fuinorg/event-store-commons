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
package org.fuin.esc.esgrpc;

import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.Subscription;
import org.fuin.objects4j.common.Immutable;
import org.jspecify.annotations.Nullable;
import org.fuin.utils4j.TestOmitted;

import java.io.Serial;
import java.util.Objects;

/**
 * Result of subscribing to a gRPC event store. Hash code and equals are based on the subscription ID.
 * The native subscription is kept transient because it is not serializable and is only required to
 * stop the subscription within the same JVM.
 */
@Immutable
@TestOmitted("Tested in the 'test' project")
public final class ESGrpcSubscription extends Subscription {

    @Serial
    private static final long serialVersionUID = 1000L;

    private final String subscriptionId;

    private final transient io.kurrent.dbclient.@Nullable Subscription nativeSubscription;

    /**
     * Creates a subscription.
     *
     * @param streamId           Unique stream identifier.
     * @param lastEventNumber    Last event seen on the stream (may be {@code null}).
     * @param nativeSubscription Underlying gRPC subscription used to unsubscribe.
     */
    public ESGrpcSubscription(final StreamId streamId,
                              @Nullable final Long lastEventNumber,
                              final io.kurrent.dbclient.Subscription nativeSubscription) {
        super(streamId, lastEventNumber);
        this.subscriptionId = nativeSubscription.getSubscriptionId();
        this.nativeSubscription = nativeSubscription;
    }

    /**
     * Returns the unique identifier of the subscription.
     *
     * @return Subscription ID assigned by the event store.
     */
    public String getSubscriptionId() {
        return subscriptionId;
    }

    /**
     * Returns the underlying gRPC subscription.
     *
     * @return Native subscription or {@code null} if this instance was deserialized.
     */
    public io.kurrent.dbclient.@Nullable Subscription getNativeSubscription() {
        return nativeSubscription;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(subscriptionId);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ESGrpcSubscription other)) {
            return false;
        }
        return subscriptionId.equals(other.subscriptionId);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "#" + subscriptionId;
    }

}
