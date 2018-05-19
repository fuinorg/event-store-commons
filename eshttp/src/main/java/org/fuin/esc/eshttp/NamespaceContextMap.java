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
package org.fuin.esc.eshttp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.objects4j.common.ConstraintViolationException;
import javax.annotation.Nullable;

/**
 * Provided a simple namespace context.
 */
public final class NamespaceContextMap implements NamespaceContext {

    private final Map<String, String> prefixMap;

    private final Map<String, List<String>> uriMap;

    /**
     * Constructor with 'prefix' + 'uri' string pairs.
     * 
     * @param values
     *            List of prefixes with their corresponding URI.
     */
    public NamespaceContextMap(@Nullable final String... values) {
        super();
        if (values != null && values.length % 2 != 0) {
            throw new ConstraintViolationException(
                    "Only an even number of strings (Pairs of 'prefix' + 'uri') is allowed");
        }
        prefixMap = new HashMap<>();
        uriMap = new HashMap<>();

        // Values that may be overwritten
        prefixMap.put(XMLConstants.DEFAULT_NS_PREFIX, XMLConstants.NULL_NS_URI);

        // User values
        if (values != null) {
            for (int i = 0; i < values.length; i = i + 2) {
                final String prefix = values[i];
                final String uri = values[i + 1];
                prefixMap.put(prefix, uri);
                List<String> prefixes = uriMap.get(uri);
                if (prefixes == null) {
                    prefixes = new ArrayList<>();
                    uriMap.put(uri, prefixes);
                }
                prefixes.add(prefix);
            }
        }

        // Values that can never be overwritten
        uriMap.put(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                EscSpiUtils.asList(new String[] { XMLConstants.XMLNS_ATTRIBUTE }));
        uriMap.put(XMLConstants.XML_NS_URI, EscSpiUtils.asList(new String[] { XMLConstants.XML_NS_PREFIX }));
        prefixMap.put(XMLConstants.XMLNS_ATTRIBUTE, XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
        prefixMap.put(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
    }

    @Override
    public final String getNamespaceURI(final String prefix) {
        final String uri = prefixMap.get(prefix);
        if (uri == null) {
            return XMLConstants.NULL_NS_URI;
        }
        return uri;
    }

    @Override
    public final String getPrefix(final String namespaceURI) {
        final List<String> prefixes = uriMap.get(namespaceURI);
        if ((prefixes == null) || (prefixes.size() < 1)) {
            return null;
        }
        return prefixes.get(0);
    }

    @Override
    public final Iterator<String> getPrefixes(final String namespaceURI) {
        List<String> prefixes = uriMap.get(namespaceURI);
        if (prefixes == null) {
            prefixes = Collections.emptyList();
        }
        return prefixes.iterator();
    }

}
