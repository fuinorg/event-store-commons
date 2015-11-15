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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 * Serializes and deserializes an object from/to XML using JAXB. The content type for serialization is always
 * "application/xml". This implementation supports {@link Node} and
 * <code>byte[]</data> for unmarshalling content.
 */
public final class XmlDeSerializer implements SerDeserializer {

    private static final Logger LOG = LoggerFactory.getLogger(XmlDeSerializer.class);

    private final EnhancedMimeType mimeType;

    private final Marshaller marshaller;

    private final Unmarshaller unmarshaller;

    /**
     * Constructor that creates a JAXB context internally and uses UTF-8 encoding.
     * 
     * @param classesToBeBound
     *            Classes to use for the JAXB context.
     */
    public XmlDeSerializer(final Class<?>... classesToBeBound) {
        this(Charset.forName("utf-8"), true, classesToBeBound);
    }

    /**
     * Constructor that creates a JAXB context internally and uses UTF-8 encoding.
     * 
     * @param jaxbFragment
     *            Generate the XML fragment or not.
     * @param classesToBeBound
     *            Classes to use for the JAXB context.
     */
    public XmlDeSerializer(final boolean jaxbFragment, final Class<?>... classesToBeBound) {
        this(Charset.forName("utf-8"), jaxbFragment, classesToBeBound);
    }

    /**
     * Constructor that creates a JAXB context internally.
     * 
     * @param encoding
     *            Encoding to use.
     * @param classesToBeBound
     *            Classes to use for the JAXB context.
     */
    public XmlDeSerializer(final Charset encoding, final Class<?>... classesToBeBound) {
        this(encoding, null, true, classesToBeBound);
    }

    /**
     * Constructor that creates a JAXB context internally.
     * 
     * @param encoding
     *            Encoding to use.
     * @param classesToBeBound
     *            Classes to use for the JAXB context.
     */
    public XmlDeSerializer(final Charset encoding, final boolean jaxbFragment,
            final Class<?>... classesToBeBound) {
        this(encoding, null, true, classesToBeBound);
    }

    /**
     * Constructor with JAXB context classes.
     * 
     * @param encoding
     *            Encoding to use.
     * @param adapters
     *            Adapters to associate with the JAXB context or <code>null</code>.
     * @param jaxbFragment
     *            Generate the XML fragment or not.
     * @param classesToBeBound
     *            Classes to use for the JAXB context.
     */
    public XmlDeSerializer(final Charset encoding, final XmlAdapter<?, ?>[] adapters,
            final boolean jaxbFragment, final Class<?>... classesToBeBound) {
        super();
        this.mimeType = EnhancedMimeType.create("application", "xml", encoding);
        try {
            final JAXBContext ctx = JAXBContext.newInstance(classesToBeBound);
            marshaller = ctx.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, jaxbFragment);
            unmarshaller = ctx.createUnmarshaller();
            if ((adapters == null) || (adapters.length == 0)) {
                LOG.debug("No adapters set");
            } else {
                for (XmlAdapter<?, ?> adapter : adapters) {
                    LOG.debug("Set adapter : " + adapter);
                    marshaller.setAdapter(adapter);
                    unmarshaller.setAdapter(adapter);
                }
            }
            unmarshaller.setEventHandler(new ValidationEventHandler() {
                @Override
                public boolean handleEvent(final ValidationEvent event) {
                    if (event.getSeverity() > 0) {
                        if (event.getLinkedException() == null) {
                            throw new RuntimeException("Error unmarshalling the data: " + event.getMessage());
                        }
                        throw new RuntimeException("Error unmarshalling the data", event.getLinkedException());
                    }
                    return true;
                }
            });
        } catch (final JAXBException ex) {
            throw new RuntimeException("Error initializing JAXB helper classes", ex);
        }
    }

    @Override
    public final EnhancedMimeType getMimeType() {
        return mimeType;
    }

    @Override
    public final byte[] marshal(final Object obj) {
        try {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
            final Writer writer = new OutputStreamWriter(bos, mimeType.getEncoding());
            marshaller.marshal(obj, writer);
            return bos.toByteArray();
        } catch (final JAXBException ex) {
            throw new RuntimeException("Error serializing data", ex);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <T> T unmarshal(final Object data, final EnhancedMimeType mimeType) {
        try {

            if (data instanceof byte[]) {
                final Reader reader = new InputStreamReader(new ByteArrayInputStream((byte[]) data),
                        mimeType.getEncoding());
                return (T) unmarshaller.unmarshal(reader);
            }
            if (data instanceof Node) {
                return (T) unmarshaller.unmarshal((Node) data);
            }
            throw new IllegalArgumentException("This deserializer only supports input of type '"
                    + Node.class.getName() + "' and 'byte[]', but was: " + data);

        } catch (final JAXBException ex) {
            throw new RuntimeException("Error de-serializing data", ex);
        }
    }

}
