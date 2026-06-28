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
package org.fuin.esc.esgrpc.example;

import io.kurrent.dbclient.KurrentDBClient;
import io.kurrent.dbclient.KurrentDBConnectionString;
import jakarta.json.bind.JsonbConfig;
import org.eclipse.yasson.FieldAccessStrategy;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.SerDeserializerRegistry;
import org.fuin.esc.api.SerializedDataTypeRegistry;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleSerializedDataTypeRegistry;
import org.fuin.esc.api.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.esgrpc.ESGrpcEventStoreAsync;
import org.fuin.esc.esgrpc.MyEvent;
import org.fuin.esc.jaxb.BaseTypeFactory;
import org.fuin.esc.jaxb.EscJaxbUtils;
import org.fuin.esc.jaxb.XmlDeSerializer;
import org.fuin.esc.jsonb.EscJsonbUtils;
import org.fuin.esc.jsonb.JsonbSerDeserializer;
import org.fuin.objects4j.jsonb.JsonbProvider;
import org.fuin.utils4j.TestOmitted;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Example showing how to use the asynchronous gRPC event store with virtual threads. This is a manual demo:
 * it requires a running KurrentDB / EventStoreDB on {@code localhost:2113} (just like the {@code App} example).
 */
@TestOmitted("Example class")
@SuppressWarnings("java:S106") // System.out is fine for an example
public final class AsyncVirtualThreadsExample {

    private AsyncVirtualThreadsExample() {
    }

    /**
     * Runs the example.
     *
     * @param args Not used.
     * @throws Exception Should not happen.
     */
    public static void main(final String[] args) throws Exception {

        System.out.println("BEGIN");

        final EnhancedMimeType xmlUtf8 = EnhancedMimeType.create("application", "xml", StandardCharsets.UTF_8);

        // Set up serialization (events are stored as an XML envelope around the JSON payload)
        final JsonbConfig jsonbConfig = new JsonbConfig()
                .withPropertyVisibilityStrategy(new FieldAccessStrategy())
                .withEncoding(StandardCharsets.UTF_8.name());
        final JsonbProvider jsonbProvider = new JsonbProvider(jsonbConfig);
        final SerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry.Builder()
                .add(MyEvent.SER_TYPE, MyEvent.class)
                .build();
        final JsonbSerDeserializer jsonbSerDeser = new JsonbSerDeserializer(jsonbProvider, typeRegistry, StandardCharsets.UTF_8);
        final XmlDeSerializer xmlSerDeser = EscJaxbUtils.xmlDeSerializerBuilder().build();
        final SerDeserializerRegistry registry = EscJaxbUtils
                .addEscSerDeserializer(new SimpleSerializerDeserializerRegistry.Builder(xmlUtf8), xmlSerDeser)
                .add(MyEvent.SER_TYPE, jsonbSerDeser, jsonbSerDeser.getMimeType())
                .build();
        jsonbConfig.withAdapters(EscJsonbUtils.createEscJsonbAdapters());
        jsonbConfig.withDeserializers(EscJsonbUtils.createEscJsonbDeserializers(registry, registry));
        jsonbConfig.withSerializers(EscJsonbUtils.createEscJsonbSerializers(registry, registry));

        // Async gRPC event store (requires a running KurrentDB / EventStoreDB on localhost:2113)
        final KurrentDBClient client = KurrentDBClient.create(
                KurrentDBConnectionString.parseOrThrow("esdb://localhost:2113?tls=false"));
        final ESGrpcEventStoreAsync es = new ESGrpcEventStoreAsync.Builder()
                .eventStore(client)
                .serDesRegistry(registry)
                .baseTypeFactory(new BaseTypeFactory())
                .targetContentType(xmlUtf8)
                .build();
        es.open().get();

        // The gRPC client completes its futures on its own I/O threads - route continuations and any
        // blocking onto virtual threads so the client threads stay free.
        try (var vexec = Executors.newVirtualThreadPerTaskExecutor()) {

            final StreamId streamId = new SimpleStreamId("vt-demo-" + UUID.randomUUID());
            final CommonEvent event = new SimpleCommonEvent(
                    new EventId(), MyEvent.TYPE, new MyEvent("Hello from a virtual thread"), null);

            // 1) Chain append -> read with the continuation running on a virtual thread
            final StreamEventsSlice slice = es
                    .appendToStream(streamId, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(), event)
                    .thenComposeAsync(v -> es.readEventsForward(streamId, 0, 10), vexec)
                    .get();
            System.out.println("read " + slice.getEvents().size() + " event(s)");

            // 2) Fan out concurrent reads, each blocking on its own virtual thread
            final List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                futures.add(vexec.submit(() -> es.streamExists(streamId).get()));
            }
            long existing = 0;
            for (final Future<Boolean> future : futures) {
                if (Boolean.TRUE.equals(future.get())) {
                    existing++;
                }
            }
            System.out.println("streamExists true for " + existing + "/" + futures.size() + " concurrent checks");

            es.deleteStream(streamId, true).get();
        } finally {
            es.close();
            client.shutdown();
        }

        System.out.println("END");
    }

}
