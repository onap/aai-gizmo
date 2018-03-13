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
package org.onap.crud.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response.Status;

import org.onap.crud.entity.Edge;
import org.onap.crud.entity.Vertex;
import org.onap.crud.exception.CrudException;
import org.onap.crud.service.BulkPayload;
import org.onap.crud.service.EdgePayload;
import org.onap.crud.service.VertexPayload;
import org.onap.schema.RelationshipSchemaLoader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class CrudResponseBuilder {

  private static final Gson gson = new GsonBuilder().create();

  public static final String SOURCE = "source";
  public static final String TARGET = "target";
  public static final String URL_BASE = "services/inventory/";

  public static String buildUpsertBulkResponse(HashMap<String, Vertex> vertices, HashMap<String, Edge> edges,
      String version, BulkPayload incomingPayload) throws CrudException {

    for (JsonElement e : incomingPayload.getObjects()) {
      List<Map.Entry<String, JsonElement>> entries = new ArrayList<Map.Entry<String, JsonElement>>(
          e.getAsJsonObject().entrySet());

      Map.Entry<String, JsonElement> item = entries.get(1);

      Vertex responseVertex = vertices.get(item.getKey());
      if (responseVertex != null) {
        JsonObject v = gson.fromJson(buildUpsertVertexResponse(responseVertex, version), JsonObject.class);
        item.setValue(v);
      } else {
        item.setValue(gson.fromJson("{}", JsonObject.class));
      }

    }
    for (JsonElement e : incomingPayload.getRelationships()) {
      List<Map.Entry<String, JsonElement>> entries = new ArrayList<Map.Entry<String, JsonElement>>(
          e.getAsJsonObject().entrySet());

      Map.Entry<String, JsonElement> item = entries.get(1);

      Edge responseEdge = edges.get(item.getKey());
      if (responseEdge != null) {
        JsonObject v = gson.fromJson(buildUpsertEdgeResponse(responseEdge, version), JsonObject.class);
        item.setValue(v);
      } else {
        item.setValue(gson.fromJson("{}", JsonObject.class));
      }

    }
    return incomingPayload.toJson();
  }

  public static String buildUpsertVertexResponse(Vertex vertex, String version) throws CrudException {
    VertexPayload payload = new VertexPayload();
    payload.setId(vertex.getId().get());
    payload.setType(vertex.getType());
    payload.setUrl(URL_BASE + version + "/" + vertex.getType() + "/" + vertex.getId().get());
    JsonObject props = new JsonObject();
    for (String key : vertex.getProperties().keySet()) {
      addJsonProperperty(props, key, vertex.getProperties().get(key));
    }
    payload.setProperties(props);
    return payload.toJson();
  }

  public static String buildUpsertEdgeResponse(Edge edge, String version) throws CrudException {
    return buildGetEdgeResponse(edge, version);
  }

  public static String buildGetVertexResponse(Vertex vertex, List<Edge> edges, String version) throws CrudException {
    VertexPayload vertexPayload = new VertexPayload();
    vertexPayload.setId(vertex.getId().get());
    vertexPayload.setType(vertex.getType());
    vertexPayload.setUrl(URL_BASE + version + "/" + vertex.getType() + "/" + vertex.getId().get());
    JsonObject props = new JsonObject();
    for (String key : vertex.getProperties().keySet()) {
      addJsonProperperty(props, key, vertex.getProperties().get(key));
    }
    vertexPayload.setProperties(props);
    List<EdgePayload> inEdges = new ArrayList<EdgePayload>();
    List<EdgePayload> outEdges = new ArrayList<EdgePayload>();
    for (Edge e : edges) {
      if (e.getTarget().getId().get().equals(vertex.getId().get())) {
        EdgePayload inEdge = new EdgePayload();
        inEdge.setId(e.getId().get());
        inEdge.setType(e.getType());
        inEdge.setUrl(URL_BASE + "relationships/" + RelationshipSchemaLoader.getLatestSchemaVersion() + "/"
            + e.getType() + "/" + e.getId().get());
        inEdge.setSource(URL_BASE + version + "/" + e.getSource().getType() + "/" + e.getSource().getId().get());

        inEdges.add(inEdge);
      } else if (e.getSource().getId().get().equals(vertex.getId().get())) {
        EdgePayload outEdge = new EdgePayload();
        outEdge.setId(e.getId().get());
        outEdge.setType(e.getType());
        outEdge.setUrl(URL_BASE + "relationships/" + RelationshipSchemaLoader.getLatestSchemaVersion() + "/"
            + e.getType() + "/" + e.getId().get());
        outEdge.setTarget(URL_BASE + version + "/" + e.getTarget().getType() + "/" + e.getTarget().getId().get());
        outEdges.add(outEdge);
      }
    }

    vertexPayload.setIn(inEdges);
    vertexPayload.setOut(outEdges);

    return vertexPayload.toJson();
  }

  public static String buildGetVerticesResponse(List<Vertex> items, String version) throws CrudException {

    JsonArray arry = new JsonArray();
    for (Vertex v : items) {
      JsonObject item = new JsonObject();
      item.addProperty("id", v.getId().get());
      item.addProperty("type", v.getType());
      item.addProperty("url", "services/inventory/" + version + "/" + v.getType() + "/" + v.getId().get());
      if (!v.getProperties().isEmpty()) {
        JsonObject propertiesObject = new JsonObject();
        for (String key : v.getProperties().keySet()) {
          propertiesObject.addProperty(key, v.getProperties().get(key).toString());
        }
        item.add("properties", propertiesObject);
      }

      arry.add(item);
    }

    return gson.toJson(arry);
  }

  public static String buildGetEdgeResponse(Edge edge, String version) throws CrudException {

    EdgePayload payload = new EdgePayload();
    payload.setId(edge.getId().get());
    payload.setType(edge.getType());
    payload.setUrl(URL_BASE + "relationships/" + version + "/" + edge.getType() + "/" + edge.getId().get());
    payload.setSource(URL_BASE + version + "/" + edge.getSource().getType() + "/" + edge.getSource().getId().get());
    payload.setTarget(URL_BASE + version + "/" + edge.getTarget().getType() + "/" + edge.getTarget().getId().get());

    JsonObject props = new JsonObject();
    for (String key : edge.getProperties().keySet()) {
      addJsonProperperty(props, key, edge.getProperties().get(key));
    }
    payload.setProperties(props);
    return payload.toJson();
  }

  public static String buildGetEdgesResponse(List<Edge> items, String version) throws CrudException {

    JsonArray arry = new JsonArray();
    for (Edge e : items) {
      JsonObject item = new JsonObject();
      item.addProperty("id", e.getId().get());
      item.addProperty("type", e.getType());
      item.addProperty("url", URL_BASE + "relationships/" + version + "/" + e.getType() + "/" + e.getId().get());
      item.addProperty(SOURCE,
          "services/inventory/" + version + "/" + e.getSource().getType() + "/" + e.getSource().getId().get());
      item.addProperty(TARGET,
          "services/inventory/" + version + "/" + e.getTarget().getType() + "/" + e.getTarget().getId().get());
      arry.add(item);
    }

    return gson.toJson(arry);
  }

  private static void addJsonProperperty(JsonObject jsonObj, String key, Object value) {
    if (value instanceof Integer) {
      jsonObj.addProperty(key, (Integer) value);
    } else if (value instanceof Boolean) {
      jsonObj.addProperty(key, (Boolean) value);
    } else if (value instanceof Double) {
      jsonObj.addProperty(key, (Double) value);
    } else if (value instanceof String) {
      jsonObj.addProperty(key, (String) value);
    } else {
      jsonObj.addProperty(key, value.toString());
    }
  }

}
