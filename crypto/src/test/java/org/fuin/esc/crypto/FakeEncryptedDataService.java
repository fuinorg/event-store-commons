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

import org.fuin.objects4j.crypto.*;

import java.util.HashMap;
import java.util.Map;

/**
 * In-memory reversible {@link EncryptedDataService} for tests. "Encryption" is a symmetric XOR with a seed derived from the key
 * identifier and version, so encryption and decryption are the same operation. Key versions are tracked to support rotation.
 */
public final class FakeEncryptedDataService implements EncryptedDataService {

    private final Map<String, Integer> keys = new HashMap<>();

    @Override
    public boolean keyExists(final String keyId) {
        return keys.containsKey(keyId);
    }

    @Override
    public void createKey(final String keyId) throws DuplicateEncryptionKeyIdException {
        if (keys.containsKey(keyId)) {
            throw new DuplicateEncryptionKeyIdException(keyId);
        }
        keys.put(keyId, 1);
    }

    @Override
    public String rotateKey(final String keyId) throws EncryptionKeyIdUnknownException {
        final int version = current(keyId) + 1;
        keys.put(keyId, version);
        return String.valueOf(version);
    }

    @Override
    public String getKeyVersion(final String keyId) throws EncryptionKeyIdUnknownException {
        return String.valueOf(current(keyId));
    }

    @Override
    public EncryptedData encrypt(final String keyId, final String dataType, final String contentType, final byte[] data)
            throws EncryptionKeyIdUnknownException {
        final int version = current(keyId);
        return new TestEncryptedData(keyId, String.valueOf(version), dataType, contentType, xor(data, keyId, version));
    }

    @Override
    public byte[] decrypt(final EncryptedData encryptedData)
            throws EncryptionKeyIdUnknownException, EncryptionKeyVersionUnknownException, DecryptionFailedException {
        final String keyId = encryptedData.getKeyId();
        final int currentVersion = current(keyId);
        final int version;
        try {
            version = Integer.parseInt(encryptedData.getKeyVersion());
        } catch (final NumberFormatException ex) {
            throw new DecryptionFailedException(ex);
        }
        if (version < 1 || version > currentVersion) {
            throw new EncryptionKeyVersionUnknownException(encryptedData.getKeyVersion());
        }
        return xor(encryptedData.getEncryptedData(), keyId, version);
    }

    private int current(final String keyId) throws EncryptionKeyIdUnknownException {
        final Integer version = keys.get(keyId);
        if (version == null) {
            throw new EncryptionKeyIdUnknownException(keyId);
        }
        return version;
    }

    private static byte[] xor(final byte[] data, final String keyId, final int version) {
        final byte seed = (byte) (keyId.hashCode() ^ version);
        final byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ seed);
        }
        return result;
    }

}
