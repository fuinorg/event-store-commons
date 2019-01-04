// CHECKSTYLE:OFF
package org.fuin.esc.test.examples;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.json.bind.JsonbConfig;

import org.eclipse.yasson.FieldAccessStrategy;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.EventStore;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.eshttp.ESEnvelopeType;
import org.fuin.esc.eshttp.ESHttpEventStore;
import org.fuin.esc.spi.EscEvent;
import org.fuin.esc.spi.EscEvents;
import org.fuin.esc.spi.EscMeta;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.esc.spi.JsonbDeSerializer;
import org.fuin.esc.spi.SerDeserializerRegistry;
import org.fuin.esc.spi.SerializedDataTypeRegistry;
import org.fuin.esc.spi.SimpleSerializedDataTypeRegistry;
import org.fuin.esc.spi.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.spi.XmlDeSerializer;

/**
 * Event Store (https://geteventstore.com/) HTTP JSON example.
 */
public final class EsHttpMixedExample {

    private EsHttpMixedExample() {
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
     * Creates a registry that connects the type with the appropriate serializer and de-serializer.
     * 
     * @param jsonbDeSer JSON-B serializer/deserializer to use.
     * @param xmlDeSer JAX-B serializer/deserializer to use.
     * 
     * @return New registry instance.
     */
    private static SerDeserializerRegistry createSerDeserializerRegistry(JsonbDeSerializer jsonbDeSer, XmlDeSerializer xmlDeSer) {
        
        SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();

        // Base types always needed (JSON-B)
        registry.add(EscEvents.SER_TYPE, "application/json", jsonbDeSer);
        registry.add(EscEvent.SER_TYPE, "application/json", jsonbDeSer);
        registry.add(EscMeta.SER_TYPE, "application/json", jsonbDeSer);

        // User defined types (JAX-B)
        registry.add(MyMeta.SER_TYPE, "application/xml", xmlDeSer);
        registry.add(BookAddedEvent.SER_TYPE, "application/xml", xmlDeSer);
        
        return registry;
    }
    
    private static JsonbDeSerializer createJsonbDeSerializer(SerializedDataTypeRegistry typeRegistry) {
        JsonbConfig config = new JsonbConfig()
                .withSerializers(EscSpiUtils.createEscJsonbSerializers(typeRegistry))
                .withDeserializers(EscSpiUtils.createEscJsonbDeserializers(typeRegistry))
                .withPropertyVisibilityStrategy(new FieldAccessStrategy());
        return new JsonbDeSerializer(config, Charset.forName("utf-8"), typeRegistry);
    }
    
    /**
     * Main method.
     * 
     * @param args
     *            Not used.
     */
    public static void main(final String[] args) throws MalformedURLException {

        // Setup for
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        URL url = new URL("http://127.0.0.1:2113/"); // Default event store port

        // Knows about all types for usage with JSON-B
        SerializedDataTypeRegistry typeRegistry = createTypeRegistry();

        // Handles XML serialization and de-serialization (JSON-B)
        XmlDeSerializer jaxbDeSer = new XmlDeSerializer(false, MyMeta.class, BookAddedEvent.class);

        // Handles JSON serialization and de-serialization (JAX-B)
        JsonbDeSerializer jsonbDeSer = createJsonbDeSerializer(typeRegistry);
        
        // Registry connects the type with the appropriate serializer and de-serializer
        SerDeserializerRegistry serDeserRegistry = createSerDeserializerRegistry(jsonbDeSer, jaxbDeSer);
        
        
        // Create an event store instance and open it
        EventStore eventStore = new ESHttpEventStore(threadFactory, url, 
                ESEnvelopeType.JSON, // This format will be used to communicate with the event store
                serDeserRegistry, // Registry used to find a serializer
                serDeserRegistry // Registry used to find a de-serializer
        );
        eventStore.open();
        try {

            // Prepare
            StreamId streamId = new SimpleStreamId("books-mixed-example"); // Unique stream name + NO PROJECTION
            EventId eventId = new EventId("b3074933-c3ac-44c1-8854-04a21d560999"); // Create a unique event ID

            BookAddedEvent event = new BookAddedEvent("Shining", "Stephen King"); // Your event
            MyMeta meta = new MyMeta("michael"); // Your meta information

            CommonEvent commonEvent = new SimpleCommonEvent(eventId, BookAddedEvent.TYPE, event, MyMeta.TYPE, meta);

            // Append the event to the stream
            eventStore.appendToStream(streamId, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(), commonEvent);

            // Read it from the stream
            CommonEvent readEvent = eventStore.readEvent(streamId, 0);

            // Prints "BookAddedEvent b3074933-c3ac-44c1-8854-04a21d560999"
            System.out.println(readEvent);

        } finally {
            // Don't forget to close
            eventStore.close();
        }

    }

}
// CHECKSTYLE:ON
