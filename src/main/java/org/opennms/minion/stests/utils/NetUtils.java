package org.opennms.minion.stests.utils;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Callable;

/**
 * Utilities for testing network connectivity.
 *
 * @author jwhite
 */
public class NetUtils {

    public static boolean isTcpPortOpen(int port) {
        return isTcpPortOpen(new InetSocketAddress("127.0.0.1", port));
    }

    public static boolean isTcpPortOpen(InetSocketAddress addr) {
        try (Socket socket = new Socket()) {
            socket.connect(addr, 100);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static Callable<Boolean> isTcpPortOpenCallable(final int port) {
        return new Callable<Boolean>() {
            public Boolean call() throws Exception {
                return isTcpPortOpen(port);
            }
        };
    }
}
