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
package org.fuin.esc.test.examples;

import java.util.concurrent.Executors;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.EventStoreSync;
import org.fuin.esc.api.EventType;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.mem.InMemoryEventStoreSync;

/**
 * In memory example.
 */
// CHECKSTYLE:OFF Shorter example code
public final class InMemoryExample {

    private InMemoryExample() {
        super();
    }

    /**
     * Main method.
     * 
     * @param args
     *            Not used.
     */
    public static void main(final String[] args) {

        // Create an event store instance and open it
        // The in-memory implementation requires no special setup except thread pool for subscriptions
        EventStoreSync eventStore = new InMemoryEventStoreSync(Executors.newCachedThreadPool());
        eventStore.open();
        try {

            // Prepare
            StreamId streamId = new SimpleStreamId("books"); // Give stream a unique name
            EventId eventId = new EventId(); // Create a unique event UUID
            EventType eventType = new EventType("BookAddedEvent");// Define unique event type (name of the event)
            BookAddedEvent event = new BookAddedEvent("Shining", "Stephen King"); // Your event
            CommonEvent commonEvent = new SimpleCommonEvent(eventId, eventType, event); // Combines user and general data
            
            // Append the event to the stream
            eventStore.appendToStream(streamId, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(), commonEvent);

            // Read it from the stream
            CommonEvent readEvent = eventStore.readEvent(streamId, 0);

            System.out.println(readEvent);

        } finally {
            // Don't forget to close
            eventStore.close();
        }

    }

}
//CHECKSTYLE:ON
