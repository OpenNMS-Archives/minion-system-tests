/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/
package org.opennms.minion.stests;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.greaterThan;

import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Date;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.opennms.core.criteria.Criteria;
import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.minion.stests.NewMinionSystem.ContainerAlias;
import org.opennms.minion.stests.utils.DaoUtils;
import org.opennms.minion.stests.utils.HibernateDaoFactory;
import org.opennms.minion.stests.utils.SshClient;
import org.opennms.netmgt.dao.api.EventDao;
import org.opennms.netmgt.dao.hibernate.EventDaoHibernate;
import org.opennms.netmgt.model.OnmsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies that syslog messages sent to the Minion generate
 * events in OpenNMS.
 *
 * @author jwhite
 */
public class SyslogTest {
    private static final Logger LOG = LoggerFactory.getLogger(SyslogTest.class);

    @ClassRule
    public static MinionSystem minionSystem = MinionSystem.builder().build();
    //public static MinionSystem minionSystem = MinionSystem.builder().skipTearDown(true).build();
    //public static MinionSystem minionSystem = MinionSystem.builder().useExisting(true).build();
    

    @Test
    public void canReceiveSyslogMessages() throws Exception {
        Date startOfTest = new Date();

        // Install the handler on the OpenNMS system (this should probably be installed by default)
        final InetSocketAddress sshAddr = minionSystem.getServiceAddress(ContainerAlias.OPENNMS, 8101);
        try (
            final SshClient sshClient = new SshClient(sshAddr, "admin", "admin");
        ) {
            PrintStream pipe = sshClient.openShell();
            // Point the syslog handler at the local ActiveMQ broker
            pipe.println("config:edit org.opennms.netmgt.syslog.handler.default");
            pipe.println("config:propset brokerUri tcp://127.0.0.1:61616");
            pipe.println("config:update");
            // Install the syslog handler feature
            pipe.println("features:install opennms-syslogd-handler-default");
            pipe.println("logout");
            try {
                await().atMost(2, MINUTES).until(sshClient.isShellClosedCallable());
            } finally {
                LOG.info("Karaf output: {}", sshClient.getStdout());
            }
        }

        // Send a syslog packet to the Minion syslog listener
        final InetSocketAddress syslogAddr = minionSystem.getServiceAddress(ContainerAlias.MINION, 1514, "udp");
        byte[] message = "<190>Mar 11 08:35:17 aaa_host 30128311: Mar 11 08:35:16.844 CST: %SEC-6-IPACCESSLOGP: list in110 denied tcp 192.168.10.100(63923) -> 192.168.11.128(1521), 1 packet\n".getBytes();
        DatagramPacket packet = new DatagramPacket(message, message.length, syslogAddr.getAddress(), syslogAddr.getPort());
        DatagramSocket dsocket = new DatagramSocket();
        dsocket.send(packet);
        dsocket.close();

        // Connect to the postgresql container
        InetSocketAddress pgsql = minionSystem.getServiceAddress(ContainerAlias.POSTGRES, 5432);
        HibernateDaoFactory daoFactory = new HibernateDaoFactory(pgsql);
        EventDao eventDao = daoFactory.getDao(EventDaoHibernate.class);

        // Parsing the message correctly relies on the customized syslogd-configuration.xml that is part of the OpenNMS image
        Criteria criteria = new CriteriaBuilder(OnmsEvent.class)
                .eq("eventUei", "uei.opennms.org/vendor/cisco/syslog/SEC-6-IPACCESSLOGP/aclDeniedIPTraffic")
                .ge("eventTime", startOfTest)
                .toCriteria();

        await().atMost(1, MINUTES).pollInterval(5, SECONDS).until(DaoUtils.countMatchingCallable(eventDao, criteria), greaterThan(0));
    }
}
