// CHECKSTYLE:OFF
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
// CHECKSTYLE:ON
