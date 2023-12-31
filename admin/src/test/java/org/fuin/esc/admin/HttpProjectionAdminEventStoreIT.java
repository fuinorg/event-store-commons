package org.fuin.esc.admin;

import org.fuin.esc.api.ProjectionStreamId;
import org.fuin.esc.api.TypeName;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.http.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link HttpProjectionAdminEventStore} class.
 */
class HttpProjectionAdminEventStoreIT {

    private static HttpClient httpClient;

    private HttpProjectionAdminEventStore testee;

    @BeforeAll
    static void beforeAll() {
        httpClient = HttpClient.newBuilder()
                .authenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication("admin", "changeit".toCharArray());
                    }
                })
                .build();
    }

    @BeforeEach
    void beforeEach() throws MalformedURLException {
        testee = new HttpProjectionAdminEventStore(httpClient, new URL("http://127.0.0.1:2113"));
    }

    @Test
    void testProjectionNotExists() {
        assertThat(testee.projectionExists(new ProjectionStreamId("http-test-not-existing"))).isFalse();
    }

    @Test
    void testEnableDisableProjection() {

        // GIVEN
        final ProjectionStreamId projectionId = new ProjectionStreamId("http-test-disabled");
        testee.createProjection(projectionId, false, new TypeName("one"), new TypeName("two"));

        // WHEN
        testee.enableProjection(projectionId);

        // THEN
        // TODO assertThat(testee.projectionEnabled()).isTrue();

    }

    @Test
    void testCreateAndExistsProjection() {

        // GIVEN
        final ProjectionStreamId projectionId = new ProjectionStreamId("http-test-create");
        assertThat(testee.projectionExists(projectionId)).isFalse();

        // WHEN
        testee.createProjection(projectionId, true, new TypeName("one"), new TypeName("two"));

        // THEN
        assertThat(testee.projectionExists(projectionId)).isTrue();

    }

    @Test
    void testDeleteProjection() {

        // GIVEN
        final ProjectionStreamId projectionId = new ProjectionStreamId("http-test-delete");
        testee.createProjection(projectionId, false, new TypeName("one"), new TypeName("two"));
        assertThat(testee.projectionExists(projectionId)).isTrue();

        // WHEN
        testee.deleteProjection(projectionId);

        // THEN
        assertThat(testee.projectionExists(projectionId)).isFalse();

    }

}
