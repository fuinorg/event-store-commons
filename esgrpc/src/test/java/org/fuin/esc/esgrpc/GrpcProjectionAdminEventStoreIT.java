package org.fuin.esc.esgrpc;

import io.kurrent.dbclient.KurrentDBClientSettings;
import io.kurrent.dbclient.KurrentDBConnectionString;
import io.kurrent.dbclient.KurrentDBProjectionManagementClient;
import org.fuin.esc.api.ProjectionAlreadyExistsException;
import org.fuin.esc.api.ProjectionId;
import org.fuin.esc.api.ProjectionStreamId;
import org.fuin.esc.api.TypeName;
import org.fuin.utils4j.TestOmitted;
import org.junit.jupiter.api.*;

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
        testee = new GrpcProjectionAdminEventStore(client, null);
    }

    @AfterAll
    static void afterAll() {
        client.shutdown();
        client = null;
    }

    @Test
    void testProjectionNotExists() {
        assertThat(testee.projectionExists(new ProjectionId("grpc-test-not-existing" + UUID.randomUUID()))).isFalse();
    }

    @Test
    @Disabled("Creating a projection currently enables it always")
    void testEnableDisableProjection() {

        // GIVEN
        final String name = "grpc-test-disabled-" + UUID.randomUUID();
        final ProjectionId projectionId = new ProjectionId(name + "-projection");
        final ProjectionStreamId streamId = new ProjectionStreamId(name + "-stream");
        testee.createProjection(projectionId, streamId, false, new TypeName("one"), new TypeName("two"));

        // WHEN
        testee.enableProjection(projectionId);

        // THEN
        // TODO assertThat(testee.projectionEnabled()).isTrue();

    }

    @Test
    void testCreateAndExistsProjection() {

        // GIVEN
        final String name = "grpc-test-create-" + UUID.randomUUID();
        final ProjectionId projectionId = new ProjectionId(name);
        final ProjectionStreamId streamId = new ProjectionStreamId(name + "-stream");
        assertThat(testee.projectionExists(projectionId)).isFalse();

        // WHEN
        testee.createProjection(projectionId, streamId, true, new TypeName("one"), new TypeName("two"));

        // THEN
        await().atMost(5, SECONDS).until(() -> (testee.projectionExists(projectionId)));

    }

    @Test
    void testCreateAlreadyExistingProjection() {

        // GIVEN
        final String name = "grpc-test-create-already-existing-" + UUID.randomUUID();
        final ProjectionId projectionId = new ProjectionId(name);
        final ProjectionStreamId streamId = new ProjectionStreamId(name + "-stream");
        assertThat(testee.projectionExists(projectionId)).isFalse();
        testee.createProjection(projectionId, streamId, true, new TypeName("one"));

        // WHEN - THEN
        assertThatThrownBy( () -> testee.createProjection(projectionId, streamId,true, new TypeName("one")))
                .isInstanceOf(ProjectionAlreadyExistsException.class);

    }

    @Test
    void testDeleteProjection() {

        // GIVEN
        final String name = "grpc-test-delete-" + UUID.randomUUID();
        final ProjectionId projectionId = new ProjectionId(name);
        final ProjectionStreamId streamId = new ProjectionStreamId(name + "-stream");
        testee.createProjection(projectionId, streamId, false, new TypeName("one"), new TypeName("two"));
        await().atMost(5, SECONDS).until(() -> testee.projectionExists(projectionId));

        // WHEN
        testee.deleteProjection(projectionId);

        // THEN
        await().atMost(5, SECONDS).until(() -> !testee.projectionExists(projectionId));

    }

}
