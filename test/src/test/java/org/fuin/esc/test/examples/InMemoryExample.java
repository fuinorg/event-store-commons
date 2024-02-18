package org.fuin.esc.test.examples;

import org.fuin.esc.api.*;
import org.fuin.esc.mem.InMemoryEventStore;

import java.util.concurrent.Executors;

/**
 * In memory example.
 */
public final class InMemoryExample {

    private InMemoryExample() {
        super();
    }

    /**
     * Main method.
     *
     * @param args Not used.
     */
    public static void main(final String[] args) {

        // Create an event store instance and open it
        // The in-memory implementation requires no special setup except thread pool for subscriptions
        EventStore eventStore = new InMemoryEventStore(Executors.newCachedThreadPool());
        eventStore.open();
        try {

            // Prepare
            StreamId streamId = new SimpleStreamId("books"); // Unique stream name + NO PROJECTION
            EventId eventId = new EventId("b3074933-c3ac-44c1-8854-04a21d560999"); // Create a unique event ID
            TypeName eventType = new TypeName("BookAddedEvent");// Define unique event type (name of the event)
            BookAddedEvent event = new BookAddedEvent("Shining", "Stephen King"); // Your event
            CommonEvent commonEvent = new SimpleCommonEvent(eventId, eventType, event); // Combines user and general data

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
