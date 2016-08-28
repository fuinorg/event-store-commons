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

import static org.fuin.esc.spi.EscSpiUtils.nodeToString;

import org.apache.commons.codec.binary.Base64;
import org.fuin.esc.spi.Base64Data;
import org.fuin.esc.spi.Deserializer;
import org.fuin.esc.spi.DeserializerRegistry;
import org.fuin.esc.spi.EnhancedMimeType;
import org.fuin.esc.spi.SerializedDataType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Unmarshals data in XML format after reading it from the event store.
 */
public final class ESHttpXmlUnmarshaller implements ESHttpUnmarshaller {

    @Override
    public final Object unmarshal(final DeserializerRegistry registry, final SerializedDataType dataType,
            final EnhancedMimeType mimeType, final Object data) {

        if (data == null) {
            return null;
        }
        if (!(data instanceof Node)) {
            throw new IllegalArgumentException("Can only unmarshal DOM nodes, but was: " + data + " ["
                    + data.getClass().getName() + "]");
        }
        final Node node = (Node) data;
        final String transferEncodingData = mimeType.getParameter("transfer-encoding");
        if (transferEncodingData == null) {
            final Node childNode = findChildNode(node, dataType.asBaseType());
            final Deserializer deSer = registry.getDeserializer(dataType, mimeType);
            return deSer.unmarshal(childNode, mimeType);
        }
        final Node childNode = findChildNode(node, Base64Data.EL_ROOT_NAME);
        final String base64str = childNode.getTextContent();
        final byte[] bytes = Base64.decodeBase64(base64str);
        final Deserializer deSer = registry.getDeserializer(dataType, mimeType);
        return deSer.unmarshal(bytes, mimeType);

    }

    private Node findChildNode(final Node node, final String name) {
        final NodeList childs = node.getChildNodes();
        final int count = childs.getLength();
        for (int i = 0; i < count; i++) {
            final Node child = childs.item(i);
            if (name.equals(child.getNodeName())) {
                return child;
            }

        }
        throw new IllegalStateException("Child node '" + name + "' not found: " + nodeToString(node));
    }

}
