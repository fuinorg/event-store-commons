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
package org.fuin.esc.test.performance;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

import javax.json.Json;
import javax.json.JsonObject;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.EventStoreSync;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.eshttp.ESEnvelopeType;
import org.fuin.esc.eshttp.ESHttpEventStoreSync;
import org.fuin.esc.spi.JsonDeSerializer;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.SimpleSerializerDeserializerRegistry;

/**
 * Performance test for the ESC HTTP Event Store (https://geteventstore.com/) implementation.
 */
public final class EsHttpPerformance {

    private EsHttpPerformance() {
        super();
    }

    /**
     * Main method.
     * 
     * @param args
     *            Not used.
     */
    public static void main(final String[] args) throws IOException {

        System.out.println("-------------------------");
        System.out.println("Press <enter> to start");
        System.in.read();
        
        final int max = 10000;
        
        final ThreadFactory threadFactory = Executors.defaultThreadFactory();
        final URL url = new URL("http://127.0.0.1:2113/");

        final SerializedDataType serMetaType = new SerializedDataType("MyMeta");
        final SerializedDataType serDataType = new SerializedDataType("BookAddedEvent");

        final JsonDeSerializer jsonDeSer = new JsonDeSerializer();

        final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        registry.add(serDataType, "application/json", jsonDeSer);
        registry.add(serMetaType, "application/json", jsonDeSer);

        final EventStoreSync eventStore = new ESHttpEventStoreSync(threadFactory, url, ESEnvelopeType.JSON,
                registry, registry);
        eventStore.open();
        try {

            final TypeName dataType = new TypeName("BookAddedEvent");
            final TypeName metaType = new TypeName("MyMeta");
            final JsonObject event = Json.createObjectBuilder().add("name", "Shining")
                    .add("author", "Stephen King").build();
            final JsonObject meta = Json.createObjectBuilder().add("user", "michael").build();

            System.out.println("-------------------------");
            System.out.println("Press <enter> to test 1");
            System.in.read();
            
            // Append all together
            final StreamId streamId1 = new SimpleStreamId("books1", false);
            final List<CommonEvent> togetherList = new ArrayList<>();
            for (int i = 0; i < max; i++) {
                togetherList.add(new SimpleCommonEvent(new EventId(), dataType, event, metaType, meta));
            }
            measure("One append (" + togetherList.size() + " events)", togetherList.size(),
                    none -> eventStore.appendToStream(streamId1, ExpectedVersion.ANY.getNo(), togetherList));

            System.out.println("-------------------------");
            System.out.println("Press <enter> to test 2");
            System.in.read();
            
            // Append all separate
            final StreamId streamId2 = new SimpleStreamId("books2", false);
            final List<CommonEvent> separateList = new ArrayList<>();
            for (int i = 0; i < max; i++) {
                separateList.add(new SimpleCommonEvent(new EventId(), dataType, event, metaType, meta));
            }
            measure("Multiple appends (" + separateList.size() + " events)", separateList.size(), none -> {
                for (final CommonEvent ce : separateList) {
                    eventStore.appendToStream(streamId2, ExpectedVersion.ANY.getNo(), ce);
                }
            });

        } finally {
            eventStore.close();
        }

        System.out.println("-------------------------");
        System.out.println("Press <enter> to end");
        System.in.read();

    }

    private static void measure(final String name, final int count, final Consumer<Void> func) {
        final long start = System.currentTimeMillis();
        System.out.println("START          " + name + ": " + start);
        func.accept(null);
        final long end = System.currentTimeMillis();
        System.out.println("END            " + name + ": " + end);
        final double millis = end - start;
        System.out.println("DIFF           " + name + ": " + (long) millis);
        final double seconds = millis / 1000.0;
        System.out.println("SEC            " + name + ": " + (long) seconds);
        final double perSec = (count / millis) * 1000.0;
        System.out.println("EVENTS PER SEC " + name + ": " + (long) perSec);
    }

}
