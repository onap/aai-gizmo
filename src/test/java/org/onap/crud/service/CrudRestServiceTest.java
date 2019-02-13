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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.onap.crud.exception.CrudException;
import org.onap.crud.service.util.TestHeaders;
import org.onap.crud.service.util.TestRequest;
import org.onap.crud.service.util.TestUriInfo;
import org.onap.schema.EdgeRulesLoader;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.crud.OXMModelLoaderSetup;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CrudRestServiceTest extends OXMModelLoaderSetup{
  private final String putVertexPayload = "{" +
      "\"id\": \"test-uuid\"," +
      "\"type\": \"pserver\"," +
      "\"properties\": {" +
      "\"fqdn\": \"myhost.onap.com\"," +
      "\"hostname\": \"myhost\" } }";

  private final String postVertexPayload = "{" +
      "\"type\": \"pserver\"," +
      "\"properties\": {" +
      "\"fqdn\": \"myhost.onap.com\"," +
      "\"hostname\": \"myhost\" } }";

  private final String postMissingPropVertexPayload = "{" +
      "\"type\": \"pserver\"," +
      "\"properties\": {" +
      "\"fqdn\": \"myhost.onap.com\"," +
      "\"equip-type\": \"box\" } }";

  private final String postEdgePayload = "{" +
      "\"type\": \"tosca.relationships.HostedOn\"," +
      "\"source\": \"services/inventory/v11/vserver/50bdab41-ad1c-4d00-952c-a0aa5d827811\"," +
            "\"target\": \"services/inventory/v11/pserver/1d326bc7-b985-492b-9604-0d5d1f06f908\"," +
      "\"properties\": {" +
      "\"prevent-delete\": \"NONE\" } }";


  private CrudRestService mockService;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void init() throws Exception {
    System.setProperty("CONFIG_HOME", "src/test/resources");
    EdgeRulesLoader.resetSchemaVersionContext();

    ChampDaoMockTest champDaoTest = new ChampDaoMockTest();
    CrudGraphDataService service = new CrudGraphDataService(champDaoTest.getChampDao());
    CrudRestService restService = new CrudRestService(service, null);
    mockService = Mockito.spy(restService);

    Mockito.doReturn(true).when(mockService).validateRequest(Mockito.any(HttpServletRequest.class),
        Mockito.anyString(), Mockito.anyString(), Mockito.any(CrudRestService.Action.class), Mockito.anyString(),
        Mockito.any(HttpHeaders.class));
  }

  @Test
  public void testDelete() throws CrudException {
    Response response;

    response = mockService.deleteVertex("", "v11", "pserver", "872dd5df-0be9-4167-95e9-2cf4b21165ed",
        "services/inventory/v11", new TestHeaders(), null, new TestRequest());
    assertTrue(response.getStatus() == 200);

    response = mockService.deleteEdge("", "v11", "tosca.relationships.HostedOn", "872dd5df-0be9-4167-95e9-2cf4b21165ed",
        "services/inventory/v11", new TestHeaders(), null, new TestRequest());
    assertTrue(response.getStatus() == 200);
  }

  @Test
  public void testAddVertex() throws CrudException {
    Response response;

	// Cannot find OXM version
    response = mockService.addVertex(postVertexPayload, "v7", "services/inventory/v8",
            new TestHeaders(), null, new TestRequest());
    System.out.println("Response: " + response.getStatus() + "\n" + response.getEntity().toString());
    assertTrue(response.getStatus() == 500);
    Assert.assertNull(response.getEntityTag());

    response = mockService.addVertex(postMissingPropVertexPayload, "v11", "services/inventory/v11",
        new TestHeaders(), null, new TestRequest());
    System.out.println("Response: " + response.getStatus() + "\n" + response.getEntity().toString());
    assertTrue(response.getStatus() == 400);
	Assert.assertNull(response.getEntityTag());

    response = mockService.addVertex(postVertexPayload, "v11", "services/inventory/v11",
        new TestHeaders(), null, new TestRequest());
    System.out.println("Response: " + response.getStatus() + "\n" + response.getEntity().toString());
    assertTrue(response.getStatus() == 201);
	Assert.assertEquals(response.getEntityTag().getValue(), "test123");

    response = mockService.addVertex(postMissingPropVertexPayload, "v11", "pserver", "services/inventory/v11",
        new TestHeaders(), null, new TestRequest());
    System.out.println("Response: " + response.getStatus() + "\n" + response.getEntity().toString());
    assertTrue(response.getStatus() == 400);
	Assert.assertNull(response.getEntityTag());

    response = mockService.addVertex(postVertexPayload, "v11", "pserver", "services/inventory/v11",
        new TestHeaders(), null, new TestRequest());
    System.out.println("Response: " + response.getStatus() + "\n" + response.getEntity().toString());
    assertTrue(response.getStatus() == 201);
	Assert.assertEquals(response.getEntityTag().getValue(), "test123");
  }

  @Test
  public void testAddEdge() throws CrudException {
    Response response;

    response = mockService.addEdge(postEdgePayload, "v9", "services/inventory/v9",
        new TestHeaders(), null, new TestRequest());
    System.out.println("Response: " + response.getStatus() + "\n" + response.getEntity().toString());
    assertTrue(response.getStatus() == 201);
	Assert.assertEquals(response.getEntityTag().getValue(), "test123");

    response = mockService.addEdge(postEdgePayload, "v9", "tosca.relationships.HostedOn", "services/inventory/v11",
        new TestHeaders(), null, new TestRequest());
    System.out.println("Response: " + response.getStatus() + "\n" + response.getEntity().toString());
    assertTrue(response.getStatus() == 201);
	Assert.assertEquals(response.getEntityTag().getValue(), "test123");
  }

  @Test
  public void testUpdateVertex() throws CrudException {
    Response response;

	// Cannot find OXM version
    response = mockService.updateVertex(putVertexPayload, "v7", "pserver", "test-uuid", "services/inventory/v8",
            new TestHeaders(), null, new TestRequest());
    System.out.println("Response: " + response.getStatus() + "\n" + response.getEntity().toString());
    assertTrue(response.getStatus() == 500);
    Assert.assertNull(response.getEntityTag());

    // Test ID mismatch
    response = mockService.updateVertex(putVertexPayload, "v11", "pserver", "bad-id",
        "services/inventory/v11", new TestHeaders(), null, new TestRequest());
    System.out.println("Response: " + response.getStatus() + "\n" + response.getEntity().toString());
    assertTrue(response.getStatus() == 400);
    Assert.assertNull(response.getEntityTag());

    // Success case
    response = mockService.updateVertex(putVertexPayload, "v11", "pserver", "test-uuid", "services/inventory/v11",
                new TestHeaders(), null, new TestRequest());
    System.out.println("Response: " + response.getStatus() + "\n" + response.getEntity().toString());
    assertTrue(response.getStatus() == 200);
	Assert.assertEquals(response.getEntityTag().getValue(), "test123");

    // Patch
    response = mockService.patchVertex(putVertexPayload, "v11", "pserver", "test-uuid",
        "services/inventory/v11", new TestHeaders(), null, new TestRequest());
    System.out.println("Response: " + response.getStatus() + "\n" + response.getEntity().toString());
    assertTrue(response.getStatus() == 200);
	Assert.assertEquals(response.getEntityTag().getValue(), "test123");
  }

  @Test
  public void testUpdateEdge() throws CrudException {
    Response response;

    response = mockService.updateEdge(postEdgePayload, "v9", "tosca.relationships.HostedOn", "my-uuid",
        "services/inventory/v9", new TestHeaders(), null, new TestRequest());
    System.out.println("Response: " + response.getStatus() + "\n" + response.getEntity().toString());
    assertTrue(response.getStatus() == 200);
	Assert.assertEquals(response.getEntityTag().getValue(), "test123");

    // Patch
    response = mockService.patchEdge(postEdgePayload, "v9", "tosca.relationships.HostedOn", "my-uuid",
        "services/inventory/v9", new TestHeaders(), null, new TestRequest());
    System.out.println("Response: " + response.getStatus() + "\n" + response.getEntity().toString());
    assertTrue(response.getStatus() == 200);
	Assert.assertEquals(response.getEntityTag().getValue(), "test123");
  }

  @Test
  public void testGet() throws CrudException {
    Response response;

    response = mockService.getVertex("", "v11", "pserver", "872dd5df-0be9-4167-95e9-2cf4b21165ed",
        "services/inventory/v11", new TestHeaders(), new TestUriInfo(), new TestRequest());
    System.out.println("Response: " + response.getStatus() + "\n" + response.getEntity().toString());
    assertTrue(response.getStatus() == 200);

    response = mockService.getEdge("", "v11", "tosca.relationships.HostedOn", "872dd5df-0be9-4167-95e9-2cf4b21165ed",
        "services/inventory/v11", new TestHeaders(), new TestUriInfo(), new TestRequest());
    System.out.println("Response: " + response.getStatus() + "\n" + response.getEntity().toString());
    assertTrue(response.getStatus() == 200);

    response = mockService.getVertices("", "v11", "pserver",
        "services/inventory/v11", new TestHeaders(), new TestUriInfo(), new TestRequest());
    System.out.println("Response: " + response.getStatus() + "\n" + response.getEntity().toString());
    assertTrue(response.getStatus() == 200);

    response = mockService.getEdges("", "v11", "tosca.relationships.HostedOn",
        "services/inventory/v11", new TestHeaders(), new TestUriInfo(), new TestRequest());
    System.out.println("Response: " + response.getStatus() + "\n" + response.getEntity().toString());
    assertTrue(response.getStatus() == 200);
  }

  @Test
  public void testValidRequestHeader() throws CrudException {
    TestHeaders testHeaders = new TestHeaders();
    mockService.validateRequestHeader(testHeaders);
  }

  @Test
  public void testInvalidRequestHeaderXTransactionId() throws CrudException {
    thrown.expect(CrudException.class);
    thrown.expectMessage("Invalid request, Missing X-TransactionId header");

    TestHeaders testHeaders = new TestHeaders();
    testHeaders.clearRequestHeader("X-TransactionId");
    mockService.validateRequestHeader(testHeaders);
  }

  @Test
  public void testInvalidRequestHeaderXFromAppId() throws CrudException {
    thrown.expect(CrudException.class);
    thrown.expectMessage("Invalid request, Missing X-FromAppId header");

    TestHeaders testHeaders = new TestHeaders();
    testHeaders.clearRequestHeader("X-FromAppId");
    mockService.validateRequestHeader(testHeaders);
  }

  @Test
  public void testRequestHeaderWithMsId() throws CrudException {
    thrown.expect(CrudException.class);
    thrown.expectMessage("Invalid request, Missing X-FromAppId header");

    TestHeaders testHeaders = new TestHeaders();
    testHeaders.clearRequestHeader("X-TransactionId", "X-FromAppId");
    mockService.validateRequestHeader(testHeaders);
  }

  @Test
  public void testEmptyRequestHeader() throws CrudException {
    thrown.expect(CrudException.class);
    thrown.expectMessage("Invalid request, Missing X-FromAppId header");

    TestHeaders testHeaders = new TestHeaders();
    testHeaders.clearRequestHeader("X-TransactionId", "X-FromAppId");
    mockService.validateRequestHeader(testHeaders);
  }

  @Test
    public void testGetMultiOxm() throws CrudException {
        Response response;

        response = mockService.getVertex("", "v13", "pserver", "872dd5df-0be9-4167-95e9-2cf4b21165ed",
                "services/inventory/v11", new TestHeaders(), new TestUriInfo(), new TestRequest());
        System.out.println("Response: " + response.getStatus() + "\n" + response.getEntity().toString());
        assertTrue(response.getStatus() == 200);

        response =
                mockService.getEdge("", "v11", "tosca.relationships.HostedOn", "872dd5df-0be9-4167-95e9-2cf4b21165ed",
                        "services/inventory/v11", new TestHeaders(), new TestUriInfo(), new TestRequest());
        System.out.println("Response: " + response.getStatus() + "\n" + response.getEntity().toString());
        assertTrue(response.getStatus() == 200);

        response = mockService.getVertices("", "v13", "pserver", "services/inventory/v11", new TestHeaders(),
                new TestUriInfo(), new TestRequest());
        System.out.println("Response: " + response.getStatus() + "\n" + response.getEntity().toString());
        assertTrue(response.getStatus() == 200);

        response = mockService.getEdges("", "v11", "tosca.relationships.HostedOn", "services/inventory/v11",
                new TestHeaders(), new TestUriInfo(), new TestRequest());
        System.out.println("Response: " + response.getStatus() + "\n" + response.getEntity().toString());
        assertTrue(response.getStatus() == 200);
    }

  /*
  @Test
  public void testBulk() throws CrudException, IOException {
    Response response;

    File bulkFile = new File("src/test/resources/payloads/bulk.json");
    String payloadStr = readFileToString(bulkFile);
    System.out.println(payloadStr);

    response = mockService.addBulk(payloadStr, "v9", 
        "services/inventory/v9", new TestHeaders(), null, new TestRequest());
    System.out.println("Bulk Response: " + response);
    System.out.println("Response Entity: " + response.getEntity().toString());
    assertTrue(response.getStatus() == 200);
  }
*/

  public static String readFileToString(File aFile) throws IOException {

    BufferedReader br = new BufferedReader(new FileReader(aFile));
    try {
      StringBuilder sb = new StringBuilder();
      String line = br.readLine();

      while (line != null) {
        sb.append(line);
        line = br.readLine();
      }

      return sb.toString().replaceAll("\\s+", "");
    } finally {
      try {
        br.close();
      } catch (IOException e) {
        fail("Unexpected IOException: " + e.getMessage());
      }
    }
  }

}