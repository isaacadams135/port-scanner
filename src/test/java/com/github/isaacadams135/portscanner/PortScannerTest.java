package com.github.isaacadams135.portscanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PortScannerTest {

    @Test
    void findsOpenPortOnLocalhost() throws IOException {
        try (ServerSocket server = new ServerSocket(0)) {
            int port = server.getLocalPort();
            PortScanner scanner = new PortScanner("127.0.0.1", 4, 1000, false);

            List<ScanResult> open = scanner.scan(Set.of(port));

            assertEquals(1, open.size());
            assertEquals(port, open.get(0).port());
        }
    }

    @Test
    void closedPortComesBackEmpty() throws IOException {
        int port;
        try (ServerSocket server = new ServerSocket(0)) {
            port = server.getLocalPort();
        } // nothing is listening here anymore

        PortScanner scanner = new PortScanner("127.0.0.1", 4, 300, false);

        assertTrue(scanner.scan(Set.of(port)).isEmpty());
    }

    @Test
    void parsesListsAndRanges() {
        assertEquals(Set.of(22, 80, 443), PortScanner.parsePorts("443, 22,80"));
        assertEquals(Set.of(1, 2, 3, 4, 5), PortScanner.parsePorts("1-5"));
        assertEquals(Set.of(80, 81, 82, 443), PortScanner.parsePorts("80-82,443,80"));
        assertEquals(Set.of(3306), PortScanner.parsePorts(" 3306 "));
    }

    @Test
    void rejectsBadPortInput() {
        assertThrows(IllegalArgumentException.class, () -> PortScanner.parsePorts("abc"));
        assertThrows(IllegalArgumentException.class, () -> PortScanner.parsePorts("0"));
        assertThrows(IllegalArgumentException.class, () -> PortScanner.parsePorts("70000"));
        assertThrows(IllegalArgumentException.class, () -> PortScanner.parsePorts("10-5"));
        assertThrows(IllegalArgumentException.class, () -> PortScanner.parsePorts(","));
        assertThrows(IllegalArgumentException.class, () -> PortScanner.parsePorts(""));
    }

    @Test
    void unknownHostIsReported() {
        PortScanner scanner = new PortScanner("no-such-host.invalid", 2, 200, false);
        assertThrows(java.net.UnknownHostException.class, () -> scanner.scan(Set.of(80)));
    }

    @Test
    void knowsCommonServiceNames() {
        assertEquals("ssh", ServiceNames.of(22));
        assertEquals("https", ServiceNames.of(443));
        assertEquals("unknown", ServiceNames.of(31337));
    }
}
