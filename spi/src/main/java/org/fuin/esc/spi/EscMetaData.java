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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.Nullable;

/**
 * A structure that contains the user's meta data and the system's meta information.
 */
@XmlRootElement(name = "MetaData")
public final class EscMetaData {

    @XmlElement(name = "EscUserMeta")
    private DataWrapper userMeta;

    @XmlElement(name = "EscSysMeta")
    private EscSysMeta sysMeta;

    /**
     * Default constructor for JAXB.
     */
    protected EscMetaData() {
        super();
    }

    /**
     * Constructor with mandatory data.
     * 
     * @param sysMeta
     *            System's meta information.
     */
    public EscMetaData(@NotNull final EscSysMeta sysMeta) {
        this(sysMeta, null);
    }

    /**
     * Constructor with all data.
     * 
     * @param sysMeta
     *            System's meta information.
     * @param userMeta
     *            User's meta data if available.
     */
    public EscMetaData(@NotNull final EscSysMeta sysMeta, @Nullable final DataWrapper userMeta) {
        super();
        Contract.requireArgNotNull("sysMeta", sysMeta);
        this.userMeta = userMeta;
        this.sysMeta = sysMeta;
    }

    /**
     * Returns the system's meta information.
     * 
     * @return System meta data.
     */
    @NotNull
    public final EscSysMeta getSysMeta() {
        return sysMeta;
    }

    /**
     * Returns the user's meta data if available.
     * 
     * @return User meta data.
     */
    @Nullable
    public final DataWrapper getUserMeta() {
        return userMeta;
    }

}
