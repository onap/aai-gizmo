/**
 * ﻿============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Copyright © 2017-2018 Amdocs
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.crud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import javax.security.auth.x500.X500Principal;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.onap.crud.exception.CrudException;
import org.onap.crud.util.TestUtil;
import org.onap.schema.OxmModelLoader;
import org.springframework.mock.web.MockHttpServletRequest;
import com.att.aft.dme2.internal.jersey.api.client.ClientResponse.Status;

public class TestResourceServiceEdgeOperations {

    private MockHttpServletRequest servletRequest;
    private UriInfo uriInfo;
    private HttpHeaders headers;
    private AbstractGraphDataService graphDataService;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        System.setProperty("CONFIG_HOME", "src/test/resources");
        System.setProperty("AJSC_HOME", ".");
        System.setProperty("BUNDLECONFIG_DIR", "src/test/resources/bundleconfig-local");

        OxmModelLoader.loadModels();
    }

    @Before
    public void setUp() throws Exception {
        graphDataService = mock(CrudGraphDataService.class);
        uriInfo = mock(UriInfo.class);

        mockHeaderRequests(getCreateHeaders());

        servletRequest = new MockHttpServletRequest();
        servletRequest.setSecure(true);
        servletRequest.setScheme("https");
        servletRequest.setServerPort(9520);
        servletRequest.setServerName("localhost");
        servletRequest.setRequestURI("/services/inventory/relationships/");

        setUser("CN=ONAP, OU=ONAP, O=ONAP, L=Ottawa, ST=Ontario, C=CA");

        servletRequest.setAttribute("javax.servlet.request.cipher_suite", "");
    }

    private MultivaluedHashMap<String, String> getCreateHeaders() {
        MultivaluedHashMap<String, String> headersMap = new MultivaluedHashMap<>();
        headersMap.put("X-TransactionId", createSingletonList("transaction-id"));
        headersMap.put("X-FromAppId", createSingletonList("app-id"));
        headersMap.put("Host", createSingletonList("hostname"));
        return headersMap;
    }

    private void mockHeaderRequests(MultivaluedHashMap<String, String> headersMap) {
        headers = Mockito.mock(HttpHeaders.class);
        for (Entry<String, List<String>> entry : headersMap.entrySet()) {
            when(headers.getRequestHeader(entry.getKey())).thenReturn(entry.getValue());
        }
        when(headers.getRequestHeaders()).thenReturn(headersMap);
    }

    @Test
    public void testCreateRelationship() throws Exception {
        String postEdgeBody = TestUtil.getFileAsString("aai-resource-service/post-edge-auto-props.json");

        Response response = callCreateRelationship(postEdgeBody);

        assertThat(response.getStatus()).isEqualTo(Status.CREATED.getStatusCode());
    }

    @Test
    public void testCreateRelationshipWithMatchingType() throws Exception {
        String postEdgeBody = TestUtil.getFileAsString("aai-resource-service/post-edge-auto-props.json");
        String type = "tosca.relationships.HostedOn";

        Response response = createRelationshipWithType(postEdgeBody, type);

        assertThat(response.getStatus()).isEqualTo(Status.CREATED.getStatusCode());
    }

    @Test
    public void testCreateRelationshipNoMatchingType() throws Exception {
        String postEdgeBody = TestUtil.getFileAsString("aai-resource-service/post-edge-auto-props.json");
        String type = "type.does.not.match";

        Response response = createRelationshipWithType(postEdgeBody, type);

        assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testCreateRelationshipWithTypeNullPropsIsBadRequest() throws Exception {
        String postEdgeBody = TestUtil.getFileAsString("aai-resource-service/post-edge-null-props.json");
        String type = "tosca.relationships.HostedOn";

        Response response = createRelationshipWithType(postEdgeBody, type);

        assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testCreateRelationshipWithTypeWithIdIsBadRequest() throws Exception {
        String postEdgeBody = TestUtil.getFileAsString("aai-resource-service/post-edge-with-id.json");
        String type = "tosca.relationships.HostedOn";

        Response response = createRelationshipWithType(postEdgeBody, type);

        assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testInvalidUser() throws Exception {
        String postEdgeBody = TestUtil.getFileAsString("aai-resource-service/post-edge-auto-props.json");

        setUser("CN=INVALID, OU=INVALID, O=INVALID, L=Ottawa, ST=Ontario, C=CA");

        Response response = callCreateRelationship(postEdgeBody);

        assertThat(response.getStatus()).isEqualTo(Status.FORBIDDEN.getStatusCode());
    }

    @Test
    public void testNullPropertiesIsBadRequest() throws Exception {
        String postEdgeBody = TestUtil.getFileAsString("aai-resource-service/post-edge-null-props.json");

        Response response = callCreateRelationship(postEdgeBody);

        assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testNoPropertiesIsBadRequest() throws Exception {
        String postEdgeBody = TestUtil.getFileAsString("aai-resource-service/post-edge-no-props.json");

        Response response = callCreateRelationship(postEdgeBody);

        assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testCreateWithIdIsBadRequest() throws Exception {
        String postEdgeBody = TestUtil.getFileAsString("aai-resource-service/post-edge-with-id.json");

        Response response = callCreateRelationship(postEdgeBody);

        assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testNoTypeIsBadRequest() throws Exception {
        String postEdgeBody = TestUtil.getFileAsString("aai-resource-service/post-edge-no-type.json");

        Response response = callCreateRelationship(postEdgeBody);

        assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testPatchEdge() throws Exception {
        MultivaluedHashMap<String, String> headersMap = getCreateHeaders();
        headersMap.put(AaiResourceService.HTTP_PATCH_METHOD_OVERRIDE, createSingletonList("PATCH"));
        mockHeaderRequests(headersMap);

        String postEdgeBody = TestUtil.getFileAsString("aai-resource-service/post-edge-upsert.json");
        String type = "tosca.relationships.HostedOn";
        String id = "12345";
        EntityTag entityTag = new EntityTag("1234");
        
        when(graphDataService.patchEdge(any(), any(), any(), any())).thenReturn(new ImmutablePair<EntityTag, String>(entityTag, "dummy output"));
        AaiResourceService aaiResourceService = new AaiResourceService(graphDataService);
        Response response =
                aaiResourceService.upsertEdge(postEdgeBody, type, id, "uri", headers, uriInfo, servletRequest);
        assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
    }

    @Test
    public void testUpdateEdge() throws Exception {
        String postEdgeBody = TestUtil.getFileAsString("aai-resource-service/post-edge-upsert.json");
        String type = "tosca.relationships.HostedOn";
        String id = "12345";
        EntityTag entityTag = new EntityTag("1234");

        when(graphDataService.updateEdge(any(), any(), any(), any())).thenReturn(new ImmutablePair<EntityTag, String>(entityTag, "dummy output"));
        AaiResourceService aaiResourceService = new AaiResourceService(graphDataService);
        Response response =
                aaiResourceService.upsertEdge(postEdgeBody, type, id, "uri", headers, uriInfo, servletRequest);
        assertThat(response.getStatus()).isEqualTo(Status.OK.getStatusCode());
    }

    @Test
    public void testUpdateEdgeNullPropsIsBadRequest() throws Exception {
        String postEdgeBody = TestUtil.getFileAsString("aai-resource-service/post-edge-null-props.json");
        String type = "tosca.relationships.HostedOn";
        String id = "12345";
        EntityTag entityTag = new EntityTag("1234");

        when(graphDataService.updateEdge(any(), any(), any(), any())).thenReturn(new ImmutablePair<EntityTag, String>(entityTag, "dummy output"));
        AaiResourceService aaiResourceService = new AaiResourceService(graphDataService);
        Response response =
                aaiResourceService.upsertEdge(postEdgeBody, type, id, "uri", headers, uriInfo, servletRequest);
        assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    public void testUpdateEdgeWithMismatchIdIsBadRequest() throws Exception {
        String postEdgeBody = TestUtil.getFileAsString("aai-resource-service/post-edge-upsert.json");
        String type = "tosca.relationships.HostedOn";
        String id = "mismatch";
        EntityTag entityTag = new EntityTag("1234");

        when(graphDataService.updateEdge(any(), any(), any(), any())).thenReturn(new ImmutablePair<EntityTag, String>(entityTag, "dummy output"));
        AaiResourceService aaiResourceService = new AaiResourceService(graphDataService);
        Response response =
                aaiResourceService.upsertEdge(postEdgeBody, type, id, "uri", headers, uriInfo, servletRequest);
        assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
    }

    private Response createRelationshipWithType(String postEdgeBody, String type) throws CrudException, Exception {
        EntityTag entityTag = new EntityTag("1234");
        
        when(graphDataService.addEdge(any(), any(), any())).thenReturn(new ImmutablePair<EntityTag, String>(entityTag, "dummy output"));
        AaiResourceService aaiResourceService = new AaiResourceService(graphDataService);
        Response response =
                aaiResourceService.createRelationship(postEdgeBody, type, "uri", headers, uriInfo, servletRequest);
        return response;
    }

    private Response callCreateRelationship(String postEdgeBody) throws CrudException, Exception {
        EntityTag entityTag = new EntityTag("1234");
        
        when(graphDataService.addEdge(any(), any(), any())).thenReturn(new ImmutablePair<EntityTag, String>(entityTag, "dummy output"));
        AaiResourceService aaiResourceService = new AaiResourceService(graphDataService);
        Response response =
                aaiResourceService.createRelationship(postEdgeBody, "uri", headers, uriInfo, servletRequest);
        return response;
    }

    private void setUser(String user) {
        X509Certificate mockCertificate = Mockito.mock(X509Certificate.class);
        when(mockCertificate.getSubjectX500Principal()).thenReturn(new X500Principal(user));
        servletRequest.setAttribute("javax.servlet.request.X509Certificate", new X509Certificate[] {mockCertificate});
    }

    private List<String> createSingletonList(String listItem) {
        return Collections.<String>singletonList(listItem);
    }
}
