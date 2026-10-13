package com.github.isaacadams135.portscanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    void parsesHostAndOptions() {
        Main.Options opts = Main.Options.parse(
                new String[] {"example.com", "-p", "22,80", "-t", "10", "-T", "500", "-b"});

        assertEquals("example.com", opts.host());
        assertEquals(2, opts.ports().size());
        assertEquals(10, opts.threads());
        assertEquals(500, opts.timeout());
        assertTrue(opts.banner());
        assertFalse(opts.help());
    }

    @Test
    void defaultsToCommonPorts() {
        Main.Options opts = Main.Options.parse(new String[] {"10.0.0.1"});

        assertEquals(20, opts.ports().size());
        assertTrue(opts.ports().contains(443));
        assertEquals(100, opts.threads());
        assertEquals(800, opts.timeout());
    }

    @Test
    void helpDoesNotRequireHost() {
        assertTrue(Main.Options.parse(new String[] {"--help"}).help());
    }

    @Test
    void rejectsMissingHostAndBadFlags() {
        assertThrows(IllegalArgumentException.class, () -> Main.Options.parse(new String[] {"-b"}));
        assertThrows(IllegalArgumentException.class, () -> Main.Options.parse(new String[] {"a.com", "b.com"}));
        assertThrows(IllegalArgumentException.class, () -> Main.Options.parse(new String[] {"--verbose", "a.com"}));
        assertThrows(IllegalArgumentException.class, () -> Main.Options.parse(new String[] {"a.com", "-p"}));
        assertThrows(IllegalArgumentException.class, () -> Main.Options.parse(new String[] {"a.com", "-t", "0"}));
    }
}
