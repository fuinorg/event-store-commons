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
package org.fuin.esc.api;

import javax.persistence.AttributeConverter;

import org.fuin.objects4j.common.ThreadSafe;
import org.fuin.objects4j.vo.AbstractValueObjectConverter;

/**
 * Creates a {@link EventType}.
 */
@ThreadSafe
public final class EventTypeConverter extends
        AbstractValueObjectConverter<String, EventType> implements
        AttributeConverter<EventType, String> {

    @Override
    public Class<String> getBaseTypeClass() {
        return String.class;
    }

    @Override
    public final Class<EventType> getValueObjectClass() {
        return EventType.class;
    }

    @Override
    public final boolean isValid(final String value) {
        if (value == null) {
            return true;
        }
        final String trimmed = value.trim();
        return trimmed.length() > 0;
    }

    @Override
    public final EventType toVO(final String value) {
        if (value == null) {
            return null;
        }
        return new EventType(value);
    }

    @Override
    public final String fromVO(final EventType value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

}
