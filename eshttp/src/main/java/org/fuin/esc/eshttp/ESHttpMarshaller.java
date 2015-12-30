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
package org.fuin.esc.eshttp;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.spi.SerializerRegistry;
import org.fuin.objects4j.common.Nullable;

/**
 * Marshals data for sending it to the event store.
 */
public interface ESHttpMarshaller {

    /**
     * Creates a list of "application/vnd.eventstore.events(+json/+xml)" entries surrounded by "[]" (JSON) or
     * "&lt;Events&gt;&lt;/Events&gt;" (XML).
     * 
     * @param registry
     *            Registry with known serializers.
     * @param commonEvents
     *            Events to marshal.
     * 
     * @return Single event body.
     */
    public String marshal(@NotNull SerializerRegistry registry, @NotNull List<CommonEvent> commonEvents);

    /**
     * Creates a single "application/vnd.eventstore.events(+json/+xml)" entry surrounded by "[]" (JSON) or
     * "&lt;Events&gt;&lt;/Events&gt;" (XML).
     * 
     * @param registry
     *            Registry with known serializers.
     * @param commonEvent
     *            Event to marshal.
     * 
     * @return Single event body.
     */
    public String marshal(@NotNull SerializerRegistry registry, @Nullable CommonEvent commonEvent);

}
