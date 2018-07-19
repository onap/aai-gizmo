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


import java.util.HashMap;
import java.util.List;
import javax.ws.rs.core.EntityTag;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.onap.aai.restclient.client.OperationResult;
import org.onap.crud.dao.GraphDao;
import org.onap.crud.entity.Edge;
import org.onap.crud.entity.Vertex;
import org.onap.crud.exception.CrudException;
import org.onap.crud.parser.CrudResponseBuilder;
import org.onap.crud.parser.EdgePayload;
import org.onap.crud.parser.VertexPayload;
import org.onap.crud.parser.util.EdgePayloadUtil;
import org.onap.crud.util.CrudServiceUtil;
import org.onap.schema.validation.OxmModelValidator;
import org.onap.schema.validation.RelationshipSchemaValidator;


public class CrudGraphDataService extends AbstractGraphDataService {


  public CrudGraphDataService(GraphDao dao) throws CrudException {
    super();
    this.dao = dao;
    this.daoForGet = dao;
  }

  public CrudGraphDataService(GraphDao dao, GraphDao daoForGet) throws CrudException {
    super();
    this.dao = dao;
    this.daoForGet = daoForGet;
  }

  @Override
  public ImmutablePair<EntityTag, String> addVertex(String version, String type, VertexPayload payload)
            throws CrudException {
    Vertex vertex = OxmModelValidator.validateIncomingUpsertPayload(null, version, type, payload.getProperties());
    return addVertex(version, vertex);
  }

  private ImmutablePair<EntityTag, String> addVertex(String version, Vertex vertex) throws CrudException {
    OperationResult addedVertexResult = dao.addVertex(vertex.getType(), vertex.getProperties(), version);
    EntityTag entityTag = CrudServiceUtil.getETagFromHeader(addedVertexResult.getHeaders());
    Vertex addedVertex = Vertex.fromJson(addedVertexResult.getResult(), version);
    String payload = CrudResponseBuilder
        .buildUpsertVertexResponse(OxmModelValidator.validateOutgoingPayload(version, addedVertex), version);

    return new ImmutablePair<>(entityTag, payload);
  }

  @Override
  public ImmutablePair<EntityTag, String> addEdge(String version, String type, EdgePayload payload)
            throws CrudException {
    // load source and target vertex relationships for validation
    List<Edge> sourceVertexEdges =
             EdgePayloadUtil.filterEdgesByRelatedVertexAndType(EdgePayloadUtil.getVertexNodeType(payload.getSource()), type,
                          daoForGet.getVertexEdges(EdgePayloadUtil.getVertexNodeId(payload.getSource()), null, null));

    List<Edge> targetVertexEdges =
              EdgePayloadUtil.filterEdgesByRelatedVertexAndType(EdgePayloadUtil.getVertexNodeType(payload.getTarget()), type,
                          daoForGet.getVertexEdges(EdgePayloadUtil.getVertexNodeId(payload.getTarget()), null, null));

    Edge edge = RelationshipSchemaValidator.validateIncomingAddPayload(version, type, payload, sourceVertexEdges, targetVertexEdges);

    return addEdge(version, edge);
  }

  private ImmutablePair<EntityTag, String> addEdge(String version, Edge edge) throws CrudException {
    OperationResult addedEdgeResult = dao.addEdge(edge.getType(), edge.getSource(), edge.getTarget(), edge.getProperties(), version);
    EntityTag entityTag = CrudServiceUtil.getETagFromHeader(addedEdgeResult.getHeaders());
    Edge addedEdge = Edge.fromJson(addedEdgeResult.getResult());
    String payload = CrudResponseBuilder
      .buildUpsertEdgeResponse(RelationshipSchemaValidator.validateOutgoingPayload(version, addedEdge), version);

    return new ImmutablePair<>(entityTag, payload);
  }

  @Override
  public ImmutablePair<EntityTag, String> updateVertex(String version, String id, String type, VertexPayload payload)
            throws CrudException {
    Vertex vertex = OxmModelValidator.validateIncomingUpsertPayload(id, version, type, payload.getProperties());
    return updateVertex(version, vertex);
  }

  private ImmutablePair<EntityTag, String> updateVertex(String version, Vertex vertex) throws CrudException {
    OperationResult updatedVertexResult = dao.updateVertex(vertex.getId().get(), vertex.getType(), vertex.getProperties(), version);
    String payload = getUpdatedVertexPayload(version, updatedVertexResult);
    EntityTag entityTag = CrudServiceUtil.getETagFromHeader(updatedVertexResult.getHeaders());

    return new ImmutablePair<>(entityTag, payload);
  }

  private String getUpdatedVertexPayload(String version, OperationResult updatedVertexResult) throws CrudException {
    Vertex updatedVertex = Vertex.fromJson(updatedVertexResult.getResult(), version);

    return CrudResponseBuilder
      .buildUpsertVertexResponse(OxmModelValidator.validateOutgoingPayload(version, updatedVertex), version);
  }

  @Override
  public ImmutablePair<EntityTag, String> patchVertex(String version, String id, String type, VertexPayload payload)
            throws CrudException {
    OperationResult existingVertexOpResult = dao.getVertex(id, OxmModelValidator.resolveCollectionType(version, type), version, new HashMap<String, String>());
    Vertex existingVertex = Vertex.fromJson(existingVertexOpResult.getResult(), version);
    Vertex vertex = OxmModelValidator.validateIncomingPatchPayload(id, version, type, payload.getProperties(),
          existingVertex);
    return updateVertex(version, vertex);
  }

  @Override
  public String deleteVertex(String version, String id, String type) throws CrudException {
    type = OxmModelValidator.resolveCollectionType(version, type);
    dao.deleteVertex(id, type);
    return "";
  }

  @Override
  public String deleteEdge(String version, String id, String type) throws CrudException {
    RelationshipSchemaValidator.validateType(version, type);
    dao.deleteEdge(id);
    return "";
  }

  @Override
  public ImmutablePair<EntityTag, String> updateEdge(String version, String id, String type, EdgePayload payload)
            throws CrudException {
    OperationResult edgeResult = dao.getEdge(id, type, new HashMap<String, String>());
    Edge edge = Edge.fromJson(edgeResult.getResult());

    // load source and target vertex relationships for validation
    List<Edge> sourceVertexEdges =
             EdgePayloadUtil.filterEdgesByRelatedVertexAndType(EdgePayloadUtil.getVertexNodeType(payload.getSource()), type,
                          daoForGet.getVertexEdges(EdgePayloadUtil.getVertexNodeId(payload.getSource()), null, null));

    List<Edge> targetVertexEdges =
              EdgePayloadUtil.filterEdgesByRelatedVertexAndType(EdgePayloadUtil.getVertexNodeType(payload.getTarget()), type,
                          daoForGet.getVertexEdges(EdgePayloadUtil.getVertexNodeId(payload.getTarget()), null, null));

    Edge validatedEdge = RelationshipSchemaValidator.validateIncomingUpdatePayload(edge, version, payload, type, sourceVertexEdges, targetVertexEdges);

    return updateEdge(version, validatedEdge);
  }

  private ImmutablePair<EntityTag, String> updateEdge(String version, Edge edge) throws CrudException {
    OperationResult updatedEdgeResult = dao.updateEdge(edge);
    String payload = getUpdatedEdgePayload(version, updatedEdgeResult);
    EntityTag entityTag = CrudServiceUtil.getETagFromHeader(updatedEdgeResult.getHeaders());

    return new ImmutablePair<>(entityTag, payload);
  }

  private String getUpdatedEdgePayload(String version, OperationResult updatedEdgeResult) throws CrudException {
    Edge updatedEdge = Edge.fromJson(updatedEdgeResult.getResult());

    return CrudResponseBuilder
      .buildUpsertEdgeResponse(RelationshipSchemaValidator.validateOutgoingPayload(version, updatedEdge), version);
  }

  @Override
  public ImmutablePair<EntityTag, String> patchEdge(String version, String id, String type, EdgePayload payload)
            throws CrudException {
    OperationResult operationResult = dao.getEdge(id, type, new HashMap<String, String>());
    Edge patchedEdge = RelationshipSchemaValidator.validateIncomingPatchPayload(Edge.fromJson(operationResult.getResult()), version, payload);
    return updateEdge(version, patchedEdge);
  }

  @Override
  protected Vertex addBulkVertex(Vertex vertex, String version, String dbTransId) throws CrudException {
    return dao.addVertex(vertex.getType(), vertex.getProperties(), version, dbTransId);
  }

  @Override
  protected Vertex updateBulkVertex(Vertex vertex, String id, String version, String dbTransId) throws CrudException {
    return dao.updateVertex(id, vertex.getType(), vertex.getProperties(), version, dbTransId);
  }

  @Override
  protected void deleteBulkVertex(String id, String version, String type, String dbTransId) throws CrudException {
    dao.deleteVertex(id, type, dbTransId);
  }

  @Override
  protected Edge addBulkEdge(Edge edge, String version, String dbTransId) throws CrudException {
    return dao.addEdge(edge.getType(), edge.getSource(), edge.getTarget(), edge.getProperties(), version, dbTransId);
  }

  @Override
  protected Edge updateBulkEdge(Edge edge, String version, String dbTransId) throws CrudException {
    return dao.updateEdge(edge, dbTransId);
  }

  @Override
  protected void deleteBulkEdge(String id, String version, String dbTransId) throws CrudException {
    dao.deleteEdge(id, dbTransId);
  }
}
