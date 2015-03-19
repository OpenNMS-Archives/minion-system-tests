package org.opennms.minion.stests.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.karaf.features.management.FeaturesServiceMBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An easy way of grabbing proxies to Karaf's MBeans. 
 *
 * @author jwhite
 */
public class KarafMBeanProxyHelper {

    private static final String DEFAULT_USERNAME = "karaf";

    private static final String DEFAULT_PASSWORD = "karaf";
    
    private static final Logger LOG = LoggerFactory.getLogger(KarafMBeanProxyHelper.class);

    private final String username;
    private final String password;
    private final InetSocketAddress jmxAddr;
    private final InetSocketAddress rmiAddr;

    public KarafMBeanProxyHelper(InetSocketAddress jmxAddr, InetSocketAddress rmiAddr) {
        this(jmxAddr, rmiAddr, DEFAULT_USERNAME, DEFAULT_PASSWORD);
    }

    public KarafMBeanProxyHelper(InetSocketAddress jmxAddr, InetSocketAddress rmiAddr, String username, String password) {
        this.jmxAddr = jmxAddr;
        this.rmiAddr = rmiAddr;
        this.username = username;
        this.password = password;
    }

    public FeaturesServiceMBean getFeaturesService(String instance) throws Exception {
        final MBeanServerConnection mbsc = getMBeanServerConnection(instance);
        final ObjectName mbeanName = new ObjectName(String.format("org.apache.karaf:type=features,name=%s", instance));
        return JMX.newMBeanProxy(mbsc, mbeanName, FeaturesServiceMBean.class, true);
    }

    private MBeanServerConnection getMBeanServerConnection(String instance) throws IOException {
        final HashMap<String, String[]> environment = new HashMap<String, String[]>();
        final String[] credentials = new String[] { username, password };
        environment.put("jmx.remote.credentials", credentials);

        final String url = String.format("service:jmx:rmi://%s:%d/jndi/rmi://%s:%d/karaf-%s",
                jmxAddr.getHostString(), jmxAddr.getPort(), rmiAddr.getHostString(), rmiAddr.getPort(), instance);
        LOG.debug("JMX Service URL: {}", url);

        final JMXServiceURL svcUrl = new JMXServiceURL(url);
        final JMXConnector jmxc = JMXConnectorFactory.connect(svcUrl, environment);

        return jmxc.getMBeanServerConnection();
    }
}
