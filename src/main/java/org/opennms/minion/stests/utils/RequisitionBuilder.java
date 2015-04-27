package org.opennms.minion.stests.utils;

import org.opennms.minion.stests.MinionSystemTestRule;
import org.opennms.netmgt.model.PrimaryType;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.netmgt.provision.persist.requisition.RequisitionInterface;
import org.opennms.netmgt.provision.persist.requisition.RequisitionMonitoredService;
import org.opennms.netmgt.provision.persist.requisition.RequisitionNode;

import com.spotify.docker.client.messages.ContainerInfo;

/**
 * Helper class used to simplify creating requisitions
 * for containers.
 *
 * @author jwhite
 */
public class RequisitionBuilder {

    private final MinionSystemTestRule minionSystem;
    
    private final Requisition requisition = new Requisition();

    public RequisitionBuilder(MinionSystemTestRule minionSystem) {
        this.minionSystem = minionSystem;
    }

    public RequisitionBuilder withForeignSourceName(String fsName) {
        requisition.setForeignSource(fsName);
        return this;
    }

    public RequisitionBuilder withContainer(final String alias, final String... services) {
        // We're assuming that the Minion container is on the same
        // host as the service containers
        final ContainerInfo containerInfo = minionSystem.getContainerInfo(alias);
        final String containerIpAddr = containerInfo.networkSettings().ipAddress();

        RequisitionNode node = new RequisitionNode();
        node.setNodeLabel(alias);
        node.setForeignId(alias);

        RequisitionInterface iface = new RequisitionInterface();
        iface.setSnmpPrimary(PrimaryType.PRIMARY);
        iface.setIpAddr(containerIpAddr);

        for (String svcName : services) {
            RequisitionMonitoredService svc = new RequisitionMonitoredService();
            svc.setServiceName(svcName);
            iface.putMonitoredService(svc);
        }

        node.putInterface(iface);
        requisition.putNode(node);

        return this;
    }

    public Requisition build() {
        return requisition;
    }
}
