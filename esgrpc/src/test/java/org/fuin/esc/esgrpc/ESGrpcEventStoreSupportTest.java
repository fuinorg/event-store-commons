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
package org.fuin.esc.esgrpc;

import io.grpc.Status;
import org.fuin.esc.api.EscConnectionException;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.ProjectionStreamId;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamReadOnlyException;
import org.fuin.esc.api.TenantStreamId;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Test for the pure helper logic of {@link ESGrpcEventStoreSupport}. The converter-backed and
 * gRPC-client-backed methods are covered end-to-end in the 'test' project.
 */
public final class ESGrpcEventStoreSupportTest {

    @Test
    public void testVersion2StateReturnsAStateForEachKind() {

        // The concrete gRPC state semantics are verified end-to-end; here we only ensure every
        // expected-version kind is mapped to a non-null state (i.e. all branches are reachable).
        assertThat(ESGrpcEventStoreSupport.version2State(ExpectedVersion.ANY.getNo())).isNotNull();
        assertThat(ESGrpcEventStoreSupport.version2State(ExpectedVersion.NO_OR_EMPTY_STREAM.getNo())).isNotNull();
        assertThat(ESGrpcEventStoreSupport.version2State(5L)).isNotNull();
    }

    @Test
    public void testStatusIsDeletedForDeletedFailedPrecondition() {

        // PREPARE
        final Throwable cause = Status.FAILED_PRECONDITION
                .withDescription("Event stream 'foo' is deleted.").asRuntimeException();

        // TEST & VERIFY
        assertThat(ESGrpcEventStoreSupport.statusIsDeleted(cause)).isTrue();
    }

    @Test
    public void testStatusIsDeletedForOtherStatus() {

        // PREPARE
        final Throwable cause = Status.UNAVAILABLE.withDescription("Server down").asRuntimeException();

        // TEST & VERIFY
        assertThat(ESGrpcEventStoreSupport.statusIsDeleted(cause)).isFalse();
    }

    @Test
    public void testStatusIsDeletedForUnrelatedCause() {
        assertThat(ESGrpcEventStoreSupport.statusIsDeleted(new IllegalStateException("boom"))).isFalse();
        assertThat(ESGrpcEventStoreSupport.statusIsDeleted(null)).isFalse();
    }

    @Test
    public void testEnsureStreamNoProjectionForProjection() {
        assertThatThrownBy(() -> ESGrpcEventStoreSupport.ensureStreamNoProjection(new ProjectionStreamId("$ce-foo")))
                .isInstanceOf(StreamReadOnlyException.class);
    }

    @Test
    public void testEnsureStreamNoProjectionForRegularStream() {
        // Must not throw for a non projection stream
        ESGrpcEventStoreSupport.ensureStreamNoProjection(new SimpleStreamId("foo"));
    }

    @Test
    public void testMapExceptionForDeletedStatus() {

        // PREPARE
        final TenantStreamId sid = new TenantStreamId(null, new SimpleStreamId("foo"));
        final Throwable cause = Status.FAILED_PRECONDITION
                .withDescription("Event stream 'foo' is deleted.").asRuntimeException();

        // TEST
        final RuntimeException result = ESGrpcEventStoreSupport.mapException(cause, sid, ExpectedVersion.ANY.getNo());

        // VERIFY
        assertThat(result).isInstanceOf(StreamDeletedException.class);
    }

    @Test
    public void testMapExceptionForUnknownCause() {

        // PREPARE
        final TenantStreamId sid = new TenantStreamId(null, new SimpleStreamId("foo"));
        final Throwable cause = new IllegalStateException("boom");

        // TEST
        final RuntimeException result = ESGrpcEventStoreSupport.mapException(cause, sid, ExpectedVersion.ANY.getNo());

        // VERIFY
        assertThat(result).isExactlyInstanceOf(RuntimeException.class);
        assertThat(result.getCause()).isSameAs(cause);
    }

    @Test
    public void testMapExceptionForNullCause() {
        final TenantStreamId sid = new TenantStreamId(null, new SimpleStreamId("foo"));
        final Throwable result = catchThrowable(() -> {
            throw ESGrpcEventStoreSupport.mapException(null, sid, ExpectedVersion.ANY.getNo());
        });
        assertThat(result).isExactlyInstanceOf(RuntimeException.class);
    }


    @Test
    void testStatusIsConnectivityProblem() {
        assertThat(ESGrpcEventStoreSupport.statusIsConnectivityProblem(
                Status.UNAVAILABLE.asRuntimeException())).isTrue();
        assertThat(ESGrpcEventStoreSupport.statusIsConnectivityProblem(
                Status.DEADLINE_EXCEEDED.asRuntimeException())).isTrue();
        assertThat(ESGrpcEventStoreSupport.statusIsConnectivityProblem(
                Status.RESOURCE_EXHAUSTED.asRuntimeException())).isTrue();
        assertThat(ESGrpcEventStoreSupport.statusIsConnectivityProblem(
                Status.ABORTED.asRuntimeException())).isTrue();
        assertThat(ESGrpcEventStoreSupport.statusIsConnectivityProblem(new InterruptedException())).isTrue();

        // A business answer is not a connectivity problem.
        assertThat(ESGrpcEventStoreSupport.statusIsConnectivityProblem(
                Status.NOT_FOUND.asRuntimeException())).isFalse();
        assertThat(ESGrpcEventStoreSupport.statusIsConnectivityProblem(
                Status.FAILED_PRECONDITION.asRuntimeException())).isFalse();
        assertThat(ESGrpcEventStoreSupport.statusIsConnectivityProblem(new IllegalStateException())).isFalse();
        assertThat(ESGrpcEventStoreSupport.statusIsConnectivityProblem(null)).isFalse();
    }

    @Test
    void testDeletedIsNeitherNotFoundNorConnectivity() {
        // A hard deleted stream reports FAILED_PRECONDITION. streamExists(..) must answer "false" for it
        // (it does not exist any more), so it must be recognised as a business answer, not as a
        // connectivity problem - getting this wrong makes "read after hard delete" throw.
        final io.grpc.StatusRuntimeException deleted = io.grpc.Status.FAILED_PRECONDITION
                .withDescription("Event stream 'MyStream' is deleted.").asRuntimeException();

        assertThat(ESGrpcEventStoreSupport.statusIsDeleted(deleted)).isTrue();
        assertThat(ESGrpcEventStoreSupport.statusIsNotFound(deleted)).isFalse();
        assertThat(ESGrpcEventStoreSupport.statusIsConnectivityProblem(deleted)).isFalse();
    }

    @Test
    void testStatusIsNotFound() {
        assertThat(ESGrpcEventStoreSupport.statusIsNotFound(Status.NOT_FOUND.asRuntimeException())).isTrue();
        assertThat(ESGrpcEventStoreSupport.statusIsNotFound(Status.UNAVAILABLE.asRuntimeException())).isFalse();
        assertThat(ESGrpcEventStoreSupport.statusIsNotFound(null)).isFalse();
    }

    @Test
    void testMapExceptionConnectivity() {
        // A store that cannot be reached must be mapped to the transient type, not to the catch-all
        // RuntimeException, so retry / circuit breaker predicates recognise it.
        final TenantStreamId sid = new TenantStreamId(null, new SimpleStreamId("MyStream"));

        final RuntimeException result = ESGrpcEventStoreSupport.mapException(
                Status.UNAVAILABLE.asRuntimeException(), sid, ExpectedVersion.ANY.getNo());

        assertThat(result).isInstanceOf(EscConnectionException.class);
    }

    @Test
    void testMapExceptionKeepsAnAlreadyClassifiedFailure() {

        // PREPARE: this is what the asynchronous store sees once GrpcCalls.within(..) bounded the call.
        final TenantStreamId sid = new TenantStreamId(null, new SimpleStreamId("foo"));
        final EventStoreCallTimeoutException cause = new EventStoreCallTimeoutException(
                "appendToStream", Duration.ofSeconds(5), new java.util.concurrent.TimeoutException());

        // TEST
        final RuntimeException result = ESGrpcEventStoreSupport.mapException(cause, sid, ExpectedVersion.ANY.getNo());

        // VERIFY: not wrapped again - the operation name and the elapsed timeout stay visible.
        assertThat(result).isSameAs(cause);
    }

}
