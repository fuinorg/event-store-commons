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
package org.fuin.esc.jaxb;

import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.fuin.esc.api.HasSerializedDataTypeConstant;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.api.IEscEvent;
import org.fuin.esc.api.IEscEvents;
import org.fuin.objects4j.common.Contract;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A list of events.
 */
@HasSerializedDataTypeConstant
@XmlRootElement(name = EscEvents.EL_ROOT_NAME)
public final class EscEvents implements IEscEvents {

    /**
     * Unique name of the type.
     */
    public static final TypeName TYPE = new TypeName(EL_ROOT_NAME);

    /**
     * Unique name of the serialized type.
     */
    public static final SerializedDataType SER_TYPE = new SerializedDataType(TYPE.asBaseType());

    @XmlElement(name = EL_EVENT)
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
     * @param events Event array.
     */
    public EscEvents(@NotNull final EscEvent... events) {
        this(Arrays.asList(events));
    }

    /**
     * Constructor with list.
     *
     * @param events Event list.
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
    public List<IEscEvent> getList() {
        return Collections.unmodifiableList(list);
    }

}
