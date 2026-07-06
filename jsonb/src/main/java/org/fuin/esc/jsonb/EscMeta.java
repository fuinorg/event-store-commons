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
package org.fuin.esc.jsonb;

import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.*;
import org.fuin.objects4j.common.ConstraintViolationException;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.ImmutableAfterUnmarshal;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * A structure that contains the user's metadata and the system's meta information.
 */
@ImmutableAfterUnmarshal // Setters only called during deserialization in {@link EscMetaJsonbSerializerDeserializer}
@HasSerializedDataTypeConstant
public final class EscMeta implements IEscMeta {

    private String dataType;

    private String dataContentTypeStr;

    @Nullable
    private String tenantId;

    @Nullable
    private String metaType;

    @Nullable
    private String metaContentTypeStr;

    @Nullable
    private Object meta;

    @Nullable
    private List<String> categories;

    private transient EnhancedMimeType dataContentType;

    @Nullable
    private transient EnhancedMimeType metaContentType;

    /**
     * Default constructor for JAXB.
     */
    @SuppressWarnings("NullAway.Init") // Fields are populated by the JSON-B deserializer
    protected EscMeta() {
        super();
    }

    /**
     * Constructor with all mandatory data.
     *
     * @param dataType        Type of the data.
     * @param dataContentType Content type of the data.
     */
    public EscMeta(final String dataType, final EnhancedMimeType dataContentType) {
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
    public EscMeta(final String dataType,
                   final EnhancedMimeType dataContentType,
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
    public EscMeta(final String dataType,
                   final EnhancedMimeType dataContentType,
                   @Nullable final String metaType,
                   @Nullable final EnhancedMimeType metaContentType,
                   @Nullable final Object meta,
                   @Nullable final TenantId tenantId) {
        this(dataType, dataContentType, metaType, metaContentType, meta, tenantId, List.of());
    }

    /**
     * Constructor with all data including categories.
     *
     * @param dataType        Type of the data.
     * @param dataContentType Type of the data.
     * @param metaType        Unique name of the metadata. Must be non-null if 'meta' is not null.
     * @param metaContentType Type of the metadata. Must be non-null if 'meta' is not null.
     * @param meta            Metadata object, if available.
     * @param tenantId        Optional unique tenant identifier.
     * @param categories      Category names the event belongs to (never {@literal null}, may be empty).
     */
    public EscMeta(final String dataType,
                   final EnhancedMimeType dataContentType,
                   @Nullable final String metaType,
                   @Nullable final EnhancedMimeType metaContentType,
                   @Nullable final Object meta,
                   @Nullable final TenantId tenantId,
                   final List<String> categories) {
        super();
        Contract.requireArgNotNull("dataType", dataType);
        Contract.requireArgNotNull("dataContentType", dataContentType);
        Contract.requireArgNotNull("categories", categories);
        if (meta != null) {
            if (metaType == null) {
                throw new ConstraintViolationException("The argument 'metaType' cannot be null");
            }
            if (metaContentType == null) {
                throw new ConstraintViolationException("The argument 'metaContentType' cannot be null");
            }
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
        this.categories = categories.isEmpty() ? null : List.copyOf(categories);
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
    void setDataType(final String dataType) {
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
            dataContentType = Objects.requireNonNull(EnhancedMimeType.create(dataContentTypeStr));
        }
        return dataContentType;
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
     * Sets the type of the data.
     *
     * @param dataContentType Data type.
     */
    void setDataContentType(EnhancedMimeType dataContentType) {
        Contract.requireArgNotNull("dataContentType", dataContentType);
        this.dataContentType = dataContentType;
        this.dataContentTypeStr = dataContentType.toString();
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

    @Override
    public List<String> getCategories() {
        return categories == null ? List.of() : categories;
    }

    /**
     * Sets the category names.
     *
     * @param categories Category names (or {@literal null} / empty for none).
     */
    void setCategories(@Nullable final List<String> categories) {
        this.categories = (categories == null || categories.isEmpty()) ? null : List.copyOf(categories);
    }

}
