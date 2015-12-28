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
package org.fuin.esc.eshttp;

import static org.fuin.esc.api.ExpectedVersion.ANY;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import javax.validation.constraints.NotNull;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventStoreSync;
import org.fuin.esc.api.StreamAlreadyExistsException;
import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.StreamReadOnlyException;
import org.fuin.esc.api.StreamState;
import org.fuin.esc.api.WrongExpectedVersionException;
import org.fuin.esc.spi.DeserializerRegistry;
import org.fuin.esc.spi.EnhancedMimeType;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.esc.spi.SerializedDataType;
import org.fuin.esc.spi.SerializerRegistry;
import org.fuin.objects4j.common.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation that connects to the http://www.geteventstore.com via HTTP API.
 */
public final class ESHttpEventStoreSync implements EventStoreSync {

    private static final Logger LOG = LoggerFactory.getLogger(ESHttpEventStoreSync.class);

    private final ThreadFactory threadFactory;

    private final URL url;

    private final SerializedDataType serMetaType;

    private final ESEnvelopeType envelopeType;

    private final SerializerRegistry serRegistry;

    private final DeserializerRegistry desRegistry;

    private CloseableHttpAsyncClient httpclient;

    /**
     * Constructor with all mandatory data.
     * 
     * @param threadFactory
     *            Factory used to create the necessary internal threads.
     * @param url
     *            Event store base URL like "http://127.0.0.1:2113/".
     * @param serMetaType
     *            Unique type name for the meta data.
     * @param envelopeType
     *            Envelope type for reading/writing events.
     * @param serRegistry
     *            Registry used to locate serializers.
     * @param desRegistry
     *            Registry used to locate deserializers.
     */
    public ESHttpEventStoreSync(@NotNull final ThreadFactory threadFactory, @NotNull final URL url,
            @NotNull final SerializedDataType serMetaType, @NotNull final ESEnvelopeType envelopeType,
            @NotNull final SerializerRegistry serRegistry, @NotNull final DeserializerRegistry desRegistry) {
        super();
        Contract.requireArgNotNull("threadFactory", threadFactory);
        Contract.requireArgNotNull("url", url);
        Contract.requireArgNotNull("serMetaType", serMetaType);
        Contract.requireArgNotNull("envelopeType", envelopeType);
        Contract.requireArgNotNull("serRegistry", serRegistry);
        Contract.requireArgNotNull("desRegistry", desRegistry);
        this.threadFactory = threadFactory;
        this.url = url;
        this.serMetaType = serMetaType;
        this.envelopeType = envelopeType;
        this.serRegistry = serRegistry;
        this.desRegistry = desRegistry;
    }

    @Override
    public void open() {
        httpclient = HttpAsyncClients.custom().setThreadFactory(threadFactory).build();
        httpclient.start();
    }

    @Override
    public void close() {
        if (httpclient == null) {
            throw new IllegalStateException("The 'open()' method was never called!");
        }
        try {
            httpclient.close();
        } catch (final IOException ex) {
            throw new RuntimeException("Cannot close http client", ex);
        }
    }

    @Override
    public final void createStream(final StreamId streamId) throws StreamAlreadyExistsException {
        // Do nothing as the operation is not supported
    }

    @Override
    public final int appendToStream(final StreamId streamId, final CommonEvent... events) {
        return appendToStream(streamId, -2, EscSpiUtils.asList(events));
    }

    @Override
    public final int appendToStream(final StreamId streamId, final int expectedVersion,
            final CommonEvent... events) {
        return appendToStream(streamId, expectedVersion, EscSpiUtils.asList(events));
    }

    @Override
    public final int appendToStream(final StreamId streamId, final List<CommonEvent> events) {
        return appendToStream(streamId, -2, events);
    }

    @Override
    public int appendToStream(final StreamId streamId, final int expectedVersion,
            final List<CommonEvent> commonEvents) throws StreamDeletedException,
            WrongExpectedVersionException, StreamReadOnlyException {

        if (streamId.isProjection()) {
            throw new StreamReadOnlyException(streamId);
        }

        final ESHttpMarshaller marshaller = envelopeType.getMarshaller();
        final EnhancedMimeType mimeType = EscSpiUtils.mimeType(serRegistry, commonEvents);
        // TODO Get next expected version from event store!
        int nextExpectedVersion = 0;
        if (mimeType == null) {
            // Not all events have same type
            for (final CommonEvent commonEvent : commonEvents) {
                final String content = marshaller.marshal(serRegistry, serMetaType, commonEvent);
                appendToStream(streamId, expectedVersion, mimeType, content, 1);
            }
        } else {
            // All events are of same type
            final String content = marshaller.marshal(serRegistry, serMetaType, commonEvents);
            appendToStream(streamId, expectedVersion, mimeType, content, commonEvents.size());
        }

        return nextExpectedVersion;
    }

    private void appendToStream(final StreamId streamId, final int expectedVersion,
            final EnhancedMimeType mimeType, String content, int count) throws StreamDeletedException,
            WrongExpectedVersionException {

        final String msg = "appendToStream(" + streamId + ", " + expectedVersion + ", " + mimeType + ", "
                + count + ")";
        try {
            final URI uri = new URIBuilder(url.toURI()).setPath("/streams/" + streamId).build();
            final HttpPost post = createPost(uri, expectedVersion, content);
            LOG.debug(msg + " POST: {}", post);

            final Future<HttpResponse> future = httpclient.execute(post, null);
            final HttpResponse response = future.get();
            final StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == 201) {
                // CREATED - Event(s) where added
                LOG.debug(msg + " RESPONSE: {}", response);
                return;
            }
            if (statusLine.getStatusCode() == 301) {
                // FOUND - Event(s) already existed and where not created again
                // (Idempotency)
                LOG.debug(msg + " RESPONSE: {}", response);
                return;
            }
            if (statusLine.getStatusCode() == 400) {
                // TODO Add expected version instead of any version if ES
                // returns this in header
                LOG.debug(msg + " RESPONSE: {}", response);
                throw new WrongExpectedVersionException(streamId, expectedVersion, null);
            }
            if (statusLine.getStatusCode() == 410) {
                // Stream was hard deleted
                LOG.debug(msg + " RESPONSE: {}", response);
                throw new StreamDeletedException(streamId);
            }

            LOG.debug(msg + " RESPONSE: {}", response);
            throw new RuntimeException(msg + " [Status=" + statusLine.getStatusCode() + "]");

        } catch (final URISyntaxException | InterruptedException | ExecutionException ex) {
            throw new RuntimeException(msg, ex);
        }

    }

    @Override
    public void deleteStream(final StreamId streamId, final int expectedVersion, final boolean hardDelete)
            throws StreamNotFoundException, StreamDeletedException, WrongExpectedVersionException {

        final String msg = "deleteStream(" + streamId + ", " + expectedVersion + ", " + hardDelete + ")";
        try {
            final URI uri = new URIBuilder(url.toURI()).setPath("/streams/" + streamId).build();
            final HttpDelete delete = new HttpDelete(uri);
            delete.setHeader("ES-HardDelete", "" + hardDelete);
            delete.setHeader("ES-ExpectedVersion", "" + expectedVersion);
            LOG.debug(msg + " DELETE: {}", delete);

            final Future<HttpResponse> future = httpclient.execute(delete, null);
            final HttpResponse response = future.get();
            final StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == 204) {
                // Stream deleted
                LOG.debug(msg + " RESPONSE: {}", response);
                return;
            }
            if (statusLine.getStatusCode() == 400) {
                // TODO Add expected version instead of any version if ES
                // returns this in header
                LOG.debug(msg + " RESPONSE: {}", response);
                throw new WrongExpectedVersionException(streamId, expectedVersion, null);
            }
            if (statusLine.getStatusCode() == 410) {
                // 410 GONE - Stream was hard deleted
                LOG.debug(msg + " RESPONSE: {}", response);
                throw new StreamDeletedException(streamId);
            }

            LOG.debug(msg + " RESPONSE: {}", response);
            throw new RuntimeException(msg + " [Status=" + statusLine.getStatusCode() + "]");

        } catch (final URISyntaxException | ExecutionException | InterruptedException ex) {
            throw new RuntimeException(msg, ex);
        }

    }

    @Override
    public void deleteStream(final StreamId streamId, final boolean hardDelete)
            throws StreamNotFoundException, StreamDeletedException {
        deleteStream(streamId, ANY.getNo(), hardDelete);
    }

    @Override
    public StreamEventsSlice readEventsForward(final StreamId streamId, final int start, final int count) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("start", start, 0);
        Contract.requireArgMin("count", count, 1);

        final String msg = "readEventsForward(" + streamId + ", " + start + ", " + count + ")";
        try {
            final URI uri = new URIBuilder(url.toURI()).setPath(
                    "/streams/" + streamName(streamId) + "/" + start + "/forward/" + count).build();
            return readEvents(streamId, true, uri, start, count, msg);
        } catch (final IOException | URISyntaxException | InterruptedException | ExecutionException ex) {
            throw new RuntimeException(msg, ex);
        }

    }

    @Override
    public StreamEventsSlice readEventsBackward(final StreamId streamId, final int start, final int count) {
        final String msg = "readEventsBackward(" + streamId + ", " + start + ", " + count + ")";
        try {
            final URI uri = new URIBuilder(url.toURI()).setPath(
                    "/streams/" + streamName(streamId) + "/" + start + "/backward/" + count).build();
            return readEvents(streamId, false, uri, start, count, msg);
        } catch (final IOException | URISyntaxException | InterruptedException | ExecutionException ex) {
            throw new RuntimeException(msg, ex);
        }
    }

    @Override
    public CommonEvent readEvent(final StreamId streamId, final int eventNumber) {
        final String msg = "readEvent(" + streamId + ", " + eventNumber + ")";
        try {
            final URI uri = new URIBuilder(url.toURI()).setPath(
                    "/streams/" + streamName(streamId) + "/" + eventNumber).build();
            return readEvent(uri);
        } catch (final URISyntaxException ex) {
            throw new RuntimeException(msg, ex);
        }
    }

    @Override
    public final boolean streamExists(final StreamId streamId) {

        Contract.requireArgNotNull("streamId", streamId);

        final String msg = "streamExists(" + streamId + ")";
        try {
            final URI uri = new URIBuilder(url.toURI()).setPath("/streams/" + streamName(streamId)).build();
            LOG.info(uri.toString());
            final HttpGet get = createHttpGet(uri);
            final Future<HttpResponse> future = httpclient.execute(get, null);
            final HttpResponse response = future.get();
            final StatusLine status = response.getStatusLine();
            if (status.getStatusCode() == 404) {
                return false;
            }
            if (status.getStatusCode() == 410) {
                // Stream was hard deleted
                return false;
            }
            if (status.getStatusCode() == 200) {
                return true;
            }
            LOG.debug(msg + " RESPONSE: {}", response);
            throw new RuntimeException(msg + " [Status=" + status.getStatusCode() + "]");
        } catch (final URISyntaxException | InterruptedException | ExecutionException ex) {
            throw new RuntimeException(msg, ex);
        }

    }

    @Override
    public final StreamState streamState(final StreamId streamId) {
        Contract.requireArgNotNull("streamId", streamId);

        final String msg = "streamState(" + streamId + ")";
        try {
            final URI uri = new URIBuilder(url.toURI()).setPath("/streams/" + streamName(streamId)).build();
            LOG.info(uri.toString());
            final HttpGet get = new HttpGet(uri);
            final Future<HttpResponse> future = httpclient.execute(get, null);
            final HttpResponse response = future.get();
            final StatusLine status = response.getStatusLine();
            if (status.getStatusCode() == 200) {
                LOG.debug(msg + " RESPONSE: {}", response);
                return StreamState.ACTIVE;
            }
            if (status.getStatusCode() == 404) {
                // May have never existed or was soft deleted...
                // TODO Maybe the event store can send something to distinguish this?
                LOG.debug(msg + " RESPONSE: {}", response);
                throw new StreamNotFoundException(streamId);
            }
            if (status.getStatusCode() == 410) {
                // 410 GONE - Stream was hard deleted
                LOG.debug(msg + " RESPONSE: {}", response);
                return StreamState.HARD_DELETED;
            }
            LOG.debug(msg + " RESPONSE: {}", response);
            throw new RuntimeException(msg + " [Status=" + status.getStatusCode() + "]");
        } catch (final URISyntaxException | InterruptedException | ExecutionException ex) {
            throw new RuntimeException(msg, ex);
        }
    }

    private StreamEventsSlice readEvents(final StreamId streamId, final boolean forward, final URI uri,
            final int start, final int count, final String msg) throws InterruptedException,
            ExecutionException, IOException {
        LOG.info(uri.toString());
        final HttpGet get = createHttpGet(uri);
        final Future<HttpResponse> future = httpclient.execute(get, null);
        final HttpResponse response = future.get();
        final StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() == 200) {
            final HttpEntity entity = response.getEntity();
            final InputStream in = entity.getContent();
            try {
                final AtomFeedReader atomFeedReader = envelopeType.getAtomFeedReader();
                final List<URI> uris = atomFeedReader.readAtomFeed(in);
                return readEvents(forward, start, count, uris);
            } finally {
                in.close();
            }
        }
        if (statusLine.getStatusCode() == 404) {
            // 404 Not Found
            LOG.debug(msg + " RESPONSE: {}", response);
            throw new StreamNotFoundException(streamId);
        }
        if (statusLine.getStatusCode() == 410) {
            // Stream was hard deleted
            LOG.debug(msg + " RESPONSE: {}", response);
            throw new StreamDeletedException(streamId);
        }
        throw new RuntimeException(msg + " [Status=" + statusLine + "]");
    }

    private StreamEventsSlice readEvents(final boolean forward, final int fromEventNumber, final int count,
            final List<URI> uris) {
        final List<CommonEvent> events = new ArrayList<>();
        for (int i = uris.size() - 1; i >= 0; i--) {
            final URI uri = uris.get(i);
            events.add(readEvent(uri));
        }
        final int nextEventNumber;
        final boolean endOfStream;
        if (forward) {
            nextEventNumber = fromEventNumber + events.size();
            endOfStream = count > events.size();
        } else {
            nextEventNumber = ((fromEventNumber - count < 0)) ? 0 : fromEventNumber - count;
            endOfStream = (fromEventNumber - count < 0);
        }
        return new StreamEventsSlice(fromEventNumber, events, nextEventNumber, endOfStream);
    }

    private CommonEvent readEvent(final URI uri) {
        LOG.info(uri.toString());
        try {
            final HttpGet get = createHttpGet(uri);
            final Future<HttpResponse> future = httpclient.execute(get, null);
            final HttpResponse response = future.get();
            final StatusLine status = response.getStatusLine();
            if (status.getStatusCode() == 200) {
                final HttpEntity entity = response.getEntity();
                final InputStream in = entity.getContent();
                try {
                    return envelopeType.getAtomFeedReader().readEvent(desRegistry, serMetaType, in);
                } finally {
                    in.close();
                }
            } else {
                throw new RuntimeException("Failed to read " + uri + " [Status=" + status.getStatusCode()
                        + "]");
            }
        } catch (final InterruptedException | ExecutionException | UnsupportedOperationException
                | IOException ex) {
            throw new RuntimeException("Failed to read " + uri, ex);
        }
    }

    private String streamName(StreamId streamId) {
        if (streamId.equals(StreamId.ALL)) {
            return "$all";
        }
        return streamId.getName();
    }

    private HttpGet createHttpGet(URI uri) {
        final HttpGet request = new HttpGet(uri);
        request.setHeader("Accept", envelopeType.getReadContentType());
        return request;
    }

    private HttpPost createPost(final URI uri, final int expectedVersion, final String content) {
        final HttpPost post = new HttpPost(uri);
        post.setHeader("Content-Type",
                envelopeType.getWriteContentType() + "; charset=" + envelopeType.getMetaCharset());
        post.setHeader("ES-ExpectedVersion", "" + expectedVersion);
        final ContentType contentType = ContentType.create(envelopeType.getMetaType(),
                envelopeType.getMetaCharset());
        post.setEntity(new StringEntity(content, contentType));
        return post;
    }

}
