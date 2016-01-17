// CHECKSTYLE:OFF
package org.fuin.esc.test.examples;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.EventStore;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.eshttp.ESEnvelopeType;
import org.fuin.esc.eshttp.ESHttpEventStore;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.spi.XmlDeSerializer;

/**
 * Event Store (https://geteventstore.com/) HTTP XML example.
 */
public final class EsHttpXmlExample {

    private EsHttpXmlExample() {
        super();
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
        
        // Uniquely identifies the type of serialized data 
        SerializedDataType serMetaType = new SerializedDataType("MyMeta");
        SerializedDataType serDataType = new SerializedDataType("BookAddedEvent");
        
        // Handles XML serialization and de-serialization
        XmlDeSerializer xmlDeSer = new XmlDeSerializer(false, MyMeta.class, BookAddedEvent.class);
        
        // Registry connects the type with the appropriate serializer and de-serializer
        SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
        registry.add(serDataType, "application/xml", xmlDeSer);
        registry.add(serMetaType, "application/xml", xmlDeSer);

        // Create an event store instance and open it
        EventStore eventStore = new ESHttpEventStore(threadFactory, url, 
                ESEnvelopeType.XML, // This format will be used to communicate with the event store
                registry, // Registry used to find a serializer 
                registry  // Registry used to find a de-serializer
                );
        eventStore.open();
        try {

            // Prepare
            StreamId streamId = new SimpleStreamId("books"); // Unique stream name + NO PROJECTION
            EventId eventId = new EventId("b3074933-c3ac-44c1-8854-04a21d560999"); // Create a unique event ID
            BookAddedEvent event = new BookAddedEvent("Shining", "Stephen King"); // Your event
            CommonEvent commonEvent = new SimpleCommonEvent(eventId, BookAddedEvent.TYPE, event); // Combines user and general data
            
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
//CHECKSTYLE:ON
