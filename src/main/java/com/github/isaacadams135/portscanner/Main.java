package com.github.isaacadams135.portscanner;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public final class Main {

    private static final String USAGE = """
        usage: port-scanner [options] <host>

        options:
          -p, --ports <spec>    ports to scan: "22,80,443" or "1-1024" or both
                                 (default: 20 common ports)
          -t, --threads <n>     parallel connections (default: 100)
          -T, --timeout <ms>    connect timeout (default: 800)
          -b, --banner          grab a banner from each open port
          -o, --output <file>   write the results to a file as well
          -h, --help            show this help
        """;

    private Main() {
    }

    public static void main(String[] args) {
        Options opts;
        try {
            opts = Options.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println("error: " + e.getMessage());
            System.err.println(USAGE);
            System.exit(2);
            return;
        }

        if (opts.help()) {
            System.out.println(USAGE);
            return;
        }

        PortScanner scanner = new PortScanner(opts.host(), opts.threads(), opts.timeout(), opts.banner());
        long started = System.nanoTime();
        List<ScanResult> open;
        try {
            open = scanner.scan(opts.ports());
        } catch (UnknownHostException e) {
            System.err.println("error: could not resolve host '" + opts.host() + "'");
            System.exit(1);
            return;
        }
        double seconds = (System.nanoTime() - started) / 1e9;

        System.out.printf("host   %s%n", opts.host());
        System.out.printf("ports  %d scanned in %.2fs%n", opts.ports().size(), seconds);
        System.out.println();

        if (open.isEmpty()) {
            System.out.println("no open ports");
        } else {
            if (opts.banner()) {
                System.out.printf("%-8s %-16s %s%n", "port", "service", "banner");
            } else {
                System.out.printf("%-8s %s%n", "port", "service");
            }
            for (ScanResult result : open) {
                if (opts.banner()) {
                    System.out.printf("%-8d %-16s %s%n", result.port(), result.service(), result.banner());
                } else {
                    System.out.printf("%-8d %s%n", result.port(), result.service());
                }
            }
            System.out.printf("%n%d open port%s%n", open.size(), open.size() == 1 ? "" : "s");
        }

        if (opts.output() != null) {
            try {
                writeReport(opts.output(), opts.host(), open);
                System.out.printf("%nreport written to %s%n", opts.output());
            } catch (IOException e) {
                System.err.println("error: could not write " + opts.output() + ": " + e.getMessage());
                System.exit(1);
            }
        }
    }

    private static void writeReport(Path file, String host, List<ScanResult> open) throws IOException {
        StringBuilder report = new StringBuilder();
        report.append("host: ").append(host).append(System.lineSeparator());
        for (ScanResult result : open) {
            report.append(result.port()).append('\t').append(result.service());
            if (!result.banner().isEmpty()) {
                report.append('\t').append(result.banner());
            }
            report.append(System.lineSeparator());
        }
        Files.writeString(file, report);
    }

    record Options(String host, Set<Integer> ports, int threads, int timeout,
                   boolean banner, Path output, boolean help) {

        static Options parse(String[] args) {
            String host = null;
            String portSpec = null;
            int threads = 100;
            int timeout = 800;
            boolean banner = false;
            boolean help = false;
            Path output = null;

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "-h", "--help" -> help = true;
                    case "-b", "--banner" -> banner = true;
                    case "-p", "--ports" -> portSpec = nextValue(args, ++i, arg);
                    case "-t", "--threads" -> threads = Integer.parseInt(nextValue(args, ++i, arg));
                    case "-T", "--timeout" -> timeout = Integer.parseInt(nextValue(args, ++i, arg));
                    case "-o", "--output" -> output = Path.of(nextValue(args, ++i, arg));
                    default -> {
                        if (arg.startsWith("-")) {
                            throw new IllegalArgumentException("unknown option: " + arg);
                        }
                        if (host != null) {
                            throw new IllegalArgumentException("unexpected argument: " + arg);
                        }
                        host = arg;
                    }
                }
            }

            if (help) {
                return new Options(null, Set.of(), 0, 0, false, null, true);
            }
            if (host == null) {
                throw new IllegalArgumentException("missing host");
            }
            if (threads < 1) {
                throw new IllegalArgumentException("threads must be at least 1");
            }
            if (timeout < 1) {
                throw new IllegalArgumentException("timeout must be at least 1 ms");
            }
            Set<Integer> ports = portSpec == null
                    ? PortScanner.defaultPorts()
                    : PortScanner.parsePorts(portSpec);
            return new Options(host, ports, threads, timeout, banner, output, false);
        }

        private static String nextValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException(option + " needs a value");
            }
            return args[index];
        }
    }
}
