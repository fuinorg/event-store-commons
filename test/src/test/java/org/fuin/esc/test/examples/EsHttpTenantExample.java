// CHECKSTYLE:OFF
package org.fuin.esc.test.examples;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.EventNotFoundException;
import org.fuin.esc.api.EventStore;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.SimpleTenantId;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.TenantId;
import org.fuin.esc.eshttp.ESEnvelopeType;
import org.fuin.esc.eshttp.ESHttpEventStore;
import org.fuin.esc.spi.EscEvents;
import org.fuin.esc.spi.EscMeta;
import org.fuin.esc.spi.JsonDeSerializer;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.SimpleSerializerDeserializerRegistry;

/**
 * Event Store (https://geteventstore.com/) HTTP example with tenants.
 */
public final class EsHttpTenantExample {

    private EsHttpTenantExample() {
        super();
    }

    private static CommonEvent createBookEvent(String bookName, String author) {
        // Create a unique event ID
        EventId eventId = new EventId();
        // Define unique event type (name of the event)
        TypeName dataType = new TypeName("BookAddedEvent");
        // Define unique meta type (name of the meta data)
        TypeName metaType = new TypeName("MyMeta");
        // Event data
        JsonObject event = Json.createObjectBuilder().add("name", bookName).add("author", author).build();
        // Meta data
        JsonObject meta = Json.createObjectBuilder().add("user", "michael").build();
        // Combines user and general data
        return new SimpleCommonEvent(eventId, dataType, event, metaType, meta);
    }

    /**
     * Main method.
     * 
     * @param args
     *            Not used.
     */
    public static void main(final String[] args) throws MalformedURLException {

        // Setup for localhost
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        URL url = new URL("http://127.0.0.1:2113/"); // Default event store port
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("admin", "changeit");
        credentialsProvider.setCredentials(AuthScope.ANY, credentials);

        // Uniquely identifies the type of serialized data
        SerializedDataType serMetaType = new SerializedDataType("MyMeta");
        SerializedDataType serDataType = new SerializedDataType("BookAddedEvent");

        // Handles JSON serialization and de-serialization
        JsonDeSerializer jsonDeSer = new JsonDeSerializer();

        // Registry connects the type with the appropriate serializer and de-serializer
        SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        // Base types always needed
        registry.add(EscEvents.SER_TYPE, "application/json", jsonDeSer);
        registry.add(EscMeta.SER_TYPE, "application/json", jsonDeSer);
        // User defined types
        registry.add(serDataType, "application/json", jsonDeSer);
        registry.add(serMetaType, "application/json", jsonDeSer);

        // Define two tenants
        TenantId tenantA = new SimpleTenantId("tenant-a");
        TenantId tenantB = new SimpleTenantId("tenant-b");

        // Create two event store instances (one per tenant) and open them
        try (
                // @formatter:off
                EventStore esTenantA = new ESHttpEventStore.Builder().threadFactory(threadFactory).url(url)
                    .envelopeType(ESEnvelopeType.JSON) // This format will be used to communicate with the event store
                    .serDesRegistry(registry) // Registry used to find a serializer and a deserialize
                    .credentialsProvider(credentialsProvider).tenantId(tenantA).build().open();

                EventStore esTenantB = new ESHttpEventStore.Builder().threadFactory(threadFactory).url(url)
                    .envelopeType(ESEnvelopeType.JSON) // This format will be used to communicate with the event store
                    .serDesRegistry(registry) // Registry used to find a serializer and a deserialize
                    .credentialsProvider(credentialsProvider).tenantId(tenantB).build().open()
                // @formatter:on
        ) {

            // Prepare
            
            // Unique stream name + NO PROJECTION
            StreamId streamId = new SimpleStreamId("books-json-example");
            
            // Append the event to the stream of tenant A
            CommonEvent commonEventA = createBookEvent("Shining", "Stephen King");
            esTenantA.appendToStream(streamId, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(), commonEventA);

            // Read it from the stream of tenant A (OK)
            CommonEvent readEventA = esTenantA.readEvent(streamId, 0);

            // Try to read it from the stream of tenant B (Fails)
            try {
                esTenantB.readEvent(streamId, 0);
                throw new IllegalStateException("Should not exist!");
            } catch (EventNotFoundException ex) {
                System.out.println("OK, event does not exist on stream of tenant B");
            }

            // Prints "BookAddedEvent <UUID>"
            System.out.println(readEventA);

            // Append the event to the stream of tenant B
            CommonEvent commonEventB = createBookEvent("Carrie", "Stephen King");
            esTenantB.appendToStream(streamId, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(), commonEventB);

            // Read it from the stream of tenant B (OK)
            CommonEvent readEventB = esTenantB.readEvent(streamId, 0);

            // Try to read it from the stream of tenant A (Fails)
            try {
                esTenantA.readEvent(streamId, 1);
                throw new IllegalStateException("Should not exist!");
            } catch (EventNotFoundException ex) {
                System.out.println("OK, event does not exist on stream of tenant A");
            }

            // Prints "BookAddedEvent <UUID>"
            System.out.println(readEventB);
            
        }

    }

}
// CHECKSTYLE:ON
