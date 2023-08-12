/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. 
 * http://www.fuin.org/
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
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package org.fuin.esc.test;

import static org.fuin.utils4j.jaxb.JaxbUtils.unmarshal;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.EventStore;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.eshttp.ESEnvelopeType;
import org.fuin.esc.eshttp.ESHttpEventStore;
import org.fuin.esc.esjc.ESJCEventStore;
import org.fuin.esc.jpa.JpaEventStore;
import org.fuin.esc.mem.InMemoryEventStore;
import org.fuin.esc.spi.Base64Data;
import org.fuin.esc.spi.EnhancedMimeType;
import org.fuin.esc.spi.EscEvent;
import org.fuin.esc.spi.EscEvents;
import org.fuin.esc.spi.EscMeta;
import org.fuin.esc.spi.Events;
import org.fuin.esc.spi.JsonDeSerializer;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.spi.TextDeSerializer;
import org.fuin.esc.spi.XmlDeSerializer;
import org.fuin.esc.test.examples.BookAddedEvent;
import org.fuin.esc.test.examples.MyMeta;
import org.fuin.esc.test.jpa.TestIdStreamFactory;
import org.fuin.units4j.MultipleCommands;
import org.fuin.units4j.TestCommand;

import com.github.msemys.esjc.EventStoreBuilder;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

// CHECKSTYLE:OFF Test code
public class TestFeatures {

    private TestContext testContext;

    private SimpleSerializerDeserializerRegistry registry;

    private TestCommand<TestContext> lastCommand;

    private EntityManagerFactory emf;

    private EntityManager em;

    private Connection connection;

    @Before
    public void beforeFeature() throws MalformedURLException {

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        final UsernamePasswordCredentials credentials = new UsernamePasswordCredentials("admin", "changeit");
        credentialsProvider.setCredentials(AuthScope.ANY, credentials);

        // Use the property to select the correct implementation:
        final String currentEventStoreImplType = System.getProperty(EscCucumber.SYSTEM_PROPERTY);
        final EventStore eventStore;
        if (currentEventStoreImplType.equals("mem")) {
            eventStore = new InMemoryEventStore(Executors.newCachedThreadPool());
        } else if (currentEventStoreImplType.equals("eshttp") || currentEventStoreImplType.equals("jpa")
                || currentEventStoreImplType.equals("esjc")) {
            final XmlDeSerializer xmlDeSer = new XmlDeSerializer(false, BookAddedEvent.class, MyMeta.class, EscEvent.class, EscEvents.class,
                    EscMeta.class, Base64Data.class);
            final JsonDeSerializer jsonDeSer = new JsonDeSerializer();
            final TextDeSerializer textDeSer = new TextDeSerializer();
            registry = new SimpleSerializerDeserializerRegistry();
            registry.add(new SerializedDataType(BookAddedEvent.TYPE.asBaseType()), "application/xml", xmlDeSer);
            registry.add(new SerializedDataType(MyMeta.TYPE.asBaseType()), "application/json", jsonDeSer);
            registry.add(new SerializedDataType(Base64Data.TYPE.asBaseType()), "application/xml", xmlDeSer);
            registry.add(new SerializedDataType(EscEvent.TYPE.asBaseType()), "application/xml", xmlDeSer);
            registry.add(new SerializedDataType(EscEvents.TYPE.asBaseType()), "application/xml", xmlDeSer);
            registry.add(new SerializedDataType(EscMeta.TYPE.asBaseType()), "application/xml", xmlDeSer);
            registry.add(new SerializedDataType("TextEvent"), "text/plain", textDeSer);
            if (currentEventStoreImplType.equals("eshttp")) {
                final ThreadFactory threadFactory = Executors.defaultThreadFactory();
                final URL url = new URL("http://127.0.0.1:2113/");
                eventStore = new ESHttpEventStore.Builder().threadFactory(threadFactory).url(url).envelopeType(ESEnvelopeType.XML)
                        .serDesRegistry(registry).credentialsProvider(credentialsProvider).build();
            } else if (currentEventStoreImplType.equals("jpa")) {
                setupDb();
                eventStore = new JpaEventStore(em, new TestIdStreamFactory(), registry, registry);
            } else if (currentEventStoreImplType.equals("esjc")) {
                final com.github.msemys.esjc.EventStore es = EventStoreBuilder.newBuilder()
                        .userCredentials(credentials.getUserName(), credentials.getPassword()).singleNodeAddress("127.0.0.1", 1113).build();
                eventStore = new ESJCEventStore.Builder().eventStore(es).serDesRegistry(registry)
                        .targetContentType(EnhancedMimeType.create("application", "xml", Charset.forName("utf-8"))).build();
            } else {
                throw new IllegalStateException("Unknown type: " + currentEventStoreImplType);
            }
        } else {
            throw new IllegalStateException("Unknown type: " + currentEventStoreImplType);
        }
        eventStore.open();
        testContext = new TestContext(currentEventStoreImplType, eventStore);
        lastCommand = null;
    }

    @After
    public void afterFeature() {
        if (testContext != null) {
            testContext.getEventStore().close();
            teardownDb();
            testContext = null;
        }
        if (lastCommand != null) {
            throw new IllegalStateException("Last command was set, but not verified!");
        }
    }

    @Then("^this should give the expected results$")
    public void success() {
        verifyThen();
    }

    @Then("^this should raise no exception$")
    public void thenNoException() {
        verifyThen();
    }

    @When("^the following deletes are executed$")
    public void whenExecuteDeletes(final List<DeleteCommand> commands) {
        final TestCommand<TestContext> command = new MultipleCommands<TestContext>(commands);
        command.init(testContext);
        executeWhen(command);
    }

    @Given("^the following streams are created and a single event is appended to each$")
    public void givenCreateStreamsAndAppendSomeEvent(final List<String> streams) {

        final MultipleCommands<TestContext> command = new MultipleCommands<TestContext>();

        for (int i = 1; i < streams.size(); i++) {
            final String streamName = streams.get(i);
            command.add(new CreateStreamCommand(streamName));
            final CommonEvent event = new SimpleCommonEvent(new EventId(), BookAddedEvent.TYPE, new BookAddedEvent("Unknown", "John Doe"));
            command.add(new AppendToStreamCommand(streamName, ExpectedVersion.ANY.getNo(), null, event));
        }

        command.init(testContext);
        executeGiven(command);

    }

    @Given("^the following streams don't exist$")
    public void givenStreamsDontExist(final List<String> streams) {
        final MultipleCommands<TestContext> command = new MultipleCommands<TestContext>();
        for (int i = 1; i < streams.size(); i++) {
            final String streamName = streams.get(i);
            command.add(new StreamExistsCommand(streamName, false));
        }
        command.init(testContext);
        executeGiven(command);
    }

    @Then("^following streams should not exist$")
    public void thenStreamsShouldNotExist(final List<String> streams) {
        final MultipleCommands<TestContext> command = new MultipleCommands<TestContext>();
        for (int i = 1; i < streams.size(); i++) {
            final String streamName = streams.get(i);
            command.add(new StreamExistsCommand(streamName, false));
        }
        command.init(testContext);
        executeThen(command);
    }

    @Then("^reading forward from the following streams should raise the given exceptions$")
    public void thenReadForwardException(final List<ReadForwardExceptionCommand> commands) throws Exception {
        final TestCommand<TestContext> command = new MultipleCommands<TestContext>(commands);
        command.init(testContext);
        executeThen(command);
    }

    @When("^I read forward from the following streams$")
    public void whenReadForwardException(final List<ReadForwardExceptionCommand> commands) {
        final TestCommand<TestContext> command = new MultipleCommands<TestContext>(commands);
        command.init(testContext);
        executeWhen(command);
    }

    @When("^I read backward from the following streams$")
    public void whenReadBackwardException(final List<ReadBackwardExceptionCommand> commands) {
        final TestCommand<TestContext> command = new MultipleCommands<TestContext>(commands);
        command.init(testContext);
        executeWhen(command);
    }

    @Given("^the stream \"(.*?)\" does not exist$")
    public void givenStreamDoesNotExist(final String streamName) {
        final TestCommand<TestContext> command = new StreamExistsCommand(streamName, false);
        command.init(testContext);
        executeGiven(command);
    }

    @When("^I append the following events in the given order$")
    public void whenAppendEvents(final List<AppendToStreamCommand> commands) {
        final MultipleCommands<TestContext> command = new MultipleCommands<TestContext>();
        for (final AppendToStreamCommand cmd : commands) {
            command.add(cmd);
        }
        command.init(testContext);
        executeWhen(command);
    }

    @Then("^reading forward from stream should have the following results$")
    public void thenReadForward(final List<ReadForwardCommand> commands) throws Exception {
        final TestCommand<TestContext> command = new MultipleCommands<TestContext>(commands);
        command.init(testContext);
        executeThen(command);
    }

    @Then("^reading backward from stream should have the following results$")
    public void thenReadBackward(final List<ReadBackwardCommand> commands) throws Exception {
        final TestCommand<TestContext> command = new MultipleCommands<TestContext>(commands);
        command.init(testContext);
        executeThen(command);
    }

    @When("^I append the following events to stream \"(.*?)\"$")
    public void whenAppendXmlEvents(final String streamName, final String eventsXml) {
        whenAppendXmlEvents(streamName, ExpectedVersion.ANY.getNo(), eventsXml);
    }

    private void whenAppendXmlEvents(final String streamName, final long version, final String eventsXml) {
        final Events events = unmarshal(eventsXml, Events.class);
        final List<CommonEvent> commonEvents = events.asCommonEvents(createCtx());

        final TestCommand<TestContext> command = new AppendToStreamCommand(streamName, version, null, commonEvents);
        command.init(testContext);
        executeWhen(command);
    }

    @Then("^reading event (\\d+) from stream \"(.*?)\" should return the following event$")
    public void thenReadXmlEvent(final long eventNumber, final String streamName, final String expectedEventXml) {

        final TestCommand<TestContext> command = new ReadEventCommand(streamName, eventNumber, expectedEventXml, null);
        command.init(testContext);
        executeThen(command);

    }

    @Then("^reading event (\\d+) from stream \"(.*?)\" should throw a \"(.*?)\"$")
    public void thenReadingEventShouldThrow_a(long eventNumber, String streamName, String expectedException) {
        final TestCommand<TestContext> command = new ReadEventCommand(streamName, eventNumber, null, expectedException);
        command.init(testContext);
        executeThen(command);
    }

    @When("^the following state queries are executed$")
    public void whenStateQueriesAreExecuted(final List<StreamStateCommand> commands) {
        final TestCommand<TestContext> command = new MultipleCommands<TestContext>(commands);
        command.init(testContext);
        executeWhen(command);
    }

    @Given("^the following streams exist$")
    public void givenStreamsExist(final List<String> streams) {
        final MultipleCommands<TestContext> command = new MultipleCommands<TestContext>();
        for (int i = 1; i < streams.size(); i++) {
            final String streamName = streams.get(i);
            command.add(new StreamExistsCommand(streamName, true));
        }
        command.init(testContext);
        executeGiven(command);

    }

    @Then("^following streams should exist$")
    public void thenStreamsShouldExist(final List<String> streams) {
        final MultipleCommands<TestContext> command = new MultipleCommands<TestContext>();
        for (int i = 1; i < streams.size(); i++) {
            final String streamName = streams.get(i);
            command.add(new StreamExistsCommand(streamName, true));
        }
        command.init(testContext);
        executeThen(command);
    }

    @Then("^reading all events from stream \"(.*?)\" starting at position (\\d+) with chunk size (\\d+) should have the following results$")
    public void thenReadingAllEventsFromStream(final String streamName, final long startAtEventNo, final int chunkSize,
            final List<ReadAllForwardChunk> expectedChunks) {

        final TestCommand<TestContext> command = new ReadAllForwardCommand(streamName, startAtEventNo, chunkSize, expectedChunks);
        command.init(testContext);
        executeThen(command);

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

    private void executeGiven(TestCommand<TestContext> command) {
        beginTransaction();
        command.execute();
        endTransaction();
        command.verify();
    }

    private void executeWhen(TestCommand<TestContext> command) {
        lastCommand = command;
        beginTransaction();
        command.execute();
        endTransaction();
    }

    private void executeThen(TestCommand<TestContext> command) {
        lastCommand = null;
        beginTransaction();
        command.execute();
        endTransaction();
        command.verify();
    }

    private void verifyThen() {
        if (lastCommand == null) {
            throw new IllegalStateException("Last command was not set in the 'when' condition");
        }
        lastCommand.verify();
        lastCommand = null;
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

    private JAXBContext createCtx() {
        try {
            return JAXBContext.newInstance(BookAddedEvent.class);
        } catch (final JAXBException ex) {
            throw new RuntimeException(ex);
        }
    }

}
// CHECKSTYLE:ON
