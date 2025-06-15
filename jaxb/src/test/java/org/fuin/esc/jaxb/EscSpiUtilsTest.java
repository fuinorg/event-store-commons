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

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.IEscMeta;
import org.fuin.esc.api.SerDeserializerRegistry;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.spi.EscSpiUtils;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link EscSpiUtils} class together with {@link EscMeta} implementation of this module.
 */
public class EscSpiUtilsTest {

    private static final EnhancedMimeType DEFAULT_MIME_TYPE = EnhancedMimeType.create("application", "xml");


    @Test
    public void testCreateEscMetaTargetXmlDataXmlMetaNull() {

        // PREPARE
        final String description = "Whatever";
        final UUID uuid = UUID.randomUUID();
        final MyEvent myEvent = new MyEvent(uuid, description);
        final EventId eventId = new EventId();
        final SerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry.Builder(DEFAULT_MIME_TYPE)
                .add(MyEvent.SER_TYPE, XmlDeSerializer.builder().add(MyEvent.class).build())
                .build();
        final CommonEvent commonEvent = new SimpleCommonEvent(eventId, MyEvent.TYPE, myEvent, null);

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
        final SerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry.Builder(DEFAULT_MIME_TYPE)
                .add(MyEvent.SER_TYPE, XmlDeSerializer.builder().add(MyEvent.class).build()).build();
        final CommonEvent commonEvent = new SimpleCommonEvent(eventId, MyEvent.TYPE, myEvent, null);

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
        final SerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry.Builder(DEFAULT_MIME_TYPE)
                .add(MyEvent.SER_TYPE, XmlDeSerializer.builder().add(MyEvent.class).build())
                .add(MyMeta.SER_TYPE, XmlDeSerializer.builder().add(MyMeta.class).build())
                .build();
        final CommonEvent commonEvent = new SimpleCommonEvent(eventId, MyEvent.TYPE, myEvent, MyMeta.TYPE,
                myMeta, null);

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
        final SerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry.Builder(DEFAULT_MIME_TYPE)
                .add(MyEvent.SER_TYPE, XmlDeSerializer.builder().add(MyEvent.class).build())
                .add(MyMeta.SER_TYPE, XmlDeSerializer.builder().add(MyMeta.class).build())
                .build();
        final CommonEvent commonEvent = new SimpleCommonEvent(eventId, MyEvent.TYPE, myEvent, MyMeta.TYPE,
                myMeta, null);

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

}
