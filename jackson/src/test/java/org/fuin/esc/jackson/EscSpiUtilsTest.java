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

import jakarta.activation.MimeTypeParseException;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.IBase64Data;
import org.fuin.esc.api.IEscMeta;
import org.fuin.esc.api.SerDeserializerRegistry;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.Serializer;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.spi.EscSpiUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link EscSpiUtils} class.
 */
public class EscSpiUtilsTest extends AbstractTest {

    private static final EnhancedMimeType TARGET_CONTENT_TYPE = EnhancedMimeType.create("application/json; encoding=UTF-8");

    private static final UUID ID = UUID.fromString("f9004db0-0bd8-4e68-811c-89899e2ed9b1");

    private static final String DESC = "Whatever";
    public static final EnhancedMimeType XML_BASE64_UTF8 = EnhancedMimeType.create("application/xml; transfer-encoding=base64; encoding=utf-8");
    public static final EnhancedMimeType JSON_UTF8 = EnhancedMimeType.create("application/json; encoding=UTF-8");

    @Test
    public void testCreateEscMeta_TargetJson_DataJson_MetaNull() throws IOException {

        // PREPARE
        final MyEvent myEvent = new MyEvent(ID, DESC);
        final EventId eventId = new EventId();

        final CommonEvent commonEvent = new SimpleCommonEvent(eventId, MyEvent.TYPE, myEvent);

        // TEST
        final IEscMeta result = EscSpiUtils.createEscMeta(getSerDeserializerRegistry(), new BaseTypeFactory(), TARGET_CONTENT_TYPE, commonEvent);

        // VERIFY
        assertThat(result).isNotNull();
        assertThat(result.getDataType()).isEqualTo(MyEvent.TYPE.asBaseType());
        assertThat(result.getDataContentType()).isEqualTo(JSON_UTF8);
        assertThat(result.getMetaType()).isNull();
        assertThat(result.getMetaContentType()).isNull();
        assertThat(result.getMeta()).isNull();

    }

    @Test
    public void testCreateEscMeta_TargetJson_DataXml_MetaNull() throws IOException {

        // PREPARE
        final MyEvent myEvent = new MyEvent(ID, DESC);
        final EventId eventId = new EventId();

        final SerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry.Builder(TARGET_CONTENT_TYPE)
                .add(MyEvent.SER_TYPE, dummySerializer("application/xml; encoding=utf-8")).build();
        final CommonEvent commonEvent = new SimpleCommonEvent(eventId, MyEvent.TYPE, myEvent);

        // TEST
        final IEscMeta result = EscSpiUtils.createEscMeta(registry, new BaseTypeFactory(), TARGET_CONTENT_TYPE, commonEvent);

        // VERIFY
        assertThat(result).isNotNull();
        assertThat(result.getDataType()).isEqualTo(MyEvent.TYPE.asBaseType());
        assertThat(result.getDataContentType()).isEqualTo(XML_BASE64_UTF8);
        assertThat(result.getMetaType()).isNull();
        assertThat(result.getMetaContentType()).isNull();
        assertThat(result.getMeta()).isNull();

    }

    @Test
    public void testCreateEscMeta_TargetJson_DataJson_MetaJson() throws IOException {

        // PREPARE
        final MyEvent myEvent = new MyEvent(ID, DESC);
        final EventId eventId = new EventId();
        final MyMeta myMeta = new MyMeta("peter");

        final CommonEvent commonEvent = new SimpleCommonEvent(eventId, MyEvent.TYPE, myEvent, MyMeta.TYPE,
                myMeta, null);

        // TEST
        final IEscMeta result = EscSpiUtils.createEscMeta(getSerDeserializerRegistry(), new BaseTypeFactory(), TARGET_CONTENT_TYPE, commonEvent);

        // VERIFY
        assertThat(result).isNotNull();
        assertThat(result.getDataType()).isEqualTo(MyEvent.TYPE.asBaseType());
        assertThat(result.getDataContentType()).isEqualTo(JSON_UTF8);
        assertThat(result.getMetaType()).isEqualTo(MyMeta.TYPE.asBaseType());
        assertThat(result.getMetaContentType()).isEqualTo(JSON_UTF8);
        assertThat(result.getMeta()).isEqualTo(myMeta);

    }

    @Test
    public void testCreateEscMeta_TargetJson_DataJson_MetaXml() throws IOException {

        // PREPARE
        final MyEvent myEvent = new MyEvent(ID, DESC);
        final EventId eventId = new EventId();
        final MyMeta myMeta = new MyMeta("peter");

        final SerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry.Builder(TARGET_CONTENT_TYPE)
                .add(MyEvent.SER_TYPE, getSerDeserializer())
                .add(MyMeta.SER_TYPE, dummySerializer("application/xml; encoding=utf-8"))
                .build();
        final CommonEvent commonEvent = new SimpleCommonEvent(eventId, MyEvent.TYPE, myEvent, MyMeta.TYPE,
                myMeta, null);

        // TEST
        final IEscMeta result = EscSpiUtils.createEscMeta(registry, new BaseTypeFactory(), TARGET_CONTENT_TYPE, commonEvent);

        // VERIFY
        assertThat(result).isNotNull();
        assertThat(result.getDataType()).isEqualTo(MyEvent.TYPE.asBaseType());
        assertThat(result.getDataContentType()).isEqualTo(JSON_UTF8);
        assertThat(result.getMetaType()).isEqualTo(MyMeta.TYPE.asBaseType());
        assertThat(result.getMetaContentType()).isEqualTo(XML_BASE64_UTF8);
        assertThat(result.getMeta()).isInstanceOf(IBase64Data.class);
        final IBase64Data base64Meta = (IBase64Data) result.getMeta();
        assertThat(new String(base64Meta.getDecoded(), StandardCharsets.UTF_8)).isEqualTo(
                "<MyMeta><user>peter</user></MyMeta>");

    }

    @Test
    public void testCreateEscMeta_TargetJson_DataXml_MetaJson() throws IOException {

        // PREPARE
        final MyEvent myEvent = new MyEvent(ID, DESC);
        final EventId eventId = new EventId();
        final MyMeta myMeta = new MyMeta("peter");

        final SerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry.Builder(TARGET_CONTENT_TYPE)
                .add(MyEvent.SER_TYPE, dummySerializer("application/xml; encoding=utf-8"))
                .add(MyMeta.SER_TYPE, getSerDeserializer())
                .build();
        final CommonEvent commonEvent = new SimpleCommonEvent(eventId, MyEvent.TYPE, myEvent, MyMeta.TYPE,
                myMeta, null);

        // TEST
        final IEscMeta result = EscSpiUtils.createEscMeta(registry, new BaseTypeFactory(), TARGET_CONTENT_TYPE, commonEvent);

        // VERIFY
        assertThat(result).isNotNull();
        assertThat(result.getDataType()).isEqualTo(MyEvent.TYPE.asBaseType());
        assertThat(result.getDataContentType()).isEqualTo(XML_BASE64_UTF8);
        assertThat(result.getMetaType()).isEqualTo(MyMeta.TYPE.asBaseType());
        assertThat(result.getMetaContentType()).isEqualTo(JSON_UTF8);
        assertThat(result.getMeta()).isInstanceOf(MyMeta.class);

    }

    private EnhancedMimeType mimeType(String str) {
        try {
            return new EnhancedMimeType(str);
        } catch (final MimeTypeParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Serializer dummySerializer(final String mimeType) {
        return new Serializer() {
            @Override
            public <T> byte[] marshal(T obj, SerializedDataType type) {
                if (obj == null) {
                    return null;
                }
                if (obj instanceof MyEvent) {
                    return ("<MyEvent><id>" + ID + "</id><description></description></MyEvent>").getBytes(StandardCharsets.UTF_8);
                }
                if (obj instanceof MyMeta) {
                    return "<MyMeta><user>peter</user></MyMeta>".getBytes(StandardCharsets.UTF_8);
                }
                throw new IllegalStateException("Unknown object type: " + obj);
            }

            @Override
            public EnhancedMimeType getMimeType() {
                return mimeType(mimeType);
            }
        };
    }

}

