package org.fuin.esc.test;

import static org.junit.Assert.fail;

import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.WritableEventStore;
import org.fuin.esc.mem.InMemoryEventStore;

import cucumber.api.PendingException;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class BasicFeature {

    private WritableEventStore eventStore;

    @Before
    public void beforeFeature() {
        // Use the property to select the correct implementation:
        // System.getProperty(EscCucumber.SYSTEM_PROPERTY)
        eventStore = new InMemoryEventStore();
    }

    @After
    public void afterFeature() {
        eventStore = null;
    }

    @Given("^The stream \"([^\"]*)\" does not exist$")
    public void The_stream_does_not_exist(String streamName) throws Throwable {
        try {
            eventStore.deleteStream(new SimpleStreamId(streamName));
            fail("The stream should not exist: " + streamName);
        } catch (final StreamNotFoundException ex) {
            // OK
        }
    }

    @When("^I write the following events to \"([^\"]*)\"$")
    public void I_write_the_following_events_to(String streamName,
            String eventsXml) throws Throwable {
        // Express the Regexp above with the code you wish you had
        throw new PendingException();
    }

    @Then("^I expect that reading all from stream \"([^\"]*)\" returns the following events$")
    public void I_expect_that_reading_all_from_stream_returns_the_following_events(
            String streamName, String eventsXml) throws Throwable {
        // Express the Regexp above with the code you wish you had
        throw new PendingException();
    }

}
