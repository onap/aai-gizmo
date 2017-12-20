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

import java.util.List;
import java.util.Map;

import org.onap.crud.dao.GraphDao;
import org.onap.crud.entity.Edge;
import org.onap.crud.entity.Vertex;
import org.onap.crud.exception.CrudException;
import org.onap.crud.parser.CrudResponseBuilder;
import org.onap.crud.util.CrudServiceUtil;
import org.onap.schema.OxmModelValidator;
import org.onap.schema.RelationshipSchemaValidator;

public abstract class AbstractGraphDataService {
  protected GraphDao dao;
  
  public AbstractGraphDataService(GraphDao dao) throws CrudException {
    this.dao = dao;

    CrudServiceUtil.loadModels();
  }
  
  public String getEdge(String version, String id, String type) throws CrudException {
    RelationshipSchemaValidator.validateType(version, type);
    Edge edge = dao.getEdge(id, type);

    return CrudResponseBuilder.buildGetEdgeResponse(RelationshipSchemaValidator.validateOutgoingPayload(version, edge), version);
  }
  
  public String getEdges(String version, String type, Map<String, String> filter) throws CrudException {
    RelationshipSchemaValidator.validateType(version, type);
    List<Edge> items = dao.getEdges(type, RelationshipSchemaValidator.resolveCollectionfilter(version, type, filter));
    return CrudResponseBuilder.buildGetEdgesResponse(items, version);
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
  
  public abstract String addVertex(String version, String type, VertexPayload payload) throws CrudException;
  public abstract String updateVertex(String version, String id, String type, VertexPayload payload) throws CrudException;
  public abstract String patchVertex(String version, String id, String type, VertexPayload payload) throws CrudException;
  public abstract String deleteVertex(String version, String id, String type) throws CrudException;
  public abstract String addEdge(String version, String type, EdgePayload payload) throws CrudException;
  public abstract String deleteEdge(String version, String id, String type) throws CrudException;
  public abstract String updateEdge(String version, String id, String type, EdgePayload payload) throws CrudException;
  public abstract String patchEdge(String version, String id, String type, EdgePayload payload) throws CrudException;
  public abstract String addBulk(String version, BulkPayload payload) throws CrudException;
}
