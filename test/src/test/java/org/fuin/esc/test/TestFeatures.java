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
package org.fuin.esc.test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.EventStoreSync;
import org.fuin.esc.api.EventType;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.eshttp.ESEnvelopeType;
import org.fuin.esc.eshttp.ESHttpEventStoreSync;
import org.fuin.esc.mem.InMemoryEventStoreSync;
import org.fuin.esc.spi.JsonDeSerializer;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.spi.XmlDeSerializer;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

// CHECKSTYLE:OFF Test code
public class TestFeatures {

    private EventStoreSync eventStore;

    @Before
    public void beforeFeature() throws MalformedURLException {
        // Use the property to select the correct implementation:
        final String type = System.getProperty(EscCucumber.SYSTEM_PROPERTY);
        if (type.equals("mem")) {
            eventStore = new InMemoryEventStoreSync(Executors.newCachedThreadPool());
        } else if (type.equals("eshttp")) {
            final ThreadFactory threadFactory = Executors.defaultThreadFactory();
            final URL url = new URL("http://127.0.0.1:2113/");
            final SerializedDataType serMetaType = new SerializedDataType("MyMeta");
            final XmlDeSerializer xmlDeSer = new XmlDeSerializer(false, BookAddedEvent.class);
            final JsonDeSerializer jsonDeSer = new JsonDeSerializer();
            final SimpleSerializerDeserializerRegistry registry = new SimpleSerializerDeserializerRegistry();
            registry.add(new SerializedDataType(BookAddedEvent.TYPE), "application/xml", xmlDeSer);
            registry.add(new SerializedDataType("MyMeta"), "application/json", jsonDeSer);
            eventStore = new ESHttpEventStoreSync(threadFactory, url, serMetaType, ESEnvelopeType.XML,
                    registry, registry);
        } else {
            throw new IllegalStateException("Unknown type: " + type);
        }
        eventStore.open();
    }

    @After
    public void afterFeature() {
        eventStore.close();
        eventStore = null;
    }

    @Then("^this should give the expected results$")
    public void success() {
        // Do nothing - Just to please the "then" phrase
    }

    @When("^the following deletes are executed$")
    public void executeDeletes(final List<DeleteCommand> commands) {
        final TestCommand command = new MultipleCommands(commands);
        command.init(eventStore);
        command.execute();
        command.verify();
    }

    @Given("^the following streams are created and a single event is appended to each$")
    public void createStreamsAndAppendSomeEvent(final List<String> streams) {

        final MultipleCommands command = new MultipleCommands();

        for (int i = 1; i < streams.size(); i++) {
            final String streamName = streams.get(i);
            command.add(new CreateStreamCommand(streamName));
            final CommonEvent event = new CommonEvent(new EventId(), new EventType(BookAddedEvent.TYPE),
                    new BookAddedEvent("Unknown", "John Doe"));
            command.add(new AppendToStreamCommand(streamName, ExpectedVersion.ANY.getNo(), null, event));
        }

        command.init(eventStore);
        command.execute();
        command.verify();

    }

    @Given("^the following streams don't exist$")
    public void givenStreamsDontExist(final List<String> streams) {
        streamsShouldNotExist(streams);
    }

    @Then("^following streams should not exist$")
    public void thenStreamsShouldNotExist(final List<String> streams) {
        streamsShouldNotExist(streams);
    }

    private void streamsShouldNotExist(final List<String> streams) {
        final MultipleCommands command = new MultipleCommands();
        for (int i = 1; i < streams.size(); i++) {
            final String streamName = streams.get(i);
            command.add(new StreamExistsCommand(streamName, false));
        }
        command.init(eventStore);
        command.execute();
        command.verify();
    }

    @Then("^reading forward from the following streams should have the given result$")
    public void thenReadForward(final List<ReadForwardCommand> commands) throws Exception {
        final TestCommand command = new MultipleCommands(commands);
        command.init(eventStore);
        command.execute();
        command.verify();
    }

    @When("^I read forward from the following streams$")
    public void whenReadForward(final List<ReadForwardCommand> commands) {
        final TestCommand command = new MultipleCommands(commands);
        command.init(eventStore);
        command.execute();
        command.verify();
    }    
    
}
// CHECKSTYLE:ON
