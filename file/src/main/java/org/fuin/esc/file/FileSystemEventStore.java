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
package org.fuin.esc.file;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.io.FileUtils;
import org.fuin.esc.api.CommonEvent;
import org.fuin.esc.api.DeserializerRegistry;
import org.fuin.esc.api.EnhancedMimeType;
import org.fuin.esc.api.EventNotFoundException;
import org.fuin.esc.api.ExpectedVersion;
import org.fuin.esc.api.IBaseTypeFactory;
import org.fuin.esc.api.SerializerRegistry;
import org.fuin.esc.api.StreamAlreadyExistsException;
import org.fuin.esc.api.StreamDeletedException;
import org.fuin.esc.api.StreamEventsSlice;
import org.fuin.esc.api.StreamId;
import org.fuin.esc.api.StreamNotFoundException;
import org.fuin.esc.api.StreamReadOnlyException;
import org.fuin.esc.api.StreamState;
import org.fuin.esc.api.TenantId;
import org.fuin.esc.api.WrongExpectedVersionException;
import org.fuin.esc.spi.AbstractReadableEventStore;
import org.fuin.esc.spi.EscSpiUtils;
import org.fuin.objects4j.common.Contract;

import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * File based implementation.
 */
@ThreadSafe
public final class FileSystemEventStore extends AbstractReadableEventStore implements IFileSystemEventStore {

    private final File rootDir;

    private final SerializerRegistry serRegistry;

    private  final DeserializerRegistry desRegistry;

    private  final IBaseTypeFactory baseTypeFactory;

    private  final EnhancedMimeType targetContentType;

    private final TenantId tenantId;

    private boolean open;

    /**
     * Constructor with all mandatory data.
     *
     * @param rootDir Directory where to persist the events.
     * @param serRegistry       Registry used to locate serializers.
     * @param desRegistry       Registry used to locate deserializers.
     * @param baseTypeFactory   Factory used to create basic types.
     * @param targetContentType Target content type (Allows only 'application/xml'
     *                          or 'application/json' with 'utf-8' encoding).
     * @param tenantId          Unique tenant identifier.
     */
    public FileSystemEventStore(@NotNull File rootDir,
                                @NotNull final SerializerRegistry serRegistry,
                                @NotNull final DeserializerRegistry desRegistry,
                                @NotNull final IBaseTypeFactory baseTypeFactory,
                                @NotNull final EnhancedMimeType targetContentType,
                                @Nullable final TenantId tenantId) {
        super();
        this.rootDir = Objects.requireNonNull(rootDir, "rootDir==null");
        this.serRegistry = Objects.requireNonNull(serRegistry, "serRegistry==null");
        this.desRegistry = Objects.requireNonNull(desRegistry, "desRegistry==null");
        this.baseTypeFactory = Objects.requireNonNull(baseTypeFactory, "baseTypeFactory==null");
        this.targetContentType = Objects.requireNonNull(targetContentType, "targetContentType==null");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId==null");

        this.open = false;
    }

    @Override
    public FileSystemEventStore open() {
        if (open) {
            // Ignore
            return this;
        }
        if (!rootDir.exists() && !rootDir.mkdirs()) {
            throw new IllegalStateException("Cannot create root directory: " + rootDir);
        }
        this.open = true;
        return this;
    }

    @Override
    public void close() {
        if (!open) {
            // Ignore
            return;
        }
        this.open = false;
    }

    @Override
    public boolean isSupportsCreateStream() {
        return true;
    }

    @Override
    public void createStream(final StreamId streamId) throws StreamAlreadyExistsException {
        final File streamDir = new File(rootDir, streamId.getName());
        if (streamDir.exists()) {
            throw new StreamAlreadyExistsException(streamId);
        }
        if (!streamDir.mkdir()) {
            throw new IllegalStateException("Failed to create stream directory: " + streamDir);
        }
    }

    @Override
    public boolean streamExists(final StreamId streamId) {

        Contract.requireArgNotNull("streamId", streamId);
        ensureOpen();

        return new File(rootDir, streamId.getName()).exists();

    }

    @Override
    public CommonEvent readEvent(final StreamId streamId, final long eventNumber) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("eventNumber", eventNumber, 0);
        ensureOpen();

        final List<CommonEvent> events = getEvents(streamId, ExpectedVersion.ANY.getNo());
        if (events.size() - 1 < eventNumber) {
            throw new EventNotFoundException(streamId, eventNumber);
        }

        return events.get((int) eventNumber);
    }

    @Override
    public StreamEventsSlice readEventsForward(final StreamId streamId, final long start, final int count) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("start", start, 0);
        Contract.requireArgMin("count", count, 1);
        ensureOpen();

        final List<CommonEvent> events = getEvents(streamId, ExpectedVersion.ANY.getNo());

        final List<CommonEvent> result = new ArrayList<>();
        for (int i = (int) start; (i < (start + count)) && (i < events.size()); i++) {
            result.add(events.get(i));
        }
        final long fromEventNumber = start;
        final long nextEventNumber = (start + result.size());
        final boolean endOfStream = (result.size() < count);

        return new StreamEventsSlice(fromEventNumber, result, nextEventNumber, endOfStream);

    }

    @Override
    public StreamEventsSlice readEventsBackward(final StreamId streamId, final long start, final int count) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgMin("start", start, 0);
        Contract.requireArgMin("count", count, 1);
        ensureOpen();

        final List<CommonEvent> events = getEvents(streamId, ExpectedVersion.ANY.getNo());

        final List<CommonEvent> result = new ArrayList<>();
        if (start < events.size()) {
            for (int i = (int) start; (i > (start - count)) && (i >= 0); i--) {
                result.add(events.get(i));
            }
        }

        final long fromEventNumber = start;
        long nextEventNumber = start - result.size();
        if (nextEventNumber < 0) {
            nextEventNumber = 0;
        }
        final boolean endOfStream = (start - count) < 0;

        return new StreamEventsSlice(fromEventNumber, result, nextEventNumber, endOfStream);
    }

    @Override
    public void deleteStream(final StreamId streamId, final long expected, final boolean hardDelete) {

        Contract.requireArgNotNull("streamId", streamId);
        ensureOpen();

        if (streamId.isProjection()) {
            throw new StreamReadOnlyException(streamId);
        }

        final File streamDir = new File(rootDir, streamId.getName());
        if (streamDir.exists()) {
            deleteExistingStream(streamDir, streamId, expected, hardDelete);
        } else {
            deleteNonExistingStream(streamId, expected, hardDelete);
        }
    }

    @Override
    public void deleteStream(final StreamId streamId, final boolean hardDelete) {
        deleteStream(streamId, ExpectedVersion.ANY.getNo(), hardDelete);
    }

    @Override
    public long appendToStream(final StreamId streamId, final long expectedVersion, final List<CommonEvent> toAppend) {

        Contract.requireArgNotNull("streamId", streamId);
        Contract.requireArgNotNull("toAppend", toAppend);
        ensureOpen();

        if (streamId.isProjection()) {
            throw new StreamReadOnlyException(streamId);
        }

        final File streamDir = streamDir(streamId);
        if (!streamDir.exists()) {
            if (hardDeleted(streamId)) {
                throw new StreamDeletedException(streamId);
            } else if (softDeleted(streamId)) {
                undelete(streamId);
            } else {
                createStream(streamId);
            }
        }
        final long streamVersion = streamVersion(streamId);
        if (expectedVersion != ExpectedVersion.ANY.getNo() && expectedVersion != streamVersion) {
            // Test for idempotency
            final StreamEventsSlice slice = readEventsBackward(streamId, streamVersion, toAppend.size());
            final List<CommonEvent> events = slice.getEvents();
            if (EscSpiUtils.eventsEqual(events, toAppend)) {
                return streamVersion;
            }
            throw new WrongExpectedVersionException(streamId, expectedVersion, streamVersion);
        }

        return addAll(streamId, streamVersion + 1, toAppend);

    }

    @Override
    public long appendToStream(final StreamId streamId, final long expectedVersion, final CommonEvent... events) {
        return appendToStream(streamId, expectedVersion, EscSpiUtils.asList(events));
    }

    @Override
    public long appendToStream(final StreamId streamId, final List<CommonEvent> toAppend) {
        return appendToStream(streamId, ExpectedVersion.ANY.getNo(), toAppend);
    }

    @Override
    public long appendToStream(final StreamId streamId, final CommonEvent... events) {
        Contract.requireArgNotNull("events", events);
        return appendToStream(streamId, EscSpiUtils.asList(events));
    }

    @Override
    public StreamState streamState(final StreamId streamId) {

        Contract.requireArgNotNull("streamId", streamId);
        ensureOpen();

        if (streamExists(streamId)) {
            return StreamState.ACTIVE;
        }
        if (softDeleted(streamId)) {
            return StreamState.SOFT_DELETED;
        }
        if (hardDeleted(streamId)) {
            return StreamState.HARD_DELETED;
        }

        throw new StreamNotFoundException(streamId);

    }

    private void ensureOpen() {
        if (!open) {
            open();
        }
    }

    private List<CommonEvent> getEvents(final StreamId streamId, final long expected) {
        if (streamExists(streamId)) {
            final long streamVersion = streamVersion(streamId);
            if (expected != ExpectedVersion.ANY.getNo() && expected != streamVersion) {
                throw new WrongExpectedVersionException(streamId, expected, streamVersion);
            }
            return readEvents(streamId, expected);
        }
        if (softDeleted(streamId)) {
            throw new StreamNotFoundException(streamId);
        }
        if (hardDeleted(streamId)) {
            throw new StreamDeletedException(streamId);
        }
        throw new StreamNotFoundException(streamId);
    }

    private boolean hardDeleted(StreamId streamId) {
        return hardDeletedFile(streamId).exists();
    }

    private boolean softDeleted(StreamId streamId) {
        return softDeletedDir(streamId).exists();
    }

    private void deleteNonExistingStream(StreamId streamId, long expected, boolean hardDelete) {
        final File softDeletedDir = softDeletedDir(streamId);
        final File hardDeletedFile = hardDeletedFile(streamId);
        if (softDeletedDir.exists()) {
            if (hardDelete) {
                createFile(hardDeletedFile);
                FileUtils.deleteQuietly(softDeletedDir);
            }
            // Ignore
            return;
        }
        if (hardDeletedFile.exists()) {
            throw new StreamDeletedException(streamId);
        }
        if ((expected == ExpectedVersion.ANY.getNo() || expected == ExpectedVersion.NO_OR_EMPTY_STREAM.getNo()) && hardDelete) {
            createFile(hardDeletedFile);
        }
    }

    private void undelete(StreamId streamId) {
        final File softDeletedDir = softDeletedDir(streamId);
        if (softDeletedDir.exists()) {
            moveDirectory(softDeletedDir, streamDir(streamId));
        }
    }

    public long addAll(final StreamId streamId, final long startVersion, final List<CommonEvent> toAppend) {
        // TODO Implement
    }

    private List<CommonEvent> readEvents(final StreamId streamId, final long expected) {
        // TODO Implement!
    }

    private long streamVersion(StreamId streamId) {
        // TODO Implement!
    }

    private void deleteExistingStream(File streamDir, StreamId streamId, long expected, boolean hardDelete) {
        final long version = streamVersion(streamId);
        if (expected != ExpectedVersion.ANY.getNo() && expected != version) {
            throw new WrongExpectedVersionException(streamId, expected, version);
        }
        if (hardDelete) {
            // Hard delete
            createFile(hardDeletedFile(streamId));
            FileUtils.deleteQuietly(streamDir);
        } else {
            // Soft delete
            moveDirectory(streamDir, softDeletedDir(streamId));
        }
    }

    private File streamDir(final StreamId streamId) {
        return new File(rootDir, streamId.getName());
    }

    private File softDeletedDir(final StreamId streamId) {
        return new File(rootDir, streamId.getName() + ".softDeleted");
    }

    private File hardDeletedFile(final StreamId streamId) {
        return new File(rootDir, streamId.getName() + ".hardDeleted");
    }

    private static void createFile(File file) {
        try {
            if (!file.createNewFile()) {
                throw new IllegalStateException("Failed to create file: " + file);
            }
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to create file: " + file, exception);
        }
    }

    private static void moveDirectory(final File source, final File target) {
        try {
            FileUtils.moveDirectory(source, target);
        } catch (final IOException exception) {
            throw new IllegalStateException("Failed to move directory from " + source + " to " + target, exception);
        }
    }

}
