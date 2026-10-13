package com.github.isaacadams135.portscanner;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * Reads the first thing a service sends on an open connection. Plenty of
 * protocols announce themselves (SSH, SMTP, FTP), but HTTP servers usually
 * wait for a request first, so those come back with an empty banner.
 */
final class BannerGrabber {

    private BannerGrabber() {
    }

    static String grab(Socket socket, int timeoutMillis) {
        try {
            socket.setSoTimeout(timeoutMillis);
            InputStream in = socket.getInputStream();
            byte[] buffer = new byte[256];
            int read = in.read(buffer);
            if (read <= 0) {
                return "";
            }
            return firstLine(buffer, read);
        } catch (IOException e) {
            return "";
        }
    }

    /** Keeps the printable part of the banner and stops at the end of the first line. */
    private static String firstLine(byte[] data, int length) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char c = (char) (data[i] & 0xff);
            if (c == '\r' || c == '\n') {
                break;
            }
            if (c >= 32 && c < 127) {
                line.append(c);
            }
        }
        return line.toString().trim();
    }
}
