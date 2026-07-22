package org.fuin.esc.esgrpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.kurrent.dbclient.CreateProjectionOptions;
import io.kurrent.dbclient.DeleteProjectionOptions;
import io.kurrent.dbclient.DisableProjectionOptions;
import io.kurrent.dbclient.KurrentDBProjectionManagementClient;
import org.fuin.esc.api.*;
import org.fuin.esc.spi.ProjectionJavaScriptBuilder;
import org.fuin.objects4j.common.Contract;
import org.fuin.objects4j.common.ThreadSafe;
import org.fuin.utils4j.TestOmitted;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * GRPC based eventstore projection admin implementation.
 */
@ThreadSafe
@TestOmitted("Tested in the 'test' project")
public final class GrpcProjectionAdminEventStore implements ProjectionAdminEventStore {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcProjectionAdminEventStore.class);

    /** Maximum time to wait for a freshly created projection to become disableable. */
    private static final long DISABLE_TIMEOUT_MILLIS = 30_000;

    /**
     * Per-call gRPC deadline for a single disable attempt. Must be clearly shorter than
     * {@link #DISABLE_TIMEOUT_MILLIS} so a call that hangs while the projection is still
     * initializing fails fast and can be retried within the overall budget.
     */
    private static final long DISABLE_CALL_DEADLINE_MILLIS = 2_000;

    /** Delay between retries while waiting for a projection to become disableable. */
    private static final long DISABLE_RETRY_DELAY_MILLIS = 250;

    private final KurrentDBProjectionManagementClient es;

    private final TenantContext tenantContext;

    private final Duration callTimeout;

    /**
     * Constructor with mandatory data.
     *
     * @param es            Connection that is maintained outside. Opening/Closing is up to the caller!
     * @param tenantContext Optional tenant context.
     */
    public GrpcProjectionAdminEventStore(KurrentDBProjectionManagementClient es,
                                         @Nullable TenantContext tenantContext) {
        this(es, tenantContext, GrpcCalls.DEFAULT_CALL_TIMEOUT);
    }

    /**
     * Constructor with call timeout.
     *
     * @param es            Connection that is maintained outside. Opening/Closing is up to the caller!
     * @param tenantContext Optional tenant context.
     * @param callTimeout   Maximum time to wait for a single call. Without a timeout a call whose future
     *                      is never completed blocks the calling thread forever.
     */
    public GrpcProjectionAdminEventStore(KurrentDBProjectionManagementClient es,
                                         @Nullable TenantContext tenantContext,
                                         Duration callTimeout) {
        Contract.requireArgNotNull("es", es);
        Contract.requireArgNotNull("callTimeout", callTimeout);
        this.es = es;
        this.tenantContext = tenantContext == null ? new TenantContext.NoopTenantContext() : tenantContext;
        this.callTimeout = callTimeout;
    }

    @Override
    public ProjectionAdminEventStore open() {
        // Do nothing - We assume that the eventstore is already
        // fully initialized when passed in to constructor
        return this;
    }

    @Override
    public void close() {
        // Do nothing - Connection is handled outside
    }

    @Override
    public boolean projectionExists(ProjectionId projectionId) {
        Contract.requireArgNotNull("projectionId", projectionId);

        try {
            GrpcCalls.await(es.getStatus(projectionName(projectionId)), callTimeout, "getStatus");
            return true;
        } catch (final InterruptedException | ExecutionException ex) { // NOSONAR
            if (ex.getCause() instanceof StatusRuntimeException sre
                    && sre.getStatus().getCode().equals(Status.NOT_FOUND.getCode())) {
                return false;
            }

            throw new RuntimeException("Error waiting for getStatus(..) result", ex);
        }

    }

    @Override
    public void enableProjection(ProjectionId projectionId) throws StreamNotFoundException {
        Contract.requireArgNotNull("projectionId", projectionId);

        try {
            GrpcCalls.await(es.enable(projectionName(projectionId)), callTimeout, "enable");
        } catch (final InterruptedException | ExecutionException ex) { // NOSONAR
            throw new RuntimeException("Error waiting for enable(..) result", ex);
        }
    }

    @Override
    public void disableProjection(ProjectionId projectionId) throws StreamNotFoundException {
        Contract.requireArgNotNull("projectionId", projectionId);

        final String projectionName = projectionName(projectionId);
        final long deadline = System.currentTimeMillis() + DISABLE_TIMEOUT_MILLIS;
        while (true) {
            try {
                GrpcCalls.await(es.disable(projectionName,
                        DisableProjectionOptions.get().deadline(DISABLE_CALL_DEADLINE_MILLIS)), callTimeout, "disable");
                return;
            } catch (final InterruptedException ex) { // NOSONAR
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted waiting for disable(..) result", ex);
            } catch (final ExecutionException ex) { // NOSONAR
                // A projection that has just been created is not immediately disableable: KurrentDB
                // either answers with a transient "OperationFailed" or lets the call block until the
                // deadline (DEADLINE_EXCEEDED) until it finished initializing the projection. Each
                // attempt uses a short per-call deadline so it fails fast and we can retry for a
                // bounded time before giving up.
                if (projectionNotReadyYet(ex) && System.currentTimeMillis() < deadline) {
                    LOG.debug("Projection '{}' not ready to be disabled yet, retrying...", projectionName);
                    sleepQuietly(DISABLE_RETRY_DELAY_MILLIS);
                    continue;
                }
                throw new RuntimeException("Error waiting for disable(..) result", ex);
            }
        }
    }

    private static boolean projectionNotReadyYet(final ExecutionException ex) {
        if (!(ex.getCause() instanceof StatusRuntimeException sre)) {
            return false;
        }
        final Status.Code code = sre.getStatus().getCode();
        // The call blocked until the per-call deadline because the projection is still initializing.
        if (code.equals(Status.DEADLINE_EXCEEDED.getCode())) {
            return true;
        }
        // The server rejected the disable with a transient failure while still initializing.
        return code.equals(Status.UNKNOWN.getCode())
                && sre.getMessage() != null
                && sre.getMessage().contains("OperationFailed");
    }

    private static void sleepQuietly(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException ex) { // NOSONAR
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void createProjection(ProjectionId projectionId,
                                 ProjectionStreamId targetStreamId,
                                 boolean enable,
                                 List<TypeName> eventTypes,
                                 List<String> categoryNames) throws ProjectionAlreadyExistsException {
        Contract.requireArgNotNull("projectionId", projectionId);

        final String projectionName = projectionName(projectionId);
        LOG.info("Create projection '{}' with stream '{}' listening to events: {} / categories: {}", projectionName,
                targetStreamId, eventTypes, categoryNames);

        final ProjectionJavaScriptBuilder builder = new ProjectionJavaScriptBuilder(
                tenantContext.getTenantId().orElse(null),
                targetStreamId);
        final String javascript = builder.types(eventTypes).categories(categoryNames).build();

        try {
            GrpcCalls.await(es.create(projectionName, javascript,
                            CreateProjectionOptions.get()
                                    .emitEnabled(true)
                                    .trackEmittedStreams(true)),
                    callTimeout, "create");
        } catch (final InterruptedException | ExecutionException ex) { // NOSONAR
            if (ex.getCause() instanceof StatusRuntimeException sre
                    // TODO Are there better ways than parsing the text?
                    && sre.getStatus().getCode().equals(Status.UNKNOWN.getCode())
                    && sre.getMessage() != null && sre.getMessage().contains("Conflict")) {
                throw new ProjectionAlreadyExistsException(projectionId);
            }
            throw new RuntimeException("Error waiting for create(..) result", ex);
        }
        if (enable) {
            enableProjection(projectionId);
        } else {
            // Workaround for https://github.com/EventStore/KurrentDB-Client-Java/issues/259 (not a perfect one...)
            disableProjection(projectionId);
        }
    }

    @Override
    public void deleteProjection(ProjectionId projectionId) throws StreamNotFoundException {
        Contract.requireArgNotNull("projectionId", projectionId);

        final String projectionName = projectionName(projectionId);
        LOG.info("Delete projection '{}'", projectionName);

        disableProjection(projectionId);
        try {
            GrpcCalls.await(es.delete(projectionName,
                    DeleteProjectionOptions.get()
                            .deleteCheckpointStream()
                            .deleteStateStream()
                            .deleteEmittedStreams()), callTimeout, "delete");
        } catch (final InterruptedException | ExecutionException ex) { // NOSONAR
            throw new RuntimeException("Error waiting for delete(..) result", ex);
        }

    }

    private String projectionName(ProjectionId projectionId) {
        if (tenantContext == null) {
            return projectionId.getName();
        }
        return tenantContext.getTenantId()
                .map(id -> id.asString() + "-" + projectionId.getName())
                .orElse(projectionId.getName());
    }

}
