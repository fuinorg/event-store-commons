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

import static org.fuin.esc.eshttp.ESHttpUtils.createDocumentBuilder;
import static org.fuin.esc.eshttp.ESHttpUtils.createXPath;
import static org.fuin.esc.eshttp.ESHttpUtils.findContentInteger;
import static org.fuin.esc.eshttp.ESHttpUtils.findContentText;
import static org.fuin.esc.eshttp.ESHttpUtils.findNode;
import static org.fuin.esc.eshttp.ESHttpUtils.findNodes;
import static org.fuin.esc.eshttp.ESHttpUtils.parseDocument;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.spi.DeserializerRegistry;
import org.fuin.esc.spi.EnhancedMimeType;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.utils4j.Utils4J;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Reads and XML Atom feed.
 */
public final class AtomFeedXmlReader implements AtomFeedReader {

    @Override
    public final List<URI> readAtomFeed(final InputStream in) {

        final Document doc = parseDocument(createDocumentBuilder(), in);
        final XPath xPath = createXPath("ns", "http://www.w3.org/2005/Atom");
        final NodeList nodeList = findNodes(doc, xPath, "/ns:feed/ns:entry/ns:id");

        final List<URI> uris = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node node = nodeList.item(i);
            final String text = node.getTextContent();
            try {
                uris.add(Utils4J.url(text).toURI());
            } catch (final URISyntaxException ex) {
                throw new RuntimeException("Couldn't create URI: " + text);
            }
        }
        return uris;
    }

    @Override
    public final CommonEvent readEvent(final DeserializerRegistry desRegistry, final InputStream in) {
        final AtomEntry<Node> entry = readAtomEntry(in);

        final ESHttpXmlUnmarshaller unmarshaller = new ESHttpXmlUnmarshaller();

        final Object data = unmarshaller.unmarshal(desRegistry, new SerializedDataType(entry.getEventType()),
                entry.getDataContentType(), entry.getData());
        final Object meta;
        if (entry.getMetaType() == null) {
            meta = null;
        } else {
            meta = unmarshaller.unmarshal(desRegistry, new SerializedDataType(entry.getMetaType()),
                    entry.getMetaContentType(), entry.getMeta());
        }
        if (meta == null) {
            return new SimpleCommonEvent(new EventId(entry.getEventId()),
                    new TypeName(entry.getEventType()), data);
        }
        return new SimpleCommonEvent(new EventId(entry.getEventId()), new TypeName(entry.getEventType()),
                data, new TypeName(entry.getMetaType()), meta);

    }

    /**
     * Parses the atom data without creating the event itself from data &amp; meta data.
     * 
     * @param in
     *            Input stream to read.
     * 
     * @return Entry.
     */
    public final AtomEntry<Node> readAtomEntry(final InputStream in) {

        final Document doc = parseDocument(createDocumentBuilder(), in);
        final XPath xPath = createXPath("atom", "http://www.w3.org/2005/Atom");

        final String eventStreamId = findContentText(doc, xPath, "/atom:entry/atom:content/eventStreamId");
        final Integer eventNumber = findContentInteger(doc, xPath, "/atom:entry/atom:content/eventNumber");
        final String eventType = findContentText(doc, xPath, "/atom:entry/atom:content/eventType");
        final String eventId = findContentText(doc, xPath, "/atom:entry/atom:content/eventId");
        final String dataContextTypeStr = findContentText(doc, xPath,
                "/atom:entry/atom:content/metadata/EscSysMeta/data-content-type");
        final EnhancedMimeType dataContentType = EnhancedMimeType.create(dataContextTypeStr);
        final String metaContentTypeStr = findContentText(doc, xPath,
                "/atom:entry/atom:content/metadata/EscSysMeta/meta-content-type");
        final EnhancedMimeType metaContentType = EnhancedMimeType.create(metaContentTypeStr);
        final String metaTypeStr = findContentText(doc, xPath,
                "/atom:entry/atom:content/metadata/EscSysMeta/meta-type");
        final Node data = child(findNode(doc, xPath, "/atom:entry/atom:content/data"));
        final Node meta = child(findNode(doc, xPath, "/atom:entry/atom:content/metadata/EscUserMeta"));

        return new AtomEntry<Node>(eventStreamId, eventNumber, eventType, eventId, dataContentType,
                metaContentType, metaTypeStr, data, meta);

    }

    private Node child(final Node node) {
        if (node == null) {
            return null;
        }
        return node.getFirstChild();
    }

}
