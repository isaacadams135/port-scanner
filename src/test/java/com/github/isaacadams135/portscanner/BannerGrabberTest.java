package com.github.isaacadams135.portscanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import org.junit.jupiter.api.Test;

class BannerGrabberTest {

    @Test
    void readsFirstLineOfBanner() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            int port = server.getLocalPort();
            Thread speaker = new Thread(() -> {
                try (Socket client = server.accept()) {
                    OutputStream out = client.getOutputStream();
                    out.write("SSH-2.0-OpenSSH_9.6\r\nmore data we should not see".getBytes());
                    out.flush();
                } catch (IOException ignored) {
                    // client hung up, nothing to do
                }
            });
            speaker.start();

            try (Socket socket = new Socket("127.0.0.1", port)) {
                assertEquals("SSH-2.0-OpenSSH_9.6", BannerGrabber.grab(socket, 2000));
            }
            speaker.join(2000);
        }
    }

    @Test
    void silentServerGivesEmptyBanner() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            int port = server.getLocalPort();
            Thread listener = new Thread(() -> {
                try (Socket client = server.accept()) {
                    Thread.sleep(1500); // hold the connection open but never speak
                } catch (IOException | InterruptedException ignored) {
                    // shutting down
                }
            });
            listener.start();

            try (Socket socket = new Socket("127.0.0.1", port)) {
                assertEquals("", BannerGrabber.grab(socket, 200));
            }
            listener.join(2000);
        }
    }
}
