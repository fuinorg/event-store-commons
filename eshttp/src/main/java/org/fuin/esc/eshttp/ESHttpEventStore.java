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
package org.fuin.esc.eshttp;

import static org.fuin.esc.api.ExpectedVersion.ANY;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import jakarta.validation.constraints.NotNull;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventNotFoundException;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamAlreadyExistsException;
import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.StreamReadOnlyException;
import org.fuin.esc.api.StreamState;
import org.fuin.esc.api.TenantId;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.api.WrongExpectedVersionException;
import org.fuin.esc.spi.AbstractReadableEventStore;
import org.fuin.esc.spi.DeserializerRegistry;
import org.fuin.esc.spi.EnhancedMimeType;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.esc.spi.SerDeserializerRegistry;
import org.fuin.esc.spi.SerializerRegistry;
import org.fuin.esc.spi.TenantStreamId;
import org.fuin.objects4j.common.ConstraintViolationException;
import org.fuin.objects4j.common.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation that connects to the http://www.geteventstore.com via HTTP API.
 */
public final class ESHttpEventStore extends AbstractReadableEventStore implements IESHttpEventStore {

    private static final Logger LOG = LoggerFactory.getLogger(ESHttpEventStore.class);

    private final ThreadFactory threadFactory;

    private final URL url;

    private final ESEnvelopeType envelopeType;

    private final SerializerRegistry serRegistry;

    private final DeserializerRegistry desRegistry;

    private final CredentialsProvider credentialsProvider;

    private final TenantId tenantId;

    private CloseableHttpAsyncClient httpclient;

    private boolean open;

    /**
     * Constructor with all mandatory data.
     * 
     * @param threadFactory
     *            Factory used to create the necessary internal threads.
     * @param url
     *            Event store base URL like "http://127.0.0.1:2113/".
     * @param envelopeType
     *            Envelope type for reading/writing events.
     * @param serRegistry
     *            Registry used to locate serializers.
     * @param desRegistry
     *            Registry used to locate deserializers.
     *            
     * @deprecated Use the Builder in this class instead.
     */
    @Deprecated
    public ESHttpEventStore(@NotNull final ThreadFactory threadFactory, @NotNull final URL url, @NotNull final ESEnvelopeType envelopeType,
            @NotNull final SerializerRegistry serRegistry, @NotNull final DeserializerRegistry desRegistry) {
        this(threadFactory, url, envelopeType, serRegistry, desRegistry, null, null);
    }

    /**
     * Constructor with credential provider.
     * 
     * @param threadFactory
     *            Factory used to create the necessary internal threads.
     * @param url
     *            Event store base URL like "http://127.0.0.1:2113/".
     * @param envelopeType
     *            Envelope type for reading/writing events.
     * @param serRegistry
     *            Registry used to locate serializers.
     * @param desRegistry
     *            Registry used to locate deserializers.
     * @param credentialsProvider
     *            Provided authentication information.
     *            
     * @deprecated Use the Builder in this class instead.
     */
    @Deprecated
    public ESHttpEventStore(@NotNull final ThreadFactory threadFactory, @NotNull final URL url, @NotNull final ESEnvelopeType envelopeType,
            @NotNull final SerializerRegistry serRegistry, @NotNull final DeserializerRegistry desRegistry,
            final CredentialsProvider credentialsProvider) {
        this(threadFactory, url, envelopeType, serRegistry, desRegistry, credentialsProvider, null);
    }

    /**
     * Private constructor with all data used by the builder.
     * 
     * @param threadFactory
     *            Factory used to create the necessary internal threads.
     * @param url
     *            Event store base URL like "http://127.0.0.1:2113/".
     * @param envelopeType
     *            Envelope type for reading/writing events.
     * @param serRegistry
     *            Registry used to locate serializers.
     * @param desRegistry
     *            Registry used to locate deserializers.
     * @param credentialsProvider
     *            Provided authentication information.
     * @param tenantId
     *            Unique tenant identifier.
     */
    private ESHttpEventStore(@NotNull final ThreadFactory threadFactory, @NotNull final URL url, @NotNull final ESEnvelopeType envelopeType,
            @NotNull final SerializerRegistry serRegistry, @NotNull final DeserializerRegistry desRegistry,
            final CredentialsProvider credentialsProvider, final TenantId tenantId) {
        super();
        Contract.requireArgNotNull("threadFactory", threadFactory);
        Contract.requireArgNotNull("url", url);
        Contract.requireArgNotNull("envelopeType", envelopeType);
        Contract.requireArgNotNull("serRegistry", serRegistry);
        Contract.requireArgNotNull("desRegistry", desRegistry);
        this.threadFactory = threadFactory;
        this.url = url;
        this.envelopeType = envelopeType;
        this.serRegistry = serRegistry;
        this.desRegistry = desRegistry;
        this.tenantId = tenantId;
        this.credentialsProvider = credentialsProvider;
        this.open = false;
    }

    @Override
    public ESHttpEventStore open() {
        if (open) {
            // Ignore
            return this;
        }
        final HttpAsyncClientBuilder builder = HttpAsyncClients.custom().setThreadFactory(threadFactory);
        if (credentialsProvider != null) {
            builder.setDefaultCredentialsProvider(credentialsProvider);
        }
        final Registry<AuthSchemeProvider> authSchemeRegistry = RegistryBuilder.<AuthSchemeProvider> create()
                .register(BasicCustomScheme.NAME, new BasicCustomSchemeFactory()).build();
        builder.setDefaultAuthSchemeRegistry(authSchemeRegistry);
        builder.setDefaultRequestConfig(RequestConfig.custom().setTargetPreferredAuthSchemes(Arrays.asList("BasicCustom")).build());
        httpclient = builder.build();
        httpclient.start();
        this.open = true;
        return this;
    }

    @Override
    public void close() {
        if (!open) {
            // Ignore
            return;
        }
        try {
            httpclient.close();
        } catch (final IOException ex) {
            throw new RuntimeException("Cannot close http client", ex);
        }
        this.open = false;
    }

    @Override
    public final boolean isSupportsCreateStream() {
        return false;
    }

    @Override
    public final void createStream(final StreamId streamId) throws StreamAlreadyExistsException {
        // Do nothing as the operation is not supported
    }

    @Override
    public final long appendToStream(final StreamId streamId, final CommonEvent... events) {
        return appendToStream(streamId, -2, EscSpiUtils.asList(events));
    }

    @Override
    public final long appendToStream(final StreamId streamId, final long expectedVersion, final CommonEvent... events) {
        return appendToStream(streamId, expectedVersion, EscSpiUtils.asList(events));
    }

    @Override
    public final long appendToStream(final StreamId streamId, final List<CommonEvent> events) {
        return appendToStream(streamId, -2, events);
    }

    @Override
    public long appendToStream(final StreamId streamId, final long expectedVersion, final List<CommonEvent> commonEvents)
            throws StreamDeletedException, WrongExpectedVersionException, StreamReadOnlyException {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("expectedVersion", expectedVersion, ExpectedVersion.ANY.getNo());
        Contract.requireArgNotNull("commonEvents", commonEvents);
        ensureOpen();

        final TenantStreamId sid = new TenantStreamId(tenantId, streamId);
        if (sid.isProjection()) {
            throw new StreamReadOnlyException(sid);
        }

        final ESHttpMarshaller marshaller = envelopeType.getMarshaller();
        final EnhancedMimeType mimeType = EscSpiUtils.mimeType(serRegistry, commonEvents);
        // TODO Get next expected version from event store!
        final int nextExpectedVersion = 0;
        if (mimeType == null) {
            // Not all events have same type
            for (final CommonEvent commonEvent : commonEvents) {
                final List<CommonEvent> list = new ArrayList<>(1);
                list.add(commonEvent);
                final String content = marshaller.marshal(serRegistry, list);
                appendToStream(sid, expectedVersion, mimeType, content, 1);
            }
        } else {
            // All events are of same type
            final String content = marshaller.marshal(serRegistry, commonEvents);
            appendToStream(sid, expectedVersion, mimeType, content, commonEvents.size());
        }

        return nextExpectedVersion;
    }

    private void appendToStream(final TenantStreamId streamId, final long expectedVersion, final EnhancedMimeType mimeType, final String content,
            final int count) throws StreamDeletedException, WrongExpectedVersionException {

        final String msg = "appendToStream(" + streamId + ", " + expectedVersion + ", " + mimeType + ", " + count + ")";
        try {
            final URI uri = new URIBuilder(url.toURI()).setPath("/streams/" + streamId).build();
            final HttpPost post = createPost(uri, expectedVersion, content);
            try {
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
                    // FOUND - Event(s) already existed and where not created
                    // again (Idempotency)
                    LOG.debug(msg + " RESPONSE: {}", response);
                    return;
                }
                if ((statusLine.getStatusCode() == 400) && !statusLine.getReasonPhrase().contains("request body invalid")) {
                    LOG.debug(msg + " RESPONSE: {}", response);
                    throw new WrongExpectedVersionException(streamId, expectedVersion, currentVersion(response));
                }
                if (statusLine.getStatusCode() == 410) {
                    // Stream was hard deleted
                    LOG.debug(msg + " RESPONSE: {}", response);
                    throw new StreamDeletedException(streamId);
                }

                LOG.debug(msg + " RESPONSE: {}", response);
                throw new RuntimeException(msg + " [Status=" + statusLine + ", Content=" + content + "]");

            } finally {
                post.reset();
            }
        } catch (final URISyntaxException | InterruptedException // NOSONAR
                | ExecutionException ex) {
            throw new RuntimeException(msg, ex);
        }

    }

    @Override
    public void deleteStream(final StreamId streamId, final long expectedVersion, final boolean hardDelete)
            throws StreamNotFoundException, StreamDeletedException, WrongExpectedVersionException {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("expectedVersion", expectedVersion, ExpectedVersion.ANY.getNo());
        ensureOpen();

        final TenantStreamId sid = new TenantStreamId(tenantId, streamId);
        if (sid.isProjection()) {
            throw new StreamReadOnlyException(sid);
        }

        final String msg = "deleteStream(" + sid + ", " + expectedVersion + ", " + hardDelete + ")";
        try {
            final URI uri = new URIBuilder(url.toURI()).setPath("/streams/" + sid).build();
            final HttpDelete delete = new HttpDelete(uri);
            try {
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
                    // returns this in header
                    LOG.debug(msg + " RESPONSE: {}", response);
                    throw new WrongExpectedVersionException(sid, expectedVersion, currentVersion(response));
                }
                if (statusLine.getStatusCode() == 410) {
                    // 410 GONE - Stream was hard deleted
                    LOG.debug(msg + " RESPONSE: {}", response);
                    throw new StreamDeletedException(sid);
                }

                LOG.debug(msg + " RESPONSE: {}", response);
                throw new RuntimeException(msg + " [Status=" + statusLine + "]");

            } finally {
                delete.reset();
            }

        } catch (final URISyntaxException | ExecutionException | InterruptedException ex) { // NOSONAR
            throw new RuntimeException(msg, ex);
        }

    }

    @Override
    public void deleteStream(final StreamId streamId, final boolean hardDelete) throws StreamNotFoundException, StreamDeletedException {
        deleteStream(streamId, ANY.getNo(), hardDelete);
    }

    @Override
    public StreamEventsSlice readEventsForward(final StreamId streamId, final long start, final int count) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("start", start, 0);
        Contract.requireArgMin("count", count, 1);
        ensureOpen();

        final TenantStreamId sid = new TenantStreamId(tenantId, streamId);
        final String msg = "readEventsForward(" + sid + ", " + start + ", " + count + ")";
        try {
            final URI uri = new URIBuilder(url.toURI()).setPath("/streams/" + sid.asString() + "/" + start + "/forward/" + count).build();
            return readEvents(sid, true, uri, start, count, msg, false);
        } catch (final IOException | URISyntaxException | InterruptedException // NOSONAR
                | ExecutionException ex) {
            throw new RuntimeException(msg, ex);
        }

    }

    @Override
    public StreamEventsSlice readEventsBackward(final StreamId streamId, final long start, final int count) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("start", start, 0);
        Contract.requireArgMin("count", count, 1);
        ensureOpen();

        final TenantStreamId sid = new TenantStreamId(tenantId, streamId);
        final String msg = "readEventsBackward(" + sid + ", " + start + ", " + count + ")";
        try {
            final URI uri = new URIBuilder(url.toURI()).setPath("/streams/" + sid.asString() + "/" + start + "/backward/" + count).build();
            return readEvents(sid, false, uri, start, count, msg, true);
        } catch (final IOException | URISyntaxException | InterruptedException // NOSONAR
                | ExecutionException ex) {
            throw new RuntimeException(msg, ex);
        }
    }

    @Override
    public CommonEvent readEvent(final StreamId streamId, final long eventNumber) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("eventNumber", eventNumber, 0);
        ensureOpen();

        final TenantStreamId sid = new TenantStreamId(tenantId, streamId);
        final String msg = "readEvent(" + sid + ", " + eventNumber + ")";
        try {
            final URI uri = new URIBuilder(url.toURI()).setPath("/streams/" + sid.asString() + "/" + eventNumber).build();
            return readEvent(uri);
        } catch (final URISyntaxException ex) {
            throw new RuntimeException(msg, ex);
        }
    }

    @Override
    public final boolean streamExists(final StreamId streamId) {

        Contract.requireArgNotNull("streamId", streamId);
        ensureOpen();

        final TenantStreamId sid = new TenantStreamId(tenantId, streamId);
        final String msg = "streamExists(" + sid + ")";
        try {
            final URI uri = new URIBuilder(url.toURI()).setPath("/streams/" + sid.asString()).build();
            LOG.debug(uri.toString());
            final HttpGet get = createHttpGet(uri);
            try {
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
                throw new RuntimeException(msg + " [Status=" + status + "]");
            } finally {
                get.reset();
            }
        } catch (final URISyntaxException | InterruptedException // NOSONAR
                | ExecutionException ex) {
            throw new RuntimeException(msg, ex);
        }

    }

    @Override
    public final StreamState streamState(final StreamId streamId) {
        Contract.requireArgNotNull("streamId", streamId);
        ensureOpen();

        final TenantStreamId sid = new TenantStreamId(tenantId, streamId);
        final String msg = "streamState(" + sid + ")";
        try {
            final URI uri = new URIBuilder(url.toURI()).setPath("/streams/" + sid.asString()).build();
            LOG.debug(uri.toString());
            final HttpGet get = createHttpGet(uri);
            try {
                final Future<HttpResponse> future = httpclient.execute(get, null);
                final HttpResponse response = future.get();
                final StatusLine status = response.getStatusLine();
                if (status.getStatusCode() == 200) {
                    LOG.debug(msg + " RESPONSE: {}", response);
                    return StreamState.ACTIVE;
                }
                if (status.getStatusCode() == 404) {
                    // May have never existed or was soft deleted...
                    // TODO Maybe the event store can send something to
                    // distinguish this?
                    LOG.debug(msg + " RESPONSE: {}", response);
                    throw new StreamNotFoundException(sid);
                }
                if (status.getStatusCode() == 410) {
                    // 410 GONE - Stream was hard deleted
                    LOG.debug(msg + " RESPONSE: {}", response);
                    return StreamState.HARD_DELETED;
                }
                LOG.debug(msg + " RESPONSE: {}", response);
                throw new RuntimeException(msg + " [Status=" + status + "]");
            } finally {
                get.reset();
            }
        } catch (final URISyntaxException | InterruptedException // NOSONAR
                | ExecutionException ex) {
            throw new RuntimeException(msg, ex);
        }
    }

    @Override
    public boolean projectionExists(final StreamId projectionId) {

        Contract.requireArgNotNull("projectionId", projectionId);
        requireProjection(projectionId);
        ensureOpen();

        final TenantStreamId pid = new TenantStreamId(tenantId, projectionId);
        final String msg = "projectionExists(" + pid + ")";
        try {
            final URI uri = new URIBuilder(url.toURI()).setPath("/projection/" + pid.asString() + "/state").build();
            LOG.debug(uri.toString());
            final HttpGet get = new HttpGet(uri);
            get.setHeader("Accept", ESEnvelopeType.JSON.getMetaType());
            try {
                final Future<HttpResponse> future = httpclient.execute(get, null);
                final HttpResponse response = future.get();
                final StatusLine status = response.getStatusLine();
                if (status.getStatusCode() == 404) {
                    return false;
                }
                if (status.getStatusCode() == 200) {
                    return true;
                }
                LOG.debug(msg + " RESPONSE: {}", response);
                throw new RuntimeException(msg + " [Status=" + status + "]");
            } finally {
                get.reset();
            }
        } catch (final URISyntaxException | InterruptedException // NOSONAR
                | ExecutionException ex) {
            throw new RuntimeException(msg, ex);
        }

    }

    @Override
    public final void enableProjection(final StreamId projectionId) throws StreamNotFoundException {
        enableDisable(new TenantStreamId(tenantId, projectionId), "enable");
    }

    @Override
    public final void disableProjection(final StreamId projectionId) throws StreamNotFoundException {
        enableDisable(new TenantStreamId(tenantId, projectionId), "disable");
    }

    private void enableDisable(final TenantStreamId projectionId, final String action) {

        Contract.requireArgNotNull("projectionId", projectionId);
        requireProjection(projectionId);
        ensureOpen();

        final String msg = action + "Projection(" + projectionId + ")";
        try {
            final URI uri = new URIBuilder(url.toURI()).setPath("/projection/" + projectionId.asString() + "/command/" + action).build();
            LOG.debug("{}", uri);
            final HttpPost post = createPost(uri, "", ESEnvelopeType.JSON);
            try {
                LOG.debug(msg + " POST: {}", post);
                final Future<HttpResponse> future = httpclient.execute(post, null);
                final HttpResponse response = future.get();
                final StatusLine status = response.getStatusLine();
                LOG.debug(msg + " RESPONSE: {}", response);
                if (status.getStatusCode() == 200) {
                    return;
                }
                if (status.getStatusCode() == 404) {
                    // 404 Not Found
                    throw new StreamNotFoundException(projectionId);
                }
                throw new RuntimeException(msg + " [Status=" + status + "]");
            } finally {
                post.reset();
            }
        } catch (final URISyntaxException | InterruptedException // NOSONAR
                | ExecutionException ex) {
            throw new RuntimeException(msg, ex);
        }
    }

    @Override
    public final void createProjection(final StreamId projectionId, final boolean enable, final TypeName... eventType)
            throws StreamAlreadyExistsException {

        Contract.requireArgNotNull("eventType", eventType);
        createProjection(projectionId, enable, Arrays.asList(eventType));

    }

    @Override
    public final void createProjection(final StreamId projectionId, final boolean enable, final List<TypeName> eventTypes)
            throws StreamAlreadyExistsException {

        Contract.requireArgNotNull("projectionId", projectionId);
        Contract.requireArgNotNull("eventTypes", eventTypes);
        requireProjection(projectionId);
        ensureOpen();

        final TenantStreamId pid = new TenantStreamId(tenantId, projectionId);
        final String msg = "createProjection(" + pid + "," + enable + type2str(eventTypes) + ")";
        try {
            final URI uri = new URIBuilder(url.toURI()).setPath("/projections/continuous").addParameter("name", pid.asString())
                    .addParameter("emit", "yes").addParameter("checkpoints", "yes").addParameter("enabled", ESHttpUtils.yesNo(enable))
                    .build();
            final String javascript = new ProjectionJavaScriptBuilder(pid).types(eventTypes).build();
            LOG.debug("{}: {}", uri, javascript);
            final HttpPost post = createPost(uri, javascript, ESEnvelopeType.JSON);
            try {
                LOG.debug(msg + " POST: {}", post);
                final Future<HttpResponse> future = httpclient.execute(post, null);
                final HttpResponse response = future.get();
                final StatusLine status = response.getStatusLine();
                LOG.debug(msg + " RESPONSE: {}", response);
                if (status.getStatusCode() == 201) {
                    // CREATED
                    return;
                }
                throw new RuntimeException(msg + " [Status=" + status + "]");
            } finally {
                post.reset();
            }
        } catch (final URISyntaxException | InterruptedException // NOSONAR
                | ExecutionException ex) {
            throw new RuntimeException(msg, ex);
        }

    }

    @Override
    public final void deleteProjection(final StreamId projectionId) throws StreamNotFoundException {

        Contract.requireArgNotNull("projectionId", projectionId);
        requireProjection(projectionId);
        ensureOpen();

        final TenantStreamId pid = new TenantStreamId(tenantId, projectionId);
        final String msg = "deleteProjection(" + pid + ")";
        try {
            final URI uri = new URIBuilder(url.toURI()).setPath("/projection/" + pid.asString())
                    .addParameter("deleteCheckpointStream", "yes").addParameter("deleteStateStream", "yes").build();
            final HttpDelete delete = new HttpDelete(uri);
            try {
                LOG.debug(msg + " DELETE: {}", delete);
                final Future<HttpResponse> future = httpclient.execute(delete, null);
                final HttpResponse response = future.get();
                final StatusLine statusLine = response.getStatusLine();
                LOG.debug(msg + " RESPONSE: {}", response);
                if (statusLine.getStatusCode() == 204) {
                    // Also delete the event stream
                    deleteStream(new SimpleStreamId(pid.asString()), false);
                    return;
                }
                if (statusLine.getStatusCode() == 404) {
                    throw new StreamNotFoundException(pid);
                }
                throw new RuntimeException(msg + " [Status=" + statusLine + "]");

            } finally {
                delete.reset();
            }

        } catch (final URISyntaxException | ExecutionException | InterruptedException ex) { // NOSONAR
            throw new RuntimeException(msg, ex);
        }

    }

    private void ensureOpen() {
        if (!open) {
            open();
        }
    }

    private StreamEventsSlice readEvents(final TenantStreamId streamId, final boolean forward, final URI uri, final long start, final int count,
            final String msg, final boolean reverseOrder) throws InterruptedException, ExecutionException, IOException {
        LOG.debug(uri.toString());
        final HttpGet get = createHttpGet(uri);
        try {
            final Future<HttpResponse> future = httpclient.execute(get, null);
            final HttpResponse response = future.get();
            final StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == 200) {
                final HttpEntity entity = response.getEntity();
                try {
                    final InputStream in = entity.getContent();
                    try {
                        final AtomFeedReader atomFeedReader = envelopeType.getAtomFeedReader();
                        final List<URI> uris = atomFeedReader.readAtomFeed(in);
                        return readEvents(forward, start, count, uris, reverseOrder);
                    } finally {
                        in.close();
                    }
                } finally {
                    EntityUtils.consume(entity);
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
        } finally {
            get.reset();
        }
    }

    private StreamEventsSlice readEvents(final boolean forward, final long fromEventNumber, final int count, final List<URI> uris,
            final boolean reverseOrder) {
        final List<CommonEvent> events = new ArrayList<>();
        if (reverseOrder) {
            for (int i = 0; i < uris.size(); i++) {
                final URI uri = uris.get(i);
                events.add(readEvent(uri));
            }
        } else {
            for (int i = uris.size() - 1; i >= 0; i--) {
                final URI uri = uris.get(i);
                events.add(readEvent(uri));
            }
        }
        final long nextEventNumber;
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
        LOG.debug(uri.toString());
        final String msg = "readEvent(" + uri + ")";
        try {
            final HttpGet get = createHttpGet(uri);
            try {
                final Future<HttpResponse> future = httpclient.execute(get, null);
                final HttpResponse response = future.get();
                final StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == 200) {
                    final HttpEntity entity = response.getEntity();
                    try {
                        final InputStream in = entity.getContent();
                        try {
                            return envelopeType.getAtomFeedReader().readEvent(desRegistry, in);
                        } finally {
                            in.close();
                        }
                    } finally {
                        EntityUtils.consume(entity);
                    }
                }
                if (statusLine.getStatusCode() == 404) {
                    // 404 Not Found
                    LOG.debug(msg + " RESPONSE: {}", response);
                    final StreamId streamId = streamId(tenantId, uri);
                    final int eventNumber = eventNumber(uri);
                    throw new EventNotFoundException(streamId, eventNumber);
                }
                throw new RuntimeException(msg + " [Status=" + statusLine + "]");
            } finally {
                get.reset();
            }
        } catch (final InterruptedException | ExecutionException // NOSONAR
                | UnsupportedOperationException | IOException ex) {
            throw new RuntimeException("Failed to read " + uri, ex);
        }
    }

    private HttpGet createHttpGet(final URI uri) {
        return createHttpGet(uri, envelopeType);
    }

    private static String streamName(final URI uri) {
        // http://127.0.0.1:2113/streams/append_diff_and_read_stream/2
        final String url = uri.toString();
        final int p1 = url.indexOf("/streams/");
        if (p1 == -1) {
            throw new IllegalStateException("Failed to extract '/streams/': " + uri);
        }
        final int p2 = url.indexOf('/', p1 + 9);
        if (p2 == -1) {
            throw new IllegalStateException("Failed to extract last '/': " + uri + " (p1=" + p1 + ")");
        }
        if (p2 < p1 + 11) {
            throw new IllegalStateException("Failed to extract name: " + uri + " (p1=" + p1 + ", p2=" + p2 + ")");
        }
        return url.substring(p1 + 9, p2);
    }

    static TenantStreamId streamId(final TenantId tenantId, final URI uri) {
        // http://127.0.0.1:2113/streams/append_diff_and_read_stream/2
        return new TenantStreamId(tenantId, new SimpleStreamId(streamName(uri)));
    }

    static int eventNumber(final URI uri) {
        // http://127.0.0.1:2113/streams/append_diff_and_read_stream/2
        final String url = uri.toString();
        final String name = streamName(uri);
        final int p = url.indexOf("/" + name + "/");
        final String str = url.substring(p + name.length() + 2);
        return Integer.valueOf(str);
    }

    private static HttpGet createHttpGet(final URI uri, final ESEnvelopeType envelopeType) {
        final HttpGet request = new HttpGet(uri);
        request.setHeader("Accept", envelopeType.getReadContentType());
        return request;
    }

    private HttpPost createPost(final URI uri, final long expectedVersion, final String content) {
        return createPost(uri, expectedVersion, content, envelopeType);
    }

    private static HttpPost createPost(final URI uri, final long expectedVersion, final String content, final ESEnvelopeType envelopeType) {
        final HttpPost post = createPost(uri, content, envelopeType);
        post.setHeader("ES-ExpectedVersion", "" + expectedVersion);
        return post;
    }

    private static HttpPost createPost(final URI uri, final String content, final ESEnvelopeType envelopeType) {
        final HttpPost post = new HttpPost(uri);
        post.setHeader("Content-Type", envelopeType.getWriteContentType() + "; charset=" + envelopeType.getMetaCharset());
        final ContentType contentType = ContentType.create(envelopeType.getMetaType(), envelopeType.getMetaCharset());
        post.setEntity(new StringEntity(content, contentType));
        return post;
    }

    static String type2str(final List<TypeName> eventTypes) {
        if (eventTypes == null) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        for (final TypeName eventType : eventTypes) {
            sb.append(",");
            sb.append(eventType.asBaseType());
        }
        return sb.toString();
    }

    static void requireProjection(final StreamId projectionId) {
        if (!projectionId.isProjection()) {
            throw new ConstraintViolationException("The stream identifier is not a projection id");
        }
    }

    static Long currentVersion(final HttpResponse response) {
        final Header header = response.getFirstHeader("CurrentVersion");
        if (header == null) {
            return null;
        }
        final String versionStr = header.getValue();
        if (versionStr == null) {
            return null;
        }
        return Long.valueOf(versionStr);
    }

    /**
     * Builder used to create a new instance of the event store.
     */
    public static final class Builder {

        private ThreadFactory threadFactory;
        
        private URL url;
        
        private ESEnvelopeType envelopeType;
        
        private SerializerRegistry serRegistry;
        
        private DeserializerRegistry desRegistry;
        
        private CredentialsProvider credentialsProvider;
        
        private TenantId tenantId;

        /**
         * Sets the thread factory.
         * 
         * @param threadFactory
         *            Factory used to create the necessary internal threads.
         * 
         * @return Builder.
         */
        public Builder threadFactory(final ThreadFactory threadFactory) {
            this.threadFactory = threadFactory;
            return this;
        }

        /**
         * Sets the event store base URL.
         * 
         * @param url
         *            Event store base URL like "http://127.0.0.1:2113/".
         * 
         * @return Builder.
         */
        public Builder url(final URL url) {
            this.url = url;
            return this;
        }

        /**
         * Sets the envelope type.
         * 
         * @param envelopeType
         *            Envelope type for reading/writing events.
         * 
         * @return Builder
         */
        public Builder envelopeType(final ESEnvelopeType envelopeType) {
            this.envelopeType = envelopeType;
            return this;
        }

        /**
         * Sets the serializer registry.
         * 
         * @param serRegistry
         *            Registry used to locate serializers.
         * 
         * @return Builder.
         */
        public Builder serRegistry(final SerializerRegistry serRegistry) {
            this.serRegistry = serRegistry;
            return this;
        }

        /**
         * Sets the deserializer registry.
         * 
         * @param desRegistry
         *            Registry used to locate deserializers.
         * 
         * @return Builder.
         */
        public Builder desRegistry(final DeserializerRegistry desRegistry) {
            this.desRegistry = desRegistry;
            return this;
        }
        
        /**
         * Sets both types of registries in one call.
         * 
         * @param registry Serializer/Deserializer registry to set.
         * 
         * @return Builder.
         */
        public Builder serDesRegistry(final SerDeserializerRegistry registry) {
            this.serRegistry = registry;
            this.desRegistry = registry;
            return this;
        }
        

        /**
         * Returns the credentials provider.
         * 
         * @param credentialsProvider
         *            Provided authentication information.
         * 
         * @return Builder.
         */
        public Builder credentialsProvider(final CredentialsProvider credentialsProvider) {
            this.credentialsProvider = credentialsProvider;
            return this;
        }

        /**
         * Sets the tenant identifier.
         * 
         * @param tenantId
         *            Unique tenant identifier.
         * 
         * @return Builder
         */
        public Builder tenantId(final TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        private void verifyNotNull(final String name, final Object value) {
            if (value == null) {
                throw new IllegalStateException("It is mandatory to set the value of '" + name + "' before calling the 'build()' method");
            }
        }

        /**
         * Creates a new instance of the event store from the attributes set via the builder.
         * 
         * @return New event store instance.
         */
        public ESHttpEventStore build() {
            verifyNotNull("threadFactory", threadFactory);
            verifyNotNull("url", url);
            verifyNotNull("envelopeType", envelopeType);
            verifyNotNull("serRegistry", serRegistry);
            verifyNotNull("desRegistry", desRegistry);
            return new ESHttpEventStore(threadFactory, url, envelopeType, serRegistry, desRegistry, credentialsProvider, tenantId);
        }

    }

}
