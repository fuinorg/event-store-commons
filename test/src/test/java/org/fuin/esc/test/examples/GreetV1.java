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
package org.fuin.esc.test.examples;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.TypeName;

/**
 * Version 1 of an example event that carries the raw {@code name} (see {@link GreetV2} for the evolved version).
 * Used by the TCK to prove that an event stored at an older version is up-cast on read.
 */
@XmlRootElement(name = "greet-event")
@XmlAccessorType(XmlAccessType.FIELD)
public class GreetV1 {

    /**
     * Never changing unique event type name (shared by all versions of the greet event).
     */
    public static final TypeName TYPE = new TypeName("GreetEvent");

    /**
     * Unique name of the serialized type.
     */
    public static final SerializedDataType SER_TYPE = new SerializedDataType(TYPE.asBaseType());

    @XmlAttribute(name = "name")
    private String name;

    /**
     * Protected default constructor for deserialization.
     */
    protected GreetV1() {
        super();
    }

    /**
     * Constructor with name.
     *
     * @param name Name to greet.
     */
    public GreetV1(final String name) {
        super();
        this.name = name;
    }

    /**
     * @return the name
     */
    public final String getName() {
        return name;
    }

}
