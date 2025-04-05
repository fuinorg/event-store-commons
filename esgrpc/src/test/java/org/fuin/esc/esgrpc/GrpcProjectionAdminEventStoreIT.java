package org.fuin.esc.esgrpc;

import io.kurrent.dbclient.KurrentDBClientSettings;
import io.kurrent.dbclient.KurrentDBConnectionString;
import io.kurrent.dbclient.KurrentDBProjectionManagementClient;
import org.fuin.esc.api.ProjectionStreamId;
import org.fuin.esc.api.TypeName;
import org.fuin.utils4j.TestOmitted;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link GrpcProjectionAdminEventStore} class.
 */
@TestOmitted("This is only a test class")
@SuppressWarnings("java:S2187")
class GrpcProjectionAdminEventStoreIT {

    private static KurrentDBProjectionManagementClient client;

    private GrpcProjectionAdminEventStore testee;

    @BeforeAll
    static void beforeAll() {
        final KurrentDBClientSettings setts = KurrentDBConnectionString
                .parseOrThrow("esdb://localhost:2113?tls=false");
        client = KurrentDBProjectionManagementClient.create(setts);
    }

    @BeforeEach
    void beforeEach() throws MalformedURLException {
        testee = new GrpcProjectionAdminEventStore(client, null);
    }

    @Test
    void testProjectionNotExists() {
        assertThat(testee.projectionExists(new ProjectionStreamId("grpc-test-not-existing"))).isFalse();
    }

    @Test
    void testEnableDisableProjection() {

        // GIVEN
        final ProjectionStreamId projectionId = new ProjectionStreamId("grpc-test-disabled");
        testee.createProjection(projectionId, false, new TypeName("one"), new TypeName("two"));

        // WHEN
        testee.enableProjection(projectionId);

        // THEN
        // TODO assertThat(testee.projectionEnabled()).isTrue();

    }

    @Test
    void testCreateAndExistsProjection() {

        // GIVEN
        final ProjectionStreamId projectionId = new ProjectionStreamId("grpc-test-create");
        assertThat(testee.projectionExists(projectionId)).isFalse();

        // WHEN
        testee.createProjection(projectionId, true, new TypeName("one"), new TypeName("two"));

        // THEN
        assertThat(testee.projectionExists(projectionId)).isTrue();

    }

    @Test
    void testDeleteProjection() {

        // GIVEN
        final ProjectionStreamId projectionId = new ProjectionStreamId("grpc-test-delete");
        testee.createProjection(projectionId, false, new TypeName("one"), new TypeName("two"));
        assertThat(testee.projectionExists(projectionId)).isTrue();

        // WHEN
        testee.deleteProjection(projectionId);

        // THEN
        assertThat(testee.projectionExists(projectionId)).isFalse();

    }

}
