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

import java.util.function.BiConsumer;

import javax.validation.constraints.NotNull;

/**
 * An event store that is capable of handling volatile subscriptions. Calling
 * any method on a non-open event store will implicitly {@link #open()} it.
 */
public interface SubscribableEventStore extends EventStoreBasics {

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
     * @return Subscription result.
     * 
     * @throws StreamNotFoundException
     *             A stream with the given name does not exist in the
     *             repository.
     * @throws StreamDeletedException
     *             A stream with the given name previously existed but was
     *             deleted.
     */
    @NotNull
    public Subscription subscribeToStream(@NotNull StreamId streamId, int eventNumber,
            @NotNull BiConsumer<Subscription, CommonEvent> onEvent,
            @NotNull BiConsumer<Subscription, Exception> onDrop);

    /**
     * Unsubscribe from a stream. If the given subscription does not exist,
     * nothing happens.
     * 
     * @param subscription
     *            to be terminated.
     */
    public void unsubscribeFromStream(@NotNull Subscription subscription);

}
