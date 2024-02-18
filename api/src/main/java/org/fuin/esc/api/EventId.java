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

import jakarta.validation.constraints.NotNull;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.ValueObjectWithBaseType;
import org.fuin.objects4j.core.UUIDStr;
import org.fuin.objects4j.core.UUIDStrValidator;

import javax.annotation.concurrent.Immutable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a unique event identifier based on a UUID.
 */
@Immutable
public final class EventId implements ValueObjectWithBaseType<UUID>, Comparable<EventId>, Serializable {

    private static final long serialVersionUID = 1000L;

    private final long mostSigBits;

    private final long leastSigBits;

    // Do not access this variable directly inside this class.
    // Use <code>asBaseType()</code> to be sure this is initialized.
    private transient UUID uuid;

    // Do not access this variable directly inside this class.
    // Use <code>toString()</code> to be sure this is initialized.
    private transient String str;

    /**
     * Creates a new event ID using a random UUID.
     */
    public EventId() {
        this(UUID.randomUUID());
    }

    /**
     * Creates a new event ID using the given string.
     *
     * @param value
     *            String that represents a UUID.
     */
    public EventId(@NotNull @UUIDStr final String value) {
        this(parseArg("value", value));
    }

    /**
     * Creates a new event ID using the given value.
     *
     * @param value
     *            UUID to use.
     */
    public EventId(@NotNull final UUID value) {
        super();
        Contract.requireArgNotNull("value", value);
        this.uuid = value;
        this.mostSigBits = uuid.getMostSignificantBits();
        this.leastSigBits = uuid.getLeastSignificantBits();
    }

    @Override
    public Class<UUID> getBaseType() {
        return UUID.class;
    }

    @Override
    public UUID asBaseType() {
        if (uuid == null) {
            uuid = new UUID(mostSigBits, leastSigBits);
        }
        return uuid;
    }

    @Override
    public String toString() {
        if (str == null) {
            str = uuid.toString();
        }
        return str;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final EventId eventId = (EventId) o;
        return mostSigBits == eventId.mostSigBits && leastSigBits == eventId.leastSigBits;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mostSigBits, leastSigBits);
    }

    @Override
    public int compareTo(final EventId other) {
        return asBaseType().compareTo(other.asBaseType());
    }

    private static UUID parseArg(final String name, final String value) {
        Contract.requireArgNotNull(name, value);
        return UUIDStrValidator.parseArg(name, value);
    }

}
