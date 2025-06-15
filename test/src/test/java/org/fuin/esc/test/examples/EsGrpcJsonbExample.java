package org.fuin.esc.test.examples;

import io.kurrent.dbclient.KurrentDBClient;
import io.kurrent.dbclient.KurrentDBClientSettings;
import io.kurrent.dbclient.KurrentDBConnectionString;
import jakarta.json.bind.JsonbConfig;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.eclipse.yasson.FieldAccessStrategy;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.DeserializerRegistry;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.EventStore;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.SerDeserializerRegistry;
import org.fuin.esc.api.SerializedDataTypeRegistry;
import org.fuin.esc.api.SerializerRegistry;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleSerializedDataTypeRegistry;
import org.fuin.esc.api.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.esgrpc.ESGrpcEventStore;
import org.fuin.esc.jsonb.BaseTypeFactory;
import org.fuin.esc.jsonb.EscEvent;
import org.fuin.esc.jsonb.EscEvents;
import org.fuin.esc.jsonb.EscJsonbUtils;
import org.fuin.esc.jsonb.EscMeta;
import org.fuin.esc.jsonb.JsonbSerDeserializer;
import org.fuin.objects4j.jsonb.JsonbProvider;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;

import static org.fuin.esc.jsonb.EscJsonbUtils.MIME_TYPE;
import static org.fuin.esc.jsonb.EscJsonbUtils.addEscSerDeserializer;

/**
 * Event Store (https://geteventstore.com/) HTTP JSON-B example.
 */
public final class EsGrpcJsonbExample {

    private EsGrpcJsonbExample() {
        super();
    }

    /**
     * Creates the JSON-B config with some default values.
     *
     * @return Base configuration.
     */
    public static JsonbConfig createJsonbConfig() {
        return new JsonbConfig()
                .withPropertyVisibilityStrategy(new FieldAccessStrategy())
                .withEncoding(StandardCharsets.UTF_8.name());
    }

    /**
     * Creates a pre-configured serializer/deserializer.
     *
     * @param jsonbProvider JSON-B provider.
     * @param typeRegistry  Type registry.
     * @return New instance.
     */
    public static JsonbSerDeserializer createSerDeserializer(
            final JsonbProvider jsonbProvider,
            final SerializedDataTypeRegistry typeRegistry) {

        return new JsonbSerDeserializer(jsonbProvider, typeRegistry, StandardCharsets.UTF_8);
    }

    /**
     * Creates a mapping from type name to type class.
     *
     * @return Type to class mapping.
     */
    public static SerializedDataTypeRegistry createTypeRegistry() {
        return new SimpleSerializedDataTypeRegistry.Builder()
                // Base types always needed
                .add(EscEvent.SER_TYPE, EscEvent.class)
                .add(EscEvents.SER_TYPE, EscEvents.class)
                .add(EscMeta.SER_TYPE, EscMeta.class)
                // User defined types
                .add(MyMeta.SER_TYPE, MyMeta.class)
                .add(BookAddedEvent.SER_TYPE, BookAddedEvent.class)
                .build();
    }

    /**
     * Creates a registry that connects the type with the appropriate serializer and de-serializer.
     *
     * @param serDeserializer   JSON-B serializer/deserializer to use.
     * @return New registry instance.
     */
    public static SerDeserializerRegistry createSerDeserializerRegistry(JsonbSerDeserializer serDeserializer) {
        return addEscSerDeserializer(new SimpleSerializerDeserializerRegistry.Builder(MIME_TYPE), serDeserializer)
                .add(MyMeta.SER_TYPE, serDeserializer, serDeserializer.getMimeType())
                .add(BookAddedEvent.SER_TYPE, serDeserializer, serDeserializer.getMimeType())
                .build();
    }

    /**
     * Adds serializers and deserializer to the JSON-B config.
     *
     * @param jsonbConfig JSON-B configuration.
     * @param serializerRegistry Serializer registry.
     * @param deserializerRegistry Deserializer registry.
     */
    public static void register(JsonbConfig jsonbConfig, SerializerRegistry serializerRegistry, DeserializerRegistry deserializerRegistry) {
        jsonbConfig.withAdapters(EscJsonbUtils.createEscJsonbAdapters());
        jsonbConfig.withDeserializers(EscJsonbUtils.createEscJsonbDeserializers(serializerRegistry, deserializerRegistry));
        jsonbConfig.withSerializers(EscJsonbUtils.createEscJsonbSerializers(serializerRegistry, deserializerRegistry));
    }

    /**
     * Main method.
     *
     * @param args Not used.
     */
    public static void main(final String[] args) throws MalformedURLException {

        // Knows about all types for usage with JSON-B
        SerializedDataTypeRegistry typeRegistry = createTypeRegistry();

        // Configuration for JSON-B
        JsonbConfig jsonbConfig = createJsonbConfig();

        // Helper class to allow late creation of the JSON-B instance
        JsonbProvider jsonbProvider = new JsonbProvider(jsonbConfig);

        // Does the actual marshalling/unmarshalling
        JsonbSerDeserializer serDeserializer = createSerDeserializer(jsonbProvider, typeRegistry);

        // Registry connects the type with the appropriate serializer and de-serializer
        SerDeserializerRegistry serDeserRegistry = createSerDeserializerRegistry(serDeserializer);

        // Add serializers/deserializer to the JSON-B configuration
        register(jsonbConfig, serDeserRegistry, serDeserRegistry);

        // Create an event store instance and open it
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("admin", "changeit");
        credentialsProvider.setCredentials(AuthScope.ANY, credentials);

        final KurrentDBClientSettings setts = KurrentDBConnectionString
                .parseOrThrow("esdb://localhost:2113?tls=false");
        final KurrentDBClient client = KurrentDBClient.create(setts);
        try (final EventStore eventStore = new ESGrpcEventStore.Builder()
                .baseTypeFactory(new BaseTypeFactory())
                .eventStore(client)
                .serDesRegistry(serDeserRegistry)
                .targetContentType(EnhancedMimeType.create("application", "json", StandardCharsets.UTF_8))
                .build().open()) {

            // Prepare
            StreamId streamId = new SimpleStreamId("books-jsonb-example"); // Unique stream name + NO PROJECTION
            EventId eventId = new EventId("c8af28d4-5544-4624-99ff-7fcf1a0c8cfe"); // Create a unique event ID
            BookAddedEvent event = new BookAddedEvent("Shining", "Stephen King"); // Your event
            CommonEvent commonEvent = new SimpleCommonEvent(eventId, BookAddedEvent.TYPE, event, null); // Combines user and
            // general data

            // Append the event to the stream
            eventStore.appendToStream(streamId, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(), commonEvent);

            // Read it from the stream
            CommonEvent readEvent = eventStore.readEvent(streamId, 0);

            // Prints "BookAddedEvent c8af28d4-5544-4624-99ff-7fcf1a0c8cfe"
            System.out.println(readEvent);

        } finally {
            client.shutdown();
        }

    }

}

