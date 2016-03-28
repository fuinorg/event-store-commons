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
import javax.json.JsonObjectBuilder;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.Nullable;

/**
 * A structure that contains the system's meta information.
 */
@XmlRootElement(name = "EscSysMeta")
public final class EscSysMeta {

    @XmlElement(name = "data-content-type")
    private String dataContentTypeStr;

    @XmlElement(name = "meta-content-type")
    private String metaContentTypeStr;

    @XmlElement(name = "meta-type")
    private String metaType;

    @XmlTransient
    private EnhancedMimeType dataContentType;

    @XmlTransient
    private EnhancedMimeType metaContentType;

    /**
     * Default constructor for JAXB.
     */
    protected EscSysMeta() {
        super();
    }

    /**
     * Constructor with all mandatory data.
     * 
     * @param dataContentType
     *            Type of the data.
     */
    public EscSysMeta(@NotNull final EnhancedMimeType dataContentType) {
        this(dataContentType, null, null);
    }

    /**
     * Constructor with all data.
     * 
     * @param dataContentType
     *            Type of the data.
     * @param metaContentType
     *            Type of the meta data if meta data is available.
     * @param metaType
     *            Unique name of the meta data type if available.
     */
    public EscSysMeta(@NotNull final EnhancedMimeType dataContentType,
            @Nullable final EnhancedMimeType metaContentType, @Nullable final String metaType) {
        super();
        Contract.requireArgNotNull("dataContentType", dataContentType);

        this.dataContentType = dataContentType;
        this.dataContentTypeStr = dataContentType.toString();
        if (metaContentType != null) {
            this.metaContentType = metaContentType;
            this.metaContentTypeStr = metaContentType.toString();
        }
        this.metaType = metaType;
    }

    /**
     * Returns the type of the data.
     * 
     * @return Data type.
     */
    @NotNull
    public final EnhancedMimeType getDataContentType() {
        if (dataContentType == null) {
            dataContentType = EnhancedMimeType.create(dataContentTypeStr);
        }
        return dataContentType;
    }

    /**
     * Returns the type of the meta data if meta data is available.
     * 
     * @return Meta type.
     */
    @Nullable
    public final EnhancedMimeType getMetaContentType() {
        if ((metaContentType == null) && (metaContentTypeStr != null)) {
            metaContentType = EnhancedMimeType.create(metaContentTypeStr);
        }
        return metaContentType;
    }

    /**
     * Returns the unique name of the meta data type if available.
     * 
     * @return Meta type.
     */
    @Nullable
    public final String getMetaType() {
        return metaType;
    }

    /**
     * Converts the object into a JSON object.
     * 
     * @return JSON object.
     */
    public JsonObject toJson() {
        return Json.createObjectBuilder().add("data-content-type", dataContentTypeStr)
                .add("meta-content-type", metaContentTypeStr).add("meta-type", metaType).build();
    }

}
