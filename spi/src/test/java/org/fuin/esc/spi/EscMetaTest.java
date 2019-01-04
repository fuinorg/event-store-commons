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

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.apache.commons.io.IOUtils;
import org.eclipse.yasson.FieldAccessStrategy;
import org.junit.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

/**
 * Test for {@link EscMeta} class.
 */
// CHECKSTYLE:OFF Test
public class EscMetaTest {

    @Test
    public final void testUnMarshal() throws Exception {

        // PREPARE
        final String expectedXml = IOUtils.toString(this.getClass().getResourceAsStream("/esc-meta.xml"));

        // TEST
        final EscMeta testee = unmarshal(expectedXml, EscMeta.class, MyMeta.class, Base64Data.class);
        final String actualXml = marshal(testee, EscMeta.class, MyMeta.class, Base64Data.class);

        // VERIFY
        final Diff documentDiff = DiffBuilder.compare(expectedXml).withTest(actualXml).ignoreWhitespace().build();
        assertThat(documentDiff.hasDifferences()).describedAs(documentDiff.toString()).isFalse();

    }

    @Test
    public final void testToJson() throws Exception {

        // PREPARE
        final String expectedJson = IOUtils.toString(this.getClass().getResourceAsStream("/esc-meta.json"));

        // TEST
        final EscMeta testee = EscMeta.create(Json.createReader(new StringReader(expectedJson)).readObject());
        final StringWriter sw = new StringWriter();
        Json.createWriter(sw).writeObject(testee.toJson());
        final String actualJson = sw.toString();

        // VERIFY
        assertThatJson(expectedJson).isEqualTo(actualJson);

    }

    @Test
    public final void testJsonB() throws Exception {

        // PREPARE
        final String expectedJson = IOUtils.toString(this.getClass().getResourceAsStream("/esc-meta.json"));

        final MyMeta myMeta = new MyMeta("abc");

        final Map<String, String> params = new HashMap<>();
        params.put("transfer-encoding", "base64");
        final EnhancedMimeType dataContentType = new EnhancedMimeType("application", "xml", Charset.forName("utf-8"), "1", params);
        final EnhancedMimeType metaContentType = new EnhancedMimeType("application", "json", Charset.forName("utf-8"), "1");
        final EscMeta escMeta = new EscMeta(MyEvent.SER_TYPE.asBaseType(), dataContentType, MyMeta.SER_TYPE.asBaseType(), metaContentType,
                myMeta);

        final JsonbConfig config = new JsonbConfig().withSerializers(EscSpiUtils.createEscJsonbSerializers())
                .withPropertyVisibilityStrategy(new FieldAccessStrategy());
        final Jsonb jsonb = JsonbBuilder.create(config);

        // TEST
        final String currentJson = jsonb.toJson(escMeta);

        // VERIFY
        assertThatJson(currentJson).isEqualTo(expectedJson);

    }

}
// CHECKSTYLE:ON
