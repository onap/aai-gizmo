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
package org.onap.crud.dao.champ;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.jetty.util.security.Password;
import org.onap.aai.cl.mdc.MdcContext;
import org.onap.aai.logging.LoggingContext;
import org.onap.aai.restclient.client.OperationResult;
import org.onap.aai.restclient.client.RestClient;
import org.onap.aai.restclient.enums.RestAuthenticationMode;
import org.onap.crud.dao.GraphDao;
import org.onap.crud.entity.Edge;
import org.onap.crud.entity.Vertex;
import org.onap.crud.exception.CrudException;
import org.onap.crud.util.CrudServiceConstants;
import org.slf4j.MDC;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;

public class ChampDao implements GraphDao {
  protected RestClient client;
  protected String baseObjectUrl;
  protected String baseRelationshipUrl;
  protected String baseTransactionUrl;

  protected static final String HEADER_FROM_APP = "X-FromAppId";
  protected static final String HEADER_TRANS_ID = "X-TransactionId";
  protected static final String FROM_APP_NAME = "Gizmo";
  protected static final String OBJECT_SUB_URL = "objects";
  protected static final String RELATIONSHIP_SUB_URL = "relationships";
  protected static final String TRANSACTION_SUB_URL = "transaction";

  // We use a custom vertex serializer for champ because it expects "key"
  // instead of "id"
  protected static final Gson champGson = new GsonBuilder()
      .registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory())
      .registerTypeAdapter(Vertex.class, new ChampVertexSerializer())
      .registerTypeAdapter(Edge.class, new ChampEdgeSerializer()).create();

  public ChampDao() {
  }

  public ChampDao(String champUrl, String certPassword) {
    try {
      client = new RestClient().authenticationMode(RestAuthenticationMode.SSL_CERT).validateServerHostname(false)
          .validateServerCertChain(false).clientCertFile(CrudServiceConstants.CRD_CHAMP_AUTH_FILE)
          .clientCertPassword(Password.deobfuscate(certPassword));

      baseObjectUrl = champUrl + OBJECT_SUB_URL;
      baseRelationshipUrl = champUrl + RELATIONSHIP_SUB_URL;
      baseTransactionUrl = champUrl + TRANSACTION_SUB_URL;
    } catch (Exception e) {
      System.out.println("Error setting up Champ configuration");
      e.printStackTrace();
      System.exit(1);
    }
  }

  public ChampDao(RestClient client, String baseObjectUrl, String baseRelationshipUrl, String baseTransactionUrl) {
      this.client = client;
      this.baseObjectUrl = baseObjectUrl;
      this.baseRelationshipUrl = baseRelationshipUrl;
      this.baseTransactionUrl = baseTransactionUrl;
  }

  @Override
  public Vertex getVertex(String id, String version) throws CrudException {
    String url = baseObjectUrl + "/" + id;
    OperationResult getResult = client.get(url, createHeader(), MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == 200) {
      return Vertex.fromJson(getResult.getResult(), version);
    } else {
      // We didn't find a vertex with the supplied id, so just throw an
      // exception.
      throw createErrorException(getResult, javax.ws.rs.core.Response.Status.NOT_FOUND, "No vertex with id " + id + " found in graph");
    }
  }

  @Override
  public OperationResult getVertex(String id, String type, String version, Map<String, String> queryParams) throws CrudException {
    StringBuilder strBuild = new StringBuilder(baseObjectUrl);
    strBuild.append("/");
    strBuild.append(id);
    if(queryParams != null && !queryParams.isEmpty())
    {
        strBuild.append("?");
        strBuild.append(URLEncodedUtils.format(convertToNameValuePair(queryParams), Charset.defaultCharset()));
    }

    OperationResult getResult = client.get(strBuild.toString(), createHeader(), MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == 200) {
      Vertex vert = Vertex.fromJson(getResult.getResult(), version);

      if (!vert.getType().equalsIgnoreCase(type)) {
        // We didn't find a vertex with the supplied type, so just throw an
        // exception.
        throw new CrudException("No vertex with id " + id + " and type " + type + " found in graph",
            javax.ws.rs.core.Response.Status.NOT_FOUND);
      }
      return getResult;
    } else {
      // We didn't find a vertex with the supplied id, so just throw an
      // exception.
        throw createErrorException(getResult, javax.ws.rs.core.Response.Status.NOT_FOUND, "No vertex with id " + id + " found in graph");
    }
  }

  @Override
  public List<Edge> getVertexEdges(String id, Map<String, String> queryParams) throws CrudException {
    StringBuilder strBuild = new StringBuilder(baseObjectUrl);
    strBuild.append("/relationships/");
    strBuild.append(id);
    if(queryParams != null && !queryParams.isEmpty())
    {
        strBuild.append("?");
        strBuild.append(URLEncodedUtils.format(convertToNameValuePair(queryParams), Charset.defaultCharset()));
    }

    OperationResult getResult = client.get(strBuild.toString(), createHeader(), MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == 200) {
      return champGson.fromJson(getResult.getResult(), new TypeToken<List<Edge>>() {
      }.getType());
    } else {
      // We didn't find a vertex with the supplied id, so just throw an
      // exception.
      throw createErrorException(getResult, javax.ws.rs.core.Response.Status.NOT_FOUND, "No vertex with id " + id + " found in graph");
    }
  }

  @Override
  public OperationResult getVertices(String type, Map<String, Object> filter, String version) throws CrudException {
    return getVertices(type, filter, new HashSet<String>(), version);
  }

  @Override
  public OperationResult getVertices(String type, Map<String, Object> filter, HashSet<String> properties, String version) throws CrudException {
    filter.put(org.onap.schema.validation.OxmModelValidator.Metadata.NODE_TYPE.propertyName(), type);

    List<NameValuePair> queryParams = convertToNameValuePair(filter);
    queryParams.addAll(convertToNameValuePair("properties", properties));
    String url = baseObjectUrl + "/filter" + "?"
        + URLEncodedUtils.format(queryParams, Charset.defaultCharset());

    OperationResult getResult = client.get(url, createHeader(), MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == 200) {
      return getResult;
    } else {
      // We didn't find a vertex with the supplied id, so just throw an
      // exception.
      throw createErrorException(getResult, javax.ws.rs.core.Response.Status.NOT_FOUND, "No vertices found in graph for given filters");
    }
  }

  @Override
  public OperationResult getEdge(String id, String type, Map<String, String> queryParams) throws CrudException {
    StringBuilder strBuild = new StringBuilder(baseRelationshipUrl);
    strBuild.append("/");
    strBuild.append(id);
    if(queryParams != null && !queryParams.isEmpty())
    {
        strBuild.append("?");
        strBuild.append(URLEncodedUtils.format(convertToNameValuePair(queryParams), Charset.defaultCharset()));
    }
    OperationResult getResult = client.get(strBuild.toString(), createHeader(), MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == 200) {
      Edge edge = Edge.fromJson(getResult.getResult());

      if (!edge.getType().equalsIgnoreCase(type)) {
        // We didn't find an edge with the supplied type, so just throw an
        // exception.
        throw new CrudException("No edge with id " + id + " and type " + type + " found in graph",
            javax.ws.rs.core.Response.Status.NOT_FOUND);
      }
      return getResult;
    } else {
      // We didn't find a edge with the supplied type, so just throw an
      // exception.
      throw createErrorException(getResult, javax.ws.rs.core.Response.Status.NOT_FOUND, "No edge with id " + id + " found in graph");
    }
  }

  @Override
  public OperationResult getEdges(String type, Map<String, Object> filter) throws CrudException {
    String url = baseRelationshipUrl + "/filter" + "?"
        + URLEncodedUtils.format(convertToNameValuePair(filter), Charset.defaultCharset());

    OperationResult getResult = client.get(url, createHeader(), MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == 200) {
        return getResult;
    } else {
      // We didn't find a vertex with the supplied id, so just throw an
      // exception.
      throw createErrorException(getResult, javax.ws.rs.core.Response.Status.NOT_FOUND, "No edges found in graph  for given filters");
    }
  }

  @Override
  public OperationResult addVertex(String type, Map<String, Object> properties, String version) throws CrudException {
    String url = baseObjectUrl;

    // Add the aai_node_type so that AAI can read the data created by gizmo
    // TODO: This probably shouldn't be here
    properties.put(org.onap.schema.validation.OxmModelValidator.Metadata.NODE_TYPE.propertyName(), type);

    Vertex.Builder insertVertexBuilder = new Vertex.Builder(type);
    properties.forEach(insertVertexBuilder::property);
    Vertex insertVertex = insertVertexBuilder.build();

    OperationResult getResult = client.post(url, insertVertex.toJson(), createHeader(), MediaType.APPLICATION_JSON_TYPE,
        MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == Response.Status.CREATED.getStatusCode()) {
      return getResult;
    } else {
      // We didn't create a vertex with the supplied type, so just throw an
      // exception.
      throw new CrudException("Failed to create vertex: " + getResult.getFailureCause(), Response.Status.fromStatusCode(getResult.getResultCode()));
    }
  }

  @Override
  public OperationResult updateVertex(String id, String type, Map<String, Object> properties, String version) throws CrudException {
    String url = baseObjectUrl + "/" + id;

    // Add the aai_node_type so that AAI can read the data created by gizmo
    // TODO: This probably shouldn't be here
    properties.put(org.onap.schema.validation.OxmModelValidator.Metadata.NODE_TYPE.propertyName(), type);

    Vertex.Builder insertVertexBuilder = new Vertex.Builder(type);
    insertVertexBuilder.id(id);
    properties.forEach(insertVertexBuilder::property);
    Vertex insertVertex = insertVertexBuilder.build();

    String payload = insertVertex.toJson(champGson);
    OperationResult getResult = client.put(url, payload, createHeader(), MediaType.APPLICATION_JSON_TYPE,
        MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == Response.Status.OK.getStatusCode()) {
      return getResult;
    } else {
      // We didn't create a vertex with the supplied type, so just throw an
      // exception.
      throw new CrudException("Failed to update vertex: " + getResult.getFailureCause(), Response.Status.fromStatusCode(getResult.getResultCode()));
    }
  }

  @Override
  public void deleteVertex(String id, String type) throws CrudException {
    String url = baseObjectUrl + "/" + id;
    OperationResult getResult = client.delete(url, createHeader(), MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() != Response.Status.OK.getStatusCode()) {
      // We didn't delete a vertex with the supplied id, so just throw an
      // exception.
      throw new CrudException("Failed to delete vertex: " + getResult.getFailureCause(), Response.Status.fromStatusCode(getResult.getResultCode()));
    }
  }

  @Override
  public OperationResult addEdge(String type, Vertex source, Vertex target, Map<String, Object> properties, String version) throws CrudException {
    String url = baseRelationshipUrl;

    // Try requests to ensure source and target exist in Champ
    OperationResult dbSourceOpResult = getVertex(source.getId().get(), source.getType(), version, new HashMap<String, String>());
    Vertex dbSource = Vertex.fromJson(dbSourceOpResult.getResult(), version);
    OperationResult dbTargetOpResult = getVertex(target.getId().get(), target.getType(), version, new HashMap<String, String>());
    Vertex dbTarget = Vertex.fromJson(dbTargetOpResult.getResult(), version);

    Edge.Builder insertEdgeBuilder = new Edge.Builder(type).source(dbSource).target(dbTarget);
    properties.forEach(insertEdgeBuilder::property);
    Edge insertEdge = insertEdgeBuilder.build();

    String edgeJson = insertEdge.toJson(champGson);
    OperationResult getResult = client.post(url, edgeJson, createHeader(), MediaType.APPLICATION_JSON_TYPE,
        MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == Response.Status.CREATED.getStatusCode()) {
      return getResult;
    } else {
      // We didn't create an edge with the supplied type, so just throw an
      // exception.
      throw new CrudException("Failed to create edge: " + getResult.getFailureCause(), Response.Status.fromStatusCode(getResult.getResultCode()));
    }
  }

  @Override
  public OperationResult updateEdge(Edge edge) throws CrudException {
    if (!edge.getId().isPresent()) {
      throw new CrudException("Unable to identify edge: " + edge.toString(), Response.Status.BAD_REQUEST);
    }
    String url = baseRelationshipUrl + "/" + edge.getId().get();

    String edgeJson = edge.toJson(champGson);
    OperationResult getResult = client.put(url, edgeJson, createHeader(), MediaType.APPLICATION_JSON_TYPE,
        MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == Response.Status.OK.getStatusCode()) {
      return getResult;
    } else {
      // We didn't create an edge with the supplied type, so just throw an
      // exception.
      throw new CrudException("Failed to update edge: " + getResult.getFailureCause(), Response.Status.fromStatusCode(getResult.getResultCode()));
    }
  }

  @Override
  public void deleteEdge(String id, String type) throws CrudException {
    String url = baseRelationshipUrl + "/" + id;
    OperationResult getResult = client.delete(url, createHeader(), MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() != 200) {
      // We didn't find an edge with the supplied type, so just throw an
      // exception.
      throw createErrorException(getResult, javax.ws.rs.core.Response.Status.NOT_FOUND, "No edge with id " + id + " found in graph");
    }
  }

  @Override
  public String openTransaction() {
    String url = baseTransactionUrl;

    OperationResult getResult = client.post(url, "", createHeader(), MediaType.TEXT_PLAIN_TYPE, MediaType.TEXT_PLAIN_TYPE);

    if (getResult.getResultCode() == 200) {
      return getResult.getResult();
    } else {
      return null;
    }
  }

  @Override
  public void commitTransaction(String id) throws CrudException {
    String url = baseTransactionUrl + "/" + id;

    OperationResult getResult = client.put(url, "{\"method\": \"commit\"}", createHeader(), MediaType.APPLICATION_JSON_TYPE,
        MediaType.TEXT_PLAIN_TYPE);

    if (getResult.getResultCode() != 200) {
      throw new CrudException("Unable to commit transaction",
          Response.Status.fromStatusCode(getResult.getResultCode()));
    }
  }

  @Override
  public void rollbackTransaction(String id) throws CrudException {
    String url = baseTransactionUrl + "/" + id;

    OperationResult getResult = client.put(url, "{\"method\": \"rollback\"}", createHeader(), MediaType.APPLICATION_JSON_TYPE,
        MediaType.TEXT_PLAIN_TYPE);

    if (getResult.getResultCode() != 200) {
      throw new CrudException("Unable to rollback transaction",
          Response.Status.fromStatusCode(getResult.getResultCode()));
    }
  }

  @Override
  public boolean transactionExists(String id) throws CrudException {
    String url = baseTransactionUrl + "/" + id;
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList("Gizmo"));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(LoggingContext.LoggingField.REQUEST_ID.toString())));

    OperationResult getResult = client.get(url, headers, MediaType.APPLICATION_JSON_TYPE);

    return getResult.getResultCode() == 200;
  }

  @Override
  public Vertex addVertex(String type, Map<String, Object> properties, String version, String txId) throws CrudException {
    String url = baseObjectUrl + "?transactionId=" + txId;

    // Add the aai_node_type so that AAI can read the data created by gizmo
    // TODO: This probably shouldn't be here
    properties.put(org.onap.schema.validation.OxmModelValidator.Metadata.NODE_TYPE.propertyName(), type);

    Vertex.Builder insertVertexBuilder = new Vertex.Builder(type);
    properties.forEach(insertVertexBuilder::property);
    Vertex insertVertex = insertVertexBuilder.build();

    OperationResult getResult = client.post(url, insertVertex.toJson(), createHeader(), MediaType.APPLICATION_JSON_TYPE,
        MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == Response.Status.CREATED.getStatusCode()) {
      return Vertex.fromJson(getResult.getResult(), version);
    } else {
      // We didn't create a vertex with the supplied type, so just throw an
      // exception.
      throw new CrudException("Failed to create vertex: " + getResult.getFailureCause(), Response.Status.fromStatusCode(getResult.getResultCode()));
    }
  }

  @Override
  public Edge addEdge(String type, Vertex source, Vertex target, Map<String, Object> properties, String version, String txId)
      throws CrudException {
    String url = baseRelationshipUrl + "?transactionId=" + txId;

    // Try requests to ensure source and target exist in Champ
    Vertex dbSource = getVertex(source.getId().get(), source.getType(), version, txId);
    Vertex dbTarget = getVertex(target.getId().get(), target.getType(), version, txId);

    Edge.Builder insertEdgeBuilder = new Edge.Builder(type).source(dbSource).target(dbTarget);
    properties.forEach(insertEdgeBuilder::property);
    Edge insertEdge = insertEdgeBuilder.build();

    OperationResult getResult = client.post(url, insertEdge.toJson(champGson), createHeader(),
        MediaType.APPLICATION_JSON_TYPE, MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == Response.Status.CREATED.getStatusCode()) {
      return Edge.fromJson(getResult.getResult());
    } else {
      // We didn't create an edge with the supplied type, so just throw an
      // exception.
      throw new CrudException("Failed to create edge: " + getResult.getFailureCause(), Response.Status.fromStatusCode(getResult.getResultCode()));
    }
  }

  @Override
  public Vertex updateVertex(String id, String type, Map<String, Object> properties, String version, String txId) throws CrudException {
    String url = baseObjectUrl + "/" + id + "?transactionId=" + txId;

    // Add the aai_node_type so that AAI can read the data created by gizmo
    // TODO: This probably shouldn't be here
    properties.put(org.onap.schema.validation.OxmModelValidator.Metadata.NODE_TYPE.propertyName(), type);

    Vertex.Builder insertVertexBuilder = new Vertex.Builder(type);
    insertVertexBuilder.id(id);
    properties.forEach(insertVertexBuilder::property);
    Vertex insertVertex = insertVertexBuilder.build();

    String payload = insertVertex.toJson(champGson);
    OperationResult getResult = client.put(url, payload, createHeader(), MediaType.APPLICATION_JSON_TYPE,
        MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == Response.Status.OK.getStatusCode()) {
      return Vertex.fromJson(getResult.getResult(), version);
    } else {
      // We didn't create a vertex with the supplied type, so just throw an
      // exception.
      throw new CrudException("Failed to update vertex: " + getResult.getFailureCause(), Response.Status.fromStatusCode(getResult.getResultCode()));
    }
  }

  @Override
  public void deleteVertex(String id, String type, String txId) throws CrudException {
    String url = baseObjectUrl + "/" + id + "?transactionId=" + txId;
    OperationResult getResult = client.delete(url, createHeader(), MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() != Response.Status.OK.getStatusCode()) {
      // We didn't delete a vertex with the supplied id, so just throw an
      // exception.
      throw new CrudException("Failed to delete vertex: " + getResult.getFailureCause(), Response.Status.fromStatusCode(getResult.getResultCode()));
    }
  }

  @Override
  public Edge updateEdge(Edge edge, String txId) throws CrudException {
    if (!edge.getId().isPresent()) {
      throw new CrudException("Unable to identify edge: " + edge.toString(), Response.Status.BAD_REQUEST);
    }
    String url = baseRelationshipUrl + "/" + edge.getId().get() + "?transactionId=" + txId;
    OperationResult getResult = client.put(url, edge.toJson(champGson), createHeader(), MediaType.APPLICATION_JSON_TYPE,
        MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == Response.Status.OK.getStatusCode()) {
      return Edge.fromJson(getResult.getResult());
    } else {
      // We didn't create an edge with the supplied type, so just throw an
      // exception.
      throw new CrudException("Failed to update edge: " + getResult.getFailureCause(),
          Response.Status.fromStatusCode(getResult.getResultCode()));
    }
  }

  @Override
  public void deleteEdge(String id, String type, String txId) throws CrudException {
    String url = baseRelationshipUrl + "/" + id + "?transactionId=" + txId;
    OperationResult getResult = client.delete(url, createHeader(), MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() != 200) {
      // We didn't find an edge with the supplied type, so just throw an
      // exception.
      throw createErrorException(getResult, javax.ws.rs.core.Response.Status.NOT_FOUND, "No edge with id " + id + " found in graph");
    }
  }

  @Override
  public Edge getEdge(String id, String type, String txId) throws CrudException {
    String url = baseRelationshipUrl + "/" + id + "?transactionId=" + txId;
    OperationResult getResult = client.get(url, createHeader(), MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == 200) {
      Edge edge = Edge.fromJson(getResult.getResult());

      if (!edge.getType().equalsIgnoreCase(type)) {
        // We didn't find an edge with the supplied type, so just throw an
        // exception.
        throw new CrudException("No edge with id " + id + " and type " + type + " found in graph",
            javax.ws.rs.core.Response.Status.NOT_FOUND);
      }
      return edge;
    } else {
      // We didn't find an edge with the supplied id, so just throw an
      // exception.
      throw createErrorException(getResult, javax.ws.rs.core.Response.Status.NOT_FOUND, "No edge with id " + id + " found in graph");
    }
  }

  public Vertex getVertex(String id, String type, String version, String txId) throws CrudException {
    String url = baseObjectUrl + "/" + id + "?transactionId=" + txId;
    OperationResult getResult = client.get(url, createHeader(), MediaType.APPLICATION_JSON_TYPE);

    if (getResult.getResultCode() == 200) {
      Vertex vert = Vertex.fromJson(getResult.getResult(), version);

      if (!vert.getType().equalsIgnoreCase(type)) {
        // We didn't find a vertex with the supplied type, so just throw an
        // exception.
        throw new CrudException("No vertex with id " + id + " and type " + type + " found in graph",
            javax.ws.rs.core.Response.Status.NOT_FOUND);
      }
      return vert;
    } else {
      // We didn't find a vertex with the supplied id, so just throw an
      // exception.
      throw createErrorException(getResult, javax.ws.rs.core.Response.Status.NOT_FOUND, "No vertex with id " + id + " found in graph");
    }
  }

  // https://stackoverflow.com/questions/26942330/convert-mapstring-string-to-listnamevaluepair-is-this-the-most-efficient
  private List<NameValuePair> convertToNameValuePair(Map<String, ? super String> pairs) {
    List<NameValuePair> nvpList = new ArrayList<>(pairs.size());

    pairs.forEach((key, value) -> nvpList.add(new BasicNameValuePair(key, value.toString())));

    return nvpList;
  }

  // https://stackoverflow.com/questions/26942330/convert-mapstring-string-to-listnamevaluepair-is-this-the-most-efficient
  private List<NameValuePair> convertToNameValuePair(String k, HashSet<String> values) {
    List<NameValuePair> nvpList = new ArrayList<>(values.size());

    values.forEach((v) -> nvpList.add(new BasicNameValuePair(k, v)));

    return nvpList;
  }

  private Map<String, List<String>> createHeader() {
    Map<String, List<String>> headers = new HashMap<>();
    headers.put(HEADER_FROM_APP, Arrays.asList(FROM_APP_NAME));
    headers.put(HEADER_TRANS_ID, Arrays.asList(MDC.get(MdcContext.MDC_REQUEST_ID)));
    return headers;
  }

  private CrudException createErrorException(OperationResult result, javax.ws.rs.core.Response.Status defaultErrorCode , String defaultErrorMsg)
  {
      CrudException ce = null;
      if(result != null)
          ce = new CrudException(result.getFailureCause(), Response.Status.fromStatusCode(result.getResultCode()));
      else
          ce = new CrudException(defaultErrorMsg, defaultErrorCode);
      return ce;
  }

}
