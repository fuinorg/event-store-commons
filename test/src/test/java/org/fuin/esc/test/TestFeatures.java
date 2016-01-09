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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.EventStore;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.eshttp.ESEnvelopeType;
import org.fuin.esc.eshttp.ESHttpEventStore;
import org.fuin.esc.jpa.JpaEventStore;
import org.fuin.esc.mem.InMemoryEventStore;
import org.fuin.esc.spi.JsonDeSerializer;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.spi.TextDeSerializer;
import org.fuin.esc.spi.XmlDeSerializer;
import org.fuin.esc.test.examples.BookAddedEvent;
import org.fuin.esc.test.examples.MyMeta;
import org.fuin.esc.test.jpa.TestIdStreamFactory;
import org.fuin.units4j.Units4JUtils;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

// CHECKSTYLE:OFF Test code
public class TestFeatures {

    private EventStore eventStore;

    private SimpleSerializerDeserializerRegistry registry;

    private TestCommand lastCommand;

    private EntityManagerFactory emf;

    private EntityManager em;

    private Connection connection;

    @Before
    public void beforeFeature() throws MalformedURLException {
        // Use the property to select the correct implementation:
        final String type = System.getProperty(EscCucumber.SYSTEM_PROPERTY);
        if (type.equals("mem")) {
            eventStore = new InMemoryEventStore(Executors.newCachedThreadPool());
        } else if (type.equals("eshttp") || type.equals("jpa")) {
            final XmlDeSerializer xmlDeSer = new XmlDeSerializer(false, BookAddedEvent.class);
            final JsonDeSerializer jsonDeSer = new JsonDeSerializer();
            final TextDeSerializer textDeSer = new TextDeSerializer();
            registry = new SimpleSerializerDeserializerRegistry();
            registry.add(new SerializedDataType(BookAddedEvent.TYPE.asBaseType()), "application/xml",
                    xmlDeSer);
            registry.add(new SerializedDataType(MyMeta.TYPE.asBaseType()), "application/json", jsonDeSer);
            registry.add(new SerializedDataType("TextEvent"), "text/plain", textDeSer);
            if (type.equals("eshttp")) {
                final ThreadFactory threadFactory = Executors.defaultThreadFactory();
                final URL url = new URL("http://127.0.0.1:2113/");
                eventStore = new ESHttpEventStore(threadFactory, url, ESEnvelopeType.XML, registry,
                        registry);
            } else {
                setupDb();
                eventStore = new JpaEventStore(em, new TestIdStreamFactory(), registry, registry);
            }
        } else {
            throw new IllegalStateException("Unknown type: " + type);
        }
        eventStore.open();
        lastCommand = null;
    }

    @After
    public void afterFeature() {
        eventStore.close();
        teardownDb();
        eventStore = null;
        if (lastCommand != null) {
            throw new IllegalStateException("Last command was set, but not verified!");
        }
    }

    @Then("^this should give the expected results$")
    public void success() {
        if (lastCommand == null) {
            throw new IllegalStateException("Last command was not set in the 'when' condition");
        }
        lastCommand.verify();
        lastCommand = null;
    }

    @Then("^this should raise no exception$")
    public void thenNoException() {
        // Do nothing, just to create a nice 'then' text
    }

    @When("^the following deletes are executed$")
    public void whenExecuteDeletes(final List<DeleteCommand> commands) {
        final TestCommand command = new MultipleCommands(commands);
        command.init(eventStore);
        execute(command);
        lastCommand = command;
    }

    @Then("^executing the following deletes should have the given result$")
    public void thenExecuteDeletes(final List<DeleteCommand> commands) {
        final TestCommand command = new MultipleCommands(commands);
        command.init(eventStore);
        execute(command);
        command.verify();
    }

    @Given("^the following streams are created and a single event is appended to each$")
    public void givenCreateStreamsAndAppendSomeEvent(final List<String> streams) {

        final MultipleCommands command = new MultipleCommands();

        for (int i = 1; i < streams.size(); i++) {
            final String streamName = streams.get(i);
            command.add(new CreateStreamCommand(streamName));
            final CommonEvent event = new SimpleCommonEvent(new EventId(), BookAddedEvent.TYPE,
                    new BookAddedEvent("Unknown", "John Doe"));
            command.add(new AppendToStreamCommand(streamName, ExpectedVersion.ANY.getNo(), null, event));
        }

        command.init(eventStore);
        execute(command);
        command.verify();

    }

    @Given("^the following streams don't exist$")
    public void givenStreamsDontExist(final List<String> streams) {
        streamsExists(streams, false);
    }

    @Then("^following streams should not exist$")
    public void thenStreamsShouldNotExist(final List<String> streams) {
        streamsExists(streams, false);
    }

    @Then("^reading forward from the following streams should raise the given exceptions$")
    public void thenReadForwardException(final List<ReadForwardExceptionCommand> commands) throws Exception {
        final TestCommand command = new MultipleCommands(commands);
        command.init(eventStore);
        execute(command);
        command.verify();
    }

    @When("^I read forward from the following streams$")
    public void whenReadForwardException(final List<ReadForwardExceptionCommand> commands) {
        final TestCommand command = new MultipleCommands(commands);
        command.init(eventStore);
        execute(command);
        lastCommand = command;
    }

    @When("^I read backward from the following streams$")
    public void whenReadBackwardException(final List<ReadBackwardExceptionCommand> commands) {
        final TestCommand command = new MultipleCommands(commands);
        command.init(eventStore);
        execute(command);
        lastCommand = command;
    }

    @Given("^the stream \"(.*?)\" does not exist$")
    public void givenStreamDoesNotExist(final String streamName) {
        final TestCommand command = new StreamExistsCommand(streamName, false);
        command.init(eventStore);
        execute(command);
        command.verify();
    }

    @When("^I append the following events in the given order$")
    public void whenAppendEvents(final List<AppendToStreamCommand> commands) {
        final MultipleCommands command = new MultipleCommands();
        for (final AppendToStreamCommand cmd : commands) {
            command.add(cmd);
        }
        command.init(eventStore);
        execute(command);
        command.verify();
    }

    @Then("^reading forward from stream should have the following results$")
    public void thenReadForward(final List<ReadForwardCommand> commands) throws Exception {
        final TestCommand command = new MultipleCommands(commands);
        command.init(eventStore);
        execute(command);
        command.verify();
    }

    @Then("^reading backward from stream should have the following results$")
    public void thenReadBackward(final List<ReadBackwardCommand> commands) throws Exception {
        final TestCommand command = new MultipleCommands(commands);
        command.init(eventStore);
        execute(command);
        command.verify();
    }

    @When("^I append the following events to stream \"(.*?)\"$")
    public void whenAppendXmlEvents(final String streamName, final String eventsXml) {
        appendXmlEvents(streamName, ExpectedVersion.ANY.getNo(), eventsXml);
    }

    private void appendXmlEvents(final String streamName, final int version, final String eventsXml) {
        final Events events = Units4JUtils.unmarshal(eventsXml, Events.class);
        final List<CommonEvent> commonEvents = events.asCommonEvents(BookAddedEvent.class);

        final AppendToStreamCommand command = new AppendToStreamCommand(streamName, version, null,
                commonEvents);
        command.init(eventStore);
        execute(command);
        command.verify();
    }

    @Then("^reading event (\\d+) from stream \"(.*?)\" should return the following event$")
    public void thenReadXmlEvent(final int eventNumber, final String streamName, final String expectedEventXml) {

        final ReadEventCommand command = new ReadEventCommand(streamName, eventNumber, expectedEventXml, null);
        command.init(eventStore);
        execute(command);
        command.verify();

    }

    @Then("^reading event (\\d+) from stream \"(.*?)\" should throw a \"(.*?)\"$")
    public void thenReadingEventShouldThrow_a(int eventNumber, String streamName, String expectedException) {
        final ReadEventCommand command = new ReadEventCommand(streamName, eventNumber, null,
                expectedException);
        command.init(eventStore);
        execute(command);
        command.verify();
    }

    @When("^the following state queries are executed$")
    public void whenStateQueriesAreExecuted(final List<StreamStateCommand> commands) {
        final TestCommand command = new MultipleCommands(commands);
        command.init(eventStore);
        execute(command);
        command.verify();
    }

    @Given("^the following streams exist$")
    public void givenStreamsExist(final List<String> streams) {
        streamsExists(streams, true);
    }

    @Then("^following streams should exist$")
    public void thenStreamsShouldExist(final List<String> streams) {
        streamsExists(streams, true);
    }

    private void streamsExists(final List<String> streams, final boolean exist) {
        final MultipleCommands command = new MultipleCommands();
        for (int i = 1; i < streams.size(); i++) {
            final String streamName = streams.get(i);
            command.add(new StreamExistsCommand(streamName, exist));
        }
        command.init(eventStore);
        execute(command);
        command.verify();
    }

    private void setupDb() {
        try {
            emf = Persistence.createEntityManagerFactory("testPU");
            em = emf.createEntityManager();
            final Map<String, Object> props = emf.getProperties();
            final boolean shutdown = Boolean.valueOf("" + props.get("esctest.shutdown"));
            if (shutdown) {
                final String connUrl = "" + props.get("esctest.url");
                final String connUsername = "" + props.get("esctest.user");
                final String connPassword = "" + props.get("esctest.pw");
                connection = DriverManager.getConnection(connUrl, connUsername, connPassword);
            }
        } catch (final SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void teardownDb() {
        if (em != null) {
            em.close();
        }
        if (emf != null) {
            emf.close();
        }
        try {
            if (connection != null) {
                connection.createStatement().execute("SHUTDOWN");
            }
        } catch (final SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void execute(TestCommand command) {
        beginTransaction();
        command.execute();
        endTransaction();
    }
    
    private void beginTransaction() {
        if (em != null) {
            em.getTransaction().begin();
        }
    }

    private void endTransaction() {
        if (em != null) {
            final EntityTransaction transaction = em.getTransaction();
            if (transaction.isActive()) {
                if (transaction.getRollbackOnly()) {
                    transaction.rollback();
                } else {
                    transaction.commit();
                }
            }
        }
    }

}
// CHECKSTYLE:ON
