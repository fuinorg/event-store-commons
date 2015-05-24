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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.json.Json;

import lt.emasina.esj.Settings;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
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
        registry.setDefaultContentType(EsjEventStore.META_TYPE, jsonSerDeser.getMimeType());
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
    public void testAppendToStreamArray() throws Exception {

        // PREPARE
        final StreamId streamId = new SimpleStreamId("esc-esj-stream-1");
        final CommonEvent eventOne = new CommonEvent(
                "339aa3a7-d79d-4bc7-878c-f71a41a2d6da", "MyEvent", Json
                        .createObjectBuilder().add("name", "Peter")
                        .add("age", "22").build());
        final CommonEvent eventTwo = new CommonEvent(
                "3e998693-dbdc-437e-accd-d1af67427336", "MyEvent", Json
                        .createObjectBuilder().add("name", "Mary-Jane")
                        .add("age", "21").build());

        // TEST
        final int nextVersion = testee.appendToStream(streamId, VERSION_ANY,
                eventOne, eventTwo);

        // VERIFY
        assertThat(nextVersion).isEqualTo(1);
        final CommonEvent event1 = testee.readEvent(streamId, 0);
        final CommonEvent event2 = testee.readEvent(streamId, 1);
        assertThat(event1).isEqualTo(eventOne);
        assertThat(event2).isEqualTo(eventTwo);

    }

}
// CHECKSTYLE:ON
