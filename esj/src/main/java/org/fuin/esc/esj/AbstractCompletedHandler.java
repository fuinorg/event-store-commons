/**
 * Copyright (C) 2015 Michael Schnell. All rights reserved. <http://www.fuin.org/>
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
 * along with this library. If not, see <http://www.gnu.org/licenses/>.
 */
package org.fuin.esc.esj;

import lt.emasina.esj.ResponseReceiver;
import lt.emasina.esj.model.Message;

import org.fuin.objects4j.common.Nullable;

/**
 * Base class for completed handlers.
 */
public abstract class AbstractCompletedHandler implements ResponseReceiver {

    private volatile boolean finished = false;

    private Message msg;

    private Exception ex;

    @Override
    public final void onResponseReturn(final Message msg) {
        this.msg = msg;
        this.finished = true;
    }

    @Override
    public final void onErrorReturn(final Exception ex) {
        this.ex = ex;
        this.finished = true;
    }

    /**
     * Returns the message result.
     * 
     * @return Message.
     */
    @Nullable
    public final Message getMessage() {
        return msg;
    }

    /**
     * Returns the exception result.
     * 
     * @return Exception.
     */
    @Nullable
    public final Exception getException() {
        return ex;
    }

    /**
     * Waits for a result.
     */
    protected final void waitForResult() {
        // TODO Not very nice - CompletableFuture would be better
        while (!finished) {
            try {
                Thread.sleep(10);
            } catch (final InterruptedException ex) {
                throw new RuntimeException("Failed waiting for a response", ex);
            }
        }
    }

}
