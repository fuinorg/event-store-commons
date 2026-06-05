package org.fuin.esc.esgrpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.kurrent.dbclient.CreateProjectionOptions;
import io.kurrent.dbclient.DeleteProjectionOptions;
import io.kurrent.dbclient.KurrentDBProjectionManagementClient;
import org.jspecify.annotations.Nullable;
import org.fuin.esc.api.ProjectionAdminEventStore;
import org.fuin.esc.api.ProjectionAlreadyExistsException;
import org.fuin.esc.api.ProjectionId;
import org.fuin.esc.api.ProjectionStreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.TenantContext;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.spi.ProjectionJavaScriptBuilder;
import org.fuin.objects4j.common.Contract;
import org.fuin.utils4j.TestOmitted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * GRPC based eventstore projection admin implementation.
 */
@TestOmitted("Tested in the 'test' project")
public final class GrpcProjectionAdminEventStore implements ProjectionAdminEventStore {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcProjectionAdminEventStore.class);

    /** Maximum time to wait for a freshly created projection to become disableable. */
    private static final long DISABLE_TIMEOUT_MILLIS = 10_000;

    /** Delay between retries while waiting for a projection to become disableable. */
    private static final long DISABLE_RETRY_DELAY_MILLIS = 250;

    private final KurrentDBProjectionManagementClient es;

    private final TenantContext tenantContext;

    /**
     * Constructor with mandatory data.
     *
     * @param es            Connection that is maintained outside. Opening/Closing is up to the caller!
     * @param tenantContext Optional tenant context.
     */
    public GrpcProjectionAdminEventStore(KurrentDBProjectionManagementClient es,
                                         @Nullable TenantContext tenantContext) {
        Contract.requireArgNotNull("es", es);
        this.es = es;
        this.tenantContext = tenantContext == null ? new TenantContext.NoopTenantContext() : tenantContext;
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
            es.getStatus(projectionName(projectionId)).get();
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
            es.enable(projectionName(projectionId)).get();
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
                es.disable(projectionName).get();
                return;
            } catch (final InterruptedException ex) { // NOSONAR
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted waiting for disable(..) result", ex);
            } catch (final ExecutionException ex) { // NOSONAR
                // A projection that has just been created is not immediately disableable: KurrentDB
                // answers with a transient "OperationFailed" until it finished initializing the
                // projection. Retry for a bounded time before giving up.
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
        return ex.getCause() instanceof StatusRuntimeException sre
                && sre.getStatus().getCode().equals(Status.UNKNOWN.getCode())
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
                                 List<TypeName> eventTypes) throws ProjectionAlreadyExistsException {
        Contract.requireArgNotNull("projectionId", projectionId);

        final String projectionName = projectionName(projectionId);
        LOG.info("Create projection '{}' with stream '{}' listening to events: {}", projectionName,  targetStreamId, eventTypes);

        final ProjectionJavaScriptBuilder builder = new ProjectionJavaScriptBuilder(
                tenantContext.getTenantId().orElse(null),
                targetStreamId);
        final String javascript = builder.types(eventTypes).build();

        try {
            es.create(projectionName, javascript,
                            CreateProjectionOptions.get()
                                    .emitEnabled(true)
                                    .trackEmittedStreams(true))
                    .get();
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
            es.delete(projectionName,
                    DeleteProjectionOptions.get()
                            .deleteCheckpointStream()
                            .deleteStateStream()
                            .deleteEmittedStreams()).get();
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
