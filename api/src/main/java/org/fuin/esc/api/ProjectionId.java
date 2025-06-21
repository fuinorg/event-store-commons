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
import org.fuin.objects4j.common.ConstraintViolationException;
import org.fuin.objects4j.common.Contract;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.io.Serial;
import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * Projection identifier that is based on a name with a restricted character set.
 * See {@link #PATTERN}.
 */
@Immutable
public final class ProjectionId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final Pattern PATTERN = Pattern.compile("^[a-z|A-Z][a-z|A-Z|0-9|_|-]*[a-z|A-Z|0-9]$");

    /** Minimal length of a valid value. */
    public static final int MIN_LENGTH = 2;

    /** Maximum length of a valid value. */
    public static final int MAX_LENGTH = 1000;

    private final String name;

    /**
     * Constructor for projection.
     *
     * @param name Unique name.
     */
    public ProjectionId(@NotNull final String name) {
        Contract.requireArgNotNull("name", name);
        requireArgValid("name", name);
        this.name = name;
    }

    /**
     * Returns the name of the projection.
     *
     * @return Unique name.
     */
    @NotNull
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ProjectionId other = (ProjectionId) obj;
        return name.equals(other.name);
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Verifies that a given string can be converted into the type.
     *
     * @param value
     *            Value to validate.
     *
     * @return Returns <code>true</code> if it's a valid type else <code>false</code>.
     */
    public static boolean isValid(final String value) {
        if (value == null) {
            return true;
        }
        if (value.length() < MIN_LENGTH) {
            return false;
        }
        final String trimmed = value.trim();
        if (trimmed.length() > MAX_LENGTH) {
            return false;
        }
        return PATTERN.matcher(trimmed).matches();
    }

    /**
     * Converts the given string into the type.
     *
     * @param value
     *            Value to convert.
     *
     * @return Returns <code>true</code> if it's a valid type else <code>false</code>.
     */
    public static ProjectionId valueOf(@Nullable final String value) {
        if (value == null) {
            return null;
        }
        return new ProjectionId(value);
    }

    /**
     * Verifies if the argument is valid and throws an exception if this is not the case.
     *
     * @param name
     *            Name of the value for a possible error message.
     * @param value
     *            Value to check.
     *
     * @throws ConstraintViolationException
     *             The value was not valid.
     */
    public static void requireArgValid(@NotNull final String name, @NotNull final String value) throws ConstraintViolationException {
        if (!isValid(value)) {
            throw new ConstraintViolationException("The argument '" + name + "' is not valid: '" + value + "'");
        }
    }

}
