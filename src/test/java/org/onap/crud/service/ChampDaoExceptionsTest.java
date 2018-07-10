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

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import org.junit.Before;
import org.junit.Test;
import org.onap.aai.cl.mdc.MdcContext;
import org.onap.aai.restclient.client.OperationResult;
import org.onap.aai.restclient.client.RestClient;
import org.onap.crud.dao.champ.ChampDao;
import org.onap.crud.entity.Edge;
import org.onap.crud.entity.Vertex;
import org.onap.crud.exception.CrudException;
import org.slf4j.MDC;

public class ChampDaoExceptionsTest {
    // @formatter:off
		private final String champVertex = "{" +
		    "\"key\": \"test-uuid\"," +
		    "\"type\": \"vertexType\"," +
		    "\"properties\": {" +
		    "\"fqdn\": \"myhost.onap.com\"," +
		    "\"hostname\": \"myhost\" } }";
		
		private final String champEdge = "{" +
			    "\"key\": \"test-uuid\"," +
			    "\"type\": \"edgeType\"," +
			    "\"properties\": {" +
			    "\"prevent-delete\": \"NONE\" }," +
			    "\"source\": {" +
			    "\"key\": \"50bdab41-ad1c-4d00-952c-a0aa5d827811\", \"type\": \"vserver\"}," +
			    "\"target\": {" +
			    "\"key\": \"1d326bc7-b985-492b-9604-0d5d1f06f908\", \"type\": \"pserver\"}" +
			    " }";
		
		private final String vertexPayload = "{" + 
			"\"type\":\"pserver\"," + 
			"\"properties\":{" + 
			"\"aai-node-type\":\"pserver\"}}";
		// @formatter:on

    private RestClient restClientMock;
    private ChampDao champDao;

    static final String CHAMP_URL = "https://host:9522/services/champ-service/v1/";
    static final String OBJECT_SUB_URL = "objects";
    static final String RELATIONSHIP_SUB_URL = "relationships";
    static final String TRANSACTION_SUB_URL = "transaction";
    static final String BASE_OBJECT_URL = CHAMP_URL + OBJECT_SUB_URL;
    static final String HEADER_FROM_APP = "X-FromAppId";
    static final String HEADER_TRANS_ID = "X-TransactionId";
    static final String FROM_APP_NAME = "Gizmo";

    @Before
    public void setup() {
        restClientMock = mock(RestClient.class);
    }

    @Test
    public void testGetVertexIdNotExists() {
        String id = "test-id";
        String idNotExists = "test-id-not-exists";
        String type = "pserver";
        String version = "v11";
        String failureCauseForGetVertex = "No vertex with id " + id + " found in graph";

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("hostname", "myhost");
        mockGetVertex(idNotExists, "", "", type, 404, failureCauseForGetVertex);
        buildChampDao();

        try {
            champDao.getVertex(idNotExists, version);
        } catch (CrudException e) {
            assertEquals(404, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString(failureCauseForGetVertex));
        }
    }

    @Test
    public void testGetVertexIdNotExistsWithQueryParams() {
        String id = "test-id";
        String idNotExists = "test-id-not-exists";
        String queryParamsForMock = "?hostname=myhost";
        String type = "pserver";
        String version = "v11";
        String failureCauseForGetVertex = "No vertex with id " + id + " found in graph";

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("hostname", "myhost");
        mockGetVertex(idNotExists, queryParamsForMock, "", type, 404, failureCauseForGetVertex);
        buildChampDao();

        try {
            champDao.getVertex(idNotExists, type, version, queryParams);
        } catch (CrudException e) {
            assertEquals(404, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString(failureCauseForGetVertex));
        }
    }

    @Test
    public void testGetVertexWithQueryParamsTypeNotMatch() {
        String id = "test-id";
        String queryParamsForMock = "?hostname=myhost";
        String type = "pserver";
        String version = "v11";
        String failureCauseForGetVertexTypeNotMatches = "No vertex with id " + id + " and type vserver found in graph";

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("hostname", "myhost");
        mockGetVertex(id, queryParamsForMock, "", type, 200, "");
        buildChampDao();

        try {
            champDao.getVertex(id, "vserver", version, queryParams);
        } catch (CrudException e) {
            assertEquals(404, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString(failureCauseForGetVertexTypeNotMatches));
        }
    }

    @Test
    public void testGetVertexIdNotExistsWithTxId() {
        String id = "test-id";
        String idNotExists = "test-id-not-exists";
        String txId = "1234";
        String type = "pserver";
        String version = "v11";
        String failureCauseForGetVertex = "No vertex with id " + id + " found in graph";

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("hostname", "myhost");
        mockGetVertex(idNotExists, "", txId, type, 404, failureCauseForGetVertex);
        buildChampDao();

        try {
            champDao.getVertex(idNotExists, type, version, txId);
        } catch (CrudException e) {
            assertEquals(404, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString(failureCauseForGetVertex));
        }
    }

    @Test
    public void testGetVertexWithTxIdAndTypeNotMatch() {
        String id = "test-id";
        String txId = "1234";
        String type = "pserver";
        String version = "v11";
        String failureCauseForGetVertexTypeNotMatches = "No vertex with id " + id + " and type vserver found in graph";

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("hostname", "myhost");
        mockGetVertex(id, "", txId, type, 200, "");
        buildChampDao();

        try {
            champDao.getVertex(id, "vserver", version, txId);
        } catch (CrudException e) {
            assertEquals(404, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString(failureCauseForGetVertexTypeNotMatches));
        }
    }

    @Test
    public void testGetVertices() {
        String queryParamsForMockGetVertices = "?aai-node-type=pserver";
        String type = "pserver";
        String version = "v11";
        String failureCauseForGetVertices = "No vertices found in graph for given filters";

        Map<String, Object> filter = new HashMap<>();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("hostname", "myhost");
        mockGetVertices(queryParamsForMockGetVertices, type, 404, failureCauseForGetVertices);
        buildChampDao();

        try {
            champDao.getVertices(type, filter, version);
        } catch (CrudException e) {
            assertEquals(404, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString(failureCauseForGetVertices));
        }
    }

    @Test
    public void testGetEdgeIdNotExists() {
        String idNotExists = "test-id-not-exists";
        String id = "test-id";
        String txId = "1234";
        String type = "tosca.relationships.HostedOn";
        String failureCauseForGetEdge = "No edge with id " + id + " found in graph";

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("hostname", "myhost");
        mockGetEdge(idNotExists, "", txId, type, 404, failureCauseForGetEdge);
        buildChampDao();

        try {
            champDao.getEdge(idNotExists, type, txId);
        } catch (CrudException e) {
            assertEquals(404, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString(failureCauseForGetEdge));
        }
    }

    @Test
    public void testGetEdgeTypeNotMatch() {
        String id = "test-id";
        String txId = "1234";
        String type = "tosca.relationships.HostedOn";
        String failureCauseForGetEdgeTypeNotMatches = "No edge with id " + id + " and type " + "" + " found in graph";

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("hostname", "myhost");
        mockGetEdge(id, "", txId, type, 200, "");
        buildChampDao();

        // Type not matches
        try {
            champDao.getEdge(id, "", txId);
        } catch (CrudException e) {
            assertEquals(404, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString(failureCauseForGetEdgeTypeNotMatches));
        }
    }

    @Test
    public void testGetEdgeIdNotExistsWithQueryParams() {
        String idNotExists = "test-id-not-exists";
        String id = "test-id";
        String queryParamsForMock = "?hostname=myhost";
        String type = "tosca.relationships.HostedOn";
        String failureCauseForGetEdge = "No edge with id " + id + " found in graph";

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("hostname", "myhost");
        mockGetEdge(idNotExists, queryParamsForMock, "", type, 404, failureCauseForGetEdge);
        buildChampDao();

        try {
            champDao.getEdge(idNotExists, type, queryParams);
        } catch (CrudException e) {
            assertEquals(404, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString(failureCauseForGetEdge));
        }
    }

    @Test
    public void testGetEdgeTypeNotMatchWithQueryParams() {
        String id = "test-id";
        String queryParamsForMock = "?hostname=myhost";
        String type = "tosca.relationships.HostedOn";
        String failureCauseForGetEdgeTypeNotMatches = "No edge with id " + id + " and type " + "" + " found in graph";

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("hostname", "myhost");
        mockGetEdge(id, queryParamsForMock, "", type, 200, "");
        buildChampDao();

        // Type not matches
        try {
            champDao.getEdge(id, "", queryParams);
        } catch (CrudException e) {
            assertEquals(404, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString(failureCauseForGetEdgeTypeNotMatches));
        }
    }

    @Test
    public void testGetEdges() {
        String type = "tosca.relationships.HostedOn";
        String failureCauseForGetEdges = "No edges found in graph for given filters";

        Map<String, Object> filter = new HashMap<>();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("hostname", "myhost");
        mockGetEdges("?", type, 404, failureCauseForGetEdges);
        buildChampDao();

        try {
            champDao.getEdges(type, filter);
        } catch (CrudException e) {
            assertEquals(404, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString(failureCauseForGetEdges));
        }
    }

    @Test
    public void testGetVertexEdges() {
        String idNotExists = "test-id-not-exists";
        String id = "test-id";
        String queryParamsForMock = "?hostname=myhost";
        String type = "tosca.relationships.HostedOn";
        String failureCauseForGetVertexEdges = "No vertex with id " + id + " found in graph";

        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("hostname", "myhost");
        mockGetVertexEdges(idNotExists, queryParamsForMock, type, 404, failureCauseForGetVertexEdges);
        buildChampDao();

        try {
            champDao.getVertexEdges(idNotExists, queryParams);
        } catch (CrudException e) {
            assertEquals(404, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString(failureCauseForGetVertexEdges));
        }
    }

    @Test
    public void addVertexTest() {
        String type = "pserver";
        String txId = "1234";
        String version = "v11";

        Map<String, Object> properties = new HashMap<>();

        mockAddVertex(type, vertexPayload, "", 400);
        mockAddVertex(type, vertexPayload, txId, 400);
        buildChampDao();

        try {
            champDao.addVertex(type, properties, version);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString("Failed to create vertex"));
        }

        try {
            champDao.addVertex(type, properties, version, txId);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString("Failed to create vertex"));
        }
    }

    @Test
    public void addEdgeTest() throws CrudException {
        String txId = "1234";
        String vertexType = "pserver";
        String edgeType = "tosca.relationships.HostedOn";
        String version = "v11";

        Map<String, Object> properties = new HashMap<>();

        mockGetVertex("test-uuid", "", "", "pserver", 200, "");
        mockGetVertex("test-uuid", "", txId, "pserver", 200, "");
        mockAddEdge(edgeType, "", 400);
        mockAddEdge(edgeType, txId, 400);
        buildChampDao();

        String vertex = champVertex.replace("vertexType", vertexType);
        Vertex source = Vertex.fromJson(vertex, "v11");
        Vertex target = Vertex.fromJson(vertex, "v11");

        try {
            champDao.addEdge(edgeType, source, target, properties, version);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString("Failed to create edge"));
        }

        try {
            champDao.addEdge(edgeType, source, target, properties, version, txId);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString("Failed to create edge"));
        }
    }

    @Test
    public void updateVertexTest() {
        String id = "test-id";
        String type = "pserver";
        String txId = "1234";
        String version = "v11";

        Map<String, Object> properties = new HashMap<>();

        mockPutVertex(id, type, "", 400);
        mockPutVertex(id, type, txId, 400);
        buildChampDao();

        try {
            champDao.updateVertex(id, type, properties, version);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString("Failed to update vertex"));
        }

        try {
            champDao.updateVertex(id, type, properties, version, txId);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString("Failed to update vertex"));
        }
    }

    @Test
    public void updateEdgeTest() {
        String id = "test-uuid";
        String txId = "1234";
        String type = "tosca.relationships.HostedOn";

        mockPutEdge(id, type, "", 400);
        mockPutEdge(id, type, txId, 400);
        buildChampDao();

        String champJson = champEdge.replace("\"test-uuid\"", "null").replace("edgeType", type);
        Edge edge = Edge.fromJson(champJson);

        try {
            champDao.updateEdge(edge);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString("Unable to identify edge"));
        }

        try {
            champDao.updateEdge(edge, txId);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString("Unable to identify edge"));
        }

        champJson = champEdge.replace("edgeType", type);
        edge = Edge.fromJson(champJson);

        try {
            champDao.updateEdge(edge);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString("Failed to update edge"));
        }

        try {
            champDao.updateEdge(edge, txId);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString("Failed to update edge"));
        }
    }

    @Test
    public void deleteVertexTest() {
        String id = "test-id";
        String type = "pserver";
        String txId = "1234";

        mockDeleteVertex(id, type, "", 400);
        mockDeleteVertex(id, type, txId, 400);
        buildChampDao();

        try {
            champDao.deleteVertex(id, type);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString("Failed to delete vertex"));
        }
        try {
            champDao.deleteVertex(id, type, txId);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString("Failed to delete vertex"));
        }
    }

    @Test
    public void deleteEdgeTest() {
        String id = "test-uuid";
        String txId = "1234";
        String type = "tosca.relationships.HostedOn";
        String failureCauseFordeleteEdge = "No edge with id " + id + " found in graph";

        mockDeleteEdge(id, type, "", 400, failureCauseFordeleteEdge);
        mockDeleteEdge(id, type, txId, 400, failureCauseFordeleteEdge);
        buildChampDao();

        try {
            champDao.deleteEdge(id, type);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString(failureCauseFordeleteEdge));
        }
        try {
            champDao.deleteEdge(id, type, txId);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString(failureCauseFordeleteEdge));
        }
    }

    @Test
    public void transactionsTest() {
        String id = "test-id";
        int resultCode = 500;

        mockOpenTransaction(resultCode);
        mockRollbackTransaction(id, resultCode);
        mockCommitTransaction(id, resultCode);
        buildChampDao();

        String response = champDao.openTransaction();
        assertEquals(null, response);

        try {
            champDao.rollbackTransaction(id);
        } catch (CrudException e) {
            assertEquals(500, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString("Unable to rollback transaction"));
        }

        try {
            champDao.commitTransaction(id);
        } catch (CrudException e) {
            assertEquals(500, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), containsString("Unable to commit transaction"));
        }
    }

    public void buildChampDao() {
        String baseRelationshipUrl = CHAMP_URL + RELATIONSHIP_SUB_URL;
        String baseTransactionUrl = CHAMP_URL + TRANSACTION_SUB_URL;
        champDao = new ChampDao(restClientMock, BASE_OBJECT_URL, baseRelationshipUrl, baseTransactionUrl);
    }

    public void mockOpenTransaction(int resultCode) {
        OperationResult operationResult = new OperationResult();
        operationResult.setResult("");
        operationResult.setResultCode(resultCode);
        String url = CHAMP_URL + "transaction";

        when(restClientMock.post(url, "", createHeader(), MediaType.TEXT_PLAIN_TYPE, MediaType.TEXT_PLAIN_TYPE))
                .thenReturn(operationResult);
    }

    public void mockRollbackTransaction(String id, int resultCode) {
        OperationResult operationResult = new OperationResult();
        operationResult.setResult("");
        operationResult.setResultCode(resultCode);
        String url = CHAMP_URL + TRANSACTION_SUB_URL + "/" + id;

        when(restClientMock.put(url, "{\"method\": \"rollback\"}", createHeader(), MediaType.APPLICATION_JSON_TYPE,
                MediaType.TEXT_PLAIN_TYPE)).thenReturn(operationResult);
    }

    public void mockCommitTransaction(String id, int resultCode) {
        OperationResult operationResult = new OperationResult();
        operationResult.setResult("");
        operationResult.setResultCode(resultCode);
        String url = CHAMP_URL + TRANSACTION_SUB_URL + "/" + id;

        when(restClientMock.put(url, "{\"method\": \"commit\"}", createHeader(), MediaType.APPLICATION_JSON_TYPE,
                MediaType.TEXT_PLAIN_TYPE)).thenReturn(operationResult);
    }

    public void mockGetVertex(String id, String queryParams, String txId, String type, int resultCode,
            String failureCause) {
        String vertexResponse = champVertex.replace("vertexType", type);
        OperationResult operationResult = new OperationResult();
        operationResult.setResult(vertexResponse);
        operationResult.setResultCode(resultCode);
        operationResult.setFailureCause(failureCause);
        String url;

        if (queryParams != null && !queryParams.isEmpty() && (txId.isEmpty() || txId == null)) {
            url = BASE_OBJECT_URL + "/" + id + queryParams;
        } else if (txId != null && !txId.isEmpty() && (queryParams.isEmpty() || queryParams == null)) {
            url = BASE_OBJECT_URL + "/" + id + "?transactionId=" + txId;
        } else {
            url = BASE_OBJECT_URL + "/" + id;
        }

        when(restClientMock.get(url, createHeader(), MediaType.APPLICATION_JSON_TYPE)).thenReturn(operationResult);
    }

    public void mockGetVertexEdges(String id, String queryParams, String type, int resultCode, String failureCause) {
        String edgeResponse = champEdge.replace("edgeType", type);
        OperationResult operationResult = new OperationResult();
        List<String> edgeResponselist = new ArrayList<>();
        edgeResponselist.add(edgeResponse);
        operationResult.setResult(edgeResponselist.toString());
        operationResult.setResultCode(resultCode);
        operationResult.setFailureCause(failureCause);

        String url = BASE_OBJECT_URL + "/" + RELATIONSHIP_SUB_URL + "/" + id + queryParams;

        when(restClientMock.get(url, createHeader(), MediaType.APPLICATION_JSON_TYPE)).thenReturn(operationResult);
    }

    public void mockGetVertices(String queryParams, String type, int resultCode, String failureCause) {
        String vertexResponse = champVertex.replace("vertexType", type);
        OperationResult operationResult = new OperationResult();
        List<String> vertexResponselist = new ArrayList<>();
        vertexResponselist.add(vertexResponse);
        operationResult.setResult(vertexResponselist.toString());
        operationResult.setResultCode(resultCode);
        operationResult.setFailureCause(failureCause);

        String url = BASE_OBJECT_URL + "/" + "filter" + queryParams;

        when(restClientMock.get(url, createHeader(), MediaType.APPLICATION_JSON_TYPE)).thenReturn(operationResult);
    }

    public void mockGetEdges(String queryParams, String type, int resultCode, String failureCause) {
        String edgeResponse = champEdge.replace("edgeType", type);
        OperationResult operationResult = new OperationResult();
        List<String> edgeResponselist = new ArrayList<>();
        edgeResponselist.add(edgeResponse);
        operationResult.setResult(edgeResponselist.toString());
        operationResult.setResultCode(resultCode);
        operationResult.setFailureCause(failureCause);

        String url = CHAMP_URL + RELATIONSHIP_SUB_URL + "/" + "filter" + queryParams;

        when(restClientMock.get(url, createHeader(), MediaType.APPLICATION_JSON_TYPE)).thenReturn(operationResult);
    }

    public void mockGetEdge(String id, String queryParams, String txId, String type, int resultCode,
            String failureCause) {
        String edgeResponse = champEdge.replace("edgeType", type);
        OperationResult operationResult = new OperationResult();
        operationResult.setResult(edgeResponse);
        operationResult.setResultCode(resultCode);
        operationResult.setFailureCause(failureCause);

        String url;

        if (queryParams != null && !queryParams.isEmpty() && (txId.isEmpty() || txId == null)) {
            url = CHAMP_URL + RELATIONSHIP_SUB_URL + "/" + id + queryParams;
        } else if (txId != null && !txId.isEmpty() && (queryParams.isEmpty() || queryParams == null)) {
            url = CHAMP_URL + RELATIONSHIP_SUB_URL + "/" + id + "?transactionId=" + txId;
        } else {
            url = CHAMP_URL + RELATIONSHIP_SUB_URL + "/" + id;
        }

        when(restClientMock.get(url, createHeader(), MediaType.APPLICATION_JSON_TYPE)).thenReturn(operationResult);
    }

    public void mockAddEdge(String type, String txId, int resultCode) {
        String edgeResponse = champEdge.replace("edgeType", type);
        OperationResult operationResult = new OperationResult();
        operationResult.setResult(edgeResponse);
        operationResult.setResultCode(resultCode);

        String url;
        if (txId != null && !txId.isEmpty()) {
            url = CHAMP_URL + RELATIONSHIP_SUB_URL + "?transactionId=" + txId;
        } else {
            url = CHAMP_URL + RELATIONSHIP_SUB_URL;
        }

        when(restClientMock.post(eq(url), anyString(), eq(createHeader()), eq(MediaType.APPLICATION_JSON_TYPE),
                eq(MediaType.APPLICATION_JSON_TYPE))).thenReturn(operationResult);
    }

    public void mockAddVertex(String type, String payload, String txId, int resultCode) {
        String vertexResponse = champVertex.replace("vertexType", type);
        OperationResult operationResult = new OperationResult();
        operationResult.setResult(vertexResponse);
        operationResult.setResultCode(resultCode);

        String url;
        if (txId != null && !txId.isEmpty()) {
            url = BASE_OBJECT_URL + "?transactionId=" + txId;
        } else {
            url = BASE_OBJECT_URL;
        }

        when(restClientMock.post(url, payload, createHeader(), MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE)).thenReturn(operationResult);
    }

    public void mockPutVertex(String id, String type, String txId, int resultCode) {
        String vertexResponse = champVertex.replace("vertexType", type);
        OperationResult operationResult = new OperationResult();
        operationResult.setResult(vertexResponse);
        operationResult.setResultCode(resultCode);

        String url;
        if (txId != null && !txId.isEmpty()) {
            url = BASE_OBJECT_URL + "/" + id + "?transactionId=" + txId;
        } else {
            url = BASE_OBJECT_URL + "/" + id;
        }

        when(restClientMock.put(eq(url), anyString(), eq(createHeader()), eq(MediaType.APPLICATION_JSON_TYPE),
                eq(MediaType.APPLICATION_JSON_TYPE))).thenReturn(operationResult);
    }

    public void mockPutEdge(String id, String type, String txId, int resultCode) {
        String edgeResponse = champEdge.replace("edgeType", type);
        OperationResult operationResult = new OperationResult();
        operationResult.setResult(edgeResponse);
        operationResult.setResultCode(resultCode);

        String url;
        if (txId != null && !txId.isEmpty()) {
            url = CHAMP_URL + RELATIONSHIP_SUB_URL + "/" + id + "?transactionId=" + txId;
        } else {
            url = CHAMP_URL + RELATIONSHIP_SUB_URL + "/" + id;
        }

        when(restClientMock.put(eq(url), anyString(), eq(createHeader()), eq(MediaType.APPLICATION_JSON_TYPE),
                eq(MediaType.APPLICATION_JSON_TYPE))).thenReturn(operationResult);
    }

    public void mockDeleteVertex(String id, String type, String txId, int resultCode) {
        String vertexResponse = champVertex.replace("vertexType", type);
        OperationResult operationResult = new OperationResult();
        operationResult.setResult(vertexResponse);
        operationResult.setResultCode(resultCode);

        String url;
        if (txId != null && !txId.isEmpty()) {
            url = BASE_OBJECT_URL + "/" + id + "?transactionId=" + txId;
        } else {
            url = BASE_OBJECT_URL + "/" + id;
        }

        when(restClientMock.delete(url, createHeader(), MediaType.APPLICATION_JSON_TYPE)).thenReturn(operationResult);
    }

    public void mockDeleteEdge(String id, String type, String txId, int resultCode, String failureCause) {
        String edgeResponse = champEdge.replace("edgeType", type);
        OperationResult operationResult = new OperationResult();
        operationResult.setResult(edgeResponse);
        operationResult.setResultCode(resultCode);
        operationResult.setFailureCause(failureCause);

        String url;
        if (txId != null && !txId.isEmpty()) {
            url = CHAMP_URL + RELATIONSHIP_SUB_URL + "/" + id + "?transactionId=" + txId;
        } else {
            url = CHAMP_URL + RELATIONSHIP_SUB_URL + "/" + id;
        }

        when(restClientMock.delete(url, createHeader(), MediaType.APPLICATION_JSON_TYPE)).thenReturn(operationResult);
    }

    public ChampDao getChampDao() {
        return champDao;
    }

    public void setChampDao(ChampDao champDao) {
        this.champDao = champDao;
    }

    private Map<String, List<String>> createHeader() {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put(HEADER_FROM_APP, Arrays.asList(FROM_APP_NAME));
        headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(MdcContext.MDC_REQUEST_ID)));
        return headers;
    }
}
