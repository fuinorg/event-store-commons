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

import org.fuin.objects4j.jsonb.JsonbProvider;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for the {@link EscEncryptedData} JSON-B binding.
 */
class EscEncryptedDataTest extends AbstractTest {

    @Test
    public void testMarshalUnmarshalRoundTrip() throws Exception {

        // PREPARE
        final EscEncryptedData original = new EscEncryptedData("key-1", "1", "MyEvent",
                "application/json; encoding=UTF-8", "secret".getBytes(StandardCharsets.UTF_8));

        try (final JsonbProvider provider = getJsonbProvider()) {

            // TEST
            final String json = provider.jsonb().toJson(original);
            final EscEncryptedData copy = provider.jsonb().fromJson(json, EscEncryptedData.class);

            // VERIFY
            assertThat(json).contains("key-id").contains("key-version").contains("data-type")
                    .contains("content-type").contains("encrypted-data");
            assertThat(copy).isEqualTo(original);

        }

    }

}
