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
package org.apache.http.impl.nio.client;

import java.util.concurrent.ThreadFactory;

/**
 * Workaround to set the thread factory. It looks like this is currently not supported by the standard methods
 * in the Apache package. Unfortunately the builder class is also private. Therefore this has to be placed in
 * the same package as the Apache code.
 */
public class ESHttpAsyncClients {

    /**
     * Creates {@link CloseableHttpPipeliningClient} instance that supports pipelined request execution. This
     * client does not support authentication and automatic redirects.
     *
     * @param tf
     *            Thread factory to use.
     */
    public static CloseableHttpPipeliningClient createPipelining(final ThreadFactory tf) {
        return MinimalHttpAsyncClientBuilder.create().setThreadFactory(tf).build();
    }

}
