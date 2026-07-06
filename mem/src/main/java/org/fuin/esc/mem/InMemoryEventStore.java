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
package org.fuin.esc.mem;

import org.fuin.esc.api.ProjectionAdminEventStore;
import org.fuin.esc.spi.DelegatingSyncEventStore;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.ThreadSafe;

import java.util.concurrent.Executor;

/**
 * In-memory implementation for unit testing. This implementation is thread-safe: it maps the synchronous event store API onto the
 * asynchronous {@link InMemoryEventStoreAsync} (via {@link DelegatingSyncEventStore}) by blocking on the returned futures.
 */
@ThreadSafe
public final class InMemoryEventStore extends DelegatingSyncEventStore implements IInMemoryEventStore {

    private final InMemoryEventStoreAsync delegate;

    /**
     * Constructor with all mandatory data.
     *
     * @param executor
     *            Executor used to create the necessary threads for event notifications.
     */
    public InMemoryEventStore(final Executor executor) {
        this(new InMemoryEventStoreAsync(requireExecutor(executor)));
    }

    private InMemoryEventStore(final InMemoryEventStoreAsync delegate) {
        super(delegate);
        this.delegate = delegate;
    }

    @Override
    public InMemoryEventStore open() {
        super.open();
        return this;
    }

    /**
     * Returns a projection admin store sharing this event store's projection registry. Projections created
     * through it are immediately readable as projection streams from this event store.
     *
     * @return New projection admin store bound to this event store.
     */
    public ProjectionAdminEventStore getProjectionAdmin() {
        return delegate.getProjectionAdmin();
    }

    private static Executor requireExecutor(final Executor executor) {
        Contract.requireArgNotNull("executor", executor);
        return executor;
    }

}
