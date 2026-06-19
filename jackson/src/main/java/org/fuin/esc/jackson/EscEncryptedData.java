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

import jakarta.validation.constraints.NotEmpty;
import org.fuin.esc.api.HasSerializedDataTypeConstant;
import org.fuin.esc.api.IBaseType;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.TypeName;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.crypto.EncryptedData;

import java.io.Serial;
import java.util.Arrays;

/**
 * Jackson representation of {@link EncryptedData}. It wraps the encrypted bytes of an event together with the information needed to
 * decrypt it again (key identifier, key version, original data type and content type). Equals and hash code are based on all data.
 */
@HasSerializedDataTypeConstant
@SuppressWarnings("NullAway.Init") // Fields are populated by the Jackson deserializer
public final class EscEncryptedData implements EncryptedData, IBaseType {

    /** Unique XML/JSON root element name of the type. */
    public static final String EL_ROOT_NAME = "EscEncryptedData";

    /** Unique name of the type. */
    public static final TypeName TYPE = new TypeName(EL_ROOT_NAME);

    /** Unique name of the serialized type. */
    public static final SerializedDataType SER_TYPE = new SerializedDataType(TYPE.asBaseType());

    @Serial
    private static final long serialVersionUID = 1000L;

    @NotEmpty
    private String keyId;

    @NotEmpty
    private String keyVersion;

    @NotEmpty
    private String dataType;

    @NotEmpty
    private String contentType;

    @NotEmpty
    private byte[] encryptedData;

    /**
     * Default constructor for deserialization (Jackson).
     */
    protected EscEncryptedData() {
        super();
    }

    /**
     * Constructor with all mandatory data.
     *
     * @param keyId         Unique identifier of the private key used.
     * @param keyVersion    Version of the private key used.
     * @param dataType      Unique type of the data like "UserPersonalData" or even a fully qualified class name.
     * @param contentType   Content/Mime type like "application/json; encoding=UTF-8; version=1".
     * @param encryptedData Encrypted data.
     */
    public EscEncryptedData(@NotEmpty final String keyId, @NotEmpty final String keyVersion, @NotEmpty final String dataType,
                            @NotEmpty final String contentType, @NotEmpty final byte[] encryptedData) {
        super();
        Contract.requireArgNotEmpty("keyId", keyId);
        Contract.requireArgNotEmpty("keyVersion", keyVersion);
        Contract.requireArgNotEmpty("dataType", dataType);
        Contract.requireArgNotEmpty("contentType", contentType);
        Contract.requireArgNotNull("encryptedData", encryptedData);
        this.keyId = keyId;
        this.keyVersion = keyVersion;
        this.dataType = dataType;
        this.contentType = contentType;
        this.encryptedData = encryptedData;
    }

    /**
     * Copy constructor that creates an instance from any other {@link EncryptedData}.
     *
     * @param other Encrypted data to copy.
     */
    public EscEncryptedData(final EncryptedData other) {
        this(other.getKeyId(), other.getKeyVersion(), other.getDataType(), other.getContentType(), other.getEncryptedData());
    }

    @Override
    @NotEmpty
    public String getKeyId() {
        return keyId;
    }

    @Override
    @NotEmpty
    public String getKeyVersion() {
        return keyVersion;
    }

    @Override
    @NotEmpty
    public String getDataType() {
        return dataType;
    }

    @Override
    @NotEmpty
    public String getContentType() {
        return contentType;
    }

    @Override
    @NotEmpty
    public byte[] getEncryptedData() {
        return encryptedData;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((contentType == null) ? 0 : contentType.hashCode());
        result = prime * result + ((dataType == null) ? 0 : dataType.hashCode());
        result = prime * result + Arrays.hashCode(encryptedData);
        result = prime * result + ((keyId == null) ? 0 : keyId.hashCode());
        result = prime * result + ((keyVersion == null) ? 0 : keyVersion.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final EscEncryptedData other = (EscEncryptedData) obj;
        return java.util.Objects.equals(contentType, other.contentType)
                && java.util.Objects.equals(dataType, other.dataType)
                && Arrays.equals(encryptedData, other.encryptedData)
                && java.util.Objects.equals(keyId, other.keyId)
                && java.util.Objects.equals(keyVersion, other.keyVersion);
    }

    @Override
    public String toString() {
        return "EscEncryptedData [keyId=" + keyId + ", keyVersion=" + keyVersion + ", dataType=" + dataType
                + ", contentType=" + contentType + "]";
    }

}
