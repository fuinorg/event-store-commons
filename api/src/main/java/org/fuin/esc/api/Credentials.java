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

/**
 * A combination of user name and password.
 */
public final class Credentials {

    /** No credentials. */
    public static final Optional<Credentials> NONE = Optional.empty();

    private String username;

    private char[] password;

    /**
     * Constructor with all mandatory data.
     * 
     * @param username
     *            User name.
     * @param password
     *            Password.
     */
    public Credentials(@NotNull final String username,
            @NotNull final char[] password) {
        Contract.requireArgNotNull("username", username);
        Contract.requireArgNotNull("password", password);
        this.username = username;
        this.password = password;
    }

    /**
     * Returns the user name.
     * 
     * @return Unique name of the user.
     */
    @NotNull
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

}
