package org.opennms.minion.stests.utils;

import java.io.IOException;
import java.net.InetSocketAddress;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.common.util.Base64Utility;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.web.rest.measurements.model.QueryRequest;
import org.opennms.web.rest.measurements.model.QueryResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A ReST API client for OpenNMS.
 *
 * Uses CXF to perform automatic marshaling/unmarshaling of request and
 * response objects.
 *
 * @author jwhite
 */
public class RESTClient {

    private static final String DEFAULT_USERNAME = "admin";

    private static final String DEFAULT_PASSWORD = "admin";
    
    private final InetSocketAddress addr;

    private final String authorizationHeader;

    public RESTClient(InetSocketAddress addr) {
        this(addr, DEFAULT_USERNAME, DEFAULT_PASSWORD);
    }

    public RESTClient(InetSocketAddress addr, String username, String password) {
        this.addr = addr;
        authorizationHeader = "Basic " + Base64Utility.encode((username + ":" + password).getBytes());
    }

    public String getDisplayVersion() {
        final WebTarget target = getTarget().path("info");
        final String json = getBuilder(target).get(String.class);

        final ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode actualObj = mapper.readTree(json);
            return actualObj.get("displayVersion").asText();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
 
    public void addOrReplaceRequisition(Requisition requisition) {
        final WebTarget target = getTarget().path("requisitions");
        getBuilder(target).post(Entity.entity(requisition, MediaType.APPLICATION_XML));
    }

    public void importRequisition(final String foreignSource) {
        final WebTarget target = getTarget().path("requisitions").path(foreignSource).path("import");
        getBuilder(target).put(null);
    }

    public QueryResponse getMeasurements(final QueryRequest request) {
        final WebTarget target = getTarget().path("measurements");
        return getBuilder(target).post(Entity.entity(request, MediaType.APPLICATION_XML), QueryResponse.class);
    }

    public OnmsNode getNode(String nodeCriteria) {
        final WebTarget target = getTarget().path("nodes").path(nodeCriteria);
        return getBuilder(target).get(OnmsNode.class);
    }

    private WebTarget getTarget() {
        final Client client = ClientBuilder.newClient();
        return client.target(String.format("http://%s:%d/opennms/rest", addr.getHostString(), addr.getPort()));
    }

    private Invocation.Builder getBuilder(final WebTarget target) {
        return target.request().header("Authorization", authorizationHeader);
    }
}
