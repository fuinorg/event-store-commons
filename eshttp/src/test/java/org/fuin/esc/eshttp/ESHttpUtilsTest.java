/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. <http://www.fuin.org/>
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
 * along with this library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.fuin.esc.eshttp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Tests the {@link ESHttpUtils} class.
 */
// CHECKSTYLE:OFF Test
public class ESHttpUtilsTest {

    private Document doc;

    private XPath xpath;

    @Before
    public void setup() throws IOException {
        final InputStream in = this.getClass().getResourceAsStream("/atom-feed-stream.xml");
        try {
            doc = ESHttpUtils.parseDocument(ESHttpUtils.createDocumentBuilder(), in);
            xpath = ESHttpUtils.createXPath("ns", "http://www.w3.org/2005/Atom");
        } finally {
            in.close();
        }
    }

    @After
    public void teardown() {
        xpath = null;
        doc = null;
    }

    @Test
    public void testCreateDocumentBuilder() {

        // TEST & VERIFY
        assertThat(ESHttpUtils.createDocumentBuilder().isNamespaceAware()).isTrue();

    }

    @Test
    public void testCreateXPath() {

        // PREPARE
        final String[] namespaces = new String[] { "abc", "http://www.fuin.org/abc", "def",
                "http://www.fuin.org/def" };

        // TEST
        final XPath xpath = ESHttpUtils.createXPath(namespaces);

        // VERIFY
        assertThat(xpath.getNamespaceContext()).isNotNull();
        final NamespaceContext nsc = xpath.getNamespaceContext();
        assertThat(nsc.getNamespaceURI("abc")).isEqualTo("http://www.fuin.org/abc");
        assertThat(nsc.getNamespaceURI("def")).isEqualTo("http://www.fuin.org/def");

    }

    @Test
    public void testFindNodes() throws IOException {

        // TEST
        final NodeList nodes = ESHttpUtils.findNodes(doc, xpath, "/ns:feed/ns:entry");

        // VERIFY
        assertThat(nodes).isNotNull();
        assertThat(nodes.getLength()).isEqualTo(2);

    }

    @Test
    public void testFindNode() {

        // TEST
        final Node node = ESHttpUtils.findNode(doc, xpath, "/ns:feed/ns:entry");

        // VERIFY
        assertThat(node).isNotNull();

    }

    @Test
    public void testFindContentInteger() throws IOException {

        // PREPARE
        final InputStream in = this.getClass().getResourceAsStream("/atom-feed-event-1.xml");
        try {
            doc = ESHttpUtils.parseDocument(ESHttpUtils.createDocumentBuilder(), in);
            xpath = ESHttpUtils.createXPath("atom", "http://www.w3.org/2005/Atom");
            
            // TEST
            final Integer value = ESHttpUtils.findContentInteger(doc, xpath, "/atom:entry/atom:content/eventNumber");

            // VERIFY
            assertThat(value).isEqualTo(1);
            
        } finally {
            in.close();
        }

    }

    @Test
    public void testFindContentText() {

        // TEST
        final String value = ESHttpUtils.findContentText(doc, xpath, "/ns:feed/ns:entry/ns:title");

        // VERIFY
        assertThat(value).isEqualTo("1@MyStreamA");

    }

}
// CHECKSTYLE:ON
