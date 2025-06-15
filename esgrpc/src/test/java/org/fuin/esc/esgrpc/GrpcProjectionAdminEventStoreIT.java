package org.fuin.esc.esgrpc;

import io.kurrent.dbclient.KurrentDBClientSettings;
import io.kurrent.dbclient.KurrentDBConnectionString;
import io.kurrent.dbclient.KurrentDBProjectionManagementClient;
import org.fuin.esc.api.ProjectionStreamId;
import org.fuin.esc.api.StreamAlreadyExistsException;
import org.fuin.esc.api.TypeName;
import org.fuin.utils4j.TestOmitted;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

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
                .parseOrThrow("kurrentdb://localhost:2113?tls=false");
        client = KurrentDBProjectionManagementClient.create(setts);
    }

    @BeforeEach
    void beforeEach() throws MalformedURLException {
        testee = new GrpcProjectionAdminEventStore(client);
    }

    @AfterAll
    static void afterAll() {
        client.shutdown();
        client = null;
    }

    @Test
    void testProjectionNotExists() {
        assertThat(testee.projectionExists(new ProjectionStreamId("grpc-test-not-existing" + UUID.randomUUID()))).isFalse();
    }

    @Test
    @Disabled("Creating a projection currently enables it always")
    void testEnableDisableProjection() {

        // GIVEN
        final ProjectionStreamId projectionId = new ProjectionStreamId("grpc-test-disabled-" + UUID.randomUUID());
        testee.createProjection(projectionId, false, new TypeName("one"), new TypeName("two"));

        // WHEN
        testee.enableProjection(projectionId);

        // THEN
        // TODO assertThat(testee.projectionEnabled()).isTrue();

    }

    @Test
    void testCreateAndExistsProjection() {

        // GIVEN
        final ProjectionStreamId projectionId = new ProjectionStreamId("grpc-test-create-" +  UUID.randomUUID());
        assertThat(testee.projectionExists(projectionId)).isFalse();

        // WHEN
        testee.createProjection(projectionId, true, new TypeName("one"), new TypeName("two"));

        // THEN
        await().atMost(5, SECONDS).until(() -> (testee.projectionExists(projectionId)));

    }

    @Test
    void testCreateAlreadyExistingProjection() {

        // GIVEN
        final ProjectionStreamId projectionId = new ProjectionStreamId("grpc-test-create-already-existing-" + UUID.randomUUID());
        assertThat(testee.projectionExists(projectionId)).isFalse();
        testee.createProjection(projectionId, true, new TypeName("one"));

        // WHEN - THEN
        assertThatThrownBy( () -> testee.createProjection(projectionId, true, new TypeName("one")))
                .isInstanceOf(StreamAlreadyExistsException.class);

    }

    @Test
    void testDeleteProjection() {

        // GIVEN
        final ProjectionStreamId projectionId = new ProjectionStreamId("grpc-test-delete-" + UUID.randomUUID());
        testee.createProjection(projectionId, false, new TypeName("one"), new TypeName("two"));
        await().atMost(5, SECONDS).until(() -> testee.projectionExists(projectionId));

        // WHEN
        testee.deleteProjection(projectionId);

        // THEN
        await().atMost(5, SECONDS).until(() -> !testee.projectionExists(projectionId));

    }

}
