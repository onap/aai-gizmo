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
package org.openecomp.crud.service;

import org.onap.aai.event.api.EventPublisher;

import org.openecomp.crud.dao.GraphDao;
import org.openecomp.crud.dao.champ.ChampDao;
import org.openecomp.crud.entity.Edge;
import org.openecomp.crud.entity.Vertex;
import org.openecomp.crud.exception.CrudException;
import org.openecomp.crud.parser.CrudResponseBuilder;
import org.openecomp.crud.util.CrudProperties;
import org.openecomp.crud.util.CrudServiceConstants;
import org.openecomp.schema.OxmModelLoader;
import org.openecomp.schema.OxmModelValidator;
import org.openecomp.schema.RelationshipSchemaLoader;
import org.openecomp.schema.RelationshipSchemaValidator;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public class CrudGraphDataService {

  private GraphDao dao;

  public CrudGraphDataService(EventPublisher champEventPublisher) throws CrudException {

    // Configure the GraphDao and wire it
    Properties champProperties = new Properties();
    champProperties.put(ChampDao.CONFIG_STORAGE_BACKEND, "titan");
    champProperties.put(ChampDao.CONFIG_STORAGE_BACKEND_DB,
        CrudProperties.get(CrudServiceConstants.CRD_STORAGE_BACKEND_DB, "hbase"));
    champProperties.put(ChampDao.CONFIG_STORAGE_HOSTNAMES,
        CrudProperties.get(CrudServiceConstants.CRD_GRAPH_HOST));
    champProperties.put(ChampDao.CONFIG_STORAGE_PORT,
        CrudProperties.get(CrudServiceConstants.CRD_GRAPH_PORT, "2181"));
    champProperties.put(ChampDao.CONFIG_HBASE_ZNODE_PARENT,
        CrudProperties.get(CrudServiceConstants.CRD_HBASE_ZNODE_PARENT, "/hbase-unsecure"));

    if (CrudProperties.get("crud.graph.name") != null) {
      champProperties.put(ChampDao.CONFIG_GRAPH_NAME, CrudProperties.get("crud.graph.name"));
    }

    if (champEventPublisher != null) {
      champProperties.put(ChampDao.CONFIG_EVENT_STREAM_PUBLISHER, champEventPublisher);
    }

    if (CrudProperties.get(ChampDao.CONFIG_EVENT_STREAM_NUM_PUBLISHERS) != null) {
      champProperties.put(ChampDao.CONFIG_EVENT_STREAM_NUM_PUBLISHERS,
          Integer.parseInt(CrudProperties.get(ChampDao.CONFIG_EVENT_STREAM_NUM_PUBLISHERS)));
    }

    ChampDao champDao = new ChampDao(champProperties);

    this.dao = champDao;

    //load the schemas
    OxmModelLoader.loadModels();
    RelationshipSchemaLoader.loadModels();
  }


  public String addVertex(String version, String type, VertexPayload payload) throws CrudException {
    Vertex vertex = OxmModelValidator.validateIncomingUpsertPayload(null, version, type,
        payload.getProperties());
    return addVertex(version, vertex);
  }

  private String addVertex(String version, Vertex vertex) throws CrudException {
    Vertex addedVertex = dao.addVertex(vertex.getType(), vertex.getProperties());
    return CrudResponseBuilder
        .buildUpsertVertexResponse(OxmModelValidator.validateOutgoingPayload(version, addedVertex),
            version);
  }

  public String addEdge(String version, String type, EdgePayload payload) throws CrudException {
    Edge edge = RelationshipSchemaValidator.validateIncomingAddPayload(version, type, payload);
    return addEdge(version, edge);
  }

  private String addEdge(String version, Edge edge) throws CrudException {
    Edge addedEdge = dao.addEdge(edge.getType(), edge.getSource(), edge.getTarget(),
        edge.getProperties());
    return CrudResponseBuilder.buildUpsertEdgeResponse(
        RelationshipSchemaValidator.validateOutgoingPayload(version, addedEdge), version);
  }

  public String getEdge(String version, String id, String type) throws CrudException {
    RelationshipSchemaValidator.validateType(version, type);
    Edge edge = dao.getEdge(id, type);

    return CrudResponseBuilder.buildGetEdgeResponse(RelationshipSchemaValidator
            .validateOutgoingPayload(version, edge),
        version);
  }

  public String getEdges(String version, String type, Map<String, String> filter)
      throws CrudException {
    RelationshipSchemaValidator.validateType(version, type);
    List<Edge> items = dao.getEdges(type, RelationshipSchemaValidator
        .resolveCollectionfilter(version, type, filter));
    return CrudResponseBuilder.buildGetEdgesResponse(items, version);
  }


  public String updateVertex(String version, String id, String type, VertexPayload payload)
      throws CrudException {
    Vertex vertex = OxmModelValidator.validateIncomingUpsertPayload(id, version, type,
        payload.getProperties());
    return updateVertex(version, vertex);

  }

  private String updateVertex(String version, Vertex vertex) throws CrudException {
    Vertex updatedVertex = dao.updateVertex(vertex.getId().get(), vertex.getType(),
        vertex.getProperties());
    return CrudResponseBuilder
        .buildUpsertVertexResponse(OxmModelValidator.validateOutgoingPayload(version,
            updatedVertex), version);
  }

  public String patchVertex(String version, String id, String type, VertexPayload payload)
      throws CrudException {
    Vertex existingVertex = dao.getVertex(id, OxmModelValidator.resolveCollectionType(version,
        type));
    Vertex vertex = OxmModelValidator.validateIncomingPatchPayload(id, version, type,
        payload.getProperties(), existingVertex);
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

  public String updateEdge(String version, String id, String type, EdgePayload payload)
      throws CrudException {
    Edge edge = dao.getEdge(id, type);
    Edge validatedEdge = RelationshipSchemaValidator.validateIncomingUpdatePayload(edge,
        version, payload);
    return updateEdge(version, validatedEdge);

  }

  private String updateEdge(String version, Edge edge) throws CrudException {
    Edge updatedEdge = dao.updateEdge(edge);
    return CrudResponseBuilder.buildUpsertEdgeResponse(
        RelationshipSchemaValidator.validateOutgoingPayload(version, updatedEdge), version);
  }

  public String patchEdge(String version, String id, String type, EdgePayload payload)
      throws CrudException {
    Edge edge = dao.getEdge(id, type);
    Edge patchedEdge = RelationshipSchemaValidator.validateIncomingPatchPayload(edge,
        version, payload);
    return updateEdge(version, patchedEdge);

  }

  public String getVertex(String version, String id, String type) throws CrudException {
    type = OxmModelValidator.resolveCollectionType(version, type);
    Vertex vertex = dao.getVertex(id, type);
    List<Edge> edges = dao.getVertexEdges(id);
    return CrudResponseBuilder.buildGetVertexResponse(OxmModelValidator
            .validateOutgoingPayload(version, vertex), edges, version);
  }

  public String getVertices(String version, String type, Map<String, String> filter)
      throws CrudException {
    type = OxmModelValidator.resolveCollectionType(version, type);
    List<Vertex> items = dao.getVertices(type, OxmModelValidator.resolveCollectionfilter(version,
        type, filter));
    return CrudResponseBuilder.buildGetVerticesResponse(items, version);
  }

}
