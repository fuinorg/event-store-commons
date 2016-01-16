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

import java.util.Iterator;

import javax.xml.XMLConstants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link NamespaceContextMap} class.
 */
// CHECKSTYLE:OFF Test
public class NamespaceContextMapTest {

    private NamespaceContextMap testee;
    
    @Before
    public void setup() {
        testee = new NamespaceContextMap("abc", "http://www.fuin.org/abc", "def", "http://www.fuin.org/def");
    }

    @After
    public void teardown() {
        testee = null;
    }

    @Test
    public void testGetNamespaceURIUnbound() {

        // TEST & VERIFY
        assertThat(testee.getNamespaceURI("xyz")).isEqualTo(XMLConstants.NULL_NS_URI);

    }
    
    @Test
    public void testGetNamespaceURIBound() {

        // TEST & VERIFY
        assertThat(testee.getNamespaceURI("abc")).isEqualTo("http://www.fuin.org/abc");
        assertThat(testee.getNamespaceURI("def")).isEqualTo("http://www.fuin.org/def");        
        assertThat(testee.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX)).isEqualTo(XMLConstants.NULL_NS_URI);
        assertThat(testee.getNamespaceURI(XMLConstants.XML_NS_PREFIX)).isEqualTo(XMLConstants.XML_NS_URI);
        assertThat(testee.getNamespaceURI(XMLConstants.XMLNS_ATTRIBUTE)).isEqualTo(XMLConstants.XMLNS_ATTRIBUTE_NS_URI );

    }

    @Test
    public void testGetPrefix() {

        // TEST & VERIFY
        assertThat(testee.getPrefix("http://www.fuin.org/abc")).isEqualTo("abc");
        assertThat(testee.getPrefix("http://www.fuin.org/def")).isEqualTo("def");
        assertThat(testee.getPrefix("ghi")).isNull();
        assertThat(testee.getPrefix(XMLConstants.XML_NS_URI)).isEqualTo(XMLConstants.XML_NS_PREFIX);
        assertThat(testee.getPrefix(XMLConstants.XMLNS_ATTRIBUTE_NS_URI )).isEqualTo(XMLConstants.XMLNS_ATTRIBUTE);

    }
    
    @Test
    public void testGetPrefixesUnbound() {
        
        // PREPARE
        final NamespaceContextMap testee = new NamespaceContextMap();
        

        // TEST
        final Iterator<String> itPrefixes = testee.getPrefixes("http://www.fuin.org/abc");
        
        // VERIFY
        assertThat(itPrefixes).isNotNull();
        assertThat(itPrefixes.hasNext()).isFalse();

    }

    @Test
    public void testGetPrefixesSingle() {
        
        // PREPARE
        final NamespaceContextMap testee = new NamespaceContextMap("abc", "http://www.fuin.org/abc");
        

        // TEST
        final Iterator<String> itPrefixes = testee.getPrefixes("http://www.fuin.org/abc");
        
        // VERIFY
        assertThat(itPrefixes.next()).isEqualTo("abc");
        assertThat(itPrefixes.hasNext()).isFalse();

    }

    @Test
    public void testGetPrefixesMultiple() {
        
        // PREPARE
        final NamespaceContextMap testee = new NamespaceContextMap("abc", "http://www.fuin.org/abc", "def", "http://www.fuin.org/abc");
        

        // TEST
        final Iterator<String> itPrefixes = testee.getPrefixes("http://www.fuin.org/abc");
        
        // VERIFY
        assertThat(itPrefixes.next()).isEqualTo("abc");
        assertThat(itPrefixes.next()).isEqualTo("def");
        assertThat(itPrefixes.hasNext()).isFalse();

    }
    
    @Test
    public void testGetPrefixesXML_NS_URI() {
        
        // PREPARE
        final NamespaceContextMap testee = new NamespaceContextMap();

        // TEST
        final Iterator<String> itPrefixes = testee.getPrefixes(XMLConstants.XML_NS_URI);
        
        // VERIFY
        assertThat(itPrefixes.next()).isEqualTo(XMLConstants.XML_NS_PREFIX);
        assertThat(itPrefixes.hasNext()).isFalse();

    }

    @Test
    public void testGetPrefixesXMLNS_ATTRIBUTE_NS_URI() {
        
        // PREPARE
        final NamespaceContextMap testee = new NamespaceContextMap();

        // TEST
        final Iterator<String> itPrefixes = testee.getPrefixes(XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
        
        // VERIFY
        assertThat(itPrefixes.next()).isEqualTo(XMLConstants.XMLNS_ATTRIBUTE);
        assertThat(itPrefixes.hasNext()).isFalse();

    }

    @Test
    public void testGetPrefixesNull() {
        
        // PREPARE
        final NamespaceContextMap testee = new NamespaceContextMap();

        // TEST
        try {
            testee.getPrefixes(null);
        } catch (final IllegalArgumentException ex) {
            // VERIFY
            assertThat(ex.getMessage()).isEqualTo("");
        }
        
    }
    
}
// CHECKSTYLE:ON
