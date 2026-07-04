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
package org.fuin.esc.spi;

import org.fuin.esc.api.Converter;
import org.fuin.esc.api.ConverterRegistry;
import org.fuin.esc.api.Deserializer;
import org.fuin.esc.api.DeserializerRegistry;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.SimpleConverterRegistry;
import org.fuin.esc.api.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.api.UpcastingDeserializerRegistry;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link UpcastingDeserializerRegistry} class wired through {@link EscSpiUtils#deserialize}.
 */
public class UpcastingDeserializerRegistryTest {

    private static final EnhancedMimeType DEFAULT_MIME_TYPE = EnhancedMimeType.create("application", "json");

    private static final SerializedDataType TYPE = new SerializedDataType("MyEvent");

    @Test
    public void testDeserializeUpcastsStoredVersion() {

        // PREPARE: a v1 deserializer plus a v1 -> v2 converter
        final EnhancedMimeType mimeV1 = EnhancedMimeType.create("text", "plain", StandardCharsets.UTF_8, "1");
        final DeserializerRegistry delegate = new SimpleSerializerDeserializerRegistry.Builder(DEFAULT_MIME_TYPE)
                .add(TYPE, dummyDeserializer(), mimeV1)
                .build();
        final ConverterRegistry converters = new SimpleConverterRegistry.Builder()
                .add(TYPE, "1", "2", append("|v2"))
                .build();
        final DeserializerRegistry upcasting = new UpcastingDeserializerRegistry(delegate, converters);
        final SerializedData storedV1 = new SerializedData(TYPE, mimeV1, "raw".getBytes(StandardCharsets.UTF_8));

        // TEST
        final Object result = EscSpiUtils.deserialize(upcasting, storedV1);

        // VERIFY: deserialized "raw" then lifted to v2
        assertThat(result).isEqualTo("raw|v2");

    }

    @Test
    public void testDeserializeLeavesLatestUnchanged() {

        // PREPARE: a v2 deserializer and no converter for v2 (already latest)
        final EnhancedMimeType mimeV2 = EnhancedMimeType.create("text", "plain", StandardCharsets.UTF_8, "2");
        final DeserializerRegistry delegate = new SimpleSerializerDeserializerRegistry.Builder(DEFAULT_MIME_TYPE)
                .add(TYPE, dummyDeserializer(), mimeV2)
                .build();
        final ConverterRegistry converters = new SimpleConverterRegistry.Builder()
                .add(TYPE, "1", "2", append("|v2"))
                .build();
        final DeserializerRegistry upcasting = new UpcastingDeserializerRegistry(delegate, converters);
        final SerializedData storedV2 = new SerializedData(TYPE, mimeV2, "raw".getBytes(StandardCharsets.UTF_8));

        // TEST
        final Object result = EscSpiUtils.deserialize(upcasting, storedV2);

        // VERIFY: no v2 -> ... converter, so returned unchanged
        assertThat(result).isEqualTo("raw");

    }

    private static Deserializer dummyDeserializer() {
        return new Deserializer() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> T unmarshal(final Object data, final SerializedDataType type, final EnhancedMimeType mimeType) {
                if (data instanceof byte[] bytes) {
                    return (T) new String(bytes, StandardCharsets.UTF_8);
                }
                throw new IllegalArgumentException("Unknown input type: " + data);
            }
        };
    }

    private static Converter<String, String> append(final String suffix) {
        return new Converter<>() {
            @Override
            public Class<String> getSourceType() {
                return String.class;
            }

            @Override
            public Class<String> getTargetType() {
                return String.class;
            }

            @Override
            public String convert(final String source) {
                return source + suffix;
            }
        };
    }

}
