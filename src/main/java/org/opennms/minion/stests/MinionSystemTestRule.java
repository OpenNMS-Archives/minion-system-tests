package org.opennms.minion.stests;

import java.net.InetSocketAddress;
import java.util.Set;

import org.junit.rules.TestRule;

import com.spotify.docker.client.messages.ContainerInfo;

/**
 * The system test rule is responsible for ensuring that a valid
 * Minion System is setup before invoking any tests.
 *
 * Further, the test rule must provide all of the details required
 * to communicate with the Minion System, whether it be on the current
 * or a remote host.
 * 
 * @author jwhite
 */
public interface MinionSystemTestRule extends TestRule {

    public InetSocketAddress getServiceAddress(String alias, int port);

    public ContainerInfo getContainerInfo(String alias);

    public Set<String> getContainerAliases();

}
