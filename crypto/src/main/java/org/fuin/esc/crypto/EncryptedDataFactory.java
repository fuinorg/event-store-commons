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
package org.fuin.esc.crypto;

import org.fuin.esc.api.SerializedDataType;
import org.fuin.objects4j.common.ThreadSafe;
import org.fuin.objects4j.crypto.EncryptedData;

/**
 * Converts the contract {@link EncryptedData} returned by the {@link org.fuin.objects4j.crypto.EncryptedDataService} into a
 * representation that can be serialized by the underlying event store. This is the seam that keeps the {@link EncryptingEventStore}
 * independent of the concrete serialization format (JSON-B, JAXB, Jackson, ...).
 * <p>
 * All implementations are expected to be thread safe.
 */
@ThreadSafe
public interface EncryptedDataFactory {

    /**
     * Returns the unique type under which the encrypted data representation is registered in the
     * serializer/deserializer registries of the underlying event store.
     *
     * @return Serialized data type of the encrypted wrapper.
     */
    SerializedDataType getType();

    /**
     * Creates a serializable representation of the given encrypted data. The returned object must be
     * registered for serialization under {@link #getType()} and must implement {@link EncryptedData}
     * so it can be detected and decrypted on read.
     *
     * @param encryptedData Encrypted data to wrap.
     * @return Serializable representation (an {@link EncryptedData} instance).
     */
    EncryptedData create(EncryptedData encryptedData);

}
