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

import org.fuin.objects4j.common.Immutable;

/**
 * Immutable, backend-wide descriptor of the optional capabilities a concrete event store implementation
 * provides. It makes the differences between backends explicit so a consumer can detect at wiring time
 * (instead of by runtime failure) whether the store it was handed supports the features it needs.
 * <p>
 * Instances are obtained via {@link EventStoreBasics#capabilities()} /
 * {@link EventStoreBasicsAsync#capabilities()}. Both the synchronous and asynchronous facade of the same
 * backend report the same descriptor.
 *
 * @param subscriptions             {@code true} if the backend supports catch-up/live subscriptions
 *                                  (implements {@link SubscribableEventStoreAsync}).
 * @param persistentSubscriptions   {@code true} if the backend additionally supports server-side persistent
 *                                  subscriptions (a sub-capability of {@code subscriptions}).
 * @param projections               {@code true} if the backend supports projections
 *                                  (a {@link ProjectionAdminEventStore} is available).
 * @param hardDelete                {@code true} if the backend supports a permanent (hard) stream delete via
 *                                  {@link WritableEventStore#deleteStream(StreamId, long, boolean)}.
 * @param durablePersistence        {@code true} if appended events survive a restart of the store
 *                                  ({@code false} for a volatile in-memory backend).
 */
@Immutable
public record EventStoreCapabilities(boolean subscriptions, boolean persistentSubscriptions, boolean projections,
                                     boolean hardDelete, boolean durablePersistence) {

    /**
     * No optional capability is supported. Represents an append/read-only, volatile store and is the safe
     * default returned by the {@code capabilities()} methods for backends that do not override them.
     */
    public static final EventStoreCapabilities NONE = new EventStoreCapabilities(false, false, false, false, false);

    /**
     * Canonical constructor enforcing that persistent subscriptions imply general subscription support.
     */
    public EventStoreCapabilities {
        if (persistentSubscriptions && !subscriptions) {
            throw new IllegalArgumentException("A backend cannot support 'persistentSubscriptions' without 'subscriptions'");
        }
    }

    /**
     * Returns a new builder for capability descriptors (every capability defaults to {@code false}).
     *
     * @return New builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link EventStoreCapabilities}. Preferable to the positional constructor because
     * all parameters share the same {@code boolean} type.
     */
    public static final class Builder {

        private boolean subscriptions;

        private boolean persistentSubscriptions;

        private boolean projections;

        private boolean hardDelete;

        private boolean durablePersistence;

        /**
         * Marks catch-up/live subscription support.
         *
         * @param value TRUE if supported.
         * @return This builder.
         */
        public Builder subscriptions(final boolean value) {
            this.subscriptions = value;
            return this;
        }

        /**
         * Marks server-side persistent subscription support (implies {@link #subscriptions(boolean)}).
         *
         * @param value TRUE if supported.
         * @return This builder.
         */
        public Builder persistentSubscriptions(final boolean value) {
            this.persistentSubscriptions = value;
            return this;
        }

        /**
         * Marks projection support.
         *
         * @param value TRUE if supported.
         * @return This builder.
         */
        public Builder projections(final boolean value) {
            this.projections = value;
            return this;
        }

        /**
         * Marks permanent (hard) delete support.
         *
         * @param value TRUE if supported.
         * @return This builder.
         */
        public Builder hardDelete(final boolean value) {
            this.hardDelete = value;
            return this;
        }

        /**
         * Marks durable (restart-surviving) persistence.
         *
         * @param value TRUE if durable.
         * @return This builder.
         */
        public Builder durablePersistence(final boolean value) {
            this.durablePersistence = value;
            return this;
        }

        /**
         * Builds a new immutable descriptor.
         *
         * @return New instance.
         */
        public EventStoreCapabilities build() {
            return new EventStoreCapabilities(subscriptions, persistentSubscriptions, projections, hardDelete,
                    durablePersistence);
        }

    }

}
