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

/**
 * Transparent encryption of event data. The {@link org.fuin.esc.crypto.EncryptingEventStore}
 * decorates any other event store and encrypts the event data on write and decrypts it on read,
 * keeping the concrete implementations (memory, JPA, gRPC) unaware of encryption.
 */
@NullMarked
package org.fuin.esc.crypto;

import org.jspecify.annotations.NullMarked;
