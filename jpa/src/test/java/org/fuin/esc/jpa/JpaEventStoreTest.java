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

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.config.PropertyVisibilityStrategy;
import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.EventStore;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.SerializedDataType;
import org.fuin.esc.api.SerializedDataTypeRegistry;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleSerializedDataTypeRegistry;
import org.fuin.esc.api.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamState;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.jaxb.XmlDeSerializer;
import org.fuin.esc.jpa.examples.AggregateStreamId;
import org.fuin.esc.jpa.examples.VendorCreatedEvent;
import org.fuin.esc.jpa.examples.VendorStream;
import org.fuin.esc.jsonb.EscEvent;
import org.fuin.esc.jsonb.EscEvents;
import org.fuin.esc.jsonb.EscJsonbUtils;
import org.fuin.esc.jsonb.EscMeta;
import org.fuin.esc.jsonb.JsonbDeSerializer;
import org.fuin.objects4j.common.Contract;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


public final class JpaEventStoreTest extends AbstractPersistenceTest {

    @Test
    public void testAppendSingleSuccess() throws Exception {

        // PREPARE
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();

        final XmlDeSerializer xmlDeSer = new XmlDeSerializer(VendorCreatedEvent.class);
        final SerializedDataType serDataType = new SerializedDataType(VendorCreatedEvent.TYPE);
        registry.add(serDataType, xmlDeSer.getMimeType().getBaseType(), xmlDeSer);

        try (final JpaEventStore testee = new JpaEventStore(getEm(), new JpaIdStreamFactory() {
            @Override
            public JpaStream createStream(final StreamId streamId) {
                final String vendorId = streamId.getSingleParamValue();
                return new VendorStream(vendorId);
            }

            @Override
            public boolean containsType(final StreamId streamId) {
                return true;
            }
        }, registry, registry)) {
            testee.open();

            final String vendorId = UUID.randomUUID().toString();
            final VendorCreatedEvent vendorCreatedEvent = new VendorCreatedEvent(vendorId);
            final AggregateStreamId streamId = new AggregateStreamId("Vendor", "vendorId", vendorId);
            final EventId eventId = new EventId();
            final TypeName dataType = new TypeName(VendorCreatedEvent.TYPE);
            final CommonEvent eventData = new SimpleCommonEvent(eventId, dataType, vendorCreatedEvent);

            // TEST
            execute(testee, streamId, eventData, eventId);

        }

    }

    @Test
    public void testNoParamsStreams() throws Exception {

        // PREPARE


        final SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();
        typeRegistry.add(EventA.SER_TYPE, EventA.class);
        typeRegistry.add(EventB.SER_TYPE, EventB.class);

        try (final JsonbDeSerializer jsonbDeSer = createJsonbDeSerializer()) {
            initSerDeserializerRegistry(typeRegistry, jsonbDeSer);

            final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
            registry.add(EventA.SER_TYPE, "application/json", jsonbDeSer);
            registry.add(EventB.SER_TYPE, "application/json", jsonbDeSer);

            final EventA eventA = new EventA("John Doe");
            final SimpleStreamId streamA = new SimpleStreamId("StreamA");
            final EventId eventIdA = new EventId("84fe8213-ac1b-4cda-8321-703c2e448052");
            final CommonEvent commonEventA = new SimpleCommonEvent(eventIdA, EventA.TYPE, eventA);

            final EventB eventB = new EventB("Jane Doe");
            final SimpleStreamId streamB = new SimpleStreamId("StreamB");
            final EventId eventIdB = new EventId("23962a5e-da10-402f-8560-340745b09b2c");
            final CommonEvent commonEventB = new SimpleCommonEvent(eventIdB, EventB.TYPE, eventB);

            try (final JpaEventStore testee = new JpaEventStore(getEm(), new JpaIdStreamFactory() {
                @Override
                public JpaStream createStream(final StreamId streamId) {
                    return new NoParamsStream(streamId);
                }

                @Override
                public boolean containsType(final StreamId streamId) {
                    return true;
                }
            }, registry, registry)) {
                testee.open();

                // TEST
                execute(testee, streamA, commonEventA, eventIdA);
                execute(testee, streamB, commonEventB, eventIdB);

            }
        }

    }

    private static void execute(final EventStore eventStore, final StreamId streamId,
                                final CommonEvent commonEvent, final EventId eventId) throws Exception {
        beginTransaction();
        try {

            final long version = eventStore.appendToStream(streamId,
                    ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(), commonEvent);

            // VERIFY
            assertThat(version).isEqualTo(0);
            final boolean exists = eventStore.streamExists(streamId);
            final StreamState state = eventStore.streamState(streamId);
            final StreamEventsSlice slice = eventStore.readEventsForward(streamId, 0, 2);
            final CommonEvent readCommonEvent = eventStore.readEvent(streamId, 0);

            assertThat(exists).isTrue();
            assertThat(state).isEqualTo(StreamState.ACTIVE);
            assertThat(slice.getFromEventNumber()).isEqualTo(0);
            assertThat(slice.getNextEventNumber()).isEqualTo(1);
            assertThat(slice.isEndOfStream()).isTrue();
            assertThat(slice.getEvents()).hasSize(1);
            final CommonEvent ce = slice.getEvents().get(0);
            assertThat(ce.getId()).isEqualTo(eventId);
            assertThat(ce.getId()).isEqualTo(readCommonEvent.getId());

            commitTransaction();
        } catch (final Exception ex) {
            rollbackTransaction();
            throw ex;
        }
    }


    public static class EventA {

        public static final String EVENT = "EventA";

        public static final TypeName TYPE = new TypeName(EVENT);

        public static final SerializedDataType SER_TYPE = new SerializedDataType(EVENT);

        @JsonbProperty
        private String a;

        protected EventA() {
        }

        public EventA(String a) {
            this.a = a;
        }

    }

    public static class EventB {

        public static final String EVENT = "EventB";

        public static final TypeName TYPE = new TypeName(EVENT);

        public static final SerializedDataType SER_TYPE = new SerializedDataType(EVENT);

        @JsonbProperty
        private String b;

        protected EventB() {
        }

        public EventB(String b) {
            this.b = b;
        }

    }

    public static class FieldAccessStrategy implements PropertyVisibilityStrategy {

        public boolean isVisible(Field field) {
            return true;
        }

        public boolean isVisible(Method method) {
            return false;
        }
    }

    private static JsonbDeSerializer createJsonbDeSerializer() {
        return JsonbDeSerializer.builder()
                .withSerializers(EscJsonbUtils.createEscJsonbSerializers())
                .withDeserializers(EscJsonbUtils.createEscJsonbDeserializers())
                .withPropertyVisibilityStrategy(new FieldAccessStrategy())
                .withEncoding(StandardCharsets.UTF_8)
                .build();
    }

    /**
     * Creates a registry that connects the type with the appropriate serializer and de-serializer.
     *
     * @param typeRegistry Type registry (Mapping from type name to class).
     * @param jsonbDeSer   JSON-B serializer/deserializer to use.
     */
    public static void initSerDeserializerRegistry(@NotNull SerializedDataTypeRegistry typeRegistry,
                                                   @NotNull JsonbDeSerializer jsonbDeSer) {

        Contract.requireArgNotNull("typeRegistry", typeRegistry);
        Contract.requireArgNotNull("jsonbDeSer", jsonbDeSer);

        SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();

        // Base types always needed
        registry.add(EscEvents.SER_TYPE, jsonbDeSer.getMimeType().getBaseType(), jsonbDeSer);
        registry.add(EscEvent.SER_TYPE, jsonbDeSer.getMimeType().getBaseType(), jsonbDeSer);
        registry.add(EscMeta.SER_TYPE, jsonbDeSer.getMimeType().getBaseType(), jsonbDeSer);

        // User defined types
        registry.add(EventA.SER_TYPE, jsonbDeSer.getMimeType().getBaseType(), jsonbDeSer);
        registry.add(EventB.SER_TYPE, jsonbDeSer.getMimeType().getBaseType(), jsonbDeSer);

        jsonbDeSer.init(typeRegistry, registry, registry);

    }

}

