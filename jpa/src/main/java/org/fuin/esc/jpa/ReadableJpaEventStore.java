/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. <http://www.fuin.org/>
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
 * along with this library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.fuin.esc.jpa;

import javax.persistence.EntityManager;
import javax.validation.constraints.NotNull;

import org.fuin.esc.spi.DeserializerRegistry;
import org.fuin.esc.spi.SerializerRegistry;

/**
 * Read only JPA implementation of the event store.
 */
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
    public ReadableJpaEventStore(@NotNull final EntityManager em,
            @NotNull final SerializerRegistry serRegistry, @NotNull final DeserializerRegistry desRegistry) {
        super(em, serRegistry, desRegistry);
    }

}
