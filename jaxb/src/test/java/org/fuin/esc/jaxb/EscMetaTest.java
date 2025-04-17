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

import org.junit.jupiter.api.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.fuin.utils4j.jaxb.JaxbUtils.marshal;
import static org.fuin.utils4j.jaxb.JaxbUtils.unmarshal;

/**
 * Test for {@link EscMeta} class.
 */
public class EscMetaTest {

    @Test
    public final void testMarshalUnmarshalJaxb() throws Exception {

        // PREPARE
        final String expectedXml = """
                <esc-meta>
                    <data-type>MyEvent</data-type>
                    <data-content-type>application/xml; version=1; encoding=utf-8</data-content-type>
                    <meta-type>MyMeta</meta-type>
                    <meta-content-type>application/xml; version=1; encoding=utf-8</meta-content-type>
                    <MyMeta>
                        <user>abc</user>
                    </MyMeta>
                </esc-meta>
                """;

        // TEST
        final EscMeta testee = unmarshal(expectedXml, EscMeta.class, MyMeta.class, Base64Data.class);
        final String actualXml = marshal(testee, EscMeta.class, MyMeta.class, Base64Data.class);

        // VERIFY
        final Diff documentDiff = DiffBuilder.compare(expectedXml).withTest(actualXml).ignoreWhitespace().build();
        assertThat(documentDiff.hasDifferences()).describedAs(documentDiff.toString()).isFalse();

    }

}
