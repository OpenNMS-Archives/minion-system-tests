package org.opennms.minion.stests;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.*;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.Callable;

import javax.ws.rs.NotFoundException;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.opennms.minion.stests.MinionSystemTestRule;
import org.opennms.minion.stests.NewMinionSystem;
import org.opennms.minion.stests.utils.RESTClient;
import org.opennms.minion.stests.utils.RequisitionBuilder;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.web.rest.measurements.model.QueryRequest;
import org.opennms.web.rest.measurements.model.QueryResponse;
import org.opennms.web.rest.measurements.model.QueryResponse.WrappedPrimitive;
import org.opennms.web.rest.measurements.model.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * Verifies that the Minion System can perform data collection
 * via the supported protocols.
 *
 * The tests rely on the following containers to expose the
 * given protocols:
 *  - snmpd: SNMP
 *  - tomcat: JMX
 *
 * @author jwhite
 */
public class DataCollectionTest {

    private static final Logger LOG = LoggerFactory.getLogger(DataCollectionTest.class);

    private static final String FOREIGN_SOURCE = "CONTAINERS";

    @ClassRule
    public static MinionSystemTestRule minionLabTestRule = new NewMinionSystem();

    private RESTClient restClient;

    @Before
    public void setUp() {
        final InetSocketAddress dominionHttp = minionLabTestRule.getServiceAddress(NewMinionSystem.DOMINION, 8980);
        restClient = new RESTClient(dominionHttp);

        LOG.info("Creating and importing requisition.");
        createRequisitionAndImportNodes();

        LOG.info("Waiting for nodes to be provisioned.");
        await().atMost(1, MINUTES).pollInterval(5, SECONDS).until(getNodeForContainer(NewMinionSystem.SNMPD), is(notNullValue()));
        await().atMost(1, MINUTES).pollInterval(5, SECONDS).until(getNodeForContainer(NewMinionSystem.TOMCAT), is(notNullValue()));
    }

    @Test
    public void canCollectSNMPMetrics() {
        LOG.info("Waiting for metrics to be collected.");
        // We wait 6 minutes here since the configuration is only refreshed every 5
        await().atMost(6, MINUTES).pollInterval(5, SECONDS).until(attributeHasCollectedValue(1, "nodeSource[CONTAINERS:snmpd].nodeSnmp[]", "loadavg1"));
    }

    private void createRequisitionAndImportNodes() {
        final Requisition requisition = new RequisitionBuilder(minionLabTestRule)
            .withForeignSourceName(FOREIGN_SOURCE)
            .withContainer(NewMinionSystem.SNMPD, "SNMP")
            .withContainer(NewMinionSystem.TOMCAT, "HTTP-8080")
            .build();

        LOG.info("Adding/replacing requisition: {}", requisition);
        restClient.addOrReplaceRequisition(requisition);

        LOG.info("Importing requisition: {}", FOREIGN_SOURCE);
        restClient.importRequisition(FOREIGN_SOURCE);
    }

    private Callable<Boolean> attributeHasCollectedValue(final int inLastNMinutes, final String resourceId, final String attribute) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                LOG.info("Retrieving metrics...");
                try {
                    final long now = new Date().getTime();
                    final QueryRequest request = new QueryRequest();
                    // Use the highest available resolution
                    request.setStep(1);
                    request.setStart(now - inLastNMinutes * 60 * 1000);
                    request.setEnd(now);

                    final Source source = new Source();
                    source.setLabel(attribute);
                    source.setResourceId(resourceId);
                    source.setAttribute(attribute);
                    request.setSources(Lists.newArrayList(source));

                    final QueryResponse response = restClient.getMeasurements(request);
                    LOG.info("Query response: {}.", response);
                    for (final WrappedPrimitive p : response.getColumns()) {
                        for (final double val : p.getList()) {
                            if (!Double.isNaN(val)) {
                                return true;
                            }
                        }
                    }
                } catch (NotFoundException e) {
                    LOG.warn("The metric was not found.");
                    return false;
                }

                LOG.warn("No values found!");
                return false;
            }
        };
    }

    private Callable<OnmsNode> getNodeForContainer(final String alias) {
        return new Callable<OnmsNode>() {
            @Override
            public OnmsNode call() throws Exception {
                try {
                    return restClient.getNode(String.format("%s:%s", FOREIGN_SOURCE, alias));
                } catch (Throwable t) {
                    return null;
                }
            }
        };
    }
}
