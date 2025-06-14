package org.fuin.esc.esgrpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.kurrent.dbclient.CreateProjectionOptions;
import io.kurrent.dbclient.DeleteProjectionOptions;
import io.kurrent.dbclient.KurrentDBProjectionManagementClient;
import org.fuin.esc.api.ProjectionAdminEventStore;
import org.fuin.esc.api.ProjectionStreamId;
import org.fuin.esc.api.StreamAlreadyExistsException;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.TenantId;
import org.fuin.esc.api.TenantStreamId;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.spi.ProjectionJavaScriptBuilder;
import org.fuin.objects4j.common.ConstraintViolationException;
import org.fuin.objects4j.common.Contract;
import org.fuin.utils4j.TestOmitted;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * GRPC based eventstore projection admin implementation.
 */
@TestOmitted("Tested in the 'test' project")
public final class GrpcProjectionAdminEventStore implements ProjectionAdminEventStore {

    private final KurrentDBProjectionManagementClient es;

    /**
     * Constructor with mandatory data.
     *
     * @param es Connection that is maintained outside. Opening/Closing is up to the caller!
     */
    public GrpcProjectionAdminEventStore(KurrentDBProjectionManagementClient es) {
        Contract.requireArgNotNull("es", es);
        this.es = es;
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
    public boolean projectionExists(StreamId projectionId) {
        Contract.requireArgNotNull("projectionId", projectionId);
        requireProjection(projectionId);

        try {
            es.getStatus(projectionId.asString()).get();
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
    public void enableProjection(StreamId projectionId) throws StreamNotFoundException {
        Contract.requireArgNotNull("projectionId", projectionId);
        requireProjection(projectionId);

        try {
            es.enable(projectionId.asString()).get();
        } catch (final InterruptedException | ExecutionException ex) { // NOSONAR
            throw new RuntimeException("Error waiting for enable(..) result", ex);
        }
    }

    @Override
    public void disableProjection(StreamId projectionId) throws StreamNotFoundException {
        Contract.requireArgNotNull("projectionId", projectionId);
        requireProjection(projectionId);

        try {
            es.disable(projectionId.asString()).get();
        } catch (final InterruptedException | ExecutionException ex) { // NOSONAR
            throw new RuntimeException("Error waiting for disable(..) result", ex);
        }
    }

    @Override
    public void createProjection(TenantId tenantId,
                                 ProjectionStreamId projectionId,
                                 boolean enable,
                                 TypeName... eventType) throws StreamAlreadyExistsException {
        createProjection(tenantId, projectionId, enable, Arrays.asList(eventType));
    }

    @Override
    public void createProjection(TenantId tenantId,
                                 ProjectionStreamId projectionId,
                                 boolean enable,
                                 List<TypeName> eventTypes) throws StreamAlreadyExistsException {
        Contract.requireArgNotNull("projectionId", projectionId);
        requireProjection(projectionId);

        final ProjectionJavaScriptBuilder builder;
        if (tenantId == null) {
            builder = new ProjectionJavaScriptBuilder(projectionId);
        } else {
            builder = new ProjectionJavaScriptBuilder(new TenantStreamId(tenantId, projectionId));
        }
        final String javascript = builder.types(eventTypes).build();

        final TenantStreamId pid = new TenantStreamId(tenantId, projectionId);
        try {
            es.create(pid.asString(), javascript, CreateProjectionOptions.get()
                    .emitEnabled(false).trackEmittedStreams(true)).get();
        } catch (final InterruptedException | ExecutionException ex) { // NOSONAR
            if (ex.getCause() instanceof StatusRuntimeException sre
                    // TODO Are there better ways than parsing the text?
                    && sre.getStatus().getCode().equals(Status.UNKNOWN.getCode())
                    && sre.getMessage().contains("Conflict")) {
                throw new StreamAlreadyExistsException(projectionId);
            }
            throw new RuntimeException("Error waiting for create(..) result", ex);
        }
        if (enable) {
            enableProjection(pid);
        } else {
            // Workaround for https://github.com/EventStore/KurrentDB-Client-Java/issues/259 (not a perfect one...)
            disableProjection(pid);
        }
    }

    @Override
    public void deleteProjection(StreamId projectionId) throws StreamNotFoundException {
        Contract.requireArgNotNull("projectionId", projectionId);
        requireProjection(projectionId);

        disableProjection(projectionId);
        try {
            es.delete(projectionId.asString(), DeleteProjectionOptions.get().deleteCheckpointStream().deleteStateStream().deleteEmittedStreams()).get();
        } catch (final InterruptedException | ExecutionException ex) { // NOSONAR
            throw new RuntimeException("Error waiting for delete(..) result", ex);
        }

    }

    static void requireProjection(final StreamId projectionId) {
        if (!projectionId.isProjection()) {
            throw new ConstraintViolationException("The stream identifier is not a projection id");
        }
    }

}
