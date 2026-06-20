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

import org.fuin.objects4j.crypto.EncryptedData;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for the {@link EscEncryptedData} class.
 */
class EscEncryptedDataTest {

    private static final byte[] DATA = "secret".getBytes(StandardCharsets.UTF_8);

    @Test
    public void testConstructorAndGetters() {
        final EscEncryptedData testee = new EscEncryptedData("key-1", "1", "MyEvent",
                "application/json; encoding=UTF-8", DATA);
        assertThat(testee.getKeyId()).isEqualTo("key-1");
        assertThat(testee.getKeyVersion()).isEqualTo("1");
        assertThat(testee.getDataType()).isEqualTo("MyEvent");
        assertThat(testee.getContentType()).isEqualTo("application/json; encoding=UTF-8");
        assertThat(testee.getEncryptedData()).isEqualTo(DATA);
    }

    @Test
    public void testCopyConstructor() {
        final EscEncryptedData original = new EscEncryptedData("key-1", "1", "MyEvent",
                "application/json; encoding=UTF-8", DATA);
        final EncryptedData copy = new EscEncryptedData(original);
        assertThat(copy).isEqualTo(original);
    }

    @Test
    public void testEqualsHashCode() {
        final EscEncryptedData a = new EscEncryptedData("key-1", "1", "MyEvent", "application/json", DATA);
        final EscEncryptedData b = new EscEncryptedData("key-1", "1", "MyEvent", "application/json",
                "secret".getBytes(StandardCharsets.UTF_8));
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    public void testToString() {
        final EscEncryptedData testee = new EscEncryptedData("key-1", "1", "MyEvent",
                "application/json; encoding=UTF-8", DATA);
        assertThat(testee.toString()).contains("key-1").contains("1").contains("MyEvent");
    }

}
