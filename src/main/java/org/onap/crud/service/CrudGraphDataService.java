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
import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.restclient.client.OperationResult;
import org.onap.crud.dao.GraphDao;
import org.onap.crud.dao.champ.ChampBulkPayload;
import org.onap.crud.dao.champ.ChampBulkPayloadResponse;
import org.onap.crud.entity.Edge;
import org.onap.crud.entity.Vertex;
import org.onap.crud.exception.CrudException;
import org.onap.crud.logging.CrudServiceMsgs;
import org.onap.crud.service.BulkPayload;
import org.onap.crud.parser.CrudResponseBuilder;
import org.onap.crud.util.CrudServiceUtil;
import org.onap.schema.OxmModelValidator;
import org.onap.schema.RelationshipSchemaValidator;

import com.google.gson.GsonBuilder;


public class CrudGraphDataService extends AbstractGraphDataService {
    Logger logger = LoggerFactory.getInstance().getLogger(CrudGraphDataService.class.getName());

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
	  
	Edge edge = RelationshipSchemaValidator.validateIncomingAddPayload(version, type, payload);

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
	Edge validatedEdge = getValidatedEdge(version, id, type, payload);

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

  private Edge getValidatedEdge(String version, String id, String type, EdgePayload payload) throws CrudException {
    OperationResult operationResult = dao.getEdge(id, type, new HashMap<String, String>());
    return RelationshipSchemaValidator.validateIncomingUpdatePayload(Edge.fromJson(operationResult.getResult()), version, payload);
  }
  
  @Override
  public ImmutablePair<EntityTag, String> patchEdge(String version, String id, String type, EdgePayload payload)
            throws CrudException {
    OperationResult operationResult = dao.getEdge(id, type, new HashMap<String, String>());
    Edge patchedEdge = RelationshipSchemaValidator.validateIncomingPatchPayload(Edge.fromJson(operationResult.getResult()), version, payload);
    return updateEdge(version, patchedEdge);
  }
  
  @Override
  public String addBulk(String version, BulkPayload payload, HttpHeaders headers) throws CrudException {
      ChampBulkPayload champPayload = new ChampBulkPayload();
      champPayload.fromGizmoPayload(payload, version, headers, dao);
      logger.info(CrudServiceMsgs.CHAMP_BULK_OP_INFO, "ChampBulkPayload-> "+new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(champPayload));
      OperationResult bulkResult = dao.bulkOperation(champPayload);

      ChampBulkPayloadResponse response = ChampBulkPayloadResponse.fromJson(bulkResult.getResult());
      response.populateChampData(version);
      return CrudResponseBuilder.buildUpsertBulkResponse(response.getVertices(), response.getEdges(), version, payload);
  }
}
