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
package org.fuin.esc.jackson;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.HasSerializedDataTypeConstant;
import org.fuin.esc.api.IEscMeta;
import org.fuin.esc.api.SimpleTenantId;
import org.fuin.esc.api.TenantId;
import org.fuin.objects4j.common.Contract;

/**
 * A structure that contains the user's metadata and the system's meta information.
 */
@HasSerializedDataTypeConstant
public final class EscMeta implements IEscMeta {

    private String dataType;

    private String dataContentTypeStr;

    private String tenantId;

    private String metaType;

    private String metaContentTypeStr;

    private Object meta;

    private transient EnhancedMimeType dataContentType;

    private transient EnhancedMimeType metaContentType;

    /**
     * Default constructor for Jackson.
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
        this(dataType, dataContentType, null, null, null, null);
    }

    /**
     * Constructor with all data except tenant.
     *
     * @param dataType        Type of the data.
     * @param dataContentType Type of the data.
     * @param metaType        Unique name of the metadata. Must be non-null if 'meta' is not null.
     * @param metaContentType Type of the metadata. Must be non-null if 'meta' is not null.
     * @param meta            Metadata object, if available.
     */
    public EscMeta(@NotNull final String dataType,
                   @NotNull final EnhancedMimeType dataContentType,
                   @Nullable final String metaType,
                   @Nullable final EnhancedMimeType metaContentType,
                   @Nullable final Object meta) {
        this(dataType, dataContentType, metaType, metaContentType, meta, null);
    }

    /**
     * Constructor with all data.
     *
     * @param dataType        Type of the data.
     * @param dataContentType Type of the data.
     * @param metaType        Unique name of the metadata. Must be non-null if 'meta' is not null.
     * @param metaContentType Type of the metadata. Must be non-null if 'meta' is not null.
     * @param meta            Metadata object, if available.
     * @param tenantId        Optional unique tenant identifier.
     */
    public EscMeta(@NotNull final String dataType,
                   @NotNull final EnhancedMimeType dataContentType,
                   @Nullable final String metaType,
                   @Nullable final EnhancedMimeType metaContentType,
                   @Nullable final Object meta,
                   @Nullable final TenantId tenantId) {
        super();
        Contract.requireArgNotNull("dataType", dataType);
        Contract.requireArgNotNull("dataContentType", dataContentType);
        if (meta != null) {
            Contract.requireArgNotNull("metaType", metaType);
            Contract.requireArgNotNull("metaContentType", metaContentType);
        }

        this.dataType = dataType;
        this.dataContentType = dataContentType;
        this.dataContentTypeStr = dataContentType.toString();
        this.metaType = metaType;
        this.metaContentType = metaContentType;
        if (metaContentType != null) {
            this.metaContentTypeStr = metaContentType.toString();
        }
        this.meta = meta;
        this.tenantId = tenantId == null ? null : tenantId.asString();
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
     * Sets the unique name of the data type.
     *
     * @param dataType Data type.
     */
    void setDataType(@NotNull final String dataType) {
        Contract.requireArgNotNull("dataType", dataType);
        this.dataType = dataType;
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
     * Sets the type of the data.
     *
     * @param dataContentType Data type.
     */
    void setDataContentType(EnhancedMimeType dataContentType) {
        Contract.requireArgNotNull("dataContentType", dataContentType);
        this.dataContentType = dataContentType;
        this.dataContentTypeStr = dataContentType.toString();
    }

    @Nullable
    @Override
    public TenantId getTenantId() {
        if (tenantId == null) {
            return null;
        }
        return new SimpleTenantId(tenantId);
    }

    /**
     * Returns the unique tenant identifier.
     *
     * @param tenantId Optional tenant ID.
     */
    void setTenantId(@Nullable final TenantId tenantId) {
        if (tenantId == null) {
            this.tenantId = null;
        } else {
            this.tenantId = tenantId.toString();
        }
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
     * Sets the unique name of the meta data type if available.
     *
     * @param metaType Meta type.
     */
    public void setMetaType(@Nullable final String metaType) {
        this.metaType = metaType;
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
     * Sets the type of the metadata, if available.
     *
     * @param metaContentType Meta content type.
     */
    public void setMetaContentType(@Nullable final EnhancedMimeType metaContentType) {
        this.metaContentType = metaContentType;
        if (metaContentType == null) {
            this.metaContentTypeStr = null;
        } else {
            this.metaContentTypeStr = metaContentType.toString();
        }
    }

    /**
     * Returns the meta data object.
     *
     * @return Meta data object.
     */
    @Nullable
    public Object getMeta() {
        return meta;
    }

    /**
     * Sets the meta data object.
     *
     * @param meta Meta data object.
     */
    void setMeta(@Nullable final Object meta) {
        this.meta = meta;
    }

}
