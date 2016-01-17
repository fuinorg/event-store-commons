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
package org.fuin.esc.api;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * An event store that is capable of handling volatile subscriptions.
 */
public interface SubscribableEventStoreAsync {

    /**
     * Subscribe a stream starting with a given event number.
     * 
     * @param streamId
     *            Unique stream identifier.
     * @param eventNumber
     *            Number of the event to start (
     *            {@link EscApiUtils#SUBSCRIBE_TO_NEW_EVENTS} = New events, 0 =
     *            First event, 1..N).
     * @param onEvent
     *            Will be called for an event.
     * @param onDrop
     *            Will be called when the subscription was exceptionally
     *            dropped.
     * 
     * @return Future with subscription result.
     */
    public CompletableFuture<Subscription> subscribeToStream(StreamId streamId,
            int eventNumber, BiConsumer<Subscription, CommonEvent> onEvent,
            BiConsumer<Subscription, Exception> onDrop);

    /**
     * Unsubscribe from a stream.
     * 
     * @param subscription
     *            to be terminated.
     * 
     * @return Future with no result.
     */
    public CompletableFuture<Void> unsubscribeFromStream(
            Subscription subscription);

}
