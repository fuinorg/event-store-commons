package org.fuin.esc.esgrpc;

import io.kurrent.dbclient.KurrentDBClient;
import io.kurrent.dbclient.KurrentDBConnectionString;
import jakarta.json.bind.JsonbConfig;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.EscConnectionException;
import org.fuin.esc.api.EventId;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.SerDeserializerRegistry;
import org.fuin.esc.api.SerializedDataTypeRegistry;
import org.fuin.esc.api.SimpleCommonEvent;
import org.fuin.esc.api.SimpleSerializedDataTypeRegistry;
import org.fuin.esc.api.SimpleSerializerDeserializerRegistry;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.jsonb.BaseTypeFactory;
import org.fuin.esc.jsonb.EscJsonbUtils;
import org.fuin.esc.jsonb.JsonbSerDeserializer;
import org.fuin.objects4j.jsonb.JsonbProvider;
import org.fuin.utils4j.TestOmitted;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * Fault-injection test for the gRPC event store: it drives a <b>real</b> KurrentDB through a
 * {@link FaultInjectingProxy} and then takes the store away mid-flight.
 * <p>
 * These are the guarantees the resilience work rests on and that a unit test with a mocked client cannot
 * establish, because they depend on how the real gRPC client behaves when its connection dies:
 * <ul>
 *     <li>a call <b>fails</b> instead of hanging when the store stops answering,</li>
 *     <li>the failure is an {@link EscConnectionException} and not a bare {@link RuntimeException},</li>
 *     <li>{@code streamExists(..)} does not turn "no answer" into the business answer {@code false},</li>
 *     <li>the store works again once the connection is restored.</li>
 * </ul>
 * Requires the KurrentDB started by the docker-maven-plugin in the {@code pre-integration-test} phase.
 */
@TestOmitted("Integration test - it has no separate '*Test' counterpart")
class ESGrpcFaultInjectionIT {

    /** Short on purpose: the point is that a broken connection fails fast rather than hanging. */
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(2);

    /**
     * Generous upper bound for "did not hang". A call must fail somewhere around {@link #CALL_TIMEOUT},
     * but the assertion is deliberately not tight - the gRPC client may notice the dead connection sooner
     * and fail with {@code UNAVAILABLE}, which is just as correct an answer as the client-side timeout.
     */
    private static final long MUST_FAIL_WITHIN_MILLIS = 30_000;

    private FaultInjectingProxy proxy;

    private KurrentDBClient client;

    private ESGrpcEventStore testee;

    @BeforeEach
    void beforeEach() throws IOException {
        proxy = FaultInjectingProxy.to("localhost", 2113);
        client = KurrentDBClient.create(KurrentDBConnectionString
                .parseOrThrow("kurrentdb://localhost:" + proxy.port() + "?tls=false"));

        final EnhancedMimeType jsonUtf8 = EnhancedMimeType.create("application", "json", StandardCharsets.UTF_8);
        final SerializedDataTypeRegistry typeRegistry = EscJsonbUtils
                .addEscTypes(new SimpleSerializedDataTypeRegistry.Builder())
                .add(MyEvent.SER_TYPE, MyEvent.class)
                .build();
        final JsonbConfig jsonbConfig = TestUtils.createJsonbConfig();
        final JsonbProvider jsonbProvider = new JsonbProvider(jsonbConfig);
        final JsonbSerDeserializer serDeser = TestUtils.createSerDeserializer(jsonbProvider, typeRegistry);
        final SerDeserializerRegistry registry = EscJsonbUtils
                .addEscSerDeserializer(new SimpleSerializerDeserializerRegistry.Builder(jsonUtf8), serDeser)
                .add(MyEvent.SER_TYPE, serDeser, serDeser.getMimeType())
                .build();
        TestUtils.register(jsonbConfig, registry, registry);

        testee = new ESGrpcEventStore.Builder()
                .eventStore(client)
                .serDesRegistry(registry)
                .baseTypeFactory(new BaseTypeFactory())
                .targetContentType(jsonUtf8)
                .callTimeout(CALL_TIMEOUT)
                .build();
        testee.open();
    }

    @AfterEach
    void afterEach() {
        if (testee != null) {
            testee.close();
        }
        if (client != null) {
            client.shutdown();
        }
        if (proxy != null) {
            proxy.close();
        }
    }

    @Test
    void appendFailsFastWhenTheStoreStopsAnswering() {

        // PREPARE: prove the wiring works before breaking it
        final StreamId streamId = newStreamId("fi-append");
        testee.appendToStream(streamId, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(), event("before"));

        // TEST: the request is sent but nothing ever comes back - this used to block the caller forever
        proxy.blackhole();
        final long start = System.currentTimeMillis();
        assertThatThrownBy(() -> testee.appendToStream(streamId, 0, event("during outage")))
                .isInstanceOf(EventStoreCallTimeoutException.class)
                .isInstanceOf(EscConnectionException.class);
        final long elapsed = System.currentTimeMillis() - start;

        // VERIFY: it really did wait for the answer and then gave up - had the call failed for some other
        // reason it would have returned well before the timeout, and had F3 not bounded it, never
        assertThat(elapsed).isGreaterThanOrEqualTo(CALL_TIMEOUT.toMillis());
        assertThat(elapsed).isLessThan(MUST_FAIL_WITHIN_MILLIS);
    }

    @Test
    void readFailsWithATypedExceptionWhenTheConnectionIsCut() {

        // PREPARE
        final StreamId streamId = newStreamId("fi-read");
        testee.appendToStream(streamId, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(), event("one"));

        // TEST
        proxy.cut();

        // VERIFY: a consumer classifies this with one instanceof - not with string matching on a
        // RuntimeException, which is what the catch-all used to force
        final long start = System.currentTimeMillis();
        assertThatThrownBy(() -> testee.readEventsForward(streamId, 0, 10))
                .isInstanceOf(EscConnectionException.class);
        assertThat(System.currentTimeMillis() - start).isLessThan(MUST_FAIL_WITHIN_MILLIS);
    }

    @Test
    void streamExistsDoesNotReportAnOutageAsANonExistingStream() {

        // PREPARE: a stream that definitely exists
        final StreamId streamId = newStreamId("fi-exists");
        testee.appendToStream(streamId, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(), event("one"));
        assertThat(testee.streamExists(streamId)).isTrue();

        // TEST: the store goes away
        proxy.cut();

        // VERIFY: this is the F2 bug - returning false here would tell the caller "the stream is gone"
        // when the truth is "I could not ask", and a caller acting on that would recreate the stream
        assertThatThrownBy(() -> testee.streamExists(streamId))
                .isInstanceOf(EscConnectionException.class);
    }

    @Test
    void streamExistsStillAnswersFalseForAStreamThatIsReallyMissing() {

        // A healthy store must keep answering the business question, or the fix above would have made
        // streamExists useless.
        assertThat(testee.streamExists(newStreamId("fi-never-written"))).isFalse();
    }

    @Test
    void recoversWhenTheStoreComesBack() {

        // PREPARE
        final StreamId streamId = newStreamId("fi-recover");
        testee.appendToStream(streamId, ExpectedVersion.NO_OR_EMPTY_STREAM.getNo(), event("one"));
        proxy.cut();
        assertThatThrownBy(() -> testee.readEventsForward(streamId, 0, 10))
                .isInstanceOf(EscConnectionException.class);

        // TEST: the outage ends
        proxy.forward();

        // VERIFY: the store is usable again once the client reconnected (it backs off before retrying,
        // so the first attempt after the restore may still fail)
        await().atMost(60, SECONDS).ignoreException(EscConnectionException.class).until(() -> {
            final StreamEventsSlice slice = testee.readEventsForward(streamId, 0, 10);
            return slice.getEvents().size() == 1;
        });

        // VERIFY: and writes work again too
        testee.appendToStream(streamId, 0, event("after recovery"));
        assertThat(testee.readEventsForward(streamId, 0, 10).getEvents()).hasSize(2);
    }

    private static StreamId newStreamId(final String prefix) {
        return new SimpleStreamId(prefix + "-" + UUID.randomUUID());
    }

    private static CommonEvent event(final String description) {
        return new SimpleCommonEvent(new EventId(), MyEvent.TYPE, new MyEvent(description), null);
    }

}
