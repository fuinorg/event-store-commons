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
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.ProjectionStreamId;
import org.fuin.esc.api.SimpleStreamId;
import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamReadOnlyException;
import org.fuin.esc.api.TenantStreamId;
import org.junit.jupiter.api.Test;

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

}
