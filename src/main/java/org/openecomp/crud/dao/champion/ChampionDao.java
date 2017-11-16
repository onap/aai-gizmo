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
package org.openecomp.crud.dao.champion;

import net.dongliu.gson.GsonJava8TypeAdapterFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.jetty.util.security.Password;
import org.openecomp.aai.logging.LoggingContext;
import org.openecomp.cl.api.Logger;
import org.openecomp.cl.eelf.LoggerFactory;
import org.openecomp.crud.dao.GraphDao;
import org.openecomp.crud.entity.Edge;
import org.openecomp.crud.entity.Vertex;
import org.openecomp.crud.exception.CrudException;
import org.openecomp.crud.util.CrudServiceConstants;
import org.openecomp.restclient.client.OperationResult;
import org.openecomp.restclient.client.RestClient;
import org.openecomp.restclient.enums.RestAuthenticationMode;
import org.slf4j.MDC;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class ChampionDao implements GraphDao {
  private RestClient client;
  private String baseUrl;

  private static final String HEADER_FROM_APP = "X-FromAppId";
  private static final String HEADER_TRANS_ID = "X-TransactionId";

  private Logger logger = LoggerFactory.getInstance().getLogger(ChampionDao.class.getName());

  // We use a custom vertex serializer for Champion because it expects "key" instead of "id"
  private static final Gson championGson = new GsonBuilder()
      .registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory())
      .registerTypeAdapter(Vertex.class, new ChampionVertexSerializer())
      .registerTypeAdapter(Edge.class, new ChampionEdgeSerializer())
      .create();

  public ChampionDao(String championUrl, String certPassword) {
    try {
      client = new RestClient().authenticationMode(RestAuthenticationMode.SSL_CERT)
          .validateServerHostname(false)
          .validateServerCertChain(false)
          .clientCertFile(CrudServiceConstants.CRD_CHAMPION_AUTH_FILE)
          .clientCertPassword(Password.deobfuscate(certPassword));

      baseUrl = championUrl;
    } catch (Exception e) {
      System.out.println("Error setting up Champion configuration");
      e.printStackTrace();
      System.exit(1);
    }
  }

  @Override
  public Vertex getVertex(String id) throws CrudException {
    String url = baseUrl + "objects/" + id;
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    OperationResult getResult = client.get(url, headers, MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == 200) {
      return Vertex.fromJson(getResult.getResult());
    } else {
      // We didn't find a vertex with the supplied id, so just throw an exception.
      throw new CrudException("No vertex with id " + id + " found in graph", javax.ws.rs.core.Response.Status.NOT_FOUND);
    }
  }

  @Override
  public Vertex getVertex(String id, String type) throws CrudException {
    String url = baseUrl + "objects/" + id;
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    OperationResult getResult = client.get(url, headers, MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == 200) {
      Vertex vert = Vertex.fromJson(getResult.getResult());

      if (!vert.getType().equalsIgnoreCase(type)) {
        // We didn't find a vertex with the supplied type, so just throw an exception.
        throw new CrudException("No vertex with id " + id + "and type " + type + " found in graph", javax.ws.rs.core.Response.Status.NOT_FOUND);
      }
      return vert;
    } else {
      // We didn't find a vertex with the supplied id, so just throw an exception.
      throw new CrudException("No vertex with id " + id + " found in graph", javax.ws.rs.core.Response.Status.NOT_FOUND);
    }
  }

  @Override
  public List<Edge> getVertexEdges(String id) throws CrudException {
    String url = baseUrl + "objects/relationships/" + id;
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    OperationResult getResult = client.get(url, headers, MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == 200) {
      return championGson.fromJson(getResult.getResult(), new TypeToken<List<Edge>>(){}.getType());
    } else {
      // We didn't find a vertex with the supplied id, so just throw an exception.
      throw new CrudException("No vertex with id " + id + " found in graph", javax.ws.rs.core.Response.Status.NOT_FOUND);
    }
  }

  @Override
  public List<Vertex> getVertices(String type, Map<String, Object> filter) throws CrudException {
    filter.put(org.openecomp.schema.OxmModelValidator.Metadata.NODE_TYPE.propertyName(), type);

    String url = baseUrl + "objects/filter" + "?" +
        URLEncodedUtils.format(convertToNameValuePair(filter), Charset.defaultCharset());

    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    OperationResult getResult = client.get(url, headers, MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == 200) {
      return championGson.fromJson(getResult.getResult(), new TypeToken<List<Vertex>>(){}.getType());
    } else {
      // We didn't find a vertex with the supplied id, so just throw an exception.
      throw new CrudException("No vertices found in graph for given filters", javax.ws.rs.core.Response.Status.NOT_FOUND);
    }
  }

  @Override
  public Edge getEdge(String id, String type) throws CrudException {
    String url = baseUrl + "relationships/" + id;
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    OperationResult getResult = client.get(url, headers, MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == 200) {
      Edge edge = Edge.fromJson(getResult.getResult());

      if (!edge.getType().equalsIgnoreCase(type)) {
        // We didn't find an edge with the supplied type, so just throw an exception.
        throw new CrudException("No edge with id " + id + "and type " + type + " found in graph", javax.ws.rs.core.Response.Status.NOT_FOUND);
      }
      return edge;
    } else {
      // We didn't find a edge with the supplied type, so just throw an exception.
      throw new CrudException("No edge with id " + id + " found in graph", javax.ws.rs.core.Response.Status.NOT_FOUND);
    }
  }

  @Override
  public List<Edge> getEdges(String type, Map<String, Object> filter) throws CrudException {
    String url = baseUrl + "relationships/filter" + "?" +
        URLEncodedUtils.format(convertToNameValuePair(filter), Charset.defaultCharset());

    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    OperationResult getResult = client.get(url, headers, MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == 200) {
      return championGson.fromJson(getResult.getResult(), new TypeToken<List<Edge>>(){}.getType());
    } else {
      // We didn't find a vertex with the supplied id, so just throw an exception.
      throw new CrudException("No edges found in graph for given filters", javax.ws.rs.core.Response.Status.NOT_FOUND);
    }
  }

  @Override
  public Vertex addVertex(String type, Map<String, Object> properties) throws CrudException {
    String url = baseUrl + "objects";
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    // Add the aai_node_type so that AAI can read the data created by gizmo
    // TODO: This probably shouldn't be here
    properties.put(org.openecomp.schema.OxmModelValidator.Metadata.NODE_TYPE.propertyName(), type);

    Vertex.Builder insertVertexBuilder = new Vertex.Builder(type);
    properties.forEach(insertVertexBuilder::property);
    Vertex insertVertex = insertVertexBuilder.build();

    OperationResult getResult = client.post(url, insertVertex.toJson(), headers,
        MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == Response.Status.CREATED.getStatusCode()) {
      return Vertex.fromJson(getResult.getResult());
    } else {
      // We didn't create a vertex with the supplied type, so just throw an exception.
      throw new CrudException("Failed to create vertex", Response.Status.fromStatusCode(getResult.getResultCode()));
    }
  }

  @Override
  public Vertex updateVertex(String id, String type, Map<String, Object> properties) throws CrudException {
    String url = baseUrl + "objects/" + id;
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    // Add the aai_node_type so that AAI can read the data created by gizmo
    // TODO: This probably shouldn't be here
    properties.put(org.openecomp.schema.OxmModelValidator.Metadata.NODE_TYPE.propertyName(), type);

    Vertex.Builder insertVertexBuilder = new Vertex.Builder(type);
    insertVertexBuilder.id(id);
    properties.forEach(insertVertexBuilder::property);
    Vertex insertVertex = insertVertexBuilder.build();

    String payload = insertVertex.toJson(championGson);
    OperationResult getResult = client.put(url, payload, headers,
        MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == Response.Status.OK.getStatusCode()) {
      return Vertex.fromJson(getResult.getResult());
    } else {
      // We didn't create a vertex with the supplied type, so just throw an exception.
      throw new CrudException("Failed to update vertex", Response.Status.fromStatusCode(getResult.getResultCode()));
    }
  }

  @Override
  public void deleteVertex(String id, String type) throws CrudException {
    String url = baseUrl + "objects/" + id;
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    OperationResult getResult = client.delete(url, headers, MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() != Response.Status.OK.getStatusCode()) {
      // We didn't delete a vertex with the supplied id, so just throw an exception.
      throw new CrudException("Failed to delete vertex", Response.Status.fromStatusCode(getResult.getResultCode()));
    }
  }

  @Override
  public Edge addEdge(String type, Vertex source, Vertex target, Map<String, Object> properties) throws CrudException {
    String url = baseUrl + "relationships";
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    // Try requests to ensure source and target exist in Champion
    Vertex dbSource = getVertex(source.getId().get(), source.getType());
    Vertex dbTarget = getVertex(target.getId().get(), target.getType());

    Edge.Builder insertEdgeBuilder = new Edge.Builder(type).source(dbSource).target(dbTarget);
    properties.forEach(insertEdgeBuilder::property);
    Edge insertEdge = insertEdgeBuilder.build();

    String edgeJson = insertEdge.toJson(championGson);
    OperationResult getResult = client.post(url, edgeJson, headers,
        MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == Response.Status.CREATED.getStatusCode()) {
      return Edge.fromJson(getResult.getResult());
    } else {
      // We didn't create an edge with the supplied type, so just throw an exception.
      throw new CrudException("Failed to create edge", Response.Status.fromStatusCode(getResult.getResultCode()));
    }
  }

  @Override
  public Edge updateEdge(Edge edge) throws CrudException {
    if (!edge.getId().isPresent())
    {
      throw new CrudException("Unable to identify edge: " + edge.toString(), Response.Status.BAD_REQUEST);
    }
    String url = baseUrl + "relationships/" + edge.getId().get();
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    String edgeJson = edge.toJson(championGson);
    OperationResult getResult = client.put(url, edgeJson, headers,
        MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == Response.Status.OK.getStatusCode()) {
      return Edge.fromJson(getResult.getResult());
    } else {
      // We didn't create an edge with the supplied type, so just throw an exception.
      throw new CrudException("Failed to update edge", Response.Status.fromStatusCode(getResult.getResultCode()));
    }
  }

  @Override
  public void deleteEdge(String id, String type) throws CrudException {
    String url = baseUrl + "relationships/" + id;
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    OperationResult getResult = client.delete(url, headers, MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() != 200) {
      // We didn't find an edge with the supplied type, so just throw an exception.
      throw new CrudException("No edge with id " + id + " found in graph", javax.ws.rs.core.Response.Status.NOT_FOUND);
    }
  }

  @Override
  public String openTransaction() {
    String url = baseUrl + "transaction";
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    OperationResult getResult = client.post(url, "", headers, MediaType.TEXT_PLAIN_TYPE, MediaType.TEXT_PLAIN_TYPE);

    if (getResult.getResultCode() == 200) {
      return getResult.getResult();
    } else {
      return null;
    }
  }

  @Override
  public void commitTransaction(String id) throws CrudException {
    String url = baseUrl + "transaction/" + id;
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    OperationResult getResult = client.put(url, "{\"method\": \"commit\"}", headers, MediaType.APPLICATION_JSON_TYPE, MediaType.TEXT_PLAIN_TYPE);

    if (getResult.getResultCode() != 200) {
      throw new CrudException("Unable to commit transaction", Response.Status.fromStatusCode(getResult.getResultCode()));
    }
  }

  @Override
  public void rollbackTransaction(String id) throws CrudException {
    String url = baseUrl + "transaction/" + id;
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    OperationResult getResult = client.put(url, "{\"method\": \"rollback\"}", headers, MediaType.APPLICATION_JSON_TYPE, MediaType.TEXT_PLAIN_TYPE);

    if (getResult.getResultCode() != 200) {
      throw new CrudException("Unable to rollback transaction", Response.Status.fromStatusCode(getResult.getResultCode()));
    }
  }

  @Override
  public boolean transactionExists(String id) throws CrudException {
    String url = baseUrl + "transaction/" + id;
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    OperationResult getResult = client.get(url, headers, MediaType.APPLICATION_JSON_TYPE);

    return getResult.getResultCode() == 200;
  }

  @Override
  public Vertex addVertex(String type, Map<String, Object> properties, String txId) throws CrudException {
    String url = baseUrl + "objects?transactionId=" + txId;
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    // Add the aai_node_type so that AAI can read the data created by gizmo
    // TODO: This probably shouldn't be here
    properties.put(org.openecomp.schema.OxmModelValidator.Metadata.NODE_TYPE.propertyName(), type);

    Vertex.Builder insertVertexBuilder = new Vertex.Builder(type);
    properties.forEach(insertVertexBuilder::property);
    Vertex insertVertex = insertVertexBuilder.build();

    OperationResult getResult = client.post(url, insertVertex.toJson(), headers,
        MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == Response.Status.CREATED.getStatusCode()) {
      return Vertex.fromJson(getResult.getResult());
    } else {
      // We didn't create a vertex with the supplied type, so just throw an exception.
      throw new CrudException("Failed to create vertex", Response.Status.fromStatusCode(getResult.getResultCode()));
    }
  }

  @Override
  public Edge addEdge(String type, Vertex source, Vertex target, Map<String, Object> properties, String txId) throws CrudException {
    String url = baseUrl + "relationships?transactionId=" + txId;
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    // Try requests to ensure source and target exist in Champion
    Vertex dbSource = getVertex(source.getId().get(), source.getType(), txId);
    Vertex dbTarget = getVertex(target.getId().get(), target.getType(), txId);

    Edge.Builder insertEdgeBuilder = new Edge.Builder(type).source(dbSource).target(dbTarget);
    properties.forEach(insertEdgeBuilder::property);
    Edge insertEdge = insertEdgeBuilder.build();

    OperationResult getResult = client.post(url, insertEdge.toJson(championGson), headers,
        MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == Response.Status.CREATED.getStatusCode()) {
      return Edge.fromJson(getResult.getResult());
    } else {
      // We didn't create an edge with the supplied type, so just throw an exception.
      throw new CrudException("Failed to create edge", Response.Status.fromStatusCode(getResult.getResultCode()));
    }
  }

  @Override
  public Vertex updateVertex(String id, String type, Map<String, Object> properties, String txId) throws CrudException {
    String url = baseUrl + "objects/" + id + "?transactionId=" + txId;
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    // Add the aai_node_type so that AAI can read the data created by gizmo
    // TODO: This probably shouldn't be here
    properties.put(org.openecomp.schema.OxmModelValidator.Metadata.NODE_TYPE.propertyName(), type);

    Vertex.Builder insertVertexBuilder = new Vertex.Builder(type);
    insertVertexBuilder.id(id);
    properties.forEach(insertVertexBuilder::property);
    Vertex insertVertex = insertVertexBuilder.build();

    String payload = insertVertex.toJson(championGson);
    OperationResult getResult = client.put(url, payload, headers,
        MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == Response.Status.OK.getStatusCode()) {
      return Vertex.fromJson(getResult.getResult());
    } else {
      // We didn't create a vertex with the supplied type, so just throw an exception.
      throw new CrudException("Failed to update vertex", Response.Status.fromStatusCode(getResult.getResultCode()));
    }
  }

  @Override
  public void deleteVertex(String id, String type, String txId) throws CrudException {
    String url = baseUrl + "objects/" + id + "?transactionId=" + txId;
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    OperationResult getResult = client.delete(url, headers, MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() != Response.Status.OK.getStatusCode()) {
      // We didn't delete a vertex with the supplied id, so just throw an exception.
      throw new CrudException("Failed to delete vertex", Response.Status.fromStatusCode(getResult.getResultCode()));
    }
  }

  @Override
  public Edge updateEdge(Edge edge, String txId) throws CrudException {
    if (!edge.getId().isPresent())
    {
      throw new CrudException("Unable to identify edge: " + edge.toString(), Response.Status.BAD_REQUEST);
    }
    String url = baseUrl + "relationships/" + edge.getId().get() + "?transactionId=" + txId;
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    OperationResult getResult = client.put(url, edge.toJson(championGson), headers,
        MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == Response.Status.OK.getStatusCode()) {
      return Edge.fromJson(getResult.getResult());
    } else {
      // We didn't create an edge with the supplied type, so just throw an exception.
      throw new CrudException("Failed to update edge: " + getResult.getFailureCause(), Response.Status.fromStatusCode(getResult.getResultCode()));
    }
  }

  @Override
  public void deleteEdge(String id, String type, String txId) throws CrudException {
    String url = baseUrl + "relationships/" + id + "?transactionId=" + txId;
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    OperationResult getResult = client.delete(url, headers, MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() != 200) {
      // We didn't find an edge with the supplied type, so just throw an exception.
      throw new CrudException("No edge with id " + id + " found in graph", javax.ws.rs.core.Response.Status.NOT_FOUND);
    }
  }

  @Override
  public Edge getEdge(String id, String type, String txId) throws CrudException {
    String url = baseUrl + "relationships/" + id + "?transactionId=" + txId;
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    OperationResult getResult = client.get(url, headers, MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == 200) {
      Edge edge = Edge.fromJson(getResult.getResult());

      if (!edge.getType().equalsIgnoreCase(type)) {
        // We didn't find an edge with the supplied type, so just throw an exception.
        throw new CrudException("No edge with id " + id + "and type " + type + " found in graph", javax.ws.rs.core.Response.Status.NOT_FOUND);
      }
      return edge;
    } else {
      // We didn't find an edge with the supplied id, so just throw an exception.
      throw new CrudException("No edge with id " + id + " found in graph", javax.ws.rs.core.Response.Status.NOT_FOUND);
    }
  }

  public Vertex getVertex(String id, String type, String txId) throws CrudException {
    String url = baseUrl + "objects/" + id + "?transactionId=" + txId;
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    OperationResult getResult = client.get(url, headers, MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == 200) {
      Vertex vert = Vertex.fromJson(getResult.getResult());

      if (!vert.getType().equalsIgnoreCase(type)) {
        // We didn't find a vertex with the supplied type, so just throw an exception.
        throw new CrudException("No vertex with id " + id + "and type " + type + " found in graph", javax.ws.rs.core.Response.Status.NOT_FOUND);
      }
      return vert;
    } else {
      // We didn't find a vertex with the supplied id, so just throw an exception.
      throw new CrudException("No vertex with id " + id + " found in graph", javax.ws.rs.core.Response.Status.NOT_FOUND);
    }
  }

  // https://stackoverflow.com/questions/26942330/convert-mapstring-string-to-listnamevaluepair-is-this-the-most-efficient
  private List<NameValuePair> convertToNameValuePair(Map<String, Object> pairs) {
    List<NameValuePair> nvpList = new ArrayList<>(pairs.size());

    pairs.forEach((key, value) -> nvpList.add(new BasicNameValuePair(key, value.toString())));

    return nvpList;
  }
}
