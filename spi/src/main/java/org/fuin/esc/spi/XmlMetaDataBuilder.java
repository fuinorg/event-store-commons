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
package org.fuin.esc.spi;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Handles XML meta data creation/update.
 */
public final class XmlMetaDataBuilder implements MetaDataBuilder<Document> {

    private Document document;

    @Override
    public final void init(final Document obj) {
        if (obj == null) {
            try {
                final DocumentBuilderFactory factory = DocumentBuilderFactory
                        .newInstance();
                final DocumentBuilder builder = factory.newDocumentBuilder();
                document = builder.newDocument();
                document.appendChild(document.createElement("meta"));
            } catch (final ParserConfigurationException ex) {
                throw new RuntimeException(ex);
            }
        } else {
            document = obj;
        }
    }

    @Override
    public final void add(final String key, final String value) {
        final Element el = document.createElement(key);
        el.setTextContent(value);
        document.getDocumentElement().appendChild(el);
    }

    @Override
    public final void add(final String key, final boolean value) {
        final Element el = document.createElement(key);
        el.setTextContent("" + value);
        document.getDocumentElement().appendChild(el);
    }

    @Override
    public final void add(final String key, final int value) {
        final Element el = document.createElement(key);
        el.setTextContent("" + value);
        document.getDocumentElement().appendChild(el);
    }

    @Override
    public final Document build() {
        return document;
    }

}
