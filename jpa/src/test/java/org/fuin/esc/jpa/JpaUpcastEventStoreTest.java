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
package org.fuin.esc.jpa;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.Converter;
import org.fuin.esc.api.ConverterRegistry;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.EventStore;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleConverterRegistry;
import org.fuin.esc.api.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.jaxb.XmlDeSerializer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ESC-1 keystone integration test: an event stored at an older version is read back <b>up-cast</b> to the latest
 * in-memory representation when the {@link JpaEventStore} is built with a {@link ConverterRegistry}. Runs against
 * the shared HSQLDB in-memory unit-test database (no Docker), exercising the real serialize → store → read →
 * deserialize → up-cast path.
 */
public final class JpaUpcastEventStoreTest extends AbstractPersistenceTest {

    private static final SerializedDataType SER_TYPE = new SerializedDataType("GreetEvent");

    @Test
    public void v1EventIsUpcastToV2OnRead() throws Exception {

        // PREPARE: register a v1 (de)serializer for "GreetEvent" and a v1 -> v2 up-caster.
        final XmlDeSerializer v1 = XmlDeSerializer.builder().add(GreetV1.class).version("1").build();
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry.Builder(v1.getMimeType())
                .add(SER_TYPE, v1, v1.getMimeType())
                .build();
        final ConverterRegistry converters = new SimpleConverterRegistry.Builder()
                .add(SER_TYPE, "1", "2", new GreetV1ToV2Converter())
                .build();

        try (final JpaEventStore testee = new JpaEventStore(getEm(), noParamsFactory(), registry, registry, converters)) {
            testee.open();

            // TEST: store a v1 event, read it back
            final CommonEvent read = appendAndRead(testee, new GreetV1("World"), "s-upcast");

            // VERIFY: the reader received the up-cast v2 representation, not the stored v1
            assertThat(read.getData()).isInstanceOf(GreetV2.class);
            assertThat(((GreetV2) read.getData()).getGreeting()).isEqualTo("Hello, World");
        }
    }

    @Test
    public void withoutConvertersTheStoredVersionIsReturned() throws Exception {

        // PREPARE: same v1 (de)serializer, but a store built WITHOUT converters (the pre-ESC-1 default).
        final XmlDeSerializer v1 = XmlDeSerializer.builder().add(GreetV1.class).version("1").build();
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry.Builder(v1.getMimeType())
                .add(SER_TYPE, v1, v1.getMimeType())
                .build();

        try (final JpaEventStore testee = new JpaEventStore(getEm(), noParamsFactory(), registry, registry)) {
            testee.open();

            // TEST & VERIFY: no up-cast wiring -> the stored v1 representation comes back unchanged
            final CommonEvent read = appendAndRead(testee, new GreetV1("World"), "s-plain");
            assertThat(read.getData()).isInstanceOf(GreetV1.class);
        }
    }

    private static CommonEvent appendAndRead(final EventStore es, final Object data, final String streamName) throws Exception {
        final SimpleStreamId streamId = new SimpleStreamId(streamName);
        final CommonEvent event = new SimpleCommonEvent(new EventId(), new TypeName("GreetEvent"), data, null);
        beginTransaction();
        try {
            es.appendToStream(streamId, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(), event);
            final CommonEvent read = es.readEvent(streamId, 0);
            commitTransaction();
            return read;
        } catch (final Exception ex) {
            rollbackTransaction();
            throw ex;
        }
    }

    private static JpaIdStreamFactory noParamsFactory() {
        return new JpaIdStreamFactory() {
            @Override
            public JpaStream createStream(final StreamId streamId) {
                return new NoParamsStream(streamId);
            }

            @Override
            public boolean containsType(final StreamId streamId) {
                return true;
            }
        };
    }

    /** Version 1 of the greet event (raw name). */
    @XmlRootElement(name = "greet-event")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static final class GreetV1 {

        @XmlAttribute(name = "name")
        private String name;

        protected GreetV1() {
        }

        public GreetV1(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /** Version 2 of the greet event (rendered greeting). */
    @XmlRootElement(name = "greet-event")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static final class GreetV2 {

        @XmlAttribute(name = "greeting")
        private String greeting;

        protected GreetV2() {
        }

        public GreetV2(final String greeting) {
            this.greeting = greeting;
        }

        public String getGreeting() {
            return greeting;
        }
    }

    /** Up-casts a stored v1 greet event to the latest v2 representation. */
    static final class GreetV1ToV2Converter implements Converter<GreetV1, GreetV2> {

        @Override
        public Class<GreetV1> getSourceType() {
            return GreetV1.class;
        }

        @Override
        public Class<GreetV2> getTargetType() {
            return GreetV2.class;
        }

        @Override
        public GreetV2 convert(final GreetV1 source) {
            return new GreetV2("Hello, " + source.getName());
        }
    }
}
