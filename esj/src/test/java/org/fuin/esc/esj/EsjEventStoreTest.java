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
package org.fuin.esc.esj;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.json.Json;

import lt.emasina.esj.Settings;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.Credentials;
import org.fuin.esc.api.EventNotFoundException;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.StreamVersionConflictException;
import org.fuin.esc.spi.JsonDeSerializer;
import org.fuin.esc.spi.JsonMetaDataAccessor;
import org.fuin.esc.spi.JsonMetaDataBuilder;
import org.fuin.esc.spi.MetaDataAccessor;
import org.fuin.esc.spi.MetaDataBuilder;
import org.fuin.esc.spi.SimpleSerializerDeserializerRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link EsjEventStore} class.
 */
// CHECKSTYLE:OFF Test
public class EsjEventStoreTest {

    private final static String PREFIX = "EsjEventStoreTest.";

    private final static int VERSION_ANY = -2;
    private final static int VERSION_NO_STREAM = -1;
    private final static int VERSION_EMPTY_STREAM = 0;

    private EsjEventStore testee;

    @Before
    public void setup() throws UnknownHostException {
        final InetAddress host = InetAddress.getByName("127.0.0.1");
        final int port = 1113;
        final Settings settings = new Settings();
        final ExecutorService executor = Executors.newFixedThreadPool(10);
        final String user = "admin";
        final String password = "changeit";
        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        final JsonDeSerializer jsonSerDeser = new JsonDeSerializer();
        registry.setDefaultContentType(EsjEventStore.META_TYPE,
                jsonSerDeser.getMimeType());
        registry.addSerializer(EsjEventStore.META_TYPE, jsonSerDeser);
        registry.addSerializer("MyEvent", jsonSerDeser);
        registry.addDeserializer(EsjEventStore.META_TYPE, jsonSerDeser
                .getMimeType().getBaseType(), jsonSerDeser);
        registry.addDeserializer("MyEvent", jsonSerDeser.getMimeType()
                .getBaseType(), jsonSerDeser);
        final MetaDataBuilder metaDataBuilder = new JsonMetaDataBuilder();
        final MetaDataAccessor metaDataAccessor = new JsonMetaDataAccessor();
        testee = new EsjEventStore(host, port, settings, executor, user,
                password, registry, registry, metaDataBuilder, metaDataAccessor);
        testee.open();
    }

    @After
    public void teardown() {
        testee.close();
        testee = null;
    }

    @Test
    public void testAppendToStreamSuccess() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId(PREFIX
                + "testAppendToStreamSuccess");
        final CommonEvent eventOne = new CommonEvent(randomUUID(), "MyEvent",
                Json.createObjectBuilder().add("name", "Peter")
                        .add("age", "22").build());
        final CommonEvent eventTwo = new CommonEvent(randomUUID(), "MyEvent",
                Json.createObjectBuilder().add("name", "Mary-Jane")
                        .add("age", "21").build());

        // TEST
        final int nextVersion = testee.appendToStream(Credentials.NONE,
                streamId, VERSION_ANY, eventOne, eventTwo);

        // VERIFY
        assertThat(nextVersion).isEqualTo(1);
        final CommonEvent event1 = testee.readEvent(Credentials.NONE, streamId,
                0);
        final CommonEvent event2 = testee.readEvent(Credentials.NONE, streamId,
                1);
        assertThat(event1).isEqualTo(eventOne);
        assertThat(event2).isEqualTo(eventTwo);

    }

    @Test
    public void testAppendToStreamStreamVersionConflictException()
            throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId(PREFIX
                + "testAppendToStreamStreamVersionConflictException");
        final CommonEvent event = new CommonEvent(randomUUID(), "MyEvent", Json
                .createObjectBuilder().add("name", "Peter").build());

        // TEST
        try {
            testee.appendToStream(Credentials.NONE, streamId, 1, event);
            fail("Expected exception");
        } catch (final StreamVersionConflictException ex) {
            // VERIFIED
        }

    }

    @Test
    public void testAppendToStreamStreamDeletedException() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId(PREFIX
                + "testAppendToStreamStreamDeletedException");
        final CommonEvent eventOne = new CommonEvent(randomUUID(), "MyEvent",
                Json.createObjectBuilder().add("name", "Peter")
                        .add("age", "22").build());
        final CommonEvent eventTwo = new CommonEvent(randomUUID(), "MyEvent",
                Json.createObjectBuilder().add("name", "Mary-Jane")
                        .add("age", "21").build());
        testee.appendToStream(Credentials.NONE, streamId, VERSION_ANY, eventOne);
        testee.deleteStream(Credentials.NONE, streamId);

        // TEST
        try {
            testee.appendToStream(Credentials.NONE, streamId,
                    VERSION_NO_STREAM, eventTwo);
            fail("Expected exception");
        } catch (final StreamDeletedException ex) {
            // VERIFIED
        }

    }

    @Test
    public void testAppendToStreamStreamReadOnlyException() {
        fail("Implement!");
    }

    @Test
    public void testDeleteStreamSuccess() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId(PREFIX
                + "testDeleteStreamSuccess");
        final CommonEvent event = new CommonEvent(randomUUID(), "MyEvent", Json
                .createObjectBuilder().add("name", "Peter").build());
        final int nextVersion = testee.appendToStream(Credentials.NONE,
                streamId, VERSION_ANY, event);

        // TEST
        testee.deleteStream(Credentials.NONE, streamId, nextVersion);

        // VERIFY
        try {
            testee.readEventsForward(Credentials.NONE, streamId, nextVersion, 1);
            fail("Expected exception");
        } catch (final StreamDeletedException ex) {
            // VERIFIED
        }

    }

    @Test
    public void testDeleteStreamStreamNotFoundException() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId(PREFIX
                + "testDeleteStreamStreamNotFoundException");

        // TEST
        try {
            testee.deleteStream(Credentials.NONE, streamId, VERSION_ANY);
            fail("Expected exception, but got none");
        } catch (final StreamNotFoundException ex) {
            // VERIFIED
        }

    }

    @Test
    public void testDeleteStreamStreamVersionConflictException()
            throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId(PREFIX
                + "testDeleteStreamStreamVersionConflictException");
        final CommonEvent event = new CommonEvent(randomUUID(), "MyEvent", Json
                .createObjectBuilder().add("name", "Peter").build());
        final int nextVersion = testee.appendToStream(Credentials.NONE,
                streamId, VERSION_ANY, event);

        // TEST
        try {
            testee.deleteStream(Credentials.NONE, streamId, nextVersion + 1);
            fail("Expected exception");
        } catch (final StreamVersionConflictException ex) {
            // VERIFIED
        }

    }

    @Test
    public void testReadStreamEventsForwardSuccess() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId(PREFIX
                + "testReadStreamEventsForwardSuccess");
        final CommonEvent eventOne = new CommonEvent(randomUUID(), "MyEvent",
                Json.createObjectBuilder().add("name", "Peter")
                        .add("age", "22").build());
        final CommonEvent eventTwo = new CommonEvent(randomUUID(), "MyEvent",
                Json.createObjectBuilder().add("name", "Mary-Jane")
                        .add("age", "21").build());
        final CommonEvent eventThree = new CommonEvent(randomUUID(), "MyEvent",
                Json.createObjectBuilder().add("name", "Harry")
                        .add("age", "22").build());

        testee.appendToStream(Credentials.NONE, streamId, VERSION_ANY,
                eventOne, eventTwo, eventThree);

        // TEST
        final StreamEventsSlice slice1 = testee.readEventsForward(
                Credentials.NONE, streamId, 0, 2);

        // VERIFY
        assertThat(slice1.getFromEventNumber()).isEqualTo(0);
        assertThat(slice1.getNextEventNumber()).isEqualTo(2);
        assertThat(slice1.isEndOfStream()).isFalse();
        assertThat(slice1.getEvents()).containsExactly(eventOne, eventTwo);

        // TEST
        final StreamEventsSlice slice2 = testee.readEventsForward(
                Credentials.NONE, streamId, 2, 2);

        // VERIFY
        assertThat(slice2.getFromEventNumber()).isEqualTo(2);
        assertThat(slice2.getNextEventNumber()).isEqualTo(2);
        assertThat(slice2.isEndOfStream()).isTrue();
        assertThat(slice2.getEvents()).containsExactly(eventThree);

    }

    @Test
    public void testReadStreamEventsForwardStreamNotFoundException()
            throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId(PREFIX
                + "testReadStreamEventsForwardStreamNotFoundException");

        // TEST
        try {
            testee.readEventsForward(Credentials.NONE, streamId, 0, 1);
            fail("Expected exception");
        } catch (final StreamNotFoundException ex) {
            // VERIFIED
        }

    }

    @Test
    public void testReadStreamEventsForwardStreamDeletedException()
            throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId(PREFIX
                + "testReadStreamEventsForwardStreamDeletedException");
        final CommonEvent event = new CommonEvent(randomUUID(), "MyEvent", Json
                .createObjectBuilder().add("name", "Peter").build());
        testee.appendToStream(Credentials.NONE, streamId, VERSION_ANY, event);
        testee.deleteStream(Credentials.NONE, streamId);

        // TEST
        try {
            testee.readEventsForward(Credentials.NONE, streamId, 0, 1);
            fail("Expected exception");
        } catch (final StreamDeletedException ex) {
            // VERIFIED
        }

    }

    @Test
    public void testReadEventSuccess() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId(PREFIX
                + "testReadEventSuccess");
        final String id = randomUUID();
        final CommonEvent event = new CommonEvent(id, "MyEvent", Json
                .createObjectBuilder().add("name", "Peter").build());
        final int nextVersion = testee.appendToStream(Credentials.NONE,
                streamId, VERSION_ANY, event);

        // TEST
        final CommonEvent copy = testee.readEvent(Credentials.NONE, streamId,
                nextVersion);

        // VERIFY
        assertThat(copy.getId()).isEqualTo(id);

    }

    @Test
    public void testReadEventEventNotFoundException() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId(PREFIX
                + "testReadEventEventNotFoundException");
        final String id = randomUUID();
        final CommonEvent event = new CommonEvent(id, "MyEvent", Json
                .createObjectBuilder().add("name", "Peter").build());
        final int nextVersion = testee.appendToStream(Credentials.NONE,
                streamId, VERSION_ANY, event);

        // TEST
        try {
            testee.readEvent(Credentials.NONE, streamId, nextVersion + 1);
            fail("Expected exception");
        } catch (final EventNotFoundException ex) {
            // VERIFIED
        }

    }

    @Test
    public void testReadEventStreamNotFoundException() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId(PREFIX
                + "testReadEventStreamNotFoundException");

        // TEST
        try {
            testee.readEvent(Credentials.NONE, streamId, 1);
            fail("Expected exception");
        } catch (final StreamNotFoundException ex) {
            // VERIFIED
        }

    }

    @Test
    public void testReadEventStreamDeletedException() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId(PREFIX
                + "testReadEventStreamDeletedException");
        final String id = randomUUID();
        final CommonEvent event = new CommonEvent(id, "MyEvent", Json
                .createObjectBuilder().add("name", "Peter").build());
        final int nextVersion = testee.appendToStream(Credentials.NONE,
                streamId, VERSION_ANY, event);
        final CommonEvent copy = testee.readEvent(Credentials.NONE, streamId,
                nextVersion);
        assertThat(copy.getId()).isEqualTo(id);
        testee.deleteStream(Credentials.NONE, streamId);

        // TEST
        try {
            testee.readEvent(Credentials.NONE, streamId, nextVersion + 1);
            fail("Expected exception");
        } catch (final StreamDeletedException ex) {
            // VERIFIED
        }

    }

    private String randomUUID() {
        return UUID.randomUUID().toString();
    }

}
// CHECKSTYLE:ON
