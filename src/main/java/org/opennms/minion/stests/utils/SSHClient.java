package org.opennms.minion.stests.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 * A simple SSH client wrapper used to run shell commands.
 *
 * @author jwhite
 */
public class SSHClient implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(SSHClient.class);

    public static final int DEFAULT_TIMEOUT_MS = 5*1000;

    private final JSch jsch = new JSch();
    private Session session;
    private Channel channel;
    private InputStream stdout;
    private InputStream stderr;

    private final InetSocketAddress addr;
    private final String username;
    private final String password;

    private int timeout = DEFAULT_TIMEOUT_MS;
    
    public SSHClient(InetSocketAddress addr, String username, String password) {
        this.addr = addr;
        this.username = username;
        this.password = password;
    }

    public PrintStream openShell() throws Exception {
        // We only support one shell at a time
        close();

        session = jsch.getSession(username, addr.getHostString(), addr.getPort());
        session.setPassword(password.getBytes());
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();

        channel = session.openChannel("shell");
        stdout = channel.getInputStream();
        stderr = channel.getExtInputStream();
        channel.connect(timeout);

        OutputStream ops = channel.getOutputStream();
        PrintStream ps = new PrintStream(ops, true);
        return ps;
    }

    public String getStdout() throws IOException {
        return readAvailableBytes(stdout);
    }

    public String getStderr() throws IOException {
        return readAvailableBytes(stderr);
    }

    public void setTimeout(int timeoutInMs) {
        timeout = timeoutInMs;
    }

    @Override
    public void close() throws Exception {
        if (channel != null) {
            channel.disconnect();
            channel = null;
        }
        if (session != null) {
            session.disconnect();
            session = null;
        }
    }

    /**
     * Checks if the shell's channel is closed.
     *
     * Can be used to make sure that stdout/stderr get fully
     * populated after an exit/logout command is issued
     * in the shell.
     */
    public boolean isShellClosed() {
        if (channel == null) {
            return true;
        }
        return channel.isClosed();
    }

    /**
     * Read all of the available bytes on the given stream and converts them
     * to a string.
     *
     * Note that this may cause problems if a multi-byte character is not
     * completely read.
     */
    private static String readAvailableBytes(InputStream is) throws IOException {
        if (is == null) {
            return null;
        }

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final int BUF_LEN = 1024;
        final byte[] buffer = new byte[BUF_LEN];
        int avail = 0;
        while ((avail = is.available()) > 0) {
            int length = is.read(buffer, 0, Math.min(BUF_LEN, avail));
            baos.write(buffer, 0, length);
        }
        return baos.toString("UTF-8");
    }

    public Callable<Boolean> isShellClosedCallable() {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return isShellClosed();
            }
        };
    }

    public static Callable<Boolean> canConnectViaSsh(final InetSocketAddress addr, final String username, final String password) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                LOG.info("Attempting to SSH to {}@{}:{}", username, addr.getHostString(), addr.getPort());
                try (
                    final SSHClient client = new SSHClient(addr, username, password);
                ) {
                    client.setTimeout(1000);
                    client.openShell();
                    return true;
                } catch (Throwable t) {
                    LOG.debug("SSH connection failed.", t);
                    return false;
                }
            }
        };
    }
}
