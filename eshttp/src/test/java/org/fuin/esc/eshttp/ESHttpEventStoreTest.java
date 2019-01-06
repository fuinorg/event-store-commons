package org.fuin.esc.eshttp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.fuin.esc.api.ProjectionStreamId;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.TypeName;
import org.fuin.objects4j.common.ConstraintViolationException;
import org.junit.Test;

/**
 * Tests the {@link ESHttpEventStore} class.
 */
// CHECKSTYLE:OFF Test
public class ESHttpEventStoreTest {

    @Test
    public void testStreamId() throws URISyntaxException {

        try {
            ESHttpEventStore.streamId(new URI("https://www.fuin.org/"));
            fail();
        } catch (final IllegalStateException ex) {
            assertThat(ex.getMessage()).isEqualTo("Failed to extract '/streams/': https://www.fuin.org/");
        }

        try {
            ESHttpEventStore.streamId(new URI("http://127.0.0.1:2113/streams"));
            fail();
        } catch (final IllegalStateException ex) {
            assertThat(ex.getMessage()).isEqualTo("Failed to extract '/streams/': http://127.0.0.1:2113/streams");
        }

        try {
            ESHttpEventStore.streamId(new URI("http://127.0.0.1:2113/streams/x"));
            fail();
        } catch (final IllegalStateException ex) {
            assertThat(ex.getMessage()).isEqualTo("Failed to extract last '/': http://127.0.0.1:2113/streams/x (p1=21)");
        }

        try {
            ESHttpEventStore.streamId(new URI("http://127.0.0.1:2113/streams/"));
            fail();
        } catch (final IllegalStateException ex) {
            assertThat(ex.getMessage()).isEqualTo("Failed to extract last '/': http://127.0.0.1:2113/streams/ (p1=21)");
        }

        try {
            ESHttpEventStore.streamId(new URI("http://127.0.0.1:2113/streams//"));
            fail();
        } catch (final IllegalStateException ex) {
            assertThat(ex.getMessage()).isEqualTo("Failed to extract name: http://127.0.0.1:2113/streams// (p1=21, p2=30)");
        }

        assertThat(ESHttpEventStore.streamId(new URI("http://127.0.0.1:2113/streams/append_diff_and_read_stream/2")))
                .isEqualTo(new SimpleStreamId("append_diff_and_read_stream"));

    }

    @Test
    public void testEventNumber() throws URISyntaxException {

        try {
            ESHttpEventStore.eventNumber(new URI("https://www.fuin.org/"));
            fail();
        } catch (final IllegalStateException ex) {
            assertThat(ex.getMessage()).isEqualTo("Failed to extract '/streams/': https://www.fuin.org/");
        }

        try {
            ESHttpEventStore.eventNumber(new URI("http://127.0.0.1:2113/streams"));
            fail();
        } catch (final IllegalStateException ex) {
            assertThat(ex.getMessage()).isEqualTo("Failed to extract '/streams/': http://127.0.0.1:2113/streams");
        }

        try {
            ESHttpEventStore.eventNumber(new URI("http://127.0.0.1:2113/streams/x"));
            fail();
        } catch (final IllegalStateException ex) {
            assertThat(ex.getMessage()).isEqualTo("Failed to extract last '/': http://127.0.0.1:2113/streams/x (p1=21)");
        }

        try {
            ESHttpEventStore.eventNumber(new URI("http://127.0.0.1:2113/streams/"));
            fail();
        } catch (final IllegalStateException ex) {
            assertThat(ex.getMessage()).isEqualTo("Failed to extract last '/': http://127.0.0.1:2113/streams/ (p1=21)");
        }

        try {
            ESHttpEventStore.eventNumber(new URI("http://127.0.0.1:2113/streams//"));
            fail();
        } catch (final IllegalStateException ex) {
            assertThat(ex.getMessage()).isEqualTo("Failed to extract name: http://127.0.0.1:2113/streams// (p1=21, p2=30)");
        }

        assertThat(ESHttpEventStore.eventNumber(new URI("http://127.0.0.1:2113/streams/append_diff_and_read_stream/2"))).isEqualTo(2);
        assertThat(ESHttpEventStore.eventNumber(new URI("http://127.0.0.1:2113/streams/append_diff_and_read_stream/12345")))
                .isEqualTo(12345);

    }

    @Test
    public void testType2str() {

        // PREPARE
        final List<TypeName> name1 = new ArrayList<>();
        name1.add(new TypeName("one"));
        final List<TypeName> name2 = new ArrayList<>();
        name2.add(new TypeName("one"));
        name2.add(new TypeName("two"));
        final List<TypeName> name3 = new ArrayList<>();
        name3.add(new TypeName("one"));
        name3.add(new TypeName("two"));
        name3.add(new TypeName("three"));

        // TEST & VERIFY
        assertThat(ESHttpEventStore.type2str(null)).isEqualTo("");
        assertThat(ESHttpEventStore.type2str(new ArrayList<>())).isEqualTo("");
        assertThat(ESHttpEventStore.type2str(name1)).isEqualTo(",one");
        assertThat(ESHttpEventStore.type2str(name2)).isEqualTo(",one,two");
        assertThat(ESHttpEventStore.type2str(name3)).isEqualTo(",one,two,three");

    }

    @Test
    public void testRequireProjection() {

        ESHttpEventStore.requireProjection(new ProjectionStreamId("projection"));

        try {
            ESHttpEventStore.requireProjection(new SimpleStreamId("stream"));
            fail();
        } catch (final ConstraintViolationException ex) {
            assertThat(ex.getMessage()).isEqualTo("The stream identifier is not a projection id");
        }

    }

    @Test
    public void testCurrentVersion() {

        // PREPARE
        final BasicStatusLine statusline = new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 400, "Wrong expected EventNumber");

        // TEST & VERIFY
        final HttpResponse validResponse = new BasicHttpResponse(statusline);
        validResponse.addHeader("CurrentVersion", "4");
        assertThat(ESHttpEventStore.currentVersion(validResponse)).isEqualTo(Long.valueOf(4));

        assertThat(ESHttpEventStore.currentVersion(new BasicHttpResponse(statusline))).isNull();

        final HttpResponse invalidResponse = new BasicHttpResponse(statusline);
        invalidResponse.addHeader("CurrentVersion", null);
        assertThat(ESHttpEventStore.currentVersion(invalidResponse)).isNull();

    }

}
// CHECKSTYLE:ON
