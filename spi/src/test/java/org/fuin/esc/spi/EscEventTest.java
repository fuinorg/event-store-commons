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
package org.fuin.esc.spi;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.fuin.utils4j.JaxbUtils.marshal;
import static org.fuin.utils4j.JaxbUtils.unmarshal;

import java.nio.charset.Charset;
import java.util.UUID;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.apache.commons.io.IOUtils;
import org.eclipse.yasson.FieldAccessStrategy;
import org.junit.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

/**
 * Test for {@link EscEvent} class.
 */
// CHECKSTYLE:OFF Test
public class EscEventTest {

    @Test
    public final void testUnMarshal() throws Exception {

        // PREPARE
        final String expectedXml = IOUtils.toString(this.getClass().getResourceAsStream("/esc-event.xml"));

        // TEST
        final EscEvent testee = unmarshal(expectedXml, EscEvent.class, EscMeta.class, MyMeta.class, MyEvent.class, Base64Data.class);

        // VERIFY
        assertThat(testee).isNotNull();
        assertThat(testee.getEventId()).isEqualTo("68616d90-cf72-4c2a-b913-32bf6e6506ed");
        assertThat(testee.getEventType()).isEqualTo("MyEvent");
        assertThat(testee.getData()).isNotNull();
        assertThat(testee.getData().getObj()).isInstanceOf(MyEvent.class);
        assertThat(testee.getMeta()).isNotNull();
        assertThat(testee.getMeta().getObj()).isNotNull();
        assertThat(testee.getMeta().getObj()).isInstanceOf(EscMeta.class);

        // TEST
        final String xml = marshal(testee, EscEvent.class, EscMeta.class, MyMeta.class, MyEvent.class, Base64Data.class);

        // VERIFY
        final Diff documentDiff = DiffBuilder.compare(expectedXml).withTest(xml).ignoreWhitespace().build();
        assertThat(documentDiff.hasDifferences()).describedAs(documentDiff.toString()).isFalse();

    }

    @Test
    public void testJsonbSerializer() throws Exception {

        // PREPARE
        final String expectedJson = IOUtils.toString(this.getClass().getResourceAsStream("/esc-event.json"));

        final UUID eventId = UUID.fromString("b2a936ce-d479-414f-b67f-3df4da383d47");
        final MyEvent myEvent = new MyEvent(UUID.fromString("b2a936ce-d479-414f-b67f-3df4da383d47"), "Hello, JSON!");
        final MyMeta myMeta = new MyMeta("abc");

        final EnhancedMimeType dataContentType = new EnhancedMimeType("application", "json", Charset.forName("utf-8"), "1");
        final EnhancedMimeType metaContentType = new EnhancedMimeType("application", "json", Charset.forName("utf-8"), "1");
        final EscMeta escMeta = new EscMeta(MyEvent.SER_TYPE.asBaseType(), dataContentType, MyMeta.SER_TYPE.asBaseType(), metaContentType,
                myMeta);
        final DataWrapper dataWrapper = new DataWrapper(myEvent);
        final DataWrapper metaWrapper = new DataWrapper(escMeta);
        final EscEvent event = new EscEvent(eventId, MyEvent.TYPE.asBaseType(), dataWrapper, metaWrapper);

        final JsonbConfig config = new JsonbConfig().withSerializers(EscSpiUtils.createEscJsonbSerializers())
                .withPropertyVisibilityStrategy(new FieldAccessStrategy());
        final Jsonb jsonb = JsonbBuilder.create(config);

        // TEST
        final String currentJson = jsonb.toJson(event);

        // VERIFY
        assertThatJson(currentJson).isEqualTo(expectedJson);

    }

}
// CHECKSTYLE:ON
