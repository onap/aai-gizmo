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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import org.onap.aai.restclient.client.OperationResult;
import org.onap.aai.restclient.client.RestClient;
import org.onap.crud.dao.champ.ChampDao;

public class ChampDaoMockTest {
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

	private final String edgePayload = "{" +
		"\"type\":\"tosca.relationships.HostedOn\"," +
		"\"properties\":{" +
		"\"prevent-delete\":\"NONE\"}," +
		"\"source\":{" +
		"\"key\":\"test-uuid\"," +
		"\"type\":\"vserver\"," +
		"\"properties\":{}}," +
		"\"target\":{" +
		"\"key\":\"test-uuid\"," +
		"\"type\":\"pserver\"," +
		"\"properties\":{" +
		"\"hostname\":\"myhost\"," +
		"\"fqdn\":\"myhost.onap.com\"}}}";

	private final String edgePayloadForPut = "{" +
		"\"key\":\"test-uuid\"," +
		"\"type\":\"tosca.relationships.HostedOn\"," +
		"\"properties\":{" +
		"\"prevent-delete\":\"NONE\"}," +
		"\"source\":{" +
		"\"key\":\"50bdab41-ad1c-4d00-952c-a0aa5d827811\"," +
		"\"type\":\"vserver\"," +
		"\"properties\":{}}," +
		"\"target\":{" +
		"\"key\":\"1d326bc7-b985-492b-9604-0d5d1f06f908\"," +
		"\"type\":\"pserver\"," +
		"\"properties\":{}}}";

	private final String edgePayloadForPatch = "{" +
		"\"key\":\"test-uuid\"," +
		"\"type\":\"tosca.relationships.HostedOn\"," +
		"\"properties\":{" +
		"\"prevent-delete\":\"NONE\"}," +
		"\"source\":{" +
		"\"key\":\"50bdab41-ad1c-4d00-952c-a0aa5d827811\"," +
		"\"type\":\"vserver\"}," +
		"\"target\":{" +
		"\"key\":\"1d326bc7-b985-492b-9604-0d5d1f06f908\"," +
		"\"type\":\"pserver\"}}";

	private final String edgePayloadForPost = "{" +
		"\"type\":\"tosca.relationships.HostedOn\"," +
		"\"properties\":{" +
		"\"SVC-INFRA\":\"OUT\"," +
		"\"prevent-delete\":\"IN\"," +
		"\"delete-other-v\":\"NONE\"," +
		"\"contains-other-v\":\"NONE\"" +
		"}," +
		"\"source\":{" +
		"\"key\":\"test-uuid\"," +
		"\"type\":\"vserver\"," +
		"\"properties\":{" +
		"" +
		"}" +
		"}," +
		"\"target\":{" +
		"\"key\":\"test-uuid\"," +
		"\"type\":\"pserver\"," +
		"\"properties\":{" +
		"\"hostname\":\"myhost\"," +
		"\"fqdn\":\"myhost.onap.com\"" +
		"}" +
		"}" +
		"}";

	private final String edgePayloadForPutNoProperties = "{" +
		"\"key\":\"test-uuid\"," +
		"\"type\":\"tosca.relationships.HostedOn\"," +
		"\"properties\":{" +
		"\"SVC-INFRA\":\"OUT\"," +
		"\"prevent-delete\":\"IN\"," +
		"\"delete-other-v\":\"NONE\"," +
		"\"contains-other-v\":\"NONE\"" +
		"}," +
		"\"source\":{" +
		"\"key\":\"50bdab41-ad1c-4d00-952c-a0aa5d827811\"," +
		"\"type\":\"vserver\"," +
		"\"properties\":{" +
		"" +
		"}" +
		"}," +
		"\"target\":{" +
		"\"key\":\"1d326bc7-b985-492b-9604-0d5d1f06f908\"," +
		"\"type\":\"pserver\"," +
		"\"properties\":{" +
		"" +
		"}" +
		"}" +
		"}";

	private final String vertexPayload = "{" +
		"\"type\":\"pserver\"," +
		"\"properties\":{" +
		"\"hostname\":\"myhost\"," +
		"\"in-maint\":false," +
		"\"fqdn\":\"myhost.onap.com\"," +
		"\"last-mod-source-of-truth\":\"source-of-truth\"," +
		"\"source-of-truth\":\"source-of-truth\"," +
		"\"aai-node-type\":\"pserver\"}}";

	private final String vertexPayloadForVserver = "{" +
		"\"type\":\"vserver\"," +
		"\"properties\":{" +
		"\"in-maint\":false," +
		"\"vserver-name\":\"test-vserver\"," +
		"\"vserver-id\":\"VSER1\"," +
		"\"last-mod-source-of-truth\":\"source-of-truth\"," +
		"\"vserver-name2\":\"alt-test-vserver\"," +
		"\"source-of-truth\":\"source-of-truth\"," +
		"\"vserver-selflink\":\"http://1.2.3.4/moreInfo\"," +
		"\"is-closed-loop-disabled\":false," +
		"\"aai-node-type\":\"vserver\"}}";

	private final String vertexPayloadForPserver = "{" +
		"\"key\":\"50bdab41-ad1c-4d00-952c-a0aa5d827811\"," +
		"\"type\":\"pserver\"," +
		"\"properties\":{" +
		"\"ptnii-equip-name\":\"e-name\"," +
		"\"hostname\":\"steve-host2\"," +
		"\"equip-type\":\"server\"," +
		"\"equip-vendor\":\"HP\"," +
		"\"equip-model\":\"DL380p-nd\"," +
		"\"in-maint\":false," +
		"\"fqdn\":\"myhost.onap.net\"," +
		"\"purpose\":\"my-purpose\"," +
		"\"ipv4-oam-address\":\"1.2.3.4\"," +
		"\"last-mod-source-of-truth\":\"source-of-truth\"," +
		"\"aai-node-type\":\"pserver\"}}";

	private final String vertexPayloadForPut = "{" +
		"\"key\":\"test-uuid\"," +
		"\"type\":\"pserver\"," +
		"\"properties\":{" +
		"\"hostname\":\"myhost\"," +
		"\"in-maint\":false," +
		"\"fqdn\":\"myhost.onap.com\"," +
		"\"last-mod-source-of-truth\":\"source-of-truth\"," +
		"\"aai-node-type\":\"pserver\"}}";

	private final String vertexPayloadForPatch = "{" +
		"\"key\":\"test-uuid\"," +
		"\"type\":\"pserver\"," +
		"\"properties\":{" +
		"\"hostname\":\"myhost\"," +
		"\"fqdn\":\"myhost.onap.com\"," +
		"\"last-mod-source-of-truth\":\"source-of-truth\"," +
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
    static final String HEADER_TRANS_ID_VALUE = "1234567890";

    ChampDaoMockTest() {
        restClientMock = mock(RestClient.class);
        init();
        buildChampDao();
    }

    public void init() {

        Map<String, String> queryParamsVertex = new HashMap<>();
        queryParamsVertex.put("_reserved_version", "v11");
        queryParamsVertex.put("hostname", "myhost");
        queryParamsVertex.put("_reserved_aai-type", "pserver");

        Map<String, String> queryParamsVertexV13 = new HashMap<>();
        queryParamsVertexV13.put("_reserved_version", "v13");
        queryParamsVertexV13.put("hostname", "myhost");
        queryParamsVertexV13.put("_reserved_aai-type", "pserver");

        Map<String, String> queryParamsVertices = new HashMap<>();
        queryParamsVertices.put("_reserved_version", "v11");
        queryParamsVertices.put("hostname", "myhost");
        queryParamsVertices.put("_reserved_aai-type", "pserver");
        queryParamsVertices.put("aai-node-type", "pserver");

        Map<String, String> queryParamsVerticesV13 = new HashMap<>();
        queryParamsVerticesV13.put("_reserved_version", "v13");
        queryParamsVerticesV13.put("hostname", "myhost");
        queryParamsVerticesV13.put("_reserved_aai-type", "pserver");
        queryParamsVerticesV13.put("aai-node-type", "pserver");

        Map<String, String> queryParamsEdge = new HashMap<>();
        queryParamsEdge.put("_reserved_version", "v11");
        queryParamsEdge.put("hostname", "myhost");
        queryParamsEdge.put("_reserved_aai-type", "tosca.relationships.HostedOn");

        Map<String, String> emptyQueryParams = null;

        mockOpenTransaction();
        mockRollbackTransaction("");
        mockTransactionExists("");
        mockCommitTransaction("");
        mockGetVertex("872dd5df-0be9-4167-95e9-2cf4b21165ed", queryParamsVertex, "pserver");
        mockGetVertex("872dd5df-0be9-4167-95e9-2cf4b21165ed", queryParamsVertexV13, "pserver");
        mockGetVertex("50bdab41-ad1c-4d00-952c-a0aa5d827811", "", "vserver");
        mockGetVertex("1d326bc7-b985-492b-9604-0d5d1f06f908", "", "pserver");
        mockGetVertex("1d326bc7-b985-492b-9604-0d5d1f06f908", "?transactionId=", "pserver");
        mockGetVertex("test-uuid", "", "pserver");
        mockGetVertex("50bdab41-ad1c-4d00-952c-a0aa5d827811", "?transactionId=", "vserver");
        mockGetVertices(queryParamsVertices, "pserver");
        mockGetVertices(queryParamsVerticesV13, "pserver");
        mockGetVertexEdges("872dd5df-0be9-4167-95e9-2cf4b21165ed", queryParamsVertex, null, "tosca.relationships.HostedOn");
        mockGetVertexEdges("872dd5df-0be9-4167-95e9-2cf4b21165ed", queryParamsVertexV13, null,
                "tosca.relationships.HostedOn");
        mockGetVertexEdges("50bdab41-ad1c-4d00-952c-a0aa5d827811", emptyQueryParams, null, "tosca.relationships.HostedOn");
        mockGetVertexEdges("1d326bc7-b985-492b-9604-0d5d1f06f908", emptyQueryParams, null, "tosca.relationships.HostedOn");
        mockGetVertexEdges("50bdab41-ad1c-4d00-952c-a0aa5d827811", emptyQueryParams, "?transactionId=", "tosca.relationships.HostedOn");
        mockGetVertexEdges("1d326bc7-b985-492b-9604-0d5d1f06f908", emptyQueryParams, "?transactionId=", "tosca.relationships.HostedOn");
        mockGetEdges("?", "tosca.relationships.HostedOn");
        mockGetEdge("50bdab41-ad1c-4d00-952c-a0aa5d827811", "?transactionId=", "tosca.relationships.HostedOn");
        mockGetEdge("872dd5df-0be9-4167-95e9-2cf4b21165ed", emptyQueryParams, "tosca.relationships.HostedOn");
        mockGetEdge("872dd5df-0be9-4167-95e9-2cf4b21165ed", queryParamsEdge, "tosca.relationships.HostedOn");
        mockGetEdge("my-uuid", emptyQueryParams, "tosca.relationships.HostedOn");
        mockGetEdge("50bdab41-ad1c-4d00-952c-a0aa5d827811", queryParamsEdge, "tosca.relationships.HostedOn");
        mockPostEdge("tosca.relationships.HostedOn", "", edgePayload);
        mockPostEdge("tosca.relationships.HostedOn", "?transactionId=", edgePayloadForPost);
        mockPostVertex("pserver", vertexPayload, "");
        mockPostVertex("vserver", vertexPayloadForVserver, "?transactionId=");
        mockPutVertex("test-uuid", "pserver", vertexPayloadForPut, "");
        mockPutVertex("test-uuid", "pserver", vertexPayloadForPatch, "");
        mockPutVertex("50bdab41-ad1c-4d00-952c-a0aa5d827811", "pserver", vertexPayloadForPserver, "?transactionId=");
        mockPutEdge("test-uuid", "tosca.relationships.HostedOn", "", edgePayloadForPut);
        mockPutEdge("test-uuid", "tosca.relationships.HostedOn", "", edgePayloadForPatch);
        mockPutEdge("test-uuid", "tosca.relationships.HostedOn", "?transactionId=", edgePayloadForPutNoProperties);
        mockDeleteVertex("872dd5df-0be9-4167-95e9-2cf4b21165ed", "pserver", "");
        mockDeleteVertex("50bdab41-ad1c-4d00-952c-a0aa5d827811", "pserver", "?transactionId=");
        mockDeleteEdge("872dd5df-0be9-4167-95e9-2cf4b21165ed", "tosca.relationships.HostedOn", "");
        mockDeleteEdge("50bdab41-ad1c-4d00-952c-a0aa5d827811", "tosca.relationships.HostedOn", "?transactionId=");
    }

    public void buildChampDao() {
        String baseRelationshipUrl = CHAMP_URL + RELATIONSHIP_SUB_URL;
        String baseTransactionUrl = CHAMP_URL + TRANSACTION_SUB_URL;
        champDao = new ChampDao(restClientMock, BASE_OBJECT_URL, baseRelationshipUrl, baseTransactionUrl);
    }

    public void mockOpenTransaction() {
        OperationResult operationResult = new OperationResult();
        operationResult.setResult("");
        operationResult.setResultCode(200);
        String url = CHAMP_URL + "transaction";


        when(restClientMock.post(url, "", createHeaders(), MediaType.TEXT_PLAIN_TYPE, MediaType.TEXT_PLAIN_TYPE))
                .thenReturn(operationResult);
    }

    public void mockRollbackTransaction(String id) {
        OperationResult operationResult = new OperationResult();
        operationResult.setResult("");
        operationResult.setResultCode(200);
        String url = CHAMP_URL + TRANSACTION_SUB_URL + "/" + id;


        when(restClientMock.put(url, "{\"method\": \"rollback\"}", createHeaders(), MediaType.APPLICATION_JSON_TYPE,
                MediaType.TEXT_PLAIN_TYPE)).thenReturn(operationResult);
    }

    public void mockCommitTransaction(String id) {
        OperationResult operationResult = new OperationResult();
        operationResult.setResult("");
        operationResult.setResultCode(200);
        String url = CHAMP_URL + TRANSACTION_SUB_URL + "/" + id;


        when(restClientMock.put(url, "{\"method\": \"commit\"}", createHeaders(), MediaType.APPLICATION_JSON_TYPE,
                MediaType.TEXT_PLAIN_TYPE)).thenReturn(operationResult);
    }

    public void mockTransactionExists(String id) {
        OperationResult operationResult = new OperationResult();
        operationResult.setResult("");
        operationResult.setResultCode(200);
        String url = CHAMP_URL + TRANSACTION_SUB_URL + "/" + id;

        Map<String, List<String>> headers = new HashMap<>();
        headers.put(HEADER_FROM_APP, Arrays.asList(FROM_APP_NAME));
        headers.put(HEADER_TRANS_ID, Arrays.asList(HEADER_TRANS_ID_VALUE));

        when(restClientMock.get(url, headers, MediaType.APPLICATION_JSON_TYPE)).thenReturn(operationResult);
    }

    public void mockGetVertex(String id, String txId, String type) {
        String vertexResponse = champVertex.replace("vertexType", type);
        OperationResult operationResult = new OperationResult();
        operationResult.setResult(vertexResponse);
        operationResult.setResultCode(200);

        String url = BASE_OBJECT_URL + "/" + id + txId;


        when(restClientMock.get(url, createHeaders(), MediaType.APPLICATION_JSON_TYPE)).thenReturn(operationResult);
    }

    public void mockGetVertex(String id, Map<String, String> queryParams, String type) {
        String vertexResponse = champVertex.replace("vertexType", type);
        OperationResult operationResult = new OperationResult();
        operationResult.setResult(vertexResponse);
        operationResult.setResultCode(200);

        StringBuilder url = appendQueryParams(BASE_OBJECT_URL + "/" + id, queryParams);

        when(restClientMock.get(url.toString(), createHeaders(), MediaType.APPLICATION_JSON_TYPE))
                .thenReturn(operationResult);
    }

    public void mockGetVertexEdges(String id, Map<String, String> queryParams, String txId, String type) {
        String edgeResponse = champEdge.replace("edgeType", type);
        OperationResult operationResult = new OperationResult();
        List<String> edgeResponselist = new ArrayList<>();
        edgeResponselist.add(edgeResponse);
        operationResult.setResult(edgeResponselist.toString());
        operationResult.setResultCode(200);
        String baseUrl = BASE_OBJECT_URL + "/" + RELATIONSHIP_SUB_URL + "/" + id;
        String url;

        if (txId != null) {
            url = baseUrl + txId;
        }
        else {
            url = appendQueryParams(baseUrl, queryParams).toString();
        }

        when(restClientMock.get(url, createHeaders(), MediaType.APPLICATION_JSON_TYPE))
                .thenReturn(operationResult);
    }

    public void mockGetVertices(Map<String, String> queryParams, String type) {
        String vertexResponse = champVertex.replace("vertexType", type);
        OperationResult operationResult = new OperationResult();
        List<String> vertexResponselist = new ArrayList<>();
        vertexResponselist.add(vertexResponse);
        operationResult.setResult(vertexResponselist.toString());
        operationResult.setResultCode(200);

        StringBuilder url = appendQueryParams(BASE_OBJECT_URL + "/" + "filter", queryParams);

        when(restClientMock.get(url.toString(), createHeaders(), MediaType.APPLICATION_JSON_TYPE))
                .thenReturn(operationResult);
    }

    public void mockGetEdges(String queryParams, String type) {
        String edgeResponse = champEdge.replace("edgeType", type);
        OperationResult operationResult = new OperationResult();
        List<String> edgeResponselist = new ArrayList<>();
        edgeResponselist.add(edgeResponse);
        operationResult.setResult(edgeResponselist.toString());
        operationResult.setResultCode(200);

        String url = CHAMP_URL + RELATIONSHIP_SUB_URL + "/" + "filter" + queryParams;


        when(restClientMock.get(url, createHeaders(), MediaType.APPLICATION_JSON_TYPE)).thenReturn(operationResult);
    }

    public void mockGetEdge(String id, String txId, String type) {
        String edgeResponse = champEdge.replace("edgeType", type);
        OperationResult operationResult = new OperationResult();
        operationResult.setResult(edgeResponse);
        operationResult.setResultCode(200);

        String url = CHAMP_URL + RELATIONSHIP_SUB_URL + "/" + id + txId;

        when(restClientMock.get(url, createHeaders(), MediaType.APPLICATION_JSON_TYPE)).thenReturn(operationResult);
    }

    public void mockGetEdge(String id, Map<String, String> queryParams, String type) {
        String edgeResponse = champEdge.replace("edgeType", type);
        OperationResult operationResult = new OperationResult();
        operationResult.setResult(edgeResponse);
        operationResult.setResultCode(200);

        StringBuilder url = appendQueryParams(CHAMP_URL + RELATIONSHIP_SUB_URL + "/" + id, queryParams);

        when(restClientMock.get(url.toString(), createHeaders(), MediaType.APPLICATION_JSON_TYPE))
                .thenReturn(operationResult);
    }

    public void mockPostEdge(String type, String txId, String payload) {
        String edgeResponse = champEdge.replace("edgeType", type);
        OperationResult operationResult = new OperationResult();
        operationResult.setResult(edgeResponse);
        operationResult.setResultCode(201);
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<String, String>();
        headers.add("etag", "test123");
        operationResult.setHeaders(headers);

        String baseRelationshipUrl = CHAMP_URL + RELATIONSHIP_SUB_URL + txId;
        String url = baseRelationshipUrl;


        when(restClientMock.post(url, payload, createHeaders(), MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE)).thenReturn(operationResult);
    }

    public void mockPostVertex(String type, String payload, String txId) {
        String vertexResponse = champVertex.replace("vertexType", type);
        OperationResult operationResult = new OperationResult();
        operationResult.setResult(vertexResponse);
        operationResult.setResultCode(201);
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<String, String>();
        headers.add("etag", "test123");
        operationResult.setHeaders(headers);

        String url = BASE_OBJECT_URL + txId;


        when(restClientMock.post(url, payload, createHeaders(), MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE)).thenReturn(operationResult);
    }

    public void mockPutVertex(String id, String type, String payload, String txId) {
        String vertexResponse = champVertex.replace("vertexType", type);
        OperationResult operationResult = new OperationResult();
        operationResult.setResult(vertexResponse);
        operationResult.setResultCode(200);
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<String, String>();
        headers.add("etag", "test123");
        operationResult.setHeaders(headers);

        String url = BASE_OBJECT_URL + "/" + id + txId;


        when(restClientMock.put(url, payload, createHeaders(), MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE)).thenReturn(operationResult);
    }

    public void mockPutEdge(String id, String type, String txId, String payload) {
        String edgeResponse = champEdge.replace("edgeType", type);
        OperationResult operationResult = new OperationResult();
        operationResult.setResult(edgeResponse);
        operationResult.setResultCode(200);
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<String, String>();
        headers.add("etag", "test123");
        operationResult.setHeaders(headers);

        String url = CHAMP_URL + RELATIONSHIP_SUB_URL + "/" + id + txId;


        when(restClientMock.put(url, payload, createHeaders(), MediaType.APPLICATION_JSON_TYPE,
                MediaType.APPLICATION_JSON_TYPE)).thenReturn(operationResult);
    }

    public void mockDeleteVertex(String id, String type, String txId) {
        String vertexResponse = champVertex.replace("vertexType", type);
        OperationResult operationResult = new OperationResult();
        operationResult.setResult(vertexResponse);
        operationResult.setResultCode(200);

        String url = BASE_OBJECT_URL + "/" + id + txId;


        when(restClientMock.delete(url, createHeaders(), MediaType.APPLICATION_JSON_TYPE)).thenReturn(operationResult);
    }

    public void mockDeleteEdge(String id, String type, String txId) {
        String edgeResponse = champEdge.replace("edgeType", type);
        OperationResult operationResult = new OperationResult();
        operationResult.setResult(edgeResponse);
        operationResult.setResultCode(200);

        String url = CHAMP_URL + RELATIONSHIP_SUB_URL + "/" + id + txId;


        when(restClientMock.delete(url, createHeaders(), MediaType.APPLICATION_JSON_TYPE)).thenReturn(operationResult);
    }

    private Map<String, List<String>> createHeaders() {
        Map<String, List<String>> headers = new HashMap<>();
        List<String> listFromApp = new ArrayList<>();
        List<String> listTransId = new ArrayList<>();
        listFromApp.add(FROM_APP_NAME);
        listTransId.add(HEADER_TRANS_ID_VALUE);
        headers.put(HEADER_FROM_APP, listFromApp);
        headers.put(HEADER_TRANS_ID, listTransId);

        return headers;
    }

    private StringBuilder appendQueryParams(String url, Map<String, String> queryParams) {
        StringBuilder strBuilder = new StringBuilder(url);

        if (queryParams != null) {
            String prefix = "?";
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                strBuilder.append(prefix);
                prefix = "&";
                strBuilder.append(entry.getKey() + "=" + entry.getValue());
            }
        }
        return strBuilder;
    }

    public ChampDao getChampDao() {
        return champDao;
    }

    public void setChampDao(ChampDao champDao) {
        this.champDao = champDao;
    }
}
