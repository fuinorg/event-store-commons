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

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Handles XML meta data access.
 */
public final class XmlMetaDataAccessor implements MetaDataAccessor<Document> {

    private Document document;

    @Override
    public final void init(final Document obj) {
        document = obj;
    }

    @Override
    public final String getString(final String key) {
        final Node node = getNodeByTagName(key);
        if (node == null) {
            return null;
        }
        return node.getTextContent();
    }

    @Override
    public final Boolean getBoolean(final String key) {
        final Node node = getNodeByTagName(key);
        if (node == null) {
            return null;
        }
        final String str = node.getTextContent();
        return Boolean.valueOf(str);
    }

    @Override
    public final Integer getInteger(final String key) {
        final Node node = getNodeByTagName(key);
        if (node == null) {
            return null;
        }
        final String str = node.getTextContent();
        return Integer.valueOf(str);
    }

    private Node getNodeByTagName(final String key) {
        if (document == null) {
            return null;
        }
        final NodeList nodeList = document.getElementsByTagName(key);
        if (nodeList.getLength() == 0) {
            return null;
        }
        if (nodeList.getLength() > 1) {
            throw new IllegalArgumentException(
                    "Found more than one tag with name '" + key + "': "
                            + nodeList.getLength());
        }
        return nodeList.item(0);
    }

}
