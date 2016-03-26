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
import static org.fuin.utils4j.JaxbUtils.XML_PREFIX;
import static org.fuin.utils4j.JaxbUtils.marshal;
import static org.fuin.utils4j.JaxbUtils.unmarshal;

import org.junit.Test;

/**
 * Test for {@link EscSysMeta} class.
 */
public class EscSysMetaTest {

    private static final String MY_META = "MyMeta";

    private static final EnhancedMimeType DATA_CONTENT_TYPE = EnhancedMimeType.create("application/xml");

    private static final EnhancedMimeType META_CONTENT_TYPE = EnhancedMimeType.create("application/json");

    private static final String XML = XML_PREFIX + "<EscSysMeta>"
            + "<data-content-type>application/xml</data-content-type>"
            + "<meta-content-type>application/json</meta-content-type>" + "<meta-type>MyMeta</meta-type>"
            + "</EscSysMeta>";

    @Test
    public final void testMarshal() throws Exception {

        // PREPARE
        final EscSysMeta testee = new EscSysMeta(DATA_CONTENT_TYPE, META_CONTENT_TYPE, MY_META);

        // TEST
        final String result = marshal(testee, EscSysMeta.class);

        // VERIFY
        assertThat(result).isEqualTo(XML);

    }

    @Test
    public final void testUnmarshal() throws Exception {

        // TEST
        final EscSysMeta testee = unmarshal(XML, EscSysMeta.class);

        // VERIFY
        assertThat(testee).isNotNull();
        assertThat(testee.getDataContentType()).isEqualTo(DATA_CONTENT_TYPE);
        assertThat(testee.getMetaContentType()).isEqualTo(META_CONTENT_TYPE);
        assertThat(testee.getMetaType()).isEqualTo(MY_META);

    }

}
