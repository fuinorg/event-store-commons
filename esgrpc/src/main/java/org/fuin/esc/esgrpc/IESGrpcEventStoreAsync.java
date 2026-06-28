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

import org.fuin.esc.api.EventStoreAsync;
import org.fuin.esc.api.SubscribableEventStoreAsync;
import org.fuin.objects4j.common.ThreadSafe;

/**
 * Asynchronous gRPC event store that also supports volatile subscriptions.
 */
@ThreadSafe
public interface IESGrpcEventStoreAsync extends EventStoreAsync, SubscribableEventStoreAsync {
}
