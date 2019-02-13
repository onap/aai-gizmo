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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.onap.aai.restclient.client.OperationResult;
import org.onap.crud.dao.GraphDao;
import org.onap.crud.dao.champ.ChampEdgeSerializer;
import org.onap.crud.dao.champ.ChampVertexSerializer;
import org.onap.crud.entity.Edge;
import org.onap.crud.entity.Vertex;
import org.onap.crud.exception.CrudException;
import org.onap.crud.parser.CrudResponseBuilder;
import org.onap.crud.util.CrudServiceUtil;
import org.onap.schema.OxmModelValidator;
import org.onap.schema.RelationshipSchemaValidator;

public abstract class AbstractGraphDataService {
  protected GraphDao daoForGet;
  protected GraphDao dao;

  public AbstractGraphDataService() throws CrudException {
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
  
  public abstract String addBulk(String version, BulkPayload payload, HttpHeaders headers) throws CrudException;

}
