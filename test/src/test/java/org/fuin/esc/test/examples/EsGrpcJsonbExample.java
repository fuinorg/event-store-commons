// CHECKSTYLE:OFF
package org.fuin.esc.test.examples;

import java.net.MalformedURLException;
import java.nio.charset.Charset;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.eclipse.yasson.FieldAccessStrategy;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.EventStore;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.esgrpc.ESGrpcEventStore;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.spi.EscEvent;
import org.fuin.esc.spi.EscEvents;
import org.fuin.esc.spi.EscMeta;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.esc.api.JsonbDeSerializer;
import org.fuin.esc.api.SerDeserializerRegistry;
import org.fuin.esc.api.SerializedDataTypeRegistry;
import org.fuin.esc.api.SimpleSerializedDataTypeRegistry;
import org.fuin.esc.api.SimpleSerializerDeserializerRegistry;

import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.EventStoreDBClientSettings;
import com.eventstore.dbclient.EventStoreDBConnectionString;

/**
 * Event Store (https://geteventstore.com/) HTTP JSON-B example.
 */
public final class EsGrpcJsonbExample {

	private EsGrpcJsonbExample() {
		super();
	}

	private static SerializedDataTypeRegistry createTypeRegistry() {

		// Contains all types for usage with JSON-B
		SimpleSerializedDataTypeRegistry typeRegistry = new SimpleSerializedDataTypeRegistry();

		// Base types always needed
		typeRegistry.add(EscEvent.SER_TYPE, EscEvent.class);
		typeRegistry.add(EscEvents.SER_TYPE, EscEvents.class);
		typeRegistry.add(EscMeta.SER_TYPE, EscMeta.class);
		// User defined types
		typeRegistry.add(MyMeta.SER_TYPE, MyMeta.class);
		typeRegistry.add(BookAddedEvent.SER_TYPE, BookAddedEvent.class);

		return typeRegistry;
	}

	/**
	 * Creates a registry that connects the type with the appropriate serializer and
	 * de-serializer.
	 * 
	 * @param typeRegistry Type registry (Mapping from type name to class).
	 * @param jsonbDeSer   JSON-B serializer/deserializer to use.
	 * 
	 * @return New registry instance.
	 */
	private static SerDeserializerRegistry createSerDeserializerRegistry(SerializedDataTypeRegistry typeRegistry,
			JsonbDeSerializer jsonbDeSer) {

		SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();

		// Base types always needed
		registry.add(EscEvents.SER_TYPE, "application/json", jsonbDeSer);
		registry.add(EscEvent.SER_TYPE, "application/json", jsonbDeSer);
		registry.add(EscMeta.SER_TYPE, "application/json", jsonbDeSer);

		// User defined types
		registry.add(MyMeta.SER_TYPE, "application/json", jsonbDeSer);
		registry.add(BookAddedEvent.SER_TYPE, "application/json", jsonbDeSer);

		jsonbDeSer.init(typeRegistry, registry, registry);

		return registry;
	}

	private static JsonbDeSerializer createJsonbDeSerializer() {
		return JsonbDeSerializer.builder().withSerializers(EscSpiUtils.createEscJsonbSerializers())
				.withDeserializers(EscSpiUtils.createEscJsonbDeserializers())
				.withPropertyVisibilityStrategy(new FieldAccessStrategy()).withEncoding(Charset.forName("utf-8"))
				.build();
	}

	/**
	 * Main method.
	 * 
	 * @param args Not used.
	 */
	public static void main(final String[] args) throws MalformedURLException {

		// Knows about all types for usage with JSON-B
		SerializedDataTypeRegistry typeRegistry = createTypeRegistry();

		// Does the actual marshalling/unmarshalling
		JsonbDeSerializer jsonbDeSer = createJsonbDeSerializer();

		// Registry connects the type with the appropriate serializer and de-serializer
		SerDeserializerRegistry serDeserRegistry = createSerDeserializerRegistry(typeRegistry, jsonbDeSer);

		// Create an event store instance and open it
		final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("admin", "changeit");
		credentialsProvider.setCredentials(AuthScope.ANY, credentials);

		final EventStoreDBClientSettings setts = EventStoreDBConnectionString
				.parseOrThrow("esdb://localhost:2113?tls=false");
		final EventStoreDBClient client = EventStoreDBClient.create(setts);
		EventStore eventStore = new ESGrpcEventStore.Builder().eventStore(client).serDesRegistry(serDeserRegistry)
				.targetContentType(EnhancedMimeType.create("application", "json", Charset.forName("utf-8"))).build();

		eventStore.open();
		try {

			// Prepare
			StreamId streamId = new SimpleStreamId("books-jsonb-example"); // Unique stream name + NO PROJECTION
			EventId eventId = new EventId("c8af28d4-5544-4624-99ff-7fcf1a0c8cfe"); // Create a unique event ID
			BookAddedEvent event = new BookAddedEvent("Shining", "Stephen King"); // Your event
			CommonEvent commonEvent = new SimpleCommonEvent(eventId, BookAddedEvent.TYPE, event); // Combines user and
																									// general data

			// Append the event to the stream
			eventStore.appendToStream(streamId, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(), commonEvent);

			// Read it from the stream
			CommonEvent readEvent = eventStore.readEvent(streamId, 0);

			// Prints "BookAddedEvent c8af28d4-5544-4624-99ff-7fcf1a0c8cfe"
			System.out.println(readEvent);

		} finally {
			// Don't forget to close
			eventStore.close();
		}

	}

}
// CHECKSTYLE:ON
