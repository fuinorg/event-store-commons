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

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.HasSerializedDataTypeConstant;
import org.fuin.esc.api.IEscMeta;
import org.fuin.objects4j.common.Contract;

/**
 * A structure that contains the user's metadata and the system's meta information.
 */
@HasSerializedDataTypeConstant
@XmlRootElement(name = EscMeta.EL_ROOT_NAME)
public final class EscMeta implements IEscMeta {

    @XmlElement(name = EL_DATA_TYPE)
    private String dataType;

    @XmlElement(name = EL_DATA_CONTENT_TYPE)
    private String dataContentTypeStr;

    @XmlElement(name = EL_META_TYPE)
    private String metaType;

    @XmlElement(name = EL_META_CONTENT_TYPE)
    private String metaContentTypeStr;

    @XmlAnyElement(lax = true)
    private Object meta;

    @XmlTransient
    private EnhancedMimeType dataContentType;

    @XmlTransient
    private EnhancedMimeType metaContentType;

    /**
     * Default constructor for JAXB.
     */
    protected EscMeta() {
        super();
    }

    /**
     * Constructor with all mandatory data.
     *
     * @param dataType        Type of the data.
     * @param dataContentType Content type of the data.
     */
    public EscMeta(@NotNull final String dataType, @NotNull final EnhancedMimeType dataContentType) {
        this(dataType, dataContentType, null, null, null);
    }

    /**
     * Constructor with all data.
     *
     * @param dataType        Type of the data.
     * @param dataContentType Type of the data.
     * @param metaType        Unique name of the meta data type if available.
     * @param metaContentType Type of the meta data if meta data is available.
     * @param meta            Meta data object if available.
     */
    public EscMeta(@NotNull final String dataType, @NotNull final EnhancedMimeType dataContentType, @Nullable final String metaType,
                   @Nullable final EnhancedMimeType metaContentType, @Nullable final Object meta) {
        super();
        Contract.requireArgNotNull("dataType", dataType);
        Contract.requireArgNotNull("dataContentType", dataContentType);

        this.dataType = dataType;
        this.dataContentType = dataContentType;
        this.dataContentTypeStr = dataContentType.toString();
        this.metaType = metaType;
        this.metaContentType = metaContentType;
        if (metaContentType != null) {
            this.metaContentTypeStr = metaContentType.toString();
        }
        this.meta = meta;
    }

    /**
     * Returns the unique name of the data type.
     *
     * @return Data type.
     */
    @NotNull
    public String getDataType() {
        return dataType;
    }

    /**
     * Returns the type of the data.
     *
     * @return Data type.
     */
    @NotNull
    public EnhancedMimeType getDataContentType() {
        if (dataContentType == null) {
            dataContentType = EnhancedMimeType.create(dataContentTypeStr);
        }
        return dataContentType;
    }

    /**
     * Returns the unique name of the meta data type if available.
     *
     * @return Meta type.
     */
    @Nullable
    public String getMetaType() {
        return metaType;
    }

    /**
     * Returns the type of the metadata, if available.
     *
     * @return Meta type.
     */
    @Nullable
    public EnhancedMimeType getMetaContentType() {
        if ((metaContentType == null) && (metaContentTypeStr != null)) {
            metaContentType = EnhancedMimeType.create(metaContentTypeStr);
        }
        return metaContentType;
    }

    /**
     * Returns the meta data object.
     *
     * @return Meta data object.
     */
    @NotNull
    public Object getMeta() {
        return meta;
    }

}
