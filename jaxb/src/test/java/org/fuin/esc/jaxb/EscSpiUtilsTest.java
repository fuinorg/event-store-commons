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
package org.fuin.esc.jaxb;

import jakarta.activation.MimeTypeParseException;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.Deserializer;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.IEscMeta;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.Serializer;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.spi.EscSpiUtils;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link EscSpiUtils} class together with {@link EscMeta} implementation of this module.
 */
public class EscSpiUtilsTest {

    @Test
    public void testCreateEscMetaTargetXmlDataXmlMetaNull() {

        // PREPARE
        final String description = "Whatever";
        final UUID uuid = UUID.randomUUID();
        final MyEvent myEvent = new MyEvent(uuid, description);
        final EventId eventId = new EventId();
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        registry.addSerializer(MyEvent.SER_TYPE, new XmlDeSerializer(MyEvent.class));
        final CommonEvent commonEvent = new SimpleCommonEvent(eventId, MyEvent.TYPE, myEvent);

        // TEST
        final IEscMeta result = EscSpiUtils.createEscMeta(registry, new BaseTypeFactory(),
                EnhancedMimeType.create("application/xml; encoding=UTF-8"), commonEvent);

        // VERIFY
        assertThat(result).isNotNull();
        assertThat(result.getDataType()).isEqualTo(MyEvent.TYPE.asBaseType());
        assertThat(result.getDataContentType()).isEqualTo(
                EnhancedMimeType.create("application/xml; encoding=UTF-8"));
        assertThat(result.getMetaType()).isNull();
        assertThat(result.getMetaContentType()).isNull();
        assertThat(result.getMeta()).isNull();

    }

    @Test
    public void testCreateEscMetaTargetJsonDataXmlMetaNull() {

        // PREPARE
        final String description = "Whatever";
        final UUID uuid = UUID.randomUUID();
        final MyEvent myEvent = new MyEvent(uuid, description);
        final EventId eventId = new EventId();
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        registry.addSerializer(MyEvent.SER_TYPE, new XmlDeSerializer(MyEvent.class));
        final CommonEvent commonEvent = new SimpleCommonEvent(eventId, MyEvent.TYPE, myEvent);

        // TEST
        final IEscMeta result = EscSpiUtils.createEscMeta(registry, new BaseTypeFactory(),
                EnhancedMimeType.create("application/json"), commonEvent);

        // VERIFY
        assertThat(result).isNotNull();
        assertThat(result.getDataType()).isEqualTo(MyEvent.TYPE.asBaseType());
        assertThat(result.getDataContentType()).isEqualTo(
                EnhancedMimeType.create("application/xml; transfer-encoding=base64; encoding=UTF-8"));
        assertThat(result.getMetaType()).isNull();
        assertThat(result.getMetaContentType()).isNull();
        assertThat(result.getMeta()).isNull();

    }

    @Test
    public void testCreateEscMetaTargetXmlDataXmlMetaXml() {

        // PREPARE
        final MyEvent myEvent = new MyEvent(UUID.randomUUID(), "Whatever");
        final EventId eventId = new EventId();
        final MyMeta myMeta = new MyMeta("peter");
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        registry.addSerializer(MyEvent.SER_TYPE, new XmlDeSerializer(MyEvent.class));
        registry.addSerializer(MyMeta.SER_TYPE, new XmlDeSerializer(MyMeta.class));
        final CommonEvent commonEvent = new SimpleCommonEvent(eventId, MyEvent.TYPE, myEvent, MyMeta.TYPE,
                myMeta);

        // TEST
        final IEscMeta result = EscSpiUtils.createEscMeta(registry, new BaseTypeFactory(),
                EnhancedMimeType.create("application/xml; encoding=UTF-8"), commonEvent);

        // VERIFY
        assertThat(result).isNotNull();
        assertThat(result.getDataType()).isEqualTo(MyEvent.TYPE.asBaseType());
        assertThat(result.getDataContentType()).isEqualTo(
                EnhancedMimeType.create("application/xml; encoding=UTF-8"));
        assertThat(result.getMetaType()).isEqualTo(MyMeta.TYPE.asBaseType());
        assertThat(result.getMetaContentType()).isEqualTo(
                EnhancedMimeType.create("application/xml; encoding=UTF-8"));
        assertThat(result.getMeta()).isEqualTo(myMeta);

    }

    @Test
    public void testCreateEscMetaTargetJsonDataXmlMetaXml() {

        // PREPARE
        final String description = "Whatever";
        final UUID uuid = UUID.randomUUID();
        final MyEvent myEvent = new MyEvent(uuid, description);
        final EventId eventId = new EventId();
        final MyMeta myMeta = new MyMeta("peter");
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        registry.addSerializer(MyEvent.SER_TYPE, new XmlDeSerializer(MyEvent.class));
        registry.addSerializer(MyMeta.SER_TYPE, new XmlDeSerializer(MyMeta.class));
        final CommonEvent commonEvent = new SimpleCommonEvent(eventId, MyEvent.TYPE, myEvent, MyMeta.TYPE,
                myMeta);

        // TEST
        final IEscMeta result = EscSpiUtils.createEscMeta(registry, new BaseTypeFactory(),
                EnhancedMimeType.create("application/json"), commonEvent);

        // VERIFY
        assertThat(result).isNotNull();
        assertThat(result.getDataType()).isEqualTo(MyEvent.TYPE.asBaseType());
        assertThat(result.getDataContentType()).isEqualTo(
                EnhancedMimeType.create("application/xml; transfer-encoding=base64; encoding=UTF-8"));
        assertThat(result.getMetaType()).isEqualTo(MyMeta.TYPE.asBaseType());
        assertThat(result.getMetaContentType()).isEqualTo(
                EnhancedMimeType.create("application/xml; transfer-encoding=base64; encoding=UTF-8"));
        assertThat(result.getMeta()).isInstanceOf(Base64Data.class);
        final Base64Data base64Meta = (Base64Data) result.getMeta();
        assertThat(new String(base64Meta.getDecoded(), StandardCharsets.UTF_8)).isEqualTo(
                "<MyMeta><user>peter</user></MyMeta>");

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
