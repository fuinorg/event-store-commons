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

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Tests the {@link XmlMetaDataAccessor} class.
 */
// CHECKSTYLE:OFF Test
public class XmlMetaDataAccessorTest {

    private XmlMetaDataAccessor testee;

    @Before
    public void setup() throws Exception {

        final byte[] xml = "<meta><a>whatever</a><b>1</b><c>true</c></meta>"
                .getBytes();
        final ByteArrayInputStream bais = new ByteArrayInputStream(xml);
        final DocumentBuilderFactory factory = DocumentBuilderFactory
                .newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document doc = builder.parse(bais);

        testee = new XmlMetaDataAccessor();
        testee.init(doc);

    }

    @After
    public void teardown() {
        testee = null;
    }

    @Test
    public void testGetString() {
        assertThat(testee.getString("a")).isEqualTo("whatever");
        assertThat(testee.getString("unknown")).isNull();
    }

    @Test
    public void testGetInteger() {
        assertThat(testee.getInteger("b")).isEqualTo(1);
        assertThat(testee.getInteger("unknown")).isNull();
    }

    @Test
    public void testGetBoolean() {
        assertThat(testee.getBoolean("c")).isTrue();
        assertThat(testee.getBoolean("unknown")).isNull();;
    }

    @Test
    public void testNullDocument() {
        
        final XmlMetaDataAccessor testee = new XmlMetaDataAccessor();
        testee.init(null);
        assertThat(testee.getString("unknown")).isNull();
        assertThat(testee.getInteger("unknown")).isNull();
        assertThat(testee.getBoolean("unknown")).isNull();;
        
    }
    
}
// CHECKSTYLE:ON
