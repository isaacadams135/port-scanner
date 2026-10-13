package com.github.isaacadams135.portscanner;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Plain TCP connect scan against a single host. No raw sockets, so it works
 * without administrator rights and behaves like any other client connecting.
 */
public final class PortScanner {

    private static final int[] COMMON_PORTS = {
        21, 22, 23, 25, 53, 80, 110, 143, 443, 445,
        993, 995, 1433, 3306, 3389, 5432, 5900, 6379, 8080, 8443
    };

    private final String host;
    private final int threads;
    private final int timeoutMillis;
    private final boolean grabBanners;

    public PortScanner(String host, int threads, int timeoutMillis, boolean grabBanners) {
        if (threads < 1) {
            throw new IllegalArgumentException("threads must be at least 1");
        }
        this.host = host;
        this.threads = threads;
        this.timeoutMillis = timeoutMillis;
        this.grabBanners = grabBanners;
    }

    public static Set<Integer> defaultPorts() {
        Set<Integer> ports = new TreeSet<>();
        for (int port : COMMON_PORTS) {
            ports.add(port);
        }
        return ports;
    }

    /**
     * Accepts a comma separated list of ports and ranges, e.g. "22,80,443" or
     * "1-1024". The result is sorted and free of duplicates.
     */
    public static Set<Integer> parsePorts(String spec) {
        if (spec == null || spec.isBlank()) {
            throw new IllegalArgumentException("empty port list");
        }
        Set<Integer> ports = new TreeSet<>();
        for (String part : spec.split(",")) {
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }
            // start looking for the dash after the first char so "-1" is a bad port, not a range
            int dash = part.indexOf('-', 1);
            if (dash < 0) {
                ports.add(parsePort(part));
                continue;
            }
            int from = parsePort(part.substring(0, dash));
            int to = parsePort(part.substring(dash + 1));
            if (from > to) {
                throw new IllegalArgumentException("range is backwards: " + part);
            }
            for (int port = from; port <= to; port++) {
                ports.add(port);
            }
        }
        if (ports.isEmpty()) {
            throw new IllegalArgumentException("no ports in: " + spec);
        }
        return ports;
    }

    private static int parsePort(String value) {
        int port = Integer.parseInt(value.trim());
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port out of range: " + port);
        }
        return port;
    }

    /**
     * Scans the given ports and returns the open ones sorted by port number.
     */
    public List<ScanResult> scan(Collection<Integer> ports) throws UnknownHostException {
        InetAddress target = InetAddress.getByName(host); // fail fast on a bad hostname
        List<ScanResult> open = new ArrayList<>();

        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            List<Future<ScanResult>> jobs = new ArrayList<>(ports.size());
            for (int port : ports) {
                jobs.add(pool.submit(() -> probe(target, port)));
            }
            for (Future<ScanResult> job : jobs) {
                try {
                    ScanResult result = job.get();
                    if (result != null) {
                        open.add(result);
                    }
                } catch (ExecutionException e) {
                    // one weird probe shouldn't take the whole scan down
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException runtime) {
                        throw runtime;
                    }
                    throw new IllegalStateException(cause);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("scan was interrupted", e);
                }
            }
        }

        open.sort(Comparator.comparingInt(ScanResult::port));
        return open;
    }

    private ScanResult probe(InetAddress target, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(target, port), timeoutMillis);
            String banner = grabBanners ? BannerGrabber.grab(socket, timeoutMillis) : "";
            return new ScanResult(port, ServiceNames.of(port), banner);
        } catch (IOException e) {
            return null; // refused or timed out - as far as we can tell, closed
        }
    }
}
