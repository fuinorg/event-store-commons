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
package org.fuin.esc.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.EventStore;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamState;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.jpa.examples.AggregateStreamId;
import org.fuin.esc.jpa.examples.VendorCreatedEvent;
import org.fuin.esc.jpa.examples.VendorStream;
import org.fuin.esc.spi.JsonDeSerializer;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.spi.XmlDeSerializer;
import org.fuin.units4j.AbstractPersistenceTest;
import org.junit.Test;

// CHECKSTYLE:OFF
public final class JpaEventStoreTest extends AbstractPersistenceTest {

    @Test
    public void testAppendSingleSuccess() throws Exception {

        // PREPARE
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();

        final XmlDeSerializer xmlDeSer = new XmlDeSerializer(VendorCreatedEvent.class);
        final SerializedDataType serDataType = new SerializedDataType(VendorCreatedEvent.TYPE);
        registry.add(serDataType, xmlDeSer.getMimeType().getBaseType(), xmlDeSer);

        final JpaEventStore testee = new JpaEventStore(getEm(), new JpaIdStreamFactory() {
            @Override
            public JpaStream createStream(final StreamId streamId) {
                final String vendorId = streamId.getSingleParamValue();
                return new VendorStream(vendorId);
            }

            @Override
            public boolean containsType(final StreamId streamId) {
                return true;
            }
        }, registry, registry);
        testee.open();
        try {
            final String vendorId = UUID.randomUUID().toString();
            final VendorCreatedEvent vendorCreatedEvent = new VendorCreatedEvent(vendorId);
            final AggregateStreamId streamId = new AggregateStreamId("Vendor", "vendorId", vendorId);
            final EventId eventId = new EventId();
            final TypeName dataType = new TypeName(VendorCreatedEvent.TYPE);
            final CommonEvent eventData = new SimpleCommonEvent(eventId, dataType, vendorCreatedEvent);

            // TEST
            execute(testee, streamId, eventData, eventId);

        } finally {
            testee.close();
        }

    }

    @Test
    public void testNoParamsStreams() throws Exception {

        // PREPARE
        final String EVENT_A = "EventA";
        final String EVENT_B = "EventB";

        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        final JsonDeSerializer jsonDeSer = new JsonDeSerializer();

        final SerializedDataType serDataTypeA = new SerializedDataType(EVENT_A);
        registry.add(serDataTypeA, "application/json", jsonDeSer);
        final JsonObject eventA = Json.createObjectBuilder().add("a", "John Doe").build();
        final SimpleStreamId streamA = new SimpleStreamId("StreamA");
        final EventId eventIdA = new EventId("84fe8213-ac1b-4cda-8321-703c2e448052");
        final TypeName typeA = new TypeName(EVENT_A);
        final CommonEvent commonEventA = new SimpleCommonEvent(eventIdA, typeA, eventA);

        final SerializedDataType serDataTypeB = new SerializedDataType(EVENT_B);
        registry.add(serDataTypeB, "application/json", jsonDeSer);
        final JsonObject eventB = Json.createObjectBuilder().add("b", "Jane Doe").build();
        final SimpleStreamId streamB = new SimpleStreamId("StreamB");
        final EventId eventIdB = new EventId("23962a5e-da10-402f-8560-340745b09b2c");
        final TypeName typeB = new TypeName(EVENT_B);
        final CommonEvent commonEventB = new SimpleCommonEvent(eventIdB, typeB, eventB);

        final JpaEventStore testee = new JpaEventStore(getEm(), new JpaIdStreamFactory() {
            @Override
            public JpaStream createStream(final StreamId streamId) {
                return new NoParamsStream(streamId);
            }

            @Override
            public boolean containsType(final StreamId streamId) {
                return true;
            }
        }, registry, registry);
        testee.open();
        try {

            // TEST
            execute(testee, streamA, commonEventA, eventIdA);
            execute(testee, streamB, commonEventB, eventIdB);

        } finally {
            testee.close();
        }

    }

    private static void execute(final EventStore eventStore, final StreamId streamId,
            final CommonEvent commonEvent, final EventId eventId) throws Exception {
        beginTransaction();
        try {

            final int version = eventStore.appendToStream(streamId,
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

}
// CHECKSTYLE:ON
