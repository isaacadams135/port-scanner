package com.github.isaacadams135.portscanner;

/** One open port, as reported by a scan. */
public record ScanResult(int port, String service, String banner) {
}
