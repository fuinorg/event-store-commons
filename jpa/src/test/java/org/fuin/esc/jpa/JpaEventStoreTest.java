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
package org.fuin.esc.jpa;

import static org.fest.assertions.Assertions.assertThat;

import java.sql.SQLException;
import java.util.UUID;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.EventType;
import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.StreamVersionConflictException;
import org.fuin.esc.jpa.examples.AggregateStreamId;
import org.fuin.esc.jpa.examples.VendorCreatedEvent;
import org.fuin.esc.jpa.examples.VendorStream;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.spi.XmlDeSerializer;
import org.fuin.units4j.AbstractPersistenceTest;
import org.junit.Test;

// CHECKSTYLE:OFF
public final class JpaEventStoreTest extends AbstractPersistenceTest {

    @Test
    public void testAppendSingleSuccess() throws SQLException,
            StreamNotFoundException, StreamDeletedException,
            StreamVersionConflictException {

        // PREPARE
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();

        final XmlDeSerializer xmlDeSer = new XmlDeSerializer(
                VendorCreatedEvent.class);
        registry.addSerializer(new SerializedDataType(VendorCreatedEvent.TYPE),
                xmlDeSer);
        registry.addDeserializer(
                new SerializedDataType(VendorCreatedEvent.TYPE), xmlDeSer
                        .getMimeType().getBaseType(), xmlDeSer);

        final SerializedDataType serMetaType = new SerializedDataType(
                "META_DATA");
        registry.addSerializer(serMetaType, xmlDeSer);
        registry.addDeserializer(serMetaType, xmlDeSer.getMimeType()
                .getBaseType(), xmlDeSer);

        final JpaEventStore testee = new JpaEventStore(getEm(),
                new JpaIdStreamFactory() {
                    @Override
                    public JpaStream createStream(final StreamId streamId) {
                        final String vendorId = streamId.getSingleParamValue();
                        return new VendorStream(vendorId);
                    }

                    @Override
                    public boolean containsType(StreamId streamId) {
                        return true;
                    }
                }, registry, registry, serMetaType);
        testee.open();
        try {
            final String vendorId = UUID.randomUUID().toString();
            final VendorCreatedEvent vendorCreatedEvent = new VendorCreatedEvent(
                    vendorId);
            final AggregateStreamId streamId = new AggregateStreamId("Vendor",
                    "vendorId", vendorId);
            final EventId eventId = new EventId();
            final EventType eventType = new EventType("VendorCreatedEvent");
            final Object meta = null;
            final CommonEvent eventData = new CommonEvent(eventId, eventType,
                    vendorCreatedEvent, meta);

            // TEST
            beginTransaction();
            final int version = testee.appendToStream(streamId, 0, eventData);
            commitTransaction();

            // VERIFY
            assertThat(version).isEqualTo(1);
            beginTransaction();
            final StreamEventsSlice slice = testee.readEventsForward(streamId,
                    1, 2);
            commitTransaction();
            assertThat(slice.getFromEventNumber()).isEqualTo(1);
            assertThat(slice.getNextEventNumber()).isEqualTo(2);
            assertThat(slice.isEndOfStream()).isTrue();
            assertThat(slice.getEvents()).hasSize(1);
            final CommonEvent ed = slice.getEvents().get(0);
            assertThat(ed.getId()).isEqualTo(eventId);

        } finally {
            testee.close();
        }

    }

}
// CHECKSTYLE:ON
