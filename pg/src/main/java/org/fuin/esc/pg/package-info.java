/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved.
 * http://www.fuin.org/
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see http://www.gnu.org/licenses/.
 */

/**
 * PostgreSQL {@code LISTEN/NOTIFY} wake-up source (ESC-2 rung 2). The
 * {@link org.fuin.esc.pg.PgListenNotifyWakeupSource} is an
 * {@link org.fuin.esc.spi.WakeupSource} that lets a {@code CatchupSubscription} react to new events with
 * "read now" latency instead of a fixed poll interval, while correctness still rests on the checkpoint and
 * catch-up read (the notify is at-most-once, so it is only a latency signal, never a substitute for the
 * checkpoint). {@link org.fuin.esc.pg.PgNotify} generates the {@code INSERT} trigger that fires the notify.
 */
@NullMarked
package org.fuin.esc.pg;

import org.jspecify.annotations.NullMarked;
