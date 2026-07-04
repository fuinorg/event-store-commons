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
package org.fuin.esc.jpa;

import jakarta.persistence.EntityManager;
import org.fuin.esc.api.ConverterRegistry;
import org.fuin.esc.api.DeserializerRegistry;
import org.fuin.esc.api.SerializerRegistry;
import org.fuin.esc.api.UpcastingDeserializerRegistry;
import org.fuin.objects4j.common.NotThreadSafe;
import org.fuin.utils4j.TestOmitted;

/**
 * Read only JPA implementation of the event store.
 */
@NotThreadSafe
@TestOmitted("Tested with JpaEventStoreTest and 'esc-test' project")
public final class ReadableJpaEventStore extends AbstractJpaEventStore {

    /**
     * Constructor with all mandatory data.
     *
     * @param em
     *            Entity manager.
     * @param serRegistry
     *            Registry used to locate serializers.
     * @param desRegistry
     *            Registry used to locate deserializers.
     */
    public ReadableJpaEventStore(final EntityManager em,
                                 final SerializerRegistry serRegistry, final DeserializerRegistry desRegistry) {
        super(em, serRegistry, desRegistry);
    }

    /**
     * Constructor that additionally up-casts events on read: the deserializer registry is wrapped in an
     * {@link UpcastingDeserializerRegistry} using the given converters, so every event read from this store is
     * lifted from its stored version to the latest in-memory representation without any caller having to wrap the
     * registry itself. An empty {@link ConverterRegistry} is a no-op.
     *
     * @param em
     *            Entity manager.
     * @param serRegistry
     *            Registry used to locate serializers.
     * @param desRegistry
     *            Registry used to locate deserializers.
     * @param converters
     *            Registry of version up-casters applied after deserialization.
     */
    public ReadableJpaEventStore(final EntityManager em, final SerializerRegistry serRegistry,
                                 final DeserializerRegistry desRegistry, final ConverterRegistry converters) {
        super(em, serRegistry, new UpcastingDeserializerRegistry(desRegistry, converters));
    }

}
