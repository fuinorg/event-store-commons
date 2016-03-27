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
package org.fuin.esc.spi;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.fuin.objects4j.common.Contract;

/**
 * A list of events.
 */
@XmlRootElement(name = "Events")
public final class EscEvents {

    @XmlElement(name = "Event")
    private List<EscEvent> list;

    /**
     * Default constructor for JAXB.
     */
    protected EscEvents() {
        super();
    }

    /**
     * Constructor with array.
     * 
     * @param events
     *            Event array.
     */
    public EscEvents(@NotNull final EscEvent... events) {
        this(Arrays.asList(events));
    }

    /**
     * Constructor with list.
     * 
     * @param events
     *            Event list.
     */
    public EscEvents(@NotNull final List<EscEvent> events) {
        super();
        Contract.requireArgNotNull("events", events);
        this.list = events;
    }

    /**
     * Returns an immutable event list.
     * 
     * @return Unmodifiable list of events.
     */
    public final List<EscEvent> getList() {
        return Collections.unmodifiableList(list);
    }

}
