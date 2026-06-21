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

import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.TenantId;
import org.fuin.esc.api.TypeName;
import org.fuin.objects4j.common.ThreadSafe;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Selects the encryption key identifier to use for an event. Returning an empty {@link Optional}
 * means the event will be stored unencrypted, which allows selective encryption (for example only
 * for certain tenants or data types) and mixed plaintext/encrypted streams.
 * <p>
 * All implementations are expected to be thread safe.
 */
@ThreadSafe
public interface KeyIdResolver {

    /**
     * Returns the identifier of the key to use for encrypting the given event.
     *
     * @param streamId Stream the event is appended to.
     * @param tenantId Optional tenant the event belongs to.
     * @param dataType Type of the event data.
     * @return Key identifier or an empty {@link Optional} if the event should not be encrypted.
     */
    Optional<String> getKeyId(StreamId streamId, @Nullable TenantId tenantId, TypeName dataType);

}
