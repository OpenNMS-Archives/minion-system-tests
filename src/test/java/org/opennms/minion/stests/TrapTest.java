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
import static org.junit.Assert.assertEquals;

import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.ClassRule;
import org.junit.Test;
import org.opennms.core.criteria.Criteria;
import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.minion.stests.NewMinionSystem.ContainerAlias;
import org.opennms.minion.stests.utils.DaoUtils;
import org.opennms.minion.stests.utils.HibernateDaoFactory;
import org.opennms.minion.stests.utils.SshClient;
import org.opennms.netmgt.dao.api.EventDao;
import org.opennms.netmgt.dao.hibernate.EventDaoHibernate;
import org.opennms.netmgt.model.OnmsEvent;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpTrapBuilder;
import org.opennms.netmgt.snmp.SnmpUtils;
import org.opennms.netmgt.snmp.SnmpValue;
import org.opennms.netmgt.snmp.TrapIdentity;
import org.opennms.netmgt.snmp.TrapNotification;
import org.opennms.netmgt.snmp.TrapProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies that SNMP traps sent to the Minion generate
 * events in OpenNMS.
 *
 * @author seth
 */
public class TrapTest {

    private static final Logger LOG = LoggerFactory.getLogger(SyslogTest.class);

    @ClassRule
    //public static MinionSystem minionSystem = MinionSystem.builder().build();
    public static MinionSystem minionSystem = MinionSystem.builder().skipTearDown(true).build();
    //public static MinionSystem minionSystem = MinionSystem.builder().useExisting(true).build();

    /*
    private static class TrapNotificationLatch implements TrapNotificationHandler {
        private final CountDownLatch m_latch;
        private TrapNotification m_last = null;

        public TrapNotificationLatch(int count) {
            m_latch = new CountDownLatch(count);
        }

        @Override
        public void handleTrapNotification(TrapNotification message) {
            LOG.info("Got a trap, decrementing latch");
            m_latch.countDown();
            m_last = message;
        }

        public CountDownLatch getLatch() {
            return m_latch;
        }

        public TrapNotification getLast() {
            return m_last;
        }
    }
    */

    @Test
    public void canReceiveTraps() throws Exception {
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
            // Point the trap handler at the local ActiveMQ broker
            pipe.println("config:edit org.opennms.netmgt.trapd.handler.default");
            pipe.println("config:propset brokerUri tcp://127.0.0.1:61616");
            pipe.println("config:update");
            // Install the trap handler feature
            pipe.println("features:install opennms-trapd-handler-default");
            pipe.println("logout");
            try {
                await().atMost(2, MINUTES).until(sshClient.isShellClosedCallable());
            } finally {
                LOG.info("Karaf output: {}", sshClient.getStdout());
            }
        }

        // Send a trap to the Minion listener
        final InetSocketAddress trapAddr = minionSystem.getServiceAddress(ContainerAlias.MINION, 162, "udp");

        //final TrapNotificationLatch m_handler = new TrapNotificationLatch(3);

        for (int i = 0; i < 3; i++) {
            LOG.info("Sending trap");
            try {
                SnmpTrapBuilder pdu = SnmpUtils.getV2TrapBuilder();
                pdu.addVarBind(SnmpObjId.get(".1.3.6.1.2.1.1.3.0"), SnmpUtils.getValueFactory().getTimeTicks(0));
                pdu.addVarBind(SnmpObjId.get(".1.3.6.1.6.3.1.1.4.1.0"), SnmpUtils.getValueFactory().getObjectId(SnmpObjId.get(".1.3.6.1.4.1.5813.1")));
                pdu.addVarBind(SnmpObjId.get(".1.3.6.1.4.1.5813.20.1"), SnmpUtils.getValueFactory().getOctetString("Hello world".getBytes("UTF-8")));
                pdu.send(InetAddressUtils.str(InetAddressUtils.ONE_TWENTY_SEVEN), 10514, "public");
            } catch (Throwable e) {
                LOG.error(e.getMessage(), e);
            }
            LOG.info("Trap has been sent");
        }
        /*
        m_handler.getLatch().await(30, TimeUnit.SECONDS);
        
        TrapNotification notification = m_handler.getLast();
        notification.setTrapProcessor(new TrapProcessor() {

            @Override
            public void setCommunity(String community) {
                LOG.info("Comparing community");
                assertEquals("public", community);
            }

            @Override
            public void setTimeStamp(long timeStamp) {
                // TODO: Assert something?
            }

            @Override
            public void setVersion(String version) {
                LOG.info("Comparing version");
                assertEquals("v2", version);
            }

            @Override
            public void setAgentAddress(InetAddress agentAddress) {
                LOG.info("Comparing agent address");
                assertEquals(InetAddressUtils.ONE_TWENTY_SEVEN, agentAddress);
            }

            @Override
            public void processVarBind(SnmpObjId name, SnmpValue value) {
            }

            @Override
            public void setTrapAddress(InetAddress trapAddress) {
                LOG.info("Comparing trap address");
                assertEquals(InetAddressUtils.ONE_TWENTY_SEVEN, trapAddress);
            }

            @Override
            public void setTrapIdentity(TrapIdentity trapIdentity) {
                LOG.info("Comparing trap identity");
                assertEquals(new TrapIdentity(SnmpObjId.get(".1.3.6.1.4.1.5813"), 6, 1).toString(), trapIdentity.toString());
            }
        });

        notification.getTrapProcessor();
        */

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
