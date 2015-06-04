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
package org.fuin.esc.test;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Adapter for an {@link XMLStreamWriter}. CAUTION: If you override a method,
 * don't forget to call the "super" method!
 */
// CHECKSTYLE:OFF DesignForExtension is not possible here 
public class XMLStreamWriterAdapter implements XMLStreamWriter {

    /** All method calls are delegated to this writer. */
    private final XMLStreamWriter delegate;

    /**
     * Constructor with delegate.
     * 
     * @param delegate
     *            All method calls are delegated to this writer.
     */
    public XMLStreamWriterAdapter(final XMLStreamWriter delegate) {
        this.delegate = delegate;
    }

    @Override
    public void writeStartElement(final String localName)
            throws XMLStreamException {
        delegate.writeStartElement(localName);
    }

    @Override
    public void writeStartElement(final String namespaceURI,
            final String localName) throws XMLStreamException {
        delegate.writeStartElement(namespaceURI, localName);
    }

    @Override
    public void writeStartElement(final String prefix, final String localName,
            final String namespaceURI) throws XMLStreamException {
        delegate.writeStartElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeEmptyElement(final String namespaceURI,
            final String localName) throws XMLStreamException {
        delegate.writeEmptyElement(namespaceURI, localName);
    }

    @Override
    public void writeEmptyElement(final String prefix, final String localName,
            final String namespaceURI) throws XMLStreamException {
        delegate.writeEmptyElement(prefix, localName, namespaceURI);
    }

    @Override
    public void writeEmptyElement(final String localName)
            throws XMLStreamException {
        delegate.writeEmptyElement(localName);
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        delegate.writeEndElement();
    }

    @Override
    public void writeEndDocument() throws XMLStreamException {
        delegate.writeEndDocument();
    }

    @Override
    public void close() throws XMLStreamException {
        delegate.close();
    }

    @Override
    public void flush() throws XMLStreamException {
        delegate.flush();
    }

    @Override
    public void writeAttribute(final String localName, final String value)
            throws XMLStreamException {
        delegate.writeAttribute(localName, value);
    }

    @Override
    public void writeAttribute(final String prefix, final String namespaceURI,
            final String localName, final String value)
            throws XMLStreamException {
        delegate.writeAttribute(prefix, namespaceURI, localName, value);
    }

    @Override
    public void writeAttribute(final String namespaceURI,
            final String localName, final String value)
            throws XMLStreamException {
        delegate.writeAttribute(namespaceURI, localName, value);
    }

    @Override
    public void writeNamespace(final String prefix, final String namespaceURI)
            throws XMLStreamException {
        delegate.writeNamespace(prefix, namespaceURI);
    }

    @Override
    public void writeDefaultNamespace(final String namespaceURI)
            throws XMLStreamException {
        delegate.writeDefaultNamespace(namespaceURI);
    }

    @Override
    public void writeComment(final String data) throws XMLStreamException {
        delegate.writeComment(data);
    }

    @Override
    public void writeProcessingInstruction(final String target)
            throws XMLStreamException {
        delegate.writeProcessingInstruction(target);
    }

    @Override
    public void writeProcessingInstruction(final String target,
            final String data) throws XMLStreamException {
        delegate.writeProcessingInstruction(target, data);
    }

    @Override
    public void writeCData(final String data) throws XMLStreamException {
        delegate.writeCData(data);
    }

    @Override
    public void writeDTD(final String dtd) throws XMLStreamException {
        delegate.writeDTD(dtd);
    }

    @Override
    public void writeEntityRef(final String name) throws XMLStreamException {
        delegate.writeEntityRef(name);
    }

    @Override
    public void writeStartDocument() throws XMLStreamException {
        delegate.writeStartDocument();
    }

    @Override
    public void writeStartDocument(final String version)
            throws XMLStreamException {
        delegate.writeStartDocument(version);
    }

    @Override
    public void writeStartDocument(final String encoding, final String version)
            throws XMLStreamException {
        delegate.writeStartDocument(encoding, version);
    }

    @Override
    public void writeCharacters(final String text) throws XMLStreamException {
        delegate.writeCharacters(text);
    }

    @Override
    public void writeCharacters(final char[] text, final int start,
            final int len) throws XMLStreamException {
        delegate.writeCharacters(text, start, len);
    }

    @Override
    public String getPrefix(final String uri) throws XMLStreamException {
        return delegate.getPrefix(uri);
    }

    @Override
    public void setPrefix(final String prefix, final String uri)
            throws XMLStreamException {
        delegate.setPrefix(prefix, uri);
    }

    @Override
    public void setDefaultNamespace(final String uri) throws XMLStreamException {
        delegate.setDefaultNamespace(uri);
    }

    @Override
    public void setNamespaceContext(final NamespaceContext context)
            throws XMLStreamException {
        delegate.setNamespaceContext(context);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return delegate.getNamespaceContext();
    }

    @Override
    public Object getProperty(final String name)
            throws IllegalArgumentException {
        return delegate.getProperty(name);
    }

}
//CHECKSTYLE:ON
