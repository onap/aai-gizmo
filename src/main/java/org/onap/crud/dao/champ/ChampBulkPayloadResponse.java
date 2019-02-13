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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onap.crud.entity.Edge;
import org.onap.crud.entity.Vertex;
import org.onap.crud.exception.CrudException;
import org.onap.schema.OxmModelValidator;
import org.onap.schema.RelationshipSchemaValidator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ChampBulkPayloadResponse {

  private HashMap<String, Vertex> vertices = new HashMap<>();
  private HashMap<String, Edge> edges = new HashMap<>();


  @Expose
  @SerializedName(value = "objects")
  private List<JsonElement> objects = new ArrayList<JsonElement>();

  @Expose
  @SerializedName(value = "relationships")
  private List<JsonElement> relationships = new ArrayList<JsonElement>();



  private static final Gson gson = new GsonBuilder().disableHtmlEscaping().excludeFieldsWithoutExposeAnnotation()
          .create();


  public String toJson() {
    return  gson.toJson(this);
  }

  public static ChampBulkPayloadResponse fromJson(String payload) {
    ChampBulkPayloadResponse response =  gson.fromJson(payload, ChampBulkPayloadResponse.class);
    return response;
  }



  public void populateChampData(String version) throws CrudException {

    for (JsonElement object : this.getObjects()) {

      JsonObject champObject = object.getAsJsonObject();
      String itemKey = champObject.get("label").getAsString();
      JsonObject vertexObject = champObject.get("vertex").getAsJsonObject();
      Vertex vertex =  OxmModelValidator.validateOutgoingPayload(version, buildVertex(vertexObject));
      this.getVertices().put(itemKey, vertex);
    }

    for (JsonElement rel : this.getRelationships()) {

      JsonObject champRelationship = rel.getAsJsonObject();
      String itemKey = champRelationship.get("label").getAsString();
      JsonObject relObject = champRelationship.get("edge").getAsJsonObject();
      Edge edge = RelationshipSchemaValidator.validateOutgoingPayload(version, buildEdge(relObject));
      this.getEdges().put(itemKey, edge);

    }


  }


  private Edge buildEdge(JsonObject obj) {
    JsonObject relKeyObject = obj.get("key").getAsJsonObject();

    String relType = obj.get("type").getAsString();
    String relKey = relKeyObject.get("value").getAsString();

    Vertex source = buildVertex(obj.get("source").getAsJsonObject());
    Vertex target = buildVertex(obj.get("target").getAsJsonObject());

    Edge.Builder edgeBuilder = new Edge.Builder(relType).id(relKey).source(source)
            .target(target);

    if (obj.has("properties")) {
      JsonObject propsObject = obj.get("properties").getAsJsonObject();
      List<Map.Entry<String, JsonElement>> entries = new ArrayList<Map.Entry<String, JsonElement>>(
              propsObject.getAsJsonObject().entrySet());

      for (Map.Entry<String, JsonElement> entry :entries) {
        edgeBuilder.property(entry.getKey(), entry.getValue().getAsString());
      }

    }

    return edgeBuilder.build();
  }

  private Vertex buildVertex(JsonObject obj) {
    JsonObject vertexKeyObject = obj.get("key").getAsJsonObject();

    String vertexType = obj.get("type").getAsString();
    String vertexKey = vertexKeyObject.get("value").getAsString();
    Vertex.Builder vertexBuilder = new Vertex.Builder(vertexType).id(vertexKey);

    if (obj.has("properties")) {
      JsonObject propsObject = obj.get("properties").getAsJsonObject();
      List<Map.Entry<String, JsonElement>> entries = new ArrayList<Map.Entry<String, JsonElement>>(
              propsObject.getAsJsonObject().entrySet());

      for (Map.Entry<String, JsonElement> entry :entries) {
        vertexBuilder.property(entry.getKey(), entry.getValue().getAsString());
      }

    }

    return vertexBuilder.build();
  }

  public HashMap<String, Edge> getEdges() {
    return edges;
  }

  public void setEdges(HashMap<String, Edge> edges) {
    this.edges = edges;
  }

  public List<JsonElement> getObjects() {
    return objects;
  }

  public void setObjects(List<JsonElement> objects) {
    this.objects = objects;
  }

  public HashMap<String, Vertex> getVertices() {
    return vertices;
  }

  public List<JsonElement> getRelationships() {
    return relationships;
  }




}
