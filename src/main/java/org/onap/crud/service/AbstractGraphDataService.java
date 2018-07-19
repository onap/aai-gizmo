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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.onap.aai.restclient.client.OperationResult;
import org.onap.crud.dao.GraphDao;
import org.onap.crud.dao.champ.ChampEdgeSerializer;
import org.onap.crud.dao.champ.ChampVertexSerializer;
import org.onap.crud.entity.Edge;
import org.onap.crud.entity.Vertex;
import org.onap.crud.exception.CrudException;
import org.onap.crud.parser.BulkPayload;
import org.onap.crud.parser.CrudResponseBuilder;
import org.onap.crud.parser.EdgePayload;
import org.onap.crud.parser.VertexPayload;
import org.onap.crud.parser.util.EdgePayloadUtil;
import org.onap.crud.util.CrudServiceUtil;
import org.onap.schema.validation.OxmModelValidator;
import org.onap.schema.validation.RelationshipSchemaValidator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;

public abstract class AbstractGraphDataService {
  protected GraphDao daoForGet;
  protected GraphDao dao;

  public AbstractGraphDataService() throws CrudException {
    CrudServiceUtil.loadModels();
  }

  public ImmutablePair<EntityTag, String> getEdge(String version, String id, String type, Map<String, String> queryParams) throws CrudException {
    RelationshipSchemaValidator.validateType(version, type);
    OperationResult operationResult = daoForGet.getEdge(id, type, queryParams);
    EntityTag entityTag = CrudServiceUtil.getETagFromHeader(operationResult.getHeaders());
    Edge edge = Edge.fromJson(operationResult.getResult());
    return new ImmutablePair<>(entityTag, CrudResponseBuilder.buildGetEdgeResponse(RelationshipSchemaValidator.validateOutgoingPayload(version, edge), version));
  }

  public ImmutablePair<EntityTag, String> getEdges(String version, String type, Map<String, String> filter) throws CrudException {
     Gson champGson = new GsonBuilder()
              .registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory())
              .registerTypeAdapter(Vertex.class, new ChampVertexSerializer())
              .registerTypeAdapter(Edge.class, new ChampEdgeSerializer()).create();
    RelationshipSchemaValidator.validateType(version, type);
    OperationResult operationResult = daoForGet.getEdges(type, RelationshipSchemaValidator.resolveCollectionfilter(version, type, filter));
    List<Edge> items = champGson.fromJson(operationResult.getResult(), new TypeToken<List<Edge>>() {
    }.getType());
    EntityTag entityTag = CrudServiceUtil.getETagFromHeader(operationResult.getHeaders());
    return new ImmutablePair<>(entityTag, CrudResponseBuilder.buildGetEdgesResponse(items, version));
  }

  public ImmutablePair<EntityTag, String> getVertex(String version, String id, String type, Map<String, String> queryParams) throws CrudException {
    type = OxmModelValidator.resolveCollectionType(version, type);
    OperationResult vertexOpResult = daoForGet.getVertex(id, type, version, queryParams);
    Vertex vertex = Vertex.fromJson(vertexOpResult.getResult(), version);
    List<Edge> edges = daoForGet.getVertexEdges(id, queryParams, null);
    EntityTag entityTag = CrudServiceUtil.getETagFromHeader(vertexOpResult.getHeaders());
    return new ImmutablePair<>(entityTag, CrudResponseBuilder.buildGetVertexResponse(OxmModelValidator.validateOutgoingPayload(version, vertex), edges,
        version));
  }

  public ImmutablePair<EntityTag, String> getVertices(String version, String type, Map<String, String> filter, Set<String> properties) throws CrudException {
    type = OxmModelValidator.resolveCollectionType(version, type);
    OperationResult operationResult = daoForGet.getVertices(type, OxmModelValidator.resolveCollectionfilter(version, type, filter), properties, version);
    List<Vertex> vertices = Vertex.collectionFromJson(operationResult.getResult(), version);
    EntityTag entityTag = CrudServiceUtil.getETagFromHeader(operationResult.getHeaders());
    return new ImmutablePair<>(entityTag, CrudResponseBuilder.buildGetVerticesResponse(vertices, version));
  }

  public String addBulk(String version, BulkPayload payload, HttpHeaders headers) throws CrudException {
    HashMap<String, Vertex> vertices = new HashMap<>();
    HashMap<String, Edge> edges = new HashMap<>();

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
          deleteBulkEdge(edgePayload.getId(), version, txId);
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

          OperationResult existingVertexOpResult = dao.getVertex(vertexPayload.getId(), OxmModelValidator.resolveCollectionType(version, vertexPayload.getType()), version, new HashMap<String, String>());
          Vertex existingVertex = Vertex.fromJson(existingVertexOpResult.getResult(), version);
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

            // If the type isn't set, resolve it based on on the sourece and target vertex types
            if (edgePayload.getType() == null || edgePayload.getType().isEmpty()) {
              edgePayload.setType(CrudServiceUtil.determineEdgeType(edgePayload, version));
            }
            
            // TODO:  Champ needs to support getting an object's relationships within the context of an existing transaction.
            //        Currently it doesn't.  Disabling multiplicity check until this happens.
            
            List<Edge> sourceVertexEdges = new ArrayList<Edge>();
            List<Edge> targetVertexEdges = new ArrayList<Edge>();
            
            /*
            List<Edge> sourceVertexEdges =
                    EdgePayloadUtil.filterEdgesByRelatedVertexAndType(EdgePayloadUtil.getVertexNodeType(edgePayload.getSource()), edgePayload.getType(),
                                 dao.getVertexEdges(EdgePayloadUtil.getVertexNodeId(edgePayload.getSource()), null, txId));
            
            List<Edge> targetVertexEdges =
                     EdgePayloadUtil.filterEdgesByRelatedVertexAndType(EdgePayloadUtil.getVertexNodeType(edgePayload.getTarget()), edgePayload.getType(),
                                 dao.getVertexEdges(EdgePayloadUtil.getVertexNodeId(edgePayload.getTarget()), null, txId));
            */
            
            validatedEdge = RelationshipSchemaValidator.validateIncomingAddPayload(version, edgePayload.getType(), edgePayload, sourceVertexEdges,
                    targetVertexEdges);
            persistedEdge = addBulkEdge(validatedEdge, version, txId);
          } else if (opr.getValue().getAsString().equalsIgnoreCase("modify")) {
            Edge edge = dao.getEdge(edgePayload.getId(), txId);
            
            // If the type isn't set, resolve it based on on the sourece and target vertex types
            if (edgePayload.getType() == null || edgePayload.getType().isEmpty()) {
              edgePayload.setType(edge.getType());
            }

            // TODO:  Champ needs to support getting an object's relationships within the context of an existing transaction.
            //        Currently it doesn't.  Disabling multiplicity check until this happens.
            
            List<Edge> sourceVertexEdges = new ArrayList<Edge>();
            List<Edge> targetVertexEdges = new ArrayList<Edge>();
            
            /*
            // load source and target vertex relationships for validation
            List<Edge> sourceVertexEdges =
                   EdgePayloadUtil.filterEdgesByRelatedVertexAndType(EdgePayloadUtil.getVertexNodeType(edgePayload.getSource()), edgePayload.getType(),
                                dao.getVertexEdges(EdgePayloadUtil.getVertexNodeId(edgePayload.getSource()), null, txId));

            List<Edge> targetVertexEdges =
                    EdgePayloadUtil.filterEdgesByRelatedVertexAndType(EdgePayloadUtil.getVertexNodeType(edgePayload.getTarget()), edgePayload.getType(),
                                dao.getVertexEdges(EdgePayloadUtil.getVertexNodeId(edgePayload.getTarget()), null, txId));
            */
            
            validatedEdge = RelationshipSchemaValidator.validateIncomingUpdatePayload(edge, version, edgePayload, edgePayload.getType(), sourceVertexEdges, targetVertexEdges);
            persistedEdge = updateBulkEdge(validatedEdge, version, txId);
          } else {
            if (edgePayload.getId() == null) {
              throw new CrudException("id must be specified for patch request", Status.BAD_REQUEST);
            }
            Edge existingEdge = dao.getEdge(edgePayload.getId(), txId);
            
            // If the type isn't set, resolve it based on on the sourece and target vertex types
            if (edgePayload.getType() == null || edgePayload.getType().isEmpty()) {
              edgePayload.setType(existingEdge.getType());
            }
            
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


  public abstract ImmutablePair<EntityTag, String> addVertex(String version, String type, VertexPayload payload)
            throws CrudException;
  public abstract ImmutablePair<EntityTag, String> updateVertex(String version, String id, String type,
            VertexPayload payload) throws CrudException;
  public abstract ImmutablePair<EntityTag, String> patchVertex(String version, String id, String type,
            VertexPayload payload) throws CrudException;
  public abstract String deleteVertex(String version, String id, String type) throws CrudException;
  public abstract ImmutablePair<EntityTag, String> addEdge(String version, String type, EdgePayload payload)
            throws CrudException;
  public abstract String deleteEdge(String version, String id, String type) throws CrudException;
  public abstract ImmutablePair<EntityTag, String> updateEdge(String version, String id, String type,
            EdgePayload payload) throws CrudException;
  public abstract ImmutablePair<EntityTag, String> patchEdge(String version, String id, String type,
            EdgePayload payload) throws CrudException;

  protected abstract Vertex addBulkVertex(Vertex vertex, String version, String dbTransId) throws CrudException;
  protected abstract Vertex updateBulkVertex(Vertex vertex, String id, String version, String dbTransId) throws CrudException;
  protected abstract void deleteBulkVertex(String id, String version, String type, String dbTransId) throws CrudException;

  protected abstract Edge addBulkEdge(Edge edge, String version, String dbTransId) throws CrudException;
  protected abstract Edge updateBulkEdge(Edge edge, String version, String dbTransId) throws CrudException;
  protected abstract void deleteBulkEdge(String id, String version, String dbTransId) throws CrudException;

}
