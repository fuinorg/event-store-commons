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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.EventStoreSync;
import org.fuin.esc.api.EventType;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.eshttp.ESEnvelopeType;
import org.fuin.esc.eshttp.ESHttpEventStoreSync;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.spi.XmlDeSerializer;

/**
 * Event Store (https://geteventstore.com/) HTTP example.
 */
// CHECKSTYLE:OFF Shorter example code
public final class EsHttpExample {

    private EsHttpExample() {
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
        EventStoreSync eventStore = new ESHttpEventStoreSync(threadFactory, url, 
                serMetaType, // Unique type name for the meta data
                ESEnvelopeType.XML, // This format will be used to communicate with the event store
                registry, // Registry used to find a serializer 
                registry  // Registry used to find a de-serializer
                );
        eventStore.open();
        try {

            // Prepare
            StreamId streamId = new SimpleStreamId("books", false); // Unique stream name + NO PROJECTION
            EventId eventId = new EventId("b3074933-c3ac-44c1-8854-04a21d560999"); // Create a unique event ID
            EventType eventType = new EventType("BookAddedEvent");// Define unique event type (name of the event)
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
