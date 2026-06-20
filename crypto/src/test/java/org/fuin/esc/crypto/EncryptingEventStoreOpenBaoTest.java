/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved.
 * http://www.fuin.org/
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */
package org.fuin.esc.crypto;

import com.google.gson.JsonObject;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.TypeName;
import org.fuin.objects4j.crypto.EncryptedData;
import org.fuin.objects4j.openbao.BaoEncryptedDataService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that wires the {@link EncryptingEventStore} to the real OpenBao based {@link BaoEncryptedDataService} (objects4j)
 * running in a Docker container. It verifies that event data is actually encrypted by the Transit engine at rest and transparently
 * restored on read. The test is skipped automatically when no Docker environment is available.
 */
@Testcontainers(disabledWithoutDocker = true)
class EncryptingEventStoreOpenBaoTest {

    private static final TypeName DATA_TYPE = new TypeName("MyEvent");

    private static final TypeName META_TYPE = new TypeName("MyMeta");

    private static final String KEY_ID = "key-1";

    private static final String ROOT_TOKEN = "root";

    private static final DockerImageName IMAGE = DockerImageName.parse("openbao/openbao:2.5.5");

    @Container
    private static final GenericContainer<?> OPENBAO = new GenericContainer<>(IMAGE)
            .withExposedPorts(8200)
            .withEnv("BAO_DEV_ROOT_TOKEN_ID", ROOT_TOKEN)
            .withEnv("BAO_DEV_LISTEN_ADDRESS", "0.0.0.0:8200")
            .withCommand("server", "-dev")
            .waitingFor(Wait.forHttp("/v1/sys/health").forPort(8200).forStatusCode(200));

    private static BaoEncryptedDataService service;

    private TestEventStore delegate;

    private TestRegistry registry;

    @BeforeAll
    static void setupService() throws Exception {
        final String baseUrl = "http://" + OPENBAO.getHost() + ":" + OPENBAO.getMappedPort(8200);
        service = new BaoEncryptedDataService(baseUrl, ROOT_TOKEN);
        service.createTransitEngine();
        service.createKey(KEY_ID);
    }

    @BeforeEach
    void setup() {
        delegate = new TestEventStore();
        registry = new TestRegistry();
    }

    private EncryptingEventStore.Builder builder() {
        return new EncryptingEventStore.Builder()
                .delegate(delegate)
                .serRegistry(registry)
                .desRegistry(registry)
                .encryptionService(service)
                .keyIdResolver(new FixedKeyIdResolver(KEY_ID))
                .encryptedDataFactory(new TestEncryptedDataFactory());
    }

    @Test
    void testRoundTripDataEncryptedByOpenBao() {

        // PREPARE
        final EncryptingEventStore es = builder().build();
        final StreamId streamId = new SimpleStreamId("stream-1");
        final CommonEvent original = new SimpleCommonEvent(new EventId(), DATA_TYPE,
                new TestData("secret-payload"), META_TYPE, new TestData("plain-meta"), null);

        // TEST
        es.appendToStream(streamId, original);

        // VERIFY at rest: data is a real OpenBao Transit ciphertext, meta stays in plain text by default
        final CommonEvent stored = delegate.events(streamId).get(0);
        assertThatJson(toJson(stored)).isEqualTo("""
                {
                    "id": "${json-unit.any-string}",
                    "data-type": "EscEncryptedData",
                    "data": {
                        "key-id": "key-1",
                        "key-version": "1",
                        "data-type": "MyEvent",
                        "content-type": "application/octet-stream; encoding=UTF-8",
                        "encrypted-data": "${json-unit.regex}vault:v1:.+"
                    },
                    "meta-type": "MyMeta",
                    "meta": {
                        "value": "plain-meta"
                    }
                }
                """);

        // VERIFY read back transparently decrypts and restores the original (plain text) event
        final CommonEvent read = es.readEvent(streamId, 0);
        assertThat(read.getId()).isEqualTo(original.getId());
        assertThatJson(toJson(read)).isEqualTo("""
                {
                    "id": "${json-unit.any-string}",
                    "data-type": "MyEvent",
                    "data": {
                        "value": "secret-payload"
                    },
                    "meta-type": "MyMeta",
                    "meta": {
                        "value": "plain-meta"
                    }
                }
                """);
    }

    @Test
    void testEncryptedDataAtRestAsJson() {

        // PREPARE
        final EncryptingEventStore es = builder().build();
        final StreamId streamId = new SimpleStreamId("stream-json");
        es.appendToStream(streamId, new SimpleCommonEvent(new EventId(), DATA_TYPE, new TestData("secret-payload"), null));

        // TEST - render the encrypted wrapper that the underlying store holds at rest as JSON
        final EncryptedData stored = (EncryptedData) delegate.events(streamId).get(0).getData();
        final String actualJson = toJson(stored);

        // VERIFY - the key reference and types are deterministic; only the OpenBao ciphertext is random,
        // so it is matched by a regular expression that pins down the "vault:v<keyVersion>:" envelope.
        final String expectedJson = """
                {
                    "key-id": "key-1",
                    "key-version": "1",
                    "data-type": "MyEvent",
                    "content-type": "application/octet-stream; encoding=UTF-8",
                    "encrypted-data": "${json-unit.regex}vault:v1:.+"
                }
                """;
        assertThatJson(actualJson).isEqualTo(expectedJson);
    }

    @Test
    void testEncryptMeta() {

        // PREPARE
        final EncryptingEventStore es = builder().encryptMeta(true).build();
        final StreamId streamId = new SimpleStreamId("stream-meta");
        final CommonEvent original = new SimpleCommonEvent(new EventId(), DATA_TYPE,
                new TestData("d"), META_TYPE, new TestData("m"), null);

        // TEST
        es.appendToStream(streamId, original);

        // VERIFY at rest: both data and meta are encrypted into separate ciphertext envelopes
        final CommonEvent stored = delegate.events(streamId).get(0);
        assertThatJson(toJson(stored)).isEqualTo("""
                {
                    "id": "${json-unit.any-string}",
                    "data-type": "EscEncryptedData",
                    "data": {
                        "key-id": "key-1",
                        "key-version": "1",
                        "data-type": "MyEvent",
                        "content-type": "application/octet-stream; encoding=UTF-8",
                        "encrypted-data": "${json-unit.regex}vault:v1:.+"
                    },
                    "meta-type": "EscEncryptedData",
                    "meta": {
                        "key-id": "key-1",
                        "key-version": "1",
                        "data-type": "MyMeta",
                        "content-type": "application/octet-stream; encoding=UTF-8",
                        "encrypted-data": "${json-unit.regex}vault:v1:.+"
                    }
                }
                """);

        // VERIFY read back decrypts both data and meta to plain text again
        final CommonEvent read = es.readEvent(streamId, 0);
        assertThatJson(toJson(read)).isEqualTo("""
                {
                    "id": "${json-unit.any-string}",
                    "data-type": "MyEvent",
                    "data": {
                        "value": "d"
                    },
                    "meta-type": "MyMeta",
                    "meta": {
                        "value": "m"
                    }
                }
                """);
    }

    @Test
    void testKeyRotation() throws Exception {

        // PREPARE - dedicated key so the rotation does not interfere with the other tests
        final String rotateKey = "key-rotate";
        service.createKey(rotateKey);
        final EncryptingEventStore es = builder().keyIdResolver(new FixedKeyIdResolver(rotateKey)).build();
        final StreamId streamId = new SimpleStreamId("stream-rotate");

        // TEST - append with key version 1, rotate to version 2 in OpenBao, append again
        es.appendToStream(streamId, event("v1"));
        assertThat(service.rotateKey(rotateKey)).isEqualTo("2");
        es.appendToStream(streamId, event("v2"));

        // VERIFY each ciphertext is stamped with the key version that produced it (vault:v1: / vault:v2:)
        assertThatJson(toJson(delegate.events(streamId).get(0))).isEqualTo("""
                {
                    "id": "${json-unit.any-string}",
                    "data-type": "EscEncryptedData",
                    "data": {
                        "key-id": "key-rotate",
                        "key-version": "1",
                        "data-type": "MyEvent",
                        "content-type": "application/octet-stream; encoding=UTF-8",
                        "encrypted-data": "${json-unit.regex}vault:v1:.+"
                    }
                }
                """);
        assertThatJson(toJson(delegate.events(streamId).get(1))).isEqualTo("""
                {
                    "id": "${json-unit.any-string}",
                    "data-type": "EscEncryptedData",
                    "data": {
                        "key-id": "key-rotate",
                        "key-version": "2",
                        "data-type": "MyEvent",
                        "content-type": "application/octet-stream; encoding=UTF-8",
                        "encrypted-data": "${json-unit.regex}vault:v2:.+"
                    }
                }
                """);

        // VERIFY both the old (v1) and the new (v2) ciphertext still decrypt back to plain text
        assertThatJson(toJson(es.readEvent(streamId, 0))).isEqualTo("""
                {
                    "id": "${json-unit.any-string}",
                    "data-type": "MyEvent",
                    "data": {
                        "value": "v1"
                    }
                }
                """);
        assertThatJson(toJson(es.readEvent(streamId, 1))).isEqualTo("""
                {
                    "id": "${json-unit.any-string}",
                    "data-type": "MyEvent",
                    "data": {
                        "value": "v2"
                    }
                }
                """);
    }

    private static CommonEvent event(final String payload) {
        return new SimpleCommonEvent(new EventId(), DATA_TYPE, new TestData(payload), null);
    }

    /**
     * Renders a complete event (as held by the underlying store, or as read back) to JSON. The data and meta payloads are rendered
     * according to their runtime type: an {@link EncryptedData} wrapper as the ciphertext envelope, a plain {@link TestData} as its value.
     */
    private static String toJson(final CommonEvent event) {
        final JsonObject json = new JsonObject();
        json.addProperty("id", event.getId().asString());
        json.addProperty("data-type", event.getDataType().asBaseType());
        json.add("data", toJsonValue(event.getData()));
        if (event.getMeta() != null) {
            json.addProperty("meta-type", event.getMetaType().asBaseType());
            json.add("meta", toJsonValue(event.getMeta()));
        }
        return json.toString();
    }

    private static String toJson(final EncryptedData data) {
        return toJsonValue(data).toString();
    }

    private static JsonObject toJsonValue(final Object payload) {
        final JsonObject json = new JsonObject();
        if (payload instanceof EncryptedData data) {
            json.addProperty("key-id", data.getKeyId());
            json.addProperty("key-version", data.getKeyVersion());
            json.addProperty("data-type", data.getDataType());
            json.addProperty("content-type", data.getContentType());
            json.addProperty("encrypted-data", new String(data.getEncryptedData(), StandardCharsets.UTF_8));
        } else if (payload instanceof TestData data) {
            json.addProperty("value", data.getValue());
        } else {
            throw new IllegalArgumentException("Unexpected payload type: " + payload);
        }
        return json;
    }

}
