/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved.
 * http://www.fuin.org/
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package org.fuin.esc.jaxb;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.SerDeserializer;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.objects4j.common.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Serializes and deserializes an object from/to XML using JAXB. The content type for serialization is always
 * "application/xml". This implementation supports {@link Node} and <code>byte[]</code> for unmarshalling
 * content.
 */
public final class XmlDeSerializer implements SerDeserializer {

    private static final Logger LOG = LoggerFactory.getLogger(XmlDeSerializer.class);

    @NotNull
    private final EnhancedMimeType mimeType;

    @NotNull
    private final Marshaller marshaller;

    @NotNull
    private final Unmarshaller unmarshaller;

    /**
     * Constructor that creates a JAXB context internally and uses UTF-8 encoding.
     *
     * @param classesToBeBound Classes to use for the JAXB context.
     */
    public XmlDeSerializer(@NotNull final Class<?>... classesToBeBound) {
        this(StandardCharsets.UTF_8, true, classesToBeBound);
    }

    /**
     * Constructor that creates a JAXB context internally and uses UTF-8 encoding.
     *
     * @param jaxbFragment     Generate the XML fragment or not.
     * @param classesToBeBound Classes to use for the JAXB context.
     */
    public XmlDeSerializer(final boolean jaxbFragment,
                           @NotNull final Class<?>... classesToBeBound) {
        this(StandardCharsets.UTF_8, jaxbFragment, classesToBeBound);
    }

    /**
     * Constructor that creates a JAXB context internally.
     *
     * @param encoding         Encoding to use.
     * @param classesToBeBound Classes to use for the JAXB context.
     */
    public XmlDeSerializer(@NotNull final Charset encoding,
                           @NotNull final Class<?>... classesToBeBound) {
        this(encoding, null, true, classesToBeBound);
    }

    /**
     * Constructor that creates a JAXB context internally.
     *
     * @param encoding         Encoding to use.
     * @param jaxbFragment     Generate the XML fragment or not.
     * @param classesToBeBound Classes to use for the JAXB context.
     */
    public XmlDeSerializer(@NotNull final Charset encoding,
                           final boolean jaxbFragment,
                           @NotNull final Class<?>... classesToBeBound) {
        this(encoding, null, jaxbFragment, classesToBeBound);
    }

    /**
     * Constructor with JAXB context classes.
     *
     * @param encoding         Encoding to use.
     * @param adapters         Adapters to associate with the JAXB context or <code>null</code>.
     * @param jaxbFragment     Generate the XML fragment or not.
     * @param classesToBeBound Classes to use for the JAXB context.
     */
    public XmlDeSerializer(@NotNull final Charset encoding,
                           @Nullable final XmlAdapter<?, ?>[] adapters,
                           final boolean jaxbFragment,
                           @NotNull final Class<?>... classesToBeBound) {
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
                    LOG.debug("Set adapter : {}", adapter);
                    marshaller.setAdapter(adapter);
                    unmarshaller.setAdapter(adapter);
                }
            }
            unmarshaller.setEventHandler(event -> {
                if (event.getSeverity() > 0) {
                    if (event.getLinkedException() == null) {
                        throw new RuntimeException("Error unmarshalling the data: " + event.getMessage());
                    }
                    throw new RuntimeException("Error unmarshalling " + "the data", event
                            .getLinkedException());
                }
                return true;
            });
        } catch (final JAXBException ex) {
            throw new RuntimeException("Error initializing JAXB helper classes", ex);
        }
    }

    @Override
    public EnhancedMimeType getMimeType() {
        return mimeType;
    }

    @Override
    public byte[] marshal(@NotNull final Object obj, @NotNull final SerializedDataType type) {
        Contract.requireArgNotNull("obj", obj);
        Contract.requireArgNotNull("type", type);
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
    public <T> T unmarshal(final Object data, final SerializedDataType type, final EnhancedMimeType mimeType) {
        try {

            if (data instanceof byte[]) {
                final Reader reader = new InputStreamReader(new ByteArrayInputStream((byte[]) data), mimeType.getEncoding());
                return (T) unmarshaller.unmarshal(reader);
            }
            if (data instanceof Node) {
                return (T) unmarshaller.unmarshal((Node) data);
            }
            return (T) data;

        } catch (final JAXBException ex) {
            throw new RuntimeException("Error de-serializing data", ex);
        }
    }

}
