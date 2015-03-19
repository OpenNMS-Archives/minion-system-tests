package org.opennms.minion.stests;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Set;

import org.junit.rules.ExternalResource;

import com.google.common.collect.Maps;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.PortBinding;

/**
 * Used to point to any existing Minion System that was setup
 * with the NewMinionSystem(true).
 *
 * This is particularly useful when developing tests, since the
 * containers do not need to be recreated everytime.
 *
 * @author jwhite
 */
public class ExistingMinionSystem extends ExternalResource implements MinionSystemTestRule {

    private DockerClient docker;

    private final Map<String, ContainerInfo> containerInfo = Maps.newHashMap();

    @Override
    protected void before() throws Throwable {
        docker = DefaultDockerClient.fromEnv().build();
        for (final Container container : docker.listContainers()) {
            final String alias = container.image().split(":")[0];
            containerInfo.put(alias, docker.inspectContainer(container.id()));
        }
    }

    @Override
    protected void after() {
        if (docker == null) {
            return;
        }
        docker.close();
        docker = null;
    }

    @Override
    public InetSocketAddress getServiceAddress(String alias, int port) {
        final ContainerInfo info = containerInfo.get(alias);
        if (info == null) {
            return null;
        }
        final PortBinding binding = info.networkSettings().ports().get(port + "/tcp").get(0);
        final String host = "0.0.0.0".equals(binding.hostIp()) ? "127.0.0.1" : binding.hostIp();
        return new InetSocketAddress(host, Integer.valueOf(binding.hostPort()));
    }

    @Override
    public ContainerInfo getContainerInfo(String alias) {
        return containerInfo.get(alias);
    }

    @Override
    public Set<String> getContainerAliases() {
        return containerInfo.keySet();
    }
}
