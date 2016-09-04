/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. 
 * http://www.fuin.org/
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
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package org.fuin.esc.test;

import javax.validation.constraints.NotNull;

import org.fuin.esc.api.EventStore;

/**
 * A command in a test scenario.
 */
public interface TestCommand {

    /**
     * Initializes the command before execuing it.
     * 
     * @param currentEventStoreImplType
     *            Type name of the currently tested event store implementation.
     *            Will be used to prefix the stream names to avoid name clashes
     *            for multiple implementations for the same backend store.
     * @param eventStore
     *            Event store to use.
     */
    public void init(@NotNull String currentEventStoreImplType, @NotNull EventStore eventStore);

    /**
     * Executes the command. Exceptions will be catched and are available for
     * verification using the {@link #getException()} method.
     */
    public void execute();

    /**
     * Returns if the command execution was successful. If this method is called
     * before {@link #execute()} was executed, an illegal state exception will
     * be thrown.
     * 
     * @return TRUE if it was successful, else FALSE if it was a failure.
     */
    public boolean isSuccessful();

    /**
     * Returns a description of the failure condition.
     * 
     * @return Expected and current result.
     */
    public String getFailureDescription();

    /**
     * Verifies that the command was successful and throws a runtime exception
     * otherwise.
     */
    public void verify();

}
