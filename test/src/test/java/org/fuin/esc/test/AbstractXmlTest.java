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

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.fuin.units4j.Units4JUtils;

/**
 * Base class for XML marshal/unmarshal tests.
 */
// CHECKSTYLE:OFF Test
public abstract class AbstractXmlTest {

    /**
     * Marshals the given data. A <code>null</code> data argument returns
     * <code>null</code>. The difference between this and the
     * {@link Units4JUtils#marshal(Object, XmlAdapter[], Class...)} class is,
     * that it handles CDATA sections correctly. This should actually be moved
     * to Units4J as standard.
     * 
     * @param data
     *            Data to serialize or <code>null</code>.
     * @param adapters
     *            Adapters to associate with the marshaller or <code>null</code>
     *            .
     * @param classesToBeBound
     *            List of java classes to be recognized by the
     *            {@link JAXBContext}.
     * 
     * @return XML data or <code>null</code>.
     * 
     * @param <T>
     *            Type of the data.
     */
    // TODO Add handling of CDATA to units4j and remove this method
    protected static <T> String marshalToStr(final T data,
            final XmlAdapter<?, ?>[] adapters,
            final Class<?>... classesToBeBound) {
        try {
            final JAXBContext ctx = JAXBContext.newInstance(classesToBeBound);
            final Marshaller marshaller = ctx.createMarshaller();
            if (adapters != null) {
                for (XmlAdapter<?, ?> adapter : adapters) {
                    marshaller.setAdapter(adapter);
                }
            }
            final StringWriter writer = new StringWriter();
            final XMLOutputFactory xof = XMLOutputFactory.newInstance();
            final XMLStreamWriter sw = xof.createXMLStreamWriter(writer);
            final XMLStreamWriterAdapter swa = new XMLStreamWriterAdapter(sw) {
                @Override
                public void writeCharacters(String text)
                        throws XMLStreamException {
                    if (text.startsWith("<![CDATA[") && text.endsWith("]]>")) {
                        final String str = text.substring(9, text.length() - 3);
                        super.writeCData(str);
                    } else {
                        super.writeCharacters(text);
                    }
                }
            };
            marshaller.marshal(data, swa);
            return writer.toString();
        } catch (final JAXBException | XMLStreamException ex) {
            throw new RuntimeException("Error marshalling test data", ex);
        }
    }

}
// CHECKSTYLE:ON
