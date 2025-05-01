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

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link Base64Data} class.
 */
class Base64DataTest {

    @Test
    void testCreateBytes() {

        // PREPARE
        final byte[] data = "Hello world!".getBytes(StandardCharsets.UTF_8);
        final String base64 = "SGVsbG8gd29ybGQh";

        // TEST
        final Base64Data testee = new Base64Data(data);

        // VERIFY
        assertThat(testee.getDecoded()).isEqualTo(data);
        assertThat(testee.getEncoded()).isEqualTo(base64);

    }

    @Test
    void testCreateBase64() {

        // PREPARE
        final byte[] data = new byte[]{72, 101, 108, 108, 111, 32, 119, 111, 114, 108, 100, 33};
        final String base64 = "SGVsbG8gd29ybGQh";

        // TEST
        final Base64Data testee = new Base64Data(base64);

        // VERIFY
        assertThat(testee.getDecoded()).isEqualTo(data);
        assertThat(testee.getEncoded()).isEqualTo(base64);

    }

}
