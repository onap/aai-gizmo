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
package org.onap.crud.event;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.ws.rs.core.Response.Status;

import org.junit.Test;
import org.onap.crud.entity.Edge;
import org.onap.crud.entity.Vertex;
import org.onap.crud.event.GraphEvent;
import org.onap.crud.event.GraphEvent.GraphEventOperation;
import org.onap.crud.event.GraphEvent.GraphEventResult;
import org.onap.crud.event.GraphEventVertex;
import org.onap.crud.exception.CrudException;

public class GraphEventTest {
  private final String vertexPayload = "{" +
      "\"key\": \"test-uuid\"," +
      "\"type\": \"pserver\"," +
      "\"properties\": {" +
      "\"fqdn\": \"myhost.onap.com\"," +
      "\"hostname\": \"myhost\" } }";
  
  private final String edgePayload = "{" +
      "\"key\": \"test-uuid\"," +
      "\"type\": \"tosca.relationships.HostedOn\"," +
      "\"properties\": {" +
      "\"prevent-delete\": \"NONE\" }," +
      "\"source\": {" +
      "\"key\": \"50bdab41-ad1c-4d00-952c-a0aa5d827811\", \"type\": \"vserver\"}," +
      "\"target\": {" +
      "\"key\": \"1d326bc7-b985-492b-9604-0d5d1f06f908\", \"type\": \"pserver\"}" +
      " }";
  
  @Test
  public void validateGraphEvent() throws CrudException, IOException {
    // Test building event from json
    File file = new File("src/test/resources/payloads/graphVertexEvent.json");
    String payloadStr = readFileToString(file); 
    GraphEvent event = GraphEvent.fromJson(payloadStr);
    assertTrue(event.getOperation() == GraphEventOperation.UPDATE);
    assertTrue(event.getDbTransactionId().equals("b3e2853e-f643-47a3-a0c3-cb54cc997ad3"));
    assertTrue(event.getTimestamp() == Long.parseLong("1514927928167"));
    assertTrue(event.getTransactionId().equals("c0a81fa7-5ef4-49cd-ab39-e42c53c9b9a4"));
    assertTrue(event.getObjectKey().equals("mykey"));
    assertTrue(event.getObjectType().equals("vertex->pserver"));
    assertTrue(event.getVertex().getId().equals("mykey"));
    assertTrue(event.getVertex().getModelVersion().equals("v11"));
    assertTrue(event.getVertex().getType().equals("pserver"));
    assertTrue(event.getVertex().getProperties() != null);
    assertTrue(event.getVertex().toVertex() != null);
    assertTrue(event.getVertex().toJson() != null);
   
    // Test building event from vertex
    Vertex vertex = Vertex.fromJson(vertexPayload, "v11");
    event = GraphEvent.builder(GraphEventOperation.CREATE).vertex(GraphEventVertex.fromVertex(vertex, "v11")).build();
    assertTrue(event.getOperation() == GraphEventOperation.CREATE);
    
    
    // Test building event from edge
    Edge edge = Edge.fromJson(edgePayload);
    event = GraphEvent.builder(GraphEventOperation.UPDATE).edge(GraphEventEdge.fromEdge(edge, "v11")).build();    
    assertTrue(event.getOperation() == GraphEventOperation.UPDATE);
    assertTrue(event.getObjectKey().equals("test-uuid"));
    assertTrue(event.getObjectType().equals("edge->tosca.relationships.HostedOn"));
    assertTrue(event.getEdge().getId().equals("test-uuid"));
    assertTrue(event.getEdge().getType().equals("tosca.relationships.HostedOn"));
    assertTrue(event.getEdge().getProperties() != null);
    assertTrue(event.getEdge().toEdge() != null);
    assertTrue(event.getEdge().toJson() != null);
 
    // Test Getters/Setters
    event.setDbTransactionId("a");
    assertTrue(event.getDbTransactionId().equals("a"));
    event.setErrorMessage("error");
    assertTrue(event.getErrorMessage().equals("error"));
    event.setResult(GraphEventResult.FAILURE);
    assertTrue(event.getResult() == GraphEventResult.FAILURE);
    event.setHttpErrorStatus(Status.BAD_REQUEST);
    assertTrue(event.getHttpErrorStatus() == Status.BAD_REQUEST);
    event.setTimestamp(1234567);
    assertTrue(event.getTimestamp() == Long.parseLong("1234567"));
  }
  
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
