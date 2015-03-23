package org.opennms.minion.stests.utils;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBUtils {

    private static final Logger LOG = LoggerFactory.getLogger(DBUtils.class);

    public static Callable<Boolean> canConnectToPostgres(final InetSocketAddress addr, final String dbname, final String username, final String password) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                try {
                    Connection connection = null;
                    connection = DriverManager.getConnection(
                            String.format("jdbc:postgresql://%s:%d/%s",
                                    addr.getHostString(), addr.getPort(), dbname),
                                    username, password);
                    connection.close();
                    return true;
                } catch (SQLException e) {
                    LOG.debug("PostgreSQL connection failed.", e);
                }
                return false;
            }
        };
    }
}
