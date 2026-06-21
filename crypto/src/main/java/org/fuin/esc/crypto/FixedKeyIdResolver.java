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

import jakarta.validation.constraints.NotEmpty;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.TenantId;
import org.fuin.esc.api.TypeName;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.Immutable;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Always returns the same key identifier for every event. Useful for simple
 * setups and tests where a single key encrypts all events.
 */
@Immutable
public final class FixedKeyIdResolver implements KeyIdResolver {

    private final String keyId;

    /**
     * Constructor with the key identifier to always return.
     *
     * @param keyId Key identifier used for all events.
     */
    public FixedKeyIdResolver(@NotEmpty final String keyId) {
        Contract.requireArgNotEmpty("keyId", keyId);
        this.keyId = keyId;
    }

    @Override
    public Optional<String> getKeyId(final StreamId streamId, @Nullable final TenantId tenantId, final TypeName dataType) {
        return Optional.of(keyId);
    }

}
