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

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.fuin.objects4j.common.Contract;

/**
 * A structure that contains the user's meta data.
 */
@XmlRootElement(name = "EscUserMeta")
public class EscUserMeta {

    @XmlAnyElement(lax = true)
    private Object meta;

    /**
     * Default constructor for JAXB.
     */
    protected EscUserMeta() {
        super();
    }

    /**
     * Constructor with mandatory data.
     * 
     * @param meta
     *            User's meta data.
     */
    public EscUserMeta(@NotNull final Object meta) {
        super();
        Contract.requireArgNotNull("meta", meta);
        this.meta = meta;
    }

    /**
     * Returns the user's meta data.
     * 
     * @return Meta data.
     */
    @NotNull
    public Object getMeta() {
        return meta;
    }

}
