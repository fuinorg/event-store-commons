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

import static org.fest.assertions.Assertions.assertThat;
import static org.fuin.units4j.Units4JUtils.unmarshal;
import static org.junit.Assert.fail;

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
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.eshttp.ESEnvelopeType;
import org.fuin.esc.eshttp.ESHttpEventStoreSync;
import org.fuin.esc.mem.InMemoryEventStoreSync;
import org.fuin.esc.spi.JsonDeSerializer;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.spi.XmlDeSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

// CHECKSTYLE:OFF Test code
public class BasicFeature {

    private static final Logger LOG = LoggerFactory.getLogger(BasicFeature.class);
    
    private static final int MAX_EVENTS = 10;

    private EventStoreSync eventStore;

    private Exception caughtException;

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
        caughtException = null;
    }

    @Given("^the stream \"([^\"]*)\" does not exist$")
    public void assertThatStreamDoesNotExist(String streamName) {
        LOG.debug("assertThatStreamDoesNotExist({})", streamName);
        try {
            if (eventStore.streamExists(new SimpleStreamId(streamName, false))) {
                fail("The stream should not exist: " + streamName);
            }
        } catch (Exception ex) {
            caughtException = ex;
        }
    }

    @When("^the stream \"(.*?)\" is hard deleted using expected version \"(.*?)\"$")
    public void hardDelete(String streamName, String version) {
        LOG.debug("hardDelete({}, {})", streamName, version);
        try {
            eventStore.deleteStream(new SimpleStreamId(streamName, false), ExpectedVersion.no(version), true);
        } catch (Exception ex) {
            caughtException = ex;
        }
    }

    @When("^I write the following events to stream \"([^\"]*)\"$")
    public void appendEventsTo(String streamName, String eventsXml) {
        LOG.debug("appendEventsTo({}, {})", streamName, eventsXml);
        try {
            final Events toAppend = unmarshal(eventsXml, Events.class);
            eventStore.appendToStream(new SimpleStreamId(streamName, false),
                    toAppend.asCommonEvents(BookAddedEvent.class));
        } catch (Exception ex) {
            caughtException = ex;
        }
    }

    @Then("^reading all events from stream \"([^\"]*)\" should return the following slices$")
    public void assertReadingAllEvents(String streamName, String slicesXml) {
        LOG.debug("assertReadingAllEvents({}, {})", streamName, slicesXml);
        try {
            final Slices expected = unmarshal(slicesXml, Slices.class);
            final Slices actual = new Slices();
            StreamEventsSlice slice = eventStore.readEventsForward(new SimpleStreamId(streamName, false), 0,
                    MAX_EVENTS);
            while (!slice.isEndOfStream()) {
                actual.append(Slice.valueOf(slice));
                slice = eventStore.readEventsForward(new SimpleStreamId(streamName, false),
                        slice.getNextEventNumber(), MAX_EVENTS);
            }
            actual.append(Slice.valueOf(slice));
            assertThat(actual.getSlices()).isEqualTo(expected.getSlices());
        } catch (Exception ex) {
            caughtException = ex;
        }
    }

    @Then("^this should be successful$")
    public void success() {
        assertThat(caughtException).isNull();
    }

    @Then("^this should fail with API \"(.*?)\"$")
    public void failsWithApiException(String exceptionClass) throws ClassNotFoundException {
        @SuppressWarnings("unchecked")
        final Class<? extends Throwable> clasz = (Class<? extends Throwable>) Class
                .forName("org.fuin.esc.api." + exceptionClass);
        assertThat(caughtException).as(
                "Expected an exception of type " + clasz.getName() + ", but was: "
                        + caughtException.getClass().getName() + ": " + caughtException.getMessage())
                .isInstanceOf(clasz);
    }

    @When("^the following deletes are executed$")
    public void executeDeletes(final List<DeleteOperation> deleteOperations) {

        for (final DeleteOperation op : deleteOperations) {
            op.init();
            try {
                LOG.debug("Execute: {}", op);
                eventStore.deleteStream(new SimpleStreamId(op.getStreamName(), false),
                        op.getExpectedVersion(), op.isHardDelete());
            } catch (Exception ex) {
                op.setException(ex);
            }
        }
        caughtException = verifyOperations(deleteOperations);

    }

    @Given("^the following streams are created and a single event is appended$")
    public void createStreamsAndAppendSomeEvent(final List<String> streams) {
        for (int i = 1; i < streams.size(); i++) {
            final String stream = streams.get(i);
            LOG.debug("Create stream and append some event: {}", stream);
            final StreamId streamId = new SimpleStreamId(stream, false);
            eventStore.createStream(streamId);
            eventStore.appendToStream(streamId, new CommonEvent(new EventId(), new EventType(
                    BookAddedEvent.TYPE), new BookAddedEvent("Unknown", "John Doe")));
        }
    }

    @Then("^following streams should not exist$")
    public void streamsShouldNotExist(final List<String> streams) {
        for (int i = 1; i < streams.size(); i++) {
            final String stream = streams.get(i);
            LOG.debug("Stream should not exist: {}", stream);
            final StreamId streamId = new SimpleStreamId(stream, false);
            assertThat(eventStore.streamExists(streamId)).describedAs(
                    "Stream '" + stream + "' does exist!").isFalse();
        }
    }

    @Then("^reading forward from the following streams should have the following result$")
    public void readForward(final List<ReadForwardOperation> readOperations) {

        for (final ReadForwardOperation op : readOperations) {
            op.init();
            try {
                LOG.debug("Execute: {}", op);
                eventStore.readEventsForward(new SimpleStreamId(op.getStreamName(), false), op.getStart(),
                        op.getCount());
            } catch (Exception ex) {
                op.setException(ex);
            }
        }
        caughtException = verifyOperations(readOperations);

    }

    private static interface Operation {

        public boolean isResultAsExpected();

        public String toResult();

    }

    public static class DeleteOperation implements Operation {

        private String streamName;
        private boolean hardDelete;
        private String expectedVersion;
        private String expectedException;
        private Exception exception;

        public void init() {
            streamName = emptyAsNull(streamName);
            expectedVersion = emptyAsNull(expectedVersion);
            expectedException = emptyAsNull(expectedException);
        }

        public String getStreamName() {
            return streamName;
        }

        public boolean isHardDelete() {
            return hardDelete;
        }

        public int getExpectedVersion() {
            return ExpectedVersion.no(expectedVersion);
        }

        public Exception getException() {
            return exception;
        }

        public void setException(Exception ex) {
            this.exception = ex;
        }

        public boolean isResultAsExpected() {
            if (expectedException == null) {
                if (exception == null) {
                    return true;
                }
                return false;
            }
            if (exception == null) {
                return false;
            }
            final Class<? extends Exception> ex = exceptionForSimpleName(expectedException);
            if (exception.getClass() == ex) {
                return true;
            }
            return false;
        }

        public String toResult() {
            return streamName + " expected: " + expectedException + ", but was: " + exception;
        }

        @Override
        public String toString() {
            return "DeleteOperation [streamName=" + streamName + ", expectedVersion=" + expectedVersion
                    + ", hardDelete=" + hardDelete + ", exception=" + exception + "]";
        }

    }

    private static class ReadForwardOperation implements Operation {

        private String streamName;
        private int start;
        private int count;
        private String expectedException;
        private Exception exception;

        public void init() {
            streamName = emptyAsNull(streamName);
            expectedException = emptyAsNull(expectedException);
        }

        public String getStreamName() {
            return streamName;
        }

        public int getStart() {
            return start;
        }

        public int getCount() {
            return count;
        }

        public Exception getException() {
            return exception;
        }

        public void setException(Exception ex) {
            this.exception = ex;
        }

        public boolean isResultAsExpected() {
            if (expectedException == null) {
                if (exception == null) {
                    return true;
                }
                return false;
            }
            if (exception == null) {
                return false;
            }
            final Class<? extends Exception> ex = exceptionForSimpleName(expectedException);
            if (exception.getClass() == ex) {
                return true;
            }
            return false;
        }

        public String toResult() {
            return streamName + " expected: " + expectedException + ", but was: " + exception;
        }

        @Override
        public String toString() {
            return "ReadForwardOperation [streamName=" + streamName + ", start=" + start + ", count=" + count
                    + ", exception=" + exception + "]";
        }

    }

    private static String emptyAsNull(String str) {
        if (str == null) {
            return null;
        }
        final String name = str.trim();
        if (name.length() == 0 || name.equals("-")) {
            return null;
        }
        return str;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Exception> exceptionForSimpleName(final String expectedException) {
        final String name = emptyAsNull(expectedException);
        if (expectedException == null) {
            return null;
        }
        try {
            return (Class<? extends Exception>) Class.forName("org.fuin.esc.api." + name);
        } catch (final ClassNotFoundException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private static Exception verifyOperations(final List<? extends Operation> operations) {
        final StringBuffer sb = new StringBuffer();
        for (Operation op : operations) {
            if (!op.isResultAsExpected()) {
                sb.append(op.toResult());
                sb.append("\n");
            }
        }
        if (sb.length() > 0) {
            return new RuntimeException("Some delete results are not as expected:\n" + sb);
        }
        return null;
    }

}
// CHECKSTYLE:ON
