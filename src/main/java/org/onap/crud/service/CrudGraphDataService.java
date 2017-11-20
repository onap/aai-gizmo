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
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import org.onap.aaiutils.oxm.OxmModelLoader;
import org.onap.aai.champcore.ChampGraph;
import org.onap.crud.dao.GraphDao;
import org.onap.crud.dao.champ.ChampDao;
import org.onap.crud.entity.Edge;

import org.onap.crud.entity.Vertex;
import org.onap.crud.exception.CrudException;
import org.onap.crud.parser.CrudResponseBuilder;
import org.onap.schema.OxmModelValidator;
import org.onap.schema.RelationshipSchemaLoader;
import org.onap.schema.RelationshipSchemaValidator;

import com.google.gson.JsonElement;

public class CrudGraphDataService {

  private GraphDao dao;

  public CrudGraphDataService(ChampGraph graphImpl) throws CrudException {
    this.dao = new ChampDao(graphImpl);

    loadModels();
  }

  public CrudGraphDataService(GraphDao dao) throws CrudException {
    this.dao = dao;

    loadModels();
  }

  private void loadModels() throws CrudException {
    // load the schemas
    try {
      OxmModelLoader.loadModels();
    } catch (Exception e) {
      throw new CrudException(e);
    }
    RelationshipSchemaLoader.loadModels();
  }

  public String addVertex(String version, String type, VertexPayload payload) throws CrudException {
    Vertex vertex = OxmModelValidator.validateIncomingUpsertPayload(null, version, type, payload.getProperties());
    return addVertex(version, vertex);
  }

  public String addBulk(String version, BulkPayload payload) throws CrudException {
    HashMap<String, Vertex> vertices = new HashMap<String, Vertex>();
    HashMap<String, Edge> edges = new HashMap<String, Edge>();
    String txId = dao.openTransaction();
    try {
      // Handle vertices
      for (JsonElement v : payload.getObjects()) {
        List<Map.Entry<String, JsonElement>> entries = new ArrayList<Map.Entry<String, JsonElement>>(
            v.getAsJsonObject().entrySet());

        if (entries.size() != 2) {
          throw new CrudException("", Status.BAD_REQUEST);
        }
        Map.Entry<String, JsonElement> opr = entries.get(0);
        Map.Entry<String, JsonElement> item = entries.get(1);

        VertexPayload vertexPayload = VertexPayload.fromJson(item.getValue().getAsJsonObject().toString());

        if (opr.getValue().getAsString().equalsIgnoreCase("add")
            || opr.getValue().getAsString().equalsIgnoreCase("modify")) {
          Vertex validatedVertex;
          Vertex persistedVertex;
          if (opr.getValue().getAsString().equalsIgnoreCase("add")) {
            validatedVertex = OxmModelValidator.validateIncomingUpsertPayload(null, version, vertexPayload.getType(),
                vertexPayload.getProperties());
            // Call champDAO to add the vertex
            persistedVertex = dao.addVertex(validatedVertex.getType(), validatedVertex.getProperties(), txId);
          } else {
            validatedVertex = OxmModelValidator.validateIncomingUpsertPayload(vertexPayload.getId(), version,
                vertexPayload.getType(), vertexPayload.getProperties());
            // Call champDAO to update the vertex
            persistedVertex = dao.updateVertex(vertexPayload.getId(), validatedVertex.getType(),
                validatedVertex.getProperties(), txId);
          }

          Vertex outgoingVertex = OxmModelValidator.validateOutgoingPayload(version, persistedVertex);

          vertices.put(item.getKey(), outgoingVertex);

        } else if (opr.getValue().getAsString().equalsIgnoreCase("delete")) {
          dao.deleteVertex(vertexPayload.getId(),
              OxmModelValidator.resolveCollectionType(version, vertexPayload.getType()), txId);
        }

      }
      // Handle Edges
      for (JsonElement v : payload.getRelationships()) {
        List<Map.Entry<String, JsonElement>> entries = new ArrayList<Map.Entry<String, JsonElement>>(
            v.getAsJsonObject().entrySet());

        if (entries.size() != 2) {
          throw new CrudException("", Status.BAD_REQUEST);
        }
        Map.Entry<String, JsonElement> opr = entries.get(0);
        Map.Entry<String, JsonElement> item = entries.get(1);

        EdgePayload edgePayload = EdgePayload.fromJson(item.getValue().getAsJsonObject().toString());

        if (opr.getValue().getAsString().equalsIgnoreCase("add")
            || opr.getValue().getAsString().equalsIgnoreCase("modify")) {
          Edge validatedEdge;
          Edge persistedEdge;
          if (opr.getValue().getAsString().equalsIgnoreCase("add")) {
            // Fix the source/detination
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
            persistedEdge = dao.addEdge(validatedEdge.getType(), validatedEdge.getSource(), validatedEdge.getTarget(),
                validatedEdge.getProperties(), txId);
          } else {
            Edge edge = dao.getEdge(edgePayload.getId(), edgePayload.getType(), txId);
            validatedEdge = RelationshipSchemaValidator.validateIncomingUpdatePayload(edge, version, edgePayload);
            persistedEdge = dao.updateEdge(edge, txId);
          }

          Edge outgoingEdge = RelationshipSchemaValidator.validateOutgoingPayload(version, persistedEdge);

          edges.put(item.getKey(), outgoingEdge);

        } else if (opr.getValue().getAsString().equalsIgnoreCase("delete")) {
          RelationshipSchemaValidator.validateType(version, edgePayload.getType());
          dao.deleteEdge(edgePayload.getId(), edgePayload.getType(), txId);
        }

      }
      // close champ TX
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

  private String addVertex(String version, Vertex vertex) throws CrudException {
    Vertex addedVertex = dao.addVertex(vertex.getType(), vertex.getProperties());
    return CrudResponseBuilder
        .buildUpsertVertexResponse(OxmModelValidator.validateOutgoingPayload(version, addedVertex), version);
  }

  public String addEdge(String version, String type, EdgePayload payload) throws CrudException {
    Edge edge = RelationshipSchemaValidator.validateIncomingAddPayload(version, type, payload);
    return addEdge(version, edge);
  }

  private String addEdge(String version, Edge edge) throws CrudException {
    Edge addedEdge = dao.addEdge(edge.getType(), edge.getSource(), edge.getTarget(), edge.getProperties());
    return CrudResponseBuilder
        .buildUpsertEdgeResponse(RelationshipSchemaValidator.validateOutgoingPayload(version, addedEdge), version);
  }

  public String getEdge(String version, String id, String type) throws CrudException {
    RelationshipSchemaValidator.validateType(version, type);
    Edge edge = dao.getEdge(id, type);

    return CrudResponseBuilder.buildGetEdgeResponse(RelationshipSchemaValidator.validateOutgoingPayload(version, edge),
        version);
  }

  public String getEdges(String version, String type, Map<String, String> filter) throws CrudException {
    RelationshipSchemaValidator.validateType(version, type);
    List<Edge> items = dao.getEdges(type, RelationshipSchemaValidator.resolveCollectionfilter(version, type, filter));
    return CrudResponseBuilder.buildGetEdgesResponse(items, version);
  }

  public String updateVertex(String version, String id, String type, VertexPayload payload) throws CrudException {
    Vertex vertex = OxmModelValidator.validateIncomingUpsertPayload(id, version, type, payload.getProperties());
    return updateVertex(version, vertex);

  }

  private String updateVertex(String version, Vertex vertex) throws CrudException {
    Vertex updatedVertex = dao.updateVertex(vertex.getId().get(), vertex.getType(), vertex.getProperties());
    return CrudResponseBuilder
        .buildUpsertVertexResponse(OxmModelValidator.validateOutgoingPayload(version, updatedVertex), version);
  }

  public String patchVertex(String version, String id, String type, VertexPayload payload) throws CrudException {
    Vertex existingVertex = dao.getVertex(id, OxmModelValidator.resolveCollectionType(version, type));
    Vertex vertex = OxmModelValidator.validateIncomingPatchPayload(id, version, type, payload.getProperties(),
        existingVertex);
    return updateVertex(version, vertex);

  }

  public String deleteVertex(String version, String id, String type) throws CrudException {
    type = OxmModelValidator.resolveCollectionType(version, type);
    dao.deleteVertex(id, type);
    return "";

  }

  public String deleteEdge(String version, String id, String type) throws CrudException {
    RelationshipSchemaValidator.validateType(version, type);
    dao.deleteEdge(id, type);
    return "";

  }

  public String updateEdge(String version, String id, String type, EdgePayload payload) throws CrudException {
    Edge edge = dao.getEdge(id, type);
    Edge validatedEdge = RelationshipSchemaValidator.validateIncomingUpdatePayload(edge, version, payload);
    return updateEdge(version, validatedEdge);

  }

  private String updateEdge(String version, Edge edge) throws CrudException {
    Edge updatedEdge = dao.updateEdge(edge);
    return CrudResponseBuilder
        .buildUpsertEdgeResponse(RelationshipSchemaValidator.validateOutgoingPayload(version, updatedEdge), version);
  }

  public String patchEdge(String version, String id, String type, EdgePayload payload) throws CrudException {
    Edge edge = dao.getEdge(id, type);
    Edge patchedEdge = RelationshipSchemaValidator.validateIncomingPatchPayload(edge, version, payload);
    return updateEdge(version, patchedEdge);

  }

  public Vertex getVertex(String id) throws CrudException {
    return dao.getVertex(id);
  }

  public String getVertex(String version, String id, String type) throws CrudException {
    type = OxmModelValidator.resolveCollectionType(version, type);
    Vertex vertex = dao.getVertex(id, type);
    List<Edge> edges = dao.getVertexEdges(id);
    return CrudResponseBuilder.buildGetVertexResponse(OxmModelValidator.validateOutgoingPayload(version, vertex), edges,
        version);
  }

  public String getVertices(String version, String type, Map<String, String> filter) throws CrudException {
    type = OxmModelValidator.resolveCollectionType(version, type);
    List<Vertex> items = dao.getVertices(type, OxmModelValidator.resolveCollectionfilter(version, type, filter));
    return CrudResponseBuilder.buildGetVerticesResponse(items, version);
  }

}
