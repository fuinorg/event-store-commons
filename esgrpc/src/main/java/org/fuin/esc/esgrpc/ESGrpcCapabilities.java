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

import org.fuin.esc.api.EventStoreCapabilities;
import org.fuin.objects4j.common.Immutable;

/**
 * Holds the capability descriptor shared by the synchronous {@link ESGrpcEventStore} and asynchronous
 * {@link ESGrpcEventStoreAsync} facades so both report the identical backend-wide truth.
 */
@Immutable
final class ESGrpcCapabilities {

    /**
     * EventStoreDB supports catch-up and persistent subscriptions, projections, hard delete and durable
     * server-side persistence.
     */
    static final EventStoreCapabilities INSTANCE = EventStoreCapabilities.builder()
            .subscriptions(true)
            .persistentSubscriptions(true)
            .projections(true)
            .hardDelete(true)
            .durablePersistence(true)
            .build();

    private ESGrpcCapabilities() {
        throw new UnsupportedOperationException("It is not allowed to create an instance of a utility class");
    }

}
