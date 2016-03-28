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

import static org.assertj.core.api.Assertions.assertThat;
import static org.fuin.utils4j.JaxbUtils.marshal;
import static org.fuin.utils4j.JaxbUtils.unmarshal;

import java.io.StringWriter;

import javax.json.Json;
import javax.json.JsonObject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

/**
 * Test for {@link EscMetaData} class.
 */
public class EscMetaDataTest {

    private static final String CONTENT_TYPE = "application/xml; version=1; encoding=utf-8";

    @Test
    public final void testUnMarshal() throws Exception {

        // PREPARE
        final String expectedXml = IOUtils
                .toString(this.getClass().getResourceAsStream("/meta-data-xml.xml"));

        // TEST
        final EscMetaData testee = unmarshal(expectedXml, EscMetaData.class, MyMeta.class, Base64Data.class);

        // VERIFY
        assertThat(testee).isNotNull();
        assertThat(testee.getUserMeta()).isNotNull();
        assertThat(testee.getUserMeta().getObj()).isInstanceOf(MyMeta.class);
        final MyMeta userMeta = (MyMeta) testee.getUserMeta().getObj();
        assertThat(userMeta.getUser()).isEqualTo("abc");

        assertThat(testee.getSysMeta()).isNotNull();
        assertThat(testee.getSysMeta().getDataContentType().toString()).isEqualTo(CONTENT_TYPE);
        assertThat(testee.getSysMeta().getMetaContentType().toString()).isEqualTo(CONTENT_TYPE);
        assertThat(testee.getSysMeta().getMetaType().toString()).isEqualTo("MyMeta");

        // TEST
        final JAXBContext ctx = JAXBContext.newInstance(EscMetaData.class, MyMeta.class, Base64Data.class);
        final Marshaller marshaller = ctx.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        final StringWriter sw = new StringWriter();
        marshaller.marshal(testee, sw);
        assertThat(sw.toString()).isEqualTo(expectedXml);

    }

    @Test
    public final void testToJson() {

        // PREPARE
        final EnhancedMimeType dataContentType = EnhancedMimeType.create("application/xml");
        final EnhancedMimeType metaContentType = EnhancedMimeType
                .create("text/plain; transfer-encoding=base64");
        final String metaType = "JustText";
        final String base64Str = "SGVsbG8gd29ybGQh";
        final EscSysMeta sysMeta = new EscSysMeta(dataContentType, metaContentType, metaType);
        final DataWrapper userMeta = new DataWrapper(new Base64Data(base64Str));
        final EscMetaData testee = new EscMetaData(sysMeta, userMeta);

        // TEST
        final JsonObject result = testee.toJson();

        // VERIFY
        assertThat(result.getJsonObject("EscSysMeta")).isNotNull();
        assertThat(result.getJsonObject("EscUserMeta")).isNotNull();
        assertThat(result.getJsonObject("EscSysMeta").getString("data-content-type")).isEqualTo(
                dataContentType.toString());
        assertThat(result.getJsonObject("EscSysMeta").getString("meta-content-type")).isEqualTo(
                metaContentType.toString());
        assertThat(result.getJsonObject("EscSysMeta").getString("meta-type")).isEqualTo(metaType);
        assertThat(result.getJsonObject("EscUserMeta").getString("Base64")).isEqualTo(base64Str);

    }

}
