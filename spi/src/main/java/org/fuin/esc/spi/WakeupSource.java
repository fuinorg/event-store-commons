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
package org.fuin.esc.spi;

import org.fuin.objects4j.common.ThreadSafe;

/**
 * Signals a catch-up consumer when it should run a read pass. It is the pluggable wake-up mechanism of the
 * read side: the portable default is interval polling ({@link IntervalWakeupSource}); a backend may plug in a
 * lower-latency signal (e.g. PostgreSQL {@code LISTEN/NOTIFY} or CDC) without changing the catch-up driver,
 * because correctness always rests on the checkpoint + poll, never on the wake-up signal.
 * <p>
 * All implementations are expected to be thread safe.
 */
@ThreadSafe
public interface WakeupSource extends AutoCloseable {

    /**
     * Starts signalling. The given callback is invoked whenever new events may be available (or on the poll
     * interval). Must be called at most once.
     *
     * @param onWakeup Callback to run a catch-up pass.
     */
    void start(Runnable onWakeup);

    /**
     * Stops signalling. Any resources owned by this source (but not caller-provided ones such as an injected
     * executor) are released. Idempotent.
     */
    @Override
    void close();

}
