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
import java.util.function.Predicate;

/**
 * GRPC based eventstore projection admin implementation.
 */
@ThreadSafe
@TestOmitted("Tested in the 'test' project")
public final class GrpcProjectionAdminEventStore implements ProjectionAdminEventStore {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcProjectionAdminEventStore.class);

    /**
     * Maximum time a projection admin call may spend on retries before it reports the last failure. Also
     * covers waiting for a freshly created projection to become disableable, which is the longest of these
     * waits.
     */
    private static final Duration RETRY_BUDGET = Duration.ofSeconds(30);

    /**
     * Per-call gRPC deadline for a single disable attempt. Must be clearly shorter than
     * {@link #RETRY_BUDGET} so a call that hangs while the projection is still initializing fails fast and
     * can be retried within the overall budget.
     */
    private static final long DISABLE_CALL_DEADLINE_MILLIS = 2_000;

    /**
     * Delay schedule between retries of a projection admin call. The jitter keeps several instances that
     * start against the same store from retrying in lockstep.
     */
    private static final Backoff RETRY_BACKOFF = new Backoff(Duration.ofMillis(250), Duration.ofSeconds(2), 2.0,
            0.5, Backoff.UNLIMITED_ATTEMPTS);

    /**
     * A repetition of these operations cannot be observed, so any failure that leaves the outcome unknown
     * is worth another attempt.
     */
    private static final Predicate<ExecutionException> RETRY_IF_TRANSIENT =
            ex -> ESGrpcEventStoreSupport.statusIsConnectivityProblem(ex.getCause());

    /**
     * Repeating these operations <em>is</em> observable - a second create answers "Conflict", a second
     * delete answers "not found" - so they are only repeated when the server certainly never took the
     * request.
     */
    private static final Predicate<ExecutionException> RETRY_IF_UNREACHABLE =
            ex -> ESGrpcEventStoreSupport.statusIsUnreachable(ex.getCause());

    /**
     * A projection that has just been created is not immediately disableable: KurrentDB either answers with
     * a transient "OperationFailed" or lets the call block until the deadline (DEADLINE_EXCEEDED) until it
     * finished initializing the projection. Each attempt uses a short per-call deadline so it fails fast and
     * can be retried within the overall budget.
     */
    private static final Predicate<ExecutionException> RETRY_IF_NOT_READY_YET =
            ex -> projectionNotReadyYet(ex) || ESGrpcEventStoreSupport.statusIsConnectivityProblem(ex.getCause());

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

        final String projectionName = projectionName(projectionId);
        try {
            GrpcCalls.awaitWithRetry(() -> es.getStatus(projectionName), callTimeout, "getStatus",
                    RETRY_BUDGET, RETRY_BACKOFF, RETRY_IF_TRANSIENT);
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

        final String projectionName = projectionName(projectionId);
        try {
            // Enabling an already enabled projection is a no-op, so any transient failure may be repeated.
            GrpcCalls.awaitWithRetry(() -> es.enable(projectionName), callTimeout, "enable",
                    RETRY_BUDGET, RETRY_BACKOFF, RETRY_IF_TRANSIENT);
        } catch (final InterruptedException ex) { // NOSONAR
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted waiting for enable(..) result", ex);
        } catch (final ExecutionException ex) { // NOSONAR
            throw new RuntimeException("Error waiting for enable(..) result", ex);
        }
    }

    @Override
    public void disableProjection(ProjectionId projectionId) throws StreamNotFoundException {
        Contract.requireArgNotNull("projectionId", projectionId);

        final String projectionName = projectionName(projectionId);
        try {
            GrpcCalls.awaitWithRetry(
                    () -> es.disable(projectionName,
                            DisableProjectionOptions.get().deadline(DISABLE_CALL_DEADLINE_MILLIS)),
                    callTimeout, "disable", RETRY_BUDGET, RETRY_BACKOFF, RETRY_IF_NOT_READY_YET);
        } catch (final InterruptedException ex) { // NOSONAR
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted waiting for disable(..) result", ex);
        } catch (final ExecutionException ex) { // NOSONAR
            throw new RuntimeException("Error waiting for disable(..) result", ex);
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
            // Only retried while the server certainly never took the request - a repetition of a create that
            // did arrive answers "Conflict", which would be reported as ProjectionAlreadyExistsException.
            GrpcCalls.awaitWithRetry(() -> es.create(projectionName, javascript,
                            CreateProjectionOptions.get()
                                    .emitEnabled(true)
                                    .trackEmittedStreams(true)),
                    callTimeout, "create", RETRY_BUDGET, RETRY_BACKOFF, RETRY_IF_UNREACHABLE);
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
            // Like create: a repetition of a delete that did arrive answers "not found", so only a request
            // that certainly never reached the server is repeated.
            GrpcCalls.awaitWithRetry(() -> es.delete(projectionName,
                    DeleteProjectionOptions.get()
                            .deleteCheckpointStream()
                            .deleteStateStream()
                            .deleteEmittedStreams()), callTimeout, "delete", RETRY_BUDGET, RETRY_BACKOFF,
                    RETRY_IF_UNREACHABLE);
        } catch (final InterruptedException ex) { // NOSONAR
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted waiting for delete(..) result", ex);
        } catch (final ExecutionException ex) { // NOSONAR
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
