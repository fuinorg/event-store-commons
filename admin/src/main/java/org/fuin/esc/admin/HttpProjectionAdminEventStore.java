package org.fuin.esc.admin;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.fuin.esc.api.*;
import org.fuin.esc.spi.ProjectionJavaScriptBuilder;
import org.fuin.esc.spi.TenantStreamId;
import org.fuin.objects4j.common.ConstraintViolationException;
import org.fuin.objects4j.common.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

/**
 * HTTP based eventstore projection admin implementation.
 */
public final class HttpProjectionAdminEventStore implements ProjectionAdminEventStore {

    private static final Logger LOG = LoggerFactory.getLogger(HttpProjectionAdminEventStore.class);

    private final HttpClient httpClient;

    private final URL url;

    private final TenantId tenantId;

    private final Duration timeout;

    public HttpProjectionAdminEventStore(@NotNull final HttpClient httpClient,
                                         @NotNull final URL url) {
        this(httpClient, url, null, null);
    }

    public HttpProjectionAdminEventStore(@NotNull final HttpClient httpClient,
                                         @NotNull final URL url,
                                         @Nullable final TenantId tenantId,
                                         @Nullable final Duration timeout) {
        Contract.requireArgNotNull("httpClient", httpClient);
        Contract.requireArgNotNull("url", url);
        this.httpClient = httpClient;
        this.url = url;
        this.tenantId = tenantId;
        this.timeout = timeout == null ? Duration.of(10, ChronoUnit.SECONDS) : timeout;
    }

    @Override
    public ProjectionAdminEventStore open() {
        // Do nothing
        return this;
    }

    @Override
    public void close() {
        // Do nothing
    }

    @Override
    public boolean projectionExists(StreamId projectionId) {
        Contract.requireArgNotNull("projectionId", projectionId);
        requireProjection(projectionId);

        final TenantStreamId pid = new TenantStreamId(tenantId, projectionId);
        final String msg = "projectionExists(" + pid + ")";

        final URI uri = URI.create(url.toString() + "/projection/" + pid.asString() + "/state");

        final HttpRequest request = HttpRequest.newBuilder().uri(uri)
                .setHeader("Accept", "application/json")
                .timeout(timeout)
                .GET()
                .build();
        LOG.debug("{} REQUEST: {}", msg, request);
        try {
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.debug("{} RESPONSE: {}", msg, response.statusCode());
            if (response.statusCode() == 404) {
                return false;
            }
            if (response.statusCode() == 200) {
                return true;
            }
            throw new RuntimeException(msg + " [Status=" + response.statusCode() + "]");
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(msg, ex);
        } catch (final IOException ex) {
            throw new RuntimeException(msg, ex);
        }

    }

    @Override
    public void enableProjection(StreamId projectionId) throws StreamNotFoundException {
        enableDisable(new TenantStreamId(tenantId, projectionId), "enable");
    }

    @Override
    public void disableProjection(StreamId projectionId) throws StreamNotFoundException {
        enableDisable(new TenantStreamId(tenantId, projectionId), "disable");
    }

    @Override
    public void createProjection(StreamId projectionId, boolean enable, @NotNull TypeName... eventType) throws StreamAlreadyExistsException {
        Contract.requireArgNotNull("eventType", eventType);
        createProjection(projectionId, enable, Arrays.asList(eventType));
    }

    @Override
    public void createProjection(StreamId projectionId, boolean enable, List<TypeName> eventTypes) throws StreamAlreadyExistsException {
        Contract.requireArgNotNull("projectionId", projectionId);
        Contract.requireArgNotNull("eventTypes", eventTypes);
        requireProjection(projectionId);

        final TenantStreamId pid = new TenantStreamId(tenantId, projectionId);
        final String msg = "createProjection(" + pid + "," + enable + type2str(eventTypes) + ")";


        final URI uri = URI.create(url.toString() + "/projections/continuous?name=" + pid.asString() + "&emit=yes&checkpoints=yes&enabled=" + yesNo(enable));

        final String javascript = new ProjectionJavaScriptBuilder(pid).types(eventTypes).build();
        final HttpRequest request = HttpRequest.newBuilder().uri(uri)
                .setHeader("Accept", "application/json")
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofString(javascript, StandardCharsets.UTF_8))
                .build();

        LOG.debug("{} REQUEST: {}", msg, request);

        try {
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.debug("{} RESPONSE: {}", msg, response.statusCode());
            if (response.statusCode() == 201) {
                // CREATED
                return;
            }
            throw new RuntimeException(msg + " [Status=" + response.statusCode() + "]");
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(msg, ex);
        } catch (final IOException ex) {
            throw new RuntimeException(msg, ex);
        }
    }

    @Override
    public void deleteProjection(StreamId projectionId) throws StreamNotFoundException {
        Contract.requireArgNotNull("projectionId", projectionId);
        requireProjection(projectionId);

        final TenantStreamId pid = new TenantStreamId(tenantId, projectionId);
        final String msg = "deleteProjection(" + pid + ")";

        final URI uri = URI.create(url.toString() + "/projection/" + projectionId.asString() + "?deleteCheckpointStream=yes&deleteStateStream=yes&deleteEmittedStreams=yes");

        final HttpRequest request = HttpRequest.newBuilder().uri(uri).timeout(timeout).DELETE().build();
        LOG.debug("{} DELETE: {}", msg, request);
        try {
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.debug("{} RESPONSE: {}", msg, response.statusCode());
            if (response.statusCode() == 200) {
                return;
            }
            if (response.statusCode() == 404) {
                throw new StreamNotFoundException(pid);
            }
            throw new RuntimeException(msg + " [Status=" + response.statusCode() + "]");

        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(msg, ex);
        } catch (final IOException ex) {
            throw new RuntimeException(msg, ex);
        }
    }

    private void enableDisable(final TenantStreamId projectionId, final String action) {

        Contract.requireArgNotNull("projectionId", projectionId);
        requireProjection(projectionId);

        final String msg = action + "Projection(" + projectionId + ")";

        final URI uri = URI.create(url.toString() + "/projection/" + projectionId.asString() + "/command/" + action);

        final HttpRequest request = HttpRequest.newBuilder().uri(uri)
                .setHeader("Accept", "application/json")
                .setHeader("Content-Type", "application/json; charset=utf-8")
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        LOG.debug("{} REQUEST: {}", msg, request);
        try {
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.debug("{} RESPONSE: {}", msg, response.statusCode());
            if (response.statusCode() == 200) {
                return;
            }
            if (response.statusCode() == 404) {
                throw new StreamNotFoundException(projectionId);
            }
            throw new RuntimeException(msg + " [Status=" + response.statusCode() + "]");
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(msg, ex);
        } catch (final IOException ex) {
            throw new RuntimeException(msg, ex);
        }
    }

    /**
     * Converts a boolean value to "yes" or "no".
     *
     * @param b Boolean value to convert.
     * @return String "yes" or "no".
     */
    static String yesNo(final boolean b) {
        if (b) {
            return "yes";
        }
        return "no";
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

}
