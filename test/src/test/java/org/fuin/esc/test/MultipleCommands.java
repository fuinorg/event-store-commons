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

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.fuin.esc.api.EventStore;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.objects4j.common.Contract;

/**
 * Combines multiple commands into one.
 */
public final class MultipleCommands implements TestCommand {

    private final List<TestCommand> commands;

    /**
     * Default constructor.
     */
    public MultipleCommands() {
        super();
        this.commands = new ArrayList<>();
    }

    /**
     * Constructor with command array.
     * 
     * @param commands
     *            Commands.
     */
    public MultipleCommands(@NotNull final TestCommand... commands) {
        this(EscSpiUtils.asList(commands));
    }

    /**
     * Constructor with command list.
     * 
     * @param commands
     *            Commands.
     */
    public MultipleCommands(@NotNull final List<? extends TestCommand> commands) {
        super();
        Contract.requireArgNotNull("commands", commands);
        this.commands = new ArrayList<>();
        this.commands.addAll(commands);
    }

    /**
     * Adds a new command.
     * 
     * @param command
     *            Command to add.
     */
    public void add(@NotNull final TestCommand command) {
        this.commands.add(command);
    }

    @Override
    public final void init(final EventStore eventStore) {
        for (final TestCommand command : commands) {
            command.init(eventStore);
        }
    }

    @Override
    public final void execute() {
        for (final TestCommand command : commands) {
            command.execute();
        }
    }

    @Override
    public final boolean isSuccessful() {
        for (final TestCommand command : commands) {
            if (!command.isSuccessful()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final String getFailureDescription() {
        final StringBuffer sb = new StringBuffer();
        for (final TestCommand command : commands) {
            if (!command.isSuccessful()) {
                sb.append(command.getFailureDescription());
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public final void verify() {
        if (!isSuccessful()) {            
            throw new RuntimeException("There was at least one failure:\n" + getFailureDescription());
        }
    }

}
