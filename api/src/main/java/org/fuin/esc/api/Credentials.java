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

import java.util.Optional;

import javax.validation.constraints.NotNull;

import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.ContractViolationException;
import org.fuin.objects4j.common.Immutable;
import org.fuin.objects4j.common.NotEmpty;
import org.fuin.objects4j.vo.ValueObject;

/**
 * A combination of user name and password. Equals, hash code and comparable are
 * based on the user name.
 */
@Immutable
public final class Credentials implements ValueObject, Comparable<Credentials> {

    /** No credentials. */
    public static final Optional<Credentials> NONE = Optional.empty();

    private final String username;

    private final char[] password;

    /**
     * Constructor with all mandatory data.
     * 
     * @param username
     *            User name.
     * @param password
     *            Password.
     */
    public Credentials(@NotEmpty final String username,
            @NotNull final char[] password) {
        Contract.requireArgNotEmpty("username", username);
        Contract.requireArgNotNull("password", password);
        if (password.length == 0) {
            throw new ContractViolationException("The argument 'password' cannot be an empty array");
        }
        this.username = username;
        this.password = password;
    }

    /**
     * Returns the user name.
     * 
     * @return Unique name of the user.
     */
    @NotEmpty
    public final String getUsername() {
        return username;
    }

    /**
     * Returns the password.
     * 
     * @return Password used to log in.
     */
    @NotNull
    public final char[] getPassword() {
        return password;
    }

    @Override
    public final String toString() {
        return username;
    }

    @Override
    public final int compareTo(final Credentials other) {
        return username.compareTo(other.username);
    }

    @Override
    public final int hashCode() {
        return username.hashCode();
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Credentials)) {
            return false;
        }
        final Credentials other = (Credentials) obj;
        return username.equals(other.username);
    }

}
