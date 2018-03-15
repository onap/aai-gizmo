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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.onap.crud.dao.GraphDao;
import org.onap.crud.entity.Edge;
import org.onap.crud.entity.Vertex;
import org.onap.crud.exception.CrudException;

public class TestDao implements GraphDao {
  
  private final String champVertex = "{" +
      "\"key\": \"test-uuid\"," +
      "\"type\": \"pserver\"," +
      "\"properties\": {" +
      "\"fqdn\": \"myhost.onap.com\"," +
      "\"hostname\": \"myhost\" } }";
  
  private final String champEdge = "{" +
      "\"key\": \"test-uuid\"," +
      "\"type\": \"tosca.relationships.HostedOn\"," +
      "\"properties\": {" +
      "\"prevent-delete\": \"NONE\" }," +
      "\"source\": {" +
      "\"key\": \"50bdab41-ad1c-4d00-952c-a0aa5d827811\", \"type\": \"vserver\"}," +
      "\"target\": {" +
      "\"key\": \"1d326bc7-b985-492b-9604-0d5d1f06f908\", \"type\": \"pserver\"}" +
      " }";

  @Override
  public Vertex getVertex(String id, String version) throws CrudException {
    return Vertex.fromJson(champVertex, "v11");
  }

  @Override
  public Vertex getVertex(String id, String type, String version, Map<String, String> queryParams)
      throws CrudException {
    return Vertex.fromJson(champVertex, "v11");
  }

  @Override
  public List<Edge> getVertexEdges(String id, Map<String, String> queryParams) throws CrudException {
    List<Edge> list = new ArrayList<Edge>();
    list.add(Edge.fromJson(champEdge));
    return list;
  }

  @Override
  public List<Vertex> getVertices(String type, Map<String, Object> filter, String version) throws CrudException {
    List<Vertex> list = new ArrayList<Vertex>();
    list.add(Vertex.fromJson(champVertex, "v11"));
    return list;
  }

  @Override
  public List<Vertex> getVertices(String type, Map<String, Object> filter, HashSet<String> properties, String version)
      throws CrudException {
    List<Vertex> list = new ArrayList<Vertex>();
    list.add(Vertex.fromJson(champVertex, "v11"));
    return list;
  }

  @Override
  public Edge getEdge(String id, String type, Map<String, String> queryParams) throws CrudException {
    return Edge.fromJson(champEdge);
  }

  @Override
  public List<Edge> getEdges(String type, Map<String, Object> filter) throws CrudException {
    List<Edge> list = new ArrayList<Edge>();
    list.add(Edge.fromJson(champEdge));
    return list;
  }

  @Override
  public Vertex addVertex(String type, Map<String, Object> properties, String version) throws CrudException {
    return Vertex.fromJson(champVertex, "v11");
  }

  @Override
  public Vertex updateVertex(String id, String type, Map<String, Object> properties, String version)
      throws CrudException {
    return Vertex.fromJson(champVertex, "v11");
  }

  @Override
  public void deleteVertex(String id, String type) throws CrudException {
    
  }

  @Override
  public Edge addEdge(String type, Vertex source, Vertex target, Map<String, Object> properties, String version)
      throws CrudException {
    return Edge.fromJson(champEdge);
  }

  @Override
  public Edge updateEdge(Edge edge) throws CrudException {
    return Edge.fromJson(champEdge);
  }

  @Override
  public void deleteEdge(String id, String type) throws CrudException {
    
  }

  @Override
  public String openTransaction() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void commitTransaction(String id) throws CrudException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void rollbackTransaction(String id) throws CrudException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public boolean transactionExists(String id) throws CrudException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Vertex addVertex(String type, Map<String, Object> properties, String version, String txId)
      throws CrudException {
    return Vertex.fromJson(champVertex, "v11");
  }

  @Override
  public Edge addEdge(String type, Vertex source, Vertex target, Map<String, Object> properties, String version,
      String txId) throws CrudException {
    return Edge.fromJson(champEdge);
  }

  @Override
  public Vertex updateVertex(String id, String type, Map<String, Object> properties, String version, String txId)
      throws CrudException {
    return Vertex.fromJson(champVertex, "v11");
  }

  @Override
  public Edge updateEdge(Edge edge, String txId) throws CrudException {
    return Edge.fromJson(champEdge);
  }

  @Override
  public void deleteVertex(String id, String type, String txId) throws CrudException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void deleteEdge(String id, String type, String txId) throws CrudException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public Edge getEdge(String id, String type, String txId) throws CrudException {
    return Edge.fromJson(champEdge);
  }

}
