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


import org.onap.crud.dao.GraphDao;
import org.onap.crud.entity.Edge;

import org.onap.crud.entity.Vertex;
import org.onap.crud.exception.CrudException;
import org.onap.crud.parser.CrudResponseBuilder;
import org.onap.schema.OxmModelValidator;
import org.onap.schema.RelationshipSchemaValidator;


public class CrudGraphDataService extends AbstractGraphDataService {

  public CrudGraphDataService(GraphDao dao) throws CrudException {
    super(dao);
  }

  public String addVertex(String version, String type, VertexPayload payload) throws CrudException {
    Vertex vertex = OxmModelValidator.validateIncomingUpsertPayload(null, version, type, payload.getProperties());
    return addVertex(version, vertex);
  }

  private String addVertex(String version, Vertex vertex) throws CrudException {
    Vertex addedVertex = dao.addVertex(vertex.getType(), vertex.getProperties(), version);
    return CrudResponseBuilder
        .buildUpsertVertexResponse(OxmModelValidator.validateOutgoingPayload(version, addedVertex), version);
  }

  public String addEdge(String version, String type, EdgePayload payload) throws CrudException {
    Edge edge = RelationshipSchemaValidator.validateIncomingAddPayload(version, type, payload);
    return addEdge(version, edge);
  }

  private String addEdge(String version, Edge edge) throws CrudException {
    Edge addedEdge = dao.addEdge(edge.getType(), edge.getSource(), edge.getTarget(), edge.getProperties(), version);
    return CrudResponseBuilder
        .buildUpsertEdgeResponse(RelationshipSchemaValidator.validateOutgoingPayload(version, addedEdge), version);
  }

  public String updateVertex(String version, String id, String type, VertexPayload payload) throws CrudException {
    Vertex vertex = OxmModelValidator.validateIncomingUpsertPayload(id, version, type, payload.getProperties());
    return updateVertex(version, vertex);

  }

  private String updateVertex(String version, Vertex vertex) throws CrudException {
    Vertex updatedVertex = dao.updateVertex(vertex.getId().get(), vertex.getType(), vertex.getProperties(), version);
    return CrudResponseBuilder
        .buildUpsertVertexResponse(OxmModelValidator.validateOutgoingPayload(version, updatedVertex), version);
  }

  public String patchVertex(String version, String id, String type, VertexPayload payload) throws CrudException {
    Vertex existingVertex = dao.getVertex(id, OxmModelValidator.resolveCollectionType(version, type), version);
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

  public Vertex getVertex(String id, String version) throws CrudException {
    return dao.getVertex(id, version);
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
  protected void deleteBulkEdge(String id, String version, String type, String dbTransId) throws CrudException {
    dao.deleteEdge(id, type, dbTransId);
  }
}
