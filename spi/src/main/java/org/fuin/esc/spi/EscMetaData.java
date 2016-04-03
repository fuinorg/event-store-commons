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

import javax.json.Json;
import javax.json.JsonObject;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.Nullable;

/**
 * A structure that contains meta data.
 */
@XmlRootElement(name = "MetaData")
public final class EscMetaData {

    @XmlElement(name = "EscMeta")
    private EscMeta meta;

    /**
     * Default constructor for JAXB.
     */
    protected EscMetaData() {
        super();
    }

    /**
     * Constructor with mandatory data.
     * 
     * @param meta
     *            Meta information.
     */
    public EscMetaData(@NotNull final EscMeta meta) {
        super();
        Contract.requireArgNotNull("sysMeta", meta);
        this.meta = meta;
    }

    /**
     * Returns the meta information.
     * 
     * @return Meta data.
     */
    @NotNull
    public final EscMeta getEscMeta() {
        return meta;
    }

    /**
     * Converts the object into a JSON object.
     * 
     * @return JSON object.
     */
    public JsonObject toJson() {
        return Json.createObjectBuilder().add("EscMeta", meta.toJson()).build();
    }
}
