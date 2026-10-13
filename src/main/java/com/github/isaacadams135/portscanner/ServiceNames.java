package com.github.isaacadams135.portscanner;

import java.util.Map;

/** Friendly names for well-known ports, straight from the usual IANA assignments. */
final class ServiceNames {

    private static final Map<Integer, String> KNOWN = Map.ofEntries(
            Map.entry(21, "ftp"),
            Map.entry(22, "ssh"),
            Map.entry(23, "telnet"),
            Map.entry(25, "smtp"),
            Map.entry(53, "domain"),
            Map.entry(80, "http"),
            Map.entry(110, "pop3"),
            Map.entry(143, "imap"),
            Map.entry(443, "https"),
            Map.entry(445, "microsoft-ds"),
            Map.entry(993, "imaps"),
            Map.entry(995, "pop3s"),
            Map.entry(1433, "ms-sql-s"),
            Map.entry(3306, "mysql"),
            Map.entry(3389, "ms-wbt"),
            Map.entry(5432, "postgresql"),
            Map.entry(5900, "vnc"),
            Map.entry(6379, "redis"),
            Map.entry(8080, "http-proxy"),
            Map.entry(8443, "https-alt"));

    private ServiceNames() {
    }

    static String of(int port) {
        return KNOWN.getOrDefault(port, "unknown");
    }
}
