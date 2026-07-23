package org.fuin.esc.esgrpc;

import org.fuin.utils4j.TestOmitted;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TCP forwarder that sits between a client and a server and can be told to misbehave, so a test can make a
 * reachable service unreachable without touching the container that runs it.
 * <p>
 * This is deliberately in-process rather than a Toxiproxy container or a Testcontainers
 * {@code pause()}: the event store these tests run against is started by the docker-maven-plugin on the host
 * network, so there is no container handle to pause, and a raw socket forwarder needs no container engine at
 * all. gRPC is HTTP/2 over TCP, so forwarding bytes is enough.
 *
 * <ul>
 *     <li>{@link #forward()} - normal operation (the default).</li>
 *     <li>{@link #blackhole()} - connections are accepted and bytes are swallowed, so a call is sent but
 *         never answered. This is the hang that a per-call timeout has to cut short.</li>
 *     <li>{@link #cut()} - existing connections are dropped and new ones refused, which is what a client
 *         sees when the store goes away.</li>
 * </ul>
 */
@TestOmitted("This is only a test helper - it is exercised by ESGrpcFaultInjectionIT")
final class FaultInjectingProxy implements AutoCloseable {

    /**
     * How the proxy treats traffic.
     */
    enum Mode {
        /** Pass everything through. */
        FORWARD,
        /** Accept, then swallow: the request never reaches the server and no answer ever comes back. */
        BLACKHOLE,
        /** Refuse new connections and drop existing ones. */
        CUT
    }

    private final ServerSocket server;

    private final String targetHost;

    private final int targetPort;

    private final List<Socket> sockets = Collections.synchronizedList(new ArrayList<>());

    private volatile Mode mode = Mode.FORWARD;

    private volatile boolean running = true;

    private FaultInjectingProxy(final String targetHost, final int targetPort) throws IOException {
        this.server = new ServerSocket(0);
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        final Thread acceptor = new Thread(this::acceptLoop, "fault-injecting-proxy");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    /**
     * Starts a proxy in front of the given target.
     *
     * @param targetHost Host to forward to.
     * @param targetPort Port to forward to.
     * @return Running proxy listening on a free port.
     * @throws IOException The listening socket could not be opened.
     */
    static FaultInjectingProxy to(final String targetHost, final int targetPort) throws IOException {
        return new FaultInjectingProxy(targetHost, targetPort);
    }

    /**
     * Returns the port clients have to connect to.
     *
     * @return Local port of this proxy.
     */
    int port() {
        return server.getLocalPort();
    }

    /**
     * Passes traffic through again and lets clients reconnect.
     */
    void forward() {
        mode = Mode.FORWARD;
    }

    /**
     * Swallows traffic: a call is sent but never answered.
     */
    void blackhole() {
        mode = Mode.BLACKHOLE;
    }

    /**
     * Drops all open connections and refuses new ones.
     */
    void cut() {
        mode = Mode.CUT;
        closeOpenSockets();
    }

    private void closeOpenSockets() {
        final List<Socket> copy;
        synchronized (sockets) {
            copy = new ArrayList<>(sockets);
            sockets.clear();
        }
        for (final Socket socket : copy) {
            closeQuietly(socket);
        }
    }

    private void acceptLoop() {
        while (running) {
            final Socket client;
            try {
                client = server.accept();
            } catch (final IOException ex) {
                return;
            }
            if (mode == Mode.CUT) {
                closeQuietly(client);
                continue;
            }
            handle(client);
        }
    }

    private void handle(final Socket client) {
        Socket upstream = null;
        try {
            register(client);
            if (mode != Mode.BLACKHOLE) {
                upstream = new Socket();
                upstream.connect(new InetSocketAddress(targetHost, targetPort), 5_000);
                register(upstream);
                pump(upstream, client, "downstream");
            }
            pump(client, upstream, "upstream");
        } catch (final IOException ex) {
            closeQuietly(client);
            closeQuietly(upstream);
        }
    }

    private void register(final Socket socket) {
        sockets.add(socket);
    }

    /**
     * Copies bytes from one socket to another on a daemon thread. A {@literal null} sink or a blackholed
     * proxy makes the bytes disappear, which is what leaves the caller waiting for an answer that never
     * comes.
     *
     * @param source Socket to read from.
     * @param sink   Socket to write to, or {@literal null} to discard.
     * @param name   Thread name suffix.
     */
    private void pump(final Socket source, final Socket sink, final String name) {
        final Thread thread = new Thread(() -> {
            final byte[] buffer = new byte[8192];
            try (InputStream in = source.getInputStream()) {
                while (running) {
                    final int read = in.read(buffer);
                    if (read < 0) {
                        break;
                    }
                    if (sink == null || mode == Mode.BLACKHOLE) {
                        // Swallow - the peer waits forever for something that will never arrive.
                        continue;
                    }
                    final OutputStream out = sink.getOutputStream();
                    out.write(buffer, 0, read);
                    out.flush();
                }
            } catch (final IOException ex) { // NOSONAR - a dropped connection is the point of this class
                // Falls through to closing both sides
            } finally {
                closeQuietly(source);
                closeQuietly(sink);
            }
        }, "fault-injecting-proxy-" + name);
        thread.setDaemon(true);
        thread.start();
    }

    private static void closeQuietly(final Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (final IOException ex) { // NOSONAR
            // Nothing left to do
        }
    }

    @Override
    public void close() {
        running = false;
        closeOpenSockets();
        try {
            server.close();
        } catch (final IOException ex) { // NOSONAR
            // Nothing left to do
        }
    }

}
