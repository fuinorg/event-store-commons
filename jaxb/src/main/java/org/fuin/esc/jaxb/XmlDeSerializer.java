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
import org.fuin.utils4j.TestOmitted;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Serializes and deserializes an object from/to XML using JAXB. The content type for serialization is always
 * "application/xml". This implementation supports {@link Node} and <code>byte[]</code> for unmarshalling
 * content.
 */
@TestOmitted("Tested with other classes")
public final class XmlDeSerializer implements SerDeserializer {

    private static final Logger LOG = LoggerFactory.getLogger(XmlDeSerializer.class);

    @NotNull
    private final EnhancedMimeType mimeType;

    @NotNull
    private final Marshaller marshaller;

    @NotNull
    private final Unmarshaller unmarshaller;

    /**
     * Constructor with JAXB context classes.
     *
     * @param encoding         Encoding to use.
     * @param version          Version of the serializer/deserializer.
     * @param adapters         Adapters to associate with the JAXB context or <code>null</code>.
     * @param jaxbFragment     Generate the XML fragment or not.
     * @param classesToBeBound Classes to use for the JAXB context.
     */
    private XmlDeSerializer(@NotNull final Charset encoding,
                           @Nullable final String version,
                           @Nullable final XmlAdapter<?, ?>[] adapters,
                           final boolean jaxbFragment,
                           @NotNull final Class<?>... classesToBeBound) {
        super();
        this.mimeType = EnhancedMimeType.create("application", "xml", encoding, version);
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
        Objects.requireNonNull(data, "data==null");
        Objects.requireNonNull(type, "type==null");
        Objects.requireNonNull(mimeType, "mimeType==null");
        if (!mimeType.getBaseType().equals(this.mimeType.getBaseType())) {
            throw new IllegalArgumentException("Cannot handle: " + mimeType);
        }
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
            final String dataStr;
            if (data instanceof Node) {
                dataStr = data.toString();
            } else if (data instanceof byte[]) {
                dataStr = new String((byte[]) data, mimeType.getEncoding());
            } else {
                dataStr = data.getClass().getName();
            }
            throw new RuntimeException("Error de-serializing data of type '" + type + "': " + dataStr, ex);
        }
    }

    /**
     * Convenience method to return a builder.
     *
     * @return
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds an instance of the outer class.
     */
    public static final class Builder {

        private Charset encoding;

        private String version;

        private boolean jaxbFragment;

        private List<Class<?>> classesToBeBound;

        private List<XmlAdapter<?, ?>> adapters;

        /**
         * Default constructor.
         */
        public Builder() {
            this.encoding = StandardCharsets.UTF_8;
            this.jaxbFragment = true;
            this.classesToBeBound = new ArrayList<>();
            this.adapters = new ArrayList<>();
        }

        /**
         * Sets the encoding to use for serialization/deserialization.
         *
         * @param encoding Encoding.
         * @return Builder.
         */
        public Builder encoding(@NotNull final Charset encoding) {
            this.encoding = Objects.requireNonNull(encoding, "encoding==null");
            return this;
        }

        /**
         * Generate an XML fragment or not.
         *
         * @param jaxbFragment If the output is an XML fragment {@literal true}, else {@literal false}.
         * @return Builder.
         */
        public Builder fragment(final boolean jaxbFragment) {
            this.jaxbFragment = jaxbFragment;
            return this;
        }

        /**
         * Adds a class to be known in the JAX-B context.
         *
         * @param classToBeBound Class to add.
         * @return Builder.
         */
        public Builder add(@NotNull final Class<?> classToBeBound) {
            this.classesToBeBound.add(Objects.requireNonNull(classToBeBound, "classToBeBound==null"));
            return this;
        }

        /**
         * Adds an adapter to associate with the JAXB context
         *
         * @param adapter Adapter to add.
         * @return Builder.
         */
        public Builder add(@NotNull final XmlAdapter<?, ?> adapter) {
            this.adapters.add(Objects.requireNonNull(adapter, "adapter==null"));
            return this;
        }

        /**
         * Sets the version of the serializer/deserializer.
         *
         * @param version Version.
         * @return Builder.
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * Builds a new instance.
         *
         * @return New instance.
         */
        public XmlDeSerializer build() {
            return new XmlDeSerializer(
                    encoding,
                    version,
                    adapters.toArray(new XmlAdapter<?, ?>[0]),
                    jaxbFragment,
                    classesToBeBound.toArray(new Class<?>[0]));
        }

    }

}
