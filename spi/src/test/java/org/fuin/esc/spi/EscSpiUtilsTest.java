/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. <http://www.fuin.org/>
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
 * along with this library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.fuin.esc.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import javax.activation.MimeTypeParseException;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.api.SimpleCommonEvent;
import org.junit.Test;

/**
 * Tests the {@link EscSpiUtils} class.
 */
// CHECKSTYLE:OFF Test
public class EscSpiUtilsTest {

    @Test
    public void testSerializeNull() {

        // PREPARE
        final SerializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        final SerializedDataType type = new SerializedDataType("whatever");

        // TEST
        final SerializedData result = EscSpiUtils.serialize(registry, type, null);

        // VERIFY
        assertThat(result).isNull();

    }

    @Test
    public void testSerializeOK() {

        // PREPARE
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        final SerializedDataType type = new SerializedDataType("whatever");
        registry.addSerializer(type, dummySerializer("text/plain"));
        final String data = "My Data";

        // TEST
        final SerializedData result = EscSpiUtils.serialize(registry, type, data);

        // VERIFY
        assertThat(result.getType()).isEqualTo(type);
        assertThat(result.getRaw()).isEqualTo(data.getBytes());

    }

    @Test
    public void testSerializeNoSerializerFound() {

        // PREPARE
        final SerializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        final SerializedDataType type = new SerializedDataType("whatever");
        final Object data = "My Data";

        // TEST
        try {
            EscSpiUtils.serialize(registry, type, data);
        } catch (final IllegalStateException ex) {
            // VERIFY
            assertThat(ex.getMessage()).isEqualTo("Couldn't get a serializer for: whatever");
        }

    }

    @Test
    public void testDeserializeNoDeserializerFound() {

        // PREPARE
        final DeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        final SerializedDataType type = new SerializedDataType("whatever");
        final SerializedData data = new SerializedData(type, mimeType("text/plain"), "whatever".getBytes());

        // TEST
        try {
            EscSpiUtils.deserialize(registry, data);
        } catch (final IllegalStateException ex) {
            // VERIFY
            assertThat(ex.getMessage()).isEqualTo("Couldn't get a deserializer for: whatever / text/plain");
        }

    }

    @Test
    public void testDeserializeOK() {

        // PREPARE
        final String value = "My Data";
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        final SerializedDataType type = new SerializedDataType("whatever");
        final SerializedData data = new SerializedData(type, mimeType("text/plain"), value.getBytes());
        registry.addDeserializer(type, "text/plain", dummyDeserializer());

        // TEST
        final Object result = EscSpiUtils.deserialize(registry, data);

        // VERIFY
        assertThat(result).isEqualTo(value);

    }

    @Test
    public void testMimeTypeSame() {

        // PREPARE
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        final String type = "TypeX";
        final SerializedDataType serType = new SerializedDataType(type);
        final TypeName eventType = new TypeName(type);
        registry.addSerializer(serType, dummySerializer("text/plain"));
        final List<CommonEvent> events = new ArrayList<>();
        events.add(new SimpleCommonEvent(new EventId(), eventType, "One"));

        // TEST
        final EnhancedMimeType mimeType = EscSpiUtils.mimeType(registry, events);

        // VERIFY
        assertThat(mimeType.toString()).isEqualTo("text/plain");

    }

    @Test
    public void testMimeTypeDifferent() {

        // PREPARE
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();

        final String typeA = "TypeA";
        final SerializedDataType serTypeA = new SerializedDataType(typeA);
        final TypeName eventTypeA = new TypeName(typeA);
        registry.addSerializer(serTypeA, dummySerializer("text/plain"));

        final String typeB = "TypeB";
        final SerializedDataType serTypeB = new SerializedDataType(typeB);
        final TypeName eventTypeB = new TypeName(typeB);
        registry.addSerializer(serTypeB, dummySerializer("application/xml"));

        final List<CommonEvent> events = new ArrayList<>();
        events.add(new SimpleCommonEvent(new EventId(), eventTypeA, "One"));
        events.add(new SimpleCommonEvent(new EventId(), eventTypeB, "<Other/>"));

        // TEST
        // TEST
        final EnhancedMimeType mimeType = EscSpiUtils.mimeType(registry, events);

        // VERIFY
        assertThat(mimeType).isNull();

    }

    private EnhancedMimeType mimeType(String str) {
        try {
            return new EnhancedMimeType(str);
        } catch (final MimeTypeParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Serializer dummySerializer(final String baseType) {
        return new Serializer() {
            @Override
            public <T> byte[] marshal(T obj) {
                if (obj == null) {
                    return null;
                }
                return obj.toString().getBytes();
            }

            @Override
            public EnhancedMimeType getMimeType() {
                return mimeType(baseType);
            }
        };
    }

    private Deserializer dummyDeserializer() {
        return new Deserializer() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> T unmarshal(Object data, EnhancedMimeType mimeType) {
                if (data instanceof byte[]) {
                    return (T) new String((byte[]) data);
                }
                throw new IllegalArgumentException("Unknown input type: " + data);
            }
        };
    }

}
// CHECKSTYLE:ON
