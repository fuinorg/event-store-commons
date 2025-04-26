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

import jakarta.activation.MimeTypeParseException;
import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.Deserializer;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.IBaseTypeFactory;
import org.fuin.esc.api.IEscMeta;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.Serializer;
import org.fuin.esc.api.SerializerRegistry;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.api.TypeName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link EscSpiUtils} class.
 */
public class EscSpiUtilsTest {

    public static final @NotNull EnhancedMimeType DEFAULT_MIME_TYPE = EnhancedMimeType.create("application", "json");

    @Test
    public void testEventsEqual() {

        // PREPARE
        final UUID uuidA = UUID.randomUUID();
        final UUID uuidB = UUID.randomUUID();
        final CommonEvent eventA = new SimpleCommonEvent(new EventId(uuidA), new TypeName("A"), "a");
        final CommonEvent eventB = new SimpleCommonEvent(new EventId(uuidB), new TypeName("B"), "b");
        final CommonEvent eventAa = new SimpleCommonEvent(new EventId(uuidA), new TypeName("Aa"), "aa");

        // TEST & VERIFY
        assertThat(EscSpiUtils.eventsEqual(null, null)).isTrue();
        assertThat(EscSpiUtils.eventsEqual(null, new ArrayList<>())).isFalse();
        assertThat(EscSpiUtils.eventsEqual(new ArrayList<>(), null)).isFalse();
        assertThat(EscSpiUtils.eventsEqual(new ArrayList<>(), new ArrayList<>())).isTrue();
        assertThat(EscSpiUtils.eventsEqual(asList(eventA), asList(eventA))).isTrue();
        assertThat(EscSpiUtils.eventsEqual(asList(eventA), asList(eventB))).isFalse();
        assertThat(EscSpiUtils.eventsEqual(asList(eventB), asList(eventA))).isFalse();
        assertThat(EscSpiUtils.eventsEqual(asList(eventA), asList(eventAa))).isTrue();
        assertThat(EscSpiUtils.eventsEqual(asList(eventA), asList(eventA, eventB))).isFalse();
        assertThat(EscSpiUtils.eventsEqual(asList(eventA, eventB), asList(eventB))).isFalse();

    }


    @Test
    public void testSerializeNull() {

        // PREPARE
        final SerializerRegistry registry = new SimpleSerializerDeserializerRegistry.Builder(DEFAULT_MIME_TYPE).build();
        final SerializedDataType type = new SerializedDataType("whatever");

        // TEST
        final SerializedData result = EscSpiUtils.serialize(registry, type, null);

        // VERIFY
        assertThat(result).isNull();

    }

    @Test
    public void testSerializeOK() {

        // PREPARE
        final SerializedDataType type = new SerializedDataType("whatever");
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry.Builder(DEFAULT_MIME_TYPE)
                .add(type, dummySerializer("text/plain"))
                .build();
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
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry.Builder(DEFAULT_MIME_TYPE).build();
        final SerializedDataType type = new SerializedDataType("whatever");
        final Object data = "My Data";

        // TEST
        try {
            EscSpiUtils.serialize(registry, type, data);
        } catch (final IllegalArgumentException ex) {
            // VERIFY
            assertThat(ex.getMessage()).isEqualTo("No serializer found for: whatever");
        }

    }

    @Test
    public void testDeserializeNoDeserializerFound() {

        // PREPARE
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry.Builder(DEFAULT_MIME_TYPE).build();
        final SerializedDataType type = new SerializedDataType("whatever");
        final SerializedData data = new SerializedData(type, mimeType("text/plain"), "whatever".getBytes());

        // TEST
        try {
            EscSpiUtils.deserialize(registry, data);
        } catch (final IllegalArgumentException ex) {
            // VERIFY
            assertThat(ex.getMessage()).isEqualTo("No deserializer found for: Key [type=whatever, mimeType=text/plain]");
        }

    }

    @Test
    public void testDeserializeOK() {

        // PREPARE
        final String value = "My Data";
        final SerializedDataType type = new SerializedDataType("whatever");
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry.Builder(DEFAULT_MIME_TYPE)
                .add(type, dummyDeserializer(), EnhancedMimeType.create("text", "plain"))
                .build();
        final SerializedData data = new SerializedData(type, mimeType("text/plain"), value.getBytes());

        // TEST
        final Object result = EscSpiUtils.deserialize(registry, data);

        // VERIFY
        assertThat(result).isEqualTo(value);

    }

    @Test
    public void testMimeTypeSame() {

        // PREPARE
        final String type = "TypeX";
        final SerializedDataType serType = new SerializedDataType(type);
        final TypeName eventType = new TypeName(type);
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry.Builder(DEFAULT_MIME_TYPE)
                .add(serType, dummySerializer("text/plain"))
                .build();
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
        final String typeA = "TypeA";
        final SerializedDataType serTypeA = new SerializedDataType(typeA);
        final TypeName eventTypeA = new TypeName(typeA);

        final String typeB = "TypeB";
        final SerializedDataType serTypeB = new SerializedDataType(typeB);
        final TypeName eventTypeB = new TypeName(typeB);

        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry.Builder(DEFAULT_MIME_TYPE)
                .add(serTypeA, dummySerializer("text/plain"))
                .add(serTypeB, dummySerializer("application/xml"))
                .build();

        final List<CommonEvent> events = new ArrayList<>();
        events.add(new SimpleCommonEvent(new EventId(), eventTypeA, "One"));
        events.add(new SimpleCommonEvent(new EventId(), eventTypeB, "<Other/>"));

        // TEST
        final EnhancedMimeType mimeType = EscSpiUtils.mimeType(registry, events);

        // VERIFY
        assertThat(mimeType).isNull();

    }

    @Test
    public void testCreateEscMetaNull() {

        // PREPARE
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry.Builder(DEFAULT_MIME_TYPE).build();
        final IBaseTypeFactory baseTypeFactory = Mockito.mock(IBaseTypeFactory.class);

        // TEST
        final IEscMeta result = EscSpiUtils.createEscMeta(registry, baseTypeFactory,
                EnhancedMimeType.create("application/xml"), null);

        // VERIFY
        assertThat(result).isNull();

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
            public <T> byte[] marshal(T obj, SerializedDataType type) {
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
            public <T> T unmarshal(Object data, SerializedDataType type, EnhancedMimeType mimeType) {
                if (data instanceof byte[]) {
                    return (T) new String((byte[]) data);
                }
                throw new IllegalArgumentException("Unknown input type: " + data);
            }
        };
    }

    private List<CommonEvent> asList(CommonEvent... events) {
        return new ArrayList<>(Arrays.asList(events));
    }

}
