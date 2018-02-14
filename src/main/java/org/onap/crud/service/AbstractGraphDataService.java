/**
 * ﻿============LICENSE_START=======================================================
 * Gizmo
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property.
 * Copyright © 2017 Amdocs
 * All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 *
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.onap.crud.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

import org.onap.crud.dao.GraphDao;
import org.onap.crud.entity.Edge;
import org.onap.crud.entity.Vertex;
import org.onap.crud.exception.CrudException;
import org.onap.crud.parser.CrudResponseBuilder;
import org.onap.crud.util.CrudServiceUtil;
import org.onap.schema.OxmModelValidator;
import org.onap.schema.RelationshipSchemaValidator;

import com.google.gson.JsonElement;

public abstract class AbstractGraphDataService {
  protected GraphDao daoForGet;
  protected GraphDao dao;
  
  public AbstractGraphDataService() throws CrudException {
    CrudServiceUtil.loadModels();
  }

  public String getEdge(String version, String id, String type, Map<String, String> queryParams) throws CrudException {
    RelationshipSchemaValidator.validateType(version, type);
    Edge edge = daoForGet.getEdge(id, type, queryParams);

    return CrudResponseBuilder.buildGetEdgeResponse(RelationshipSchemaValidator.validateOutgoingPayload(version, edge), version);
  }
  
  public String getEdges(String version, String type, Map<String, String> filter) throws CrudException {
    RelationshipSchemaValidator.validateType(version, type);
    List<Edge> items = daoForGet.getEdges(type, RelationshipSchemaValidator.resolveCollectionfilter(version, type, filter));
    return CrudResponseBuilder.buildGetEdgesResponse(items, version);
  }
  
  public String getVertex(String version, String id, String type, Map<String, String> queryParams) throws CrudException {
    type = OxmModelValidator.resolveCollectionType(version, type);
    Vertex vertex = daoForGet.getVertex(id, type, version, queryParams);
    List<Edge> edges = daoForGet.getVertexEdges(id, queryParams);
    return CrudResponseBuilder.buildGetVertexResponse(OxmModelValidator.validateOutgoingPayload(version, vertex), edges,
        version);
  }

  public String getVertices(String version, String type, Map<String, String> filter, HashSet<String> properties) throws CrudException {
    type = OxmModelValidator.resolveCollectionType(version, type);
    List<Vertex> items = daoForGet.getVertices(type, OxmModelValidator.resolveCollectionfilter(version, type, filter), properties, version);
    return CrudResponseBuilder.buildGetVerticesResponse(items, version);
  }
  
  public String addBulk(String version, BulkPayload payload, HttpHeaders headers) throws CrudException {
    HashMap<String, Vertex> vertices = new HashMap<String, Vertex>();
    HashMap<String, Edge> edges = new HashMap<String, Edge>();
    
    String txId = dao.openTransaction();   
     
    try {
      // Step 1. Handle edge deletes (must happen before vertex deletes)
      for (JsonElement v : payload.getRelationships()) {
        List<Map.Entry<String, JsonElement>> entries = new ArrayList<Map.Entry<String, JsonElement>>(
            v.getAsJsonObject().entrySet());

        if (entries.size() != 2) {
          throw new CrudException("", Status.BAD_REQUEST);
        }
        Map.Entry<String, JsonElement> opr = entries.get(0);
        Map.Entry<String, JsonElement> item = entries.get(1);
        EdgePayload edgePayload = EdgePayload.fromJson(item.getValue().getAsJsonObject().toString());

        if (opr.getValue().getAsString().equalsIgnoreCase("delete")) {
          RelationshipSchemaValidator.validateType(version, edgePayload.getType());
          deleteBulkEdge(edgePayload.getId(), version, edgePayload.getType(), txId);
        }
      } 
      
      // Step 2: Handle vertex deletes
      for (JsonElement v : payload.getObjects()) {
        List<Map.Entry<String, JsonElement>> entries = new ArrayList<Map.Entry<String, JsonElement>>(
            v.getAsJsonObject().entrySet());

        if (entries.size() != 2) {
          throw new CrudException("", Status.BAD_REQUEST);
        }

        Map.Entry<String, JsonElement> opr = entries.get(0);
        Map.Entry<String, JsonElement> item = entries.get(1);
        VertexPayload vertexPayload = VertexPayload.fromJson(item.getValue().getAsJsonObject().toString());

        if (opr.getValue().getAsString().equalsIgnoreCase("delete")) {
          String type = OxmModelValidator.resolveCollectionType(version, vertexPayload.getType());
          deleteBulkVertex(vertexPayload.getId(), version, type, txId);
        }
      }
      
      // Step 3: Handle vertex add/modify (must happen before edge adds)
      for (JsonElement v : payload.getObjects()) {
        List<Map.Entry<String, JsonElement>> entries = new ArrayList<Map.Entry<String, JsonElement>>(
            v.getAsJsonObject().entrySet());

        if (entries.size() != 2) {
          throw new CrudException("", Status.BAD_REQUEST);
        }
        Map.Entry<String, JsonElement> opr = entries.get(0);
        Map.Entry<String, JsonElement> item = entries.get(1);
        VertexPayload vertexPayload = VertexPayload.fromJson(item.getValue().getAsJsonObject().toString());
        
        // Add vertex
        if (opr.getValue().getAsString().equalsIgnoreCase("add")) {
          vertexPayload.setProperties(CrudServiceUtil.mergeHeaderInFoToPayload(vertexPayload.getProperties(), 
              headers, true));  
          Vertex validatedVertex = OxmModelValidator.validateIncomingUpsertPayload(null, version, vertexPayload.getType(),
              vertexPayload.getProperties());
          Vertex persistedVertex = addBulkVertex(validatedVertex, version, txId);
          Vertex outgoingVertex = OxmModelValidator.validateOutgoingPayload(version, persistedVertex);
          vertices.put(item.getKey(), outgoingVertex);
        }
        
        // Update vertex 
        else if (opr.getValue().getAsString().equalsIgnoreCase("modify")) {
          vertexPayload.setProperties(CrudServiceUtil.mergeHeaderInFoToPayload(vertexPayload.getProperties(), 
              headers, false));
          Vertex validatedVertex = OxmModelValidator.validateIncomingUpsertPayload(vertexPayload.getId(), version,
              vertexPayload.getType(), vertexPayload.getProperties());
          Vertex persistedVertex = updateBulkVertex(validatedVertex, vertexPayload.getId(), version, txId);
          Vertex outgoingVertex = OxmModelValidator.validateOutgoingPayload(version, persistedVertex);
          vertices.put(item.getKey(), outgoingVertex);
        }
        
        // Patch vertex 
        else if (opr.getValue().getAsString().equalsIgnoreCase("patch")) {
          if ( (vertexPayload.getId() == null) || (vertexPayload.getType() == null) ) {
            throw new CrudException("id and type must be specified for patch request", Status.BAD_REQUEST);
          }
          
          vertexPayload.setProperties(CrudServiceUtil.mergeHeaderInFoToPayload(vertexPayload.getProperties(), 
              headers, false));
          
          Vertex existingVertex = dao.getVertex(vertexPayload.getId(), OxmModelValidator.resolveCollectionType(version, vertexPayload.getType()), version, new HashMap<String, String>());
          Vertex validatedVertex = OxmModelValidator.validateIncomingPatchPayload(vertexPayload.getId(), 
              version, vertexPayload.getType(), vertexPayload.getProperties(), existingVertex);
          Vertex persistedVertex = updateBulkVertex(validatedVertex, vertexPayload.getId(), version, txId);
          Vertex outgoingVertex = OxmModelValidator.validateOutgoingPayload(version, persistedVertex);
          vertices.put(item.getKey(), outgoingVertex);
        }
      }

      // Step 4: Handle edge add/modify 
      for (JsonElement v : payload.getRelationships()) {
        List<Map.Entry<String, JsonElement>> entries = new ArrayList<Map.Entry<String, JsonElement>>(
            v.getAsJsonObject().entrySet());

        if (entries.size() != 2) {
          throw new CrudException("", Status.BAD_REQUEST);
        }
        Map.Entry<String, JsonElement> opr = entries.get(0);
        Map.Entry<String, JsonElement> item = entries.get(1);
        EdgePayload edgePayload = EdgePayload.fromJson(item.getValue().getAsJsonObject().toString());

        // Add/Update edge
        if (opr.getValue().getAsString().equalsIgnoreCase("add")
            || opr.getValue().getAsString().equalsIgnoreCase("modify") 
            || opr.getValue().getAsString().equalsIgnoreCase("patch")) {
          Edge validatedEdge;
          Edge persistedEdge;
          if (opr.getValue().getAsString().equalsIgnoreCase("add")) {
            // Fix the source/destination
            if (edgePayload.getSource().startsWith("$")) {
              Vertex source = vertices.get(edgePayload.getSource().substring(1));
              if (source == null) {
                throw new CrudException("Not able to find vertex: " + edgePayload.getSource().substring(1),
                    Status.INTERNAL_SERVER_ERROR);
              }
              edgePayload
                  .setSource("services/inventory/" + version + "/" + source.getType() + "/" + source.getId().get());
            }
            if (edgePayload.getTarget().startsWith("$")) {
              Vertex target = vertices.get(edgePayload.getTarget().substring(1));
              if (target == null) {
                throw new CrudException("Not able to find vertex: " + edgePayload.getTarget().substring(1),
                    Status.INTERNAL_SERVER_ERROR);
              }
              edgePayload
                  .setTarget("services/inventory/" + version + "/" + target.getType() + "/" + target.getId().get());
            }
            validatedEdge = RelationshipSchemaValidator.validateIncomingAddPayload(version, edgePayload.getType(),
                edgePayload);
            persistedEdge = addBulkEdge(validatedEdge, version, txId);
          } else if (opr.getValue().getAsString().equalsIgnoreCase("modify")) {
            Edge edge = dao.getEdge(edgePayload.getId(), edgePayload.getType(), txId);
            validatedEdge = RelationshipSchemaValidator.validateIncomingUpdatePayload(edge, version, edgePayload);
            persistedEdge = updateBulkEdge(validatedEdge, version, txId);
          } else {
            if ( (edgePayload.getId() == null) || (edgePayload.getType() == null) ) {
              throw new CrudException("id and type must be specified for patch request", Status.BAD_REQUEST);
            }
            Edge existingEdge = dao.getEdge(edgePayload.getId(), edgePayload.getType(), txId);
            Edge patchedEdge = RelationshipSchemaValidator.validateIncomingPatchPayload(existingEdge, version, edgePayload);
            persistedEdge = updateBulkEdge(patchedEdge, version, txId);
          }
          

          Edge outgoingEdge = RelationshipSchemaValidator.validateOutgoingPayload(version, persistedEdge);
          edges.put(item.getKey(), outgoingEdge);
        } 
      } 
      
      // commit transaction
      dao.commitTransaction(txId);
    } catch (CrudException ex) {
      dao.rollbackTransaction(txId);
      throw ex;
    } catch (Exception ex) {
      dao.rollbackTransaction(txId);
      throw ex;
    } finally {
      if (dao.transactionExists(txId)) {
        dao.rollbackTransaction(txId);
      }
    }
    
    return CrudResponseBuilder.buildUpsertBulkResponse(vertices, edges, version, payload);
  }


  public abstract String addVertex(String version, String type, VertexPayload payload) throws CrudException;
  public abstract String updateVertex(String version, String id, String type, VertexPayload payload) throws CrudException;
  public abstract String patchVertex(String version, String id, String type, VertexPayload payload) throws CrudException;
  public abstract String deleteVertex(String version, String id, String type) throws CrudException;
  public abstract String addEdge(String version, String type, EdgePayload payload) throws CrudException;
  public abstract String deleteEdge(String version, String id, String type) throws CrudException;
  public abstract String updateEdge(String version, String id, String type, EdgePayload payload) throws CrudException;
  public abstract String patchEdge(String version, String id, String type, EdgePayload payload) throws CrudException;
  
  protected abstract Vertex addBulkVertex(Vertex vertex, String version, String dbTransId) throws CrudException;
  protected abstract Vertex updateBulkVertex(Vertex vertex, String id, String version, String dbTransId) throws CrudException;
  protected abstract void deleteBulkVertex(String id, String version, String type, String dbTransId) throws CrudException;
  
  protected abstract Edge addBulkEdge(Edge edge, String version, String dbTransId) throws CrudException;
  protected abstract Edge updateBulkEdge(Edge edge, String version, String dbTransId) throws CrudException;
  protected abstract void deleteBulkEdge(String id, String version, String type, String dbTransId) throws CrudException;
  
}
