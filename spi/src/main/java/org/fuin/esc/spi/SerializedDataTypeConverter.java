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
package org.fuin.esc.spi;

import javax.persistence.AttributeConverter;

import org.fuin.objects4j.common.ThreadSafe;
import org.fuin.objects4j.vo.AbstractValueObjectConverter;

/**
 * Creates a {@link SerializedDataType}.
 */
@ThreadSafe
public final class SerializedDataTypeConverter extends
        AbstractValueObjectConverter<String, SerializedDataType> implements
        AttributeConverter<SerializedDataType, String> {

    @Override
    public Class<String> getBaseTypeClass() {
        return String.class;
    }

    @Override
    public final Class<SerializedDataType> getValueObjectClass() {
        return SerializedDataType.class;
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
    public final SerializedDataType toVO(final String value) {
        if (value == null) {
            return null;
        }
        return new SerializedDataType(value);
    }

    @Override
    public final String fromVO(final SerializedDataType value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

}
