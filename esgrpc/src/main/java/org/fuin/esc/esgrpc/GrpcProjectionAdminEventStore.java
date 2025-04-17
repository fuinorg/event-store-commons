package org.fuin.esc.esgrpc;

import com.eventstore.dbclient.CreateProjectionOptions;
import com.eventstore.dbclient.DeleteProjectionOptions;
import com.eventstore.dbclient.EventStoreDBProjectionManagementClient;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.fuin.esc.api.ProjectionAdminEventStore;
import org.fuin.esc.api.StreamAlreadyExistsException;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.TenantId;
import org.fuin.esc.api.TypeName;
import org.fuin.esc.spi.ProjectionJavaScriptBuilder;
import org.fuin.esc.spi.TenantStreamId;
import org.fuin.objects4j.common.ConstraintViolationException;
import org.fuin.objects4j.common.Contract;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * GRPC based eventstore projection admin implementation.
 */
public final class GrpcProjectionAdminEventStore implements ProjectionAdminEventStore {

    private final EventStoreDBProjectionManagementClient es;

    private final TenantId tenantId;

    /**
     * Constructor with mandatory data.
     *
     * @param es Eventstore client to use.
     */
    public GrpcProjectionAdminEventStore(EventStoreDBProjectionManagementClient es) {
        this(es, null);
    }

    /**
     * Constructor with all data.
     *
     * @param es       Eventstore client to use.
     * @param tenantId Tenant ID or {@literal null}.
     */
    public GrpcProjectionAdminEventStore(EventStoreDBProjectionManagementClient es, TenantId tenantId) {
        Contract.requireArgNotNull("es", es);
        this.es = es;
        this.tenantId = tenantId;
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

        try {
            es.getStatus(new TenantStreamId(tenantId, projectionId).asString()).get();
            return true;
        } catch (final InterruptedException | ExecutionException ex) { // NOSONAR
            if (ex.getCause() instanceof StatusRuntimeException sre) {
                if (sre.getStatus().getCode().equals(Status.UNKNOWN.getCode())
                        && sre.getMessage() != null && sre.getMessage().contains("NotFound")) {
                    return false;
                }
            }
            throw new RuntimeException("Error waiting for getStatus(..) result", ex);
        }

    }

    @Override
    public void enableProjection(StreamId projectionId) throws StreamNotFoundException {
        Contract.requireArgNotNull("projectionId", projectionId);
        requireProjection(projectionId);

        try {
            es.enable(new TenantStreamId(tenantId, projectionId).asString()).get();
        } catch (final InterruptedException | ExecutionException ex) { // NOSONAR
            throw new RuntimeException("Error waiting for enable(..) result", ex);
        }
    }

    @Override
    public void disableProjection(StreamId projectionId) throws StreamNotFoundException {
        Contract.requireArgNotNull("projectionId", projectionId);
        requireProjection(projectionId);

        try {
            es.disable(new TenantStreamId(tenantId, projectionId).asString()).get();
        } catch (final InterruptedException | ExecutionException ex) { // NOSONAR
            throw new RuntimeException("Error waiting for disable(..) result", ex);
        }
    }

    @Override
    public void createProjection(StreamId projectionId, boolean enable, TypeName... eventType) throws StreamAlreadyExistsException {
        createProjection(projectionId, enable, Arrays.asList(eventType));
    }

    @Override
    public void createProjection(StreamId projectionId, boolean enable, List<TypeName> eventTypes) throws StreamAlreadyExistsException {
        Contract.requireArgNotNull("projectionId", projectionId);
        requireProjection(projectionId);

        final TenantStreamId pid = new TenantStreamId(tenantId, projectionId);
        final String javascript = new ProjectionJavaScriptBuilder(pid).types(eventTypes).build();
        try {
            es.create(pid.asString(), javascript, CreateProjectionOptions.get().emitEnabled(false).trackEmittedStreams(true)).get();
        } catch (final InterruptedException | ExecutionException ex) { // NOSONAR
            throw new RuntimeException("Error waiting for create(..) result", ex);
        }
        if (enable) {
            enableProjection(pid);
        } else {
            // Workaround for https://github.com/EventStore/EventStoreDB-Client-Java/issues/259 (not a perfect one...)
            disableProjection(pid);
        }
    }

    @Override
    public void deleteProjection(StreamId projectionId) throws StreamNotFoundException {
        Contract.requireArgNotNull("projectionId", projectionId);
        requireProjection(projectionId);

        disableProjection(projectionId);

        final TenantStreamId pid = new TenantStreamId(tenantId, projectionId);
        try {
            es.delete(pid.asString(), DeleteProjectionOptions.get().deleteCheckpointStream().deleteStateStream().deleteEmittedStreams()).get();
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
