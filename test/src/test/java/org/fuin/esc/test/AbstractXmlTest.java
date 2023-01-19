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
package org.fuin.esc.test;

import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import org.fuin.units4j.Units4JUtils;
import org.fuin.utils4j.jaxb.CDataXmlStreamWriter;
import org.fuin.utils4j.jaxb.JaxbUtils;

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
    protected static <T> String marshalToStr(final T data, final XmlAdapter<?, ?>[] adapters,
            final Class<?>... classesToBeBound) {

        final StringWriter writer = new StringWriter();

        try (final CDataXmlStreamWriter sw = new CDataXmlStreamWriter(
                XMLOutputFactory.newInstance().createXMLStreamWriter(writer));) {

            final JAXBContext ctx = JAXBContext.newInstance(classesToBeBound);
            JaxbUtils.marshal(ctx, data, adapters, sw);

        } catch (XMLStreamException | FactoryConfigurationError | JAXBException ex) {
            throw new RuntimeException(ex);
        }

        return writer.toString();
    }

}
// CHECKSTYLE:ON
