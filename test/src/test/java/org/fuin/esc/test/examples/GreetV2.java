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

/**
 * Version 2 of the example event that carries the fully rendered {@code greeting} instead of the raw name of
 * {@link GreetV1}. Defines equals/hashCode on the greeting so the TCK can compare the up-cast read result with
 * the expected event.
 */
@XmlRootElement(name = "greet-event")
@XmlAccessorType(XmlAccessType.FIELD)
public class GreetV2 {

    @XmlAttribute(name = "greeting")
    private String greeting;

    /**
     * Protected default constructor for deserialization.
     */
    protected GreetV2() {
        super();
    }

    /**
     * Constructor with greeting.
     *
     * @param greeting Fully rendered greeting.
     */
    public GreetV2(final String greeting) {
        super();
        this.greeting = greeting;
    }

    /**
     * @return the greeting
     */
    public final String getGreeting() {
        return greeting;
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((greeting == null) ? 0 : greeting.hashCode());
        return result;
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GreetV2 other = (GreetV2) obj;
        if (greeting == null) {
            return other.greeting == null;
        }
        return greeting.equals(other.greeting);
    }

    @Override
    public final String toString() {
        return "GreetV2 [greeting=" + greeting + "]";
    }

}
