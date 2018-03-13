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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.onap.crud.exception.CrudException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response.Status;

public class BulkPayload {
  public enum OperationType {
    CREATE, UPDATE, DELETE
  }

  private List<JsonElement> objects = new ArrayList<JsonElement>();
  private List<JsonElement> relationships = new ArrayList<JsonElement>();

  private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  public String toJson() {
    return gson.toJson(this);
  }

  public static BulkPayload fromJson(String payload) throws CrudException {
    try {
      if (payload == null || payload.isEmpty()) {
        throw new CrudException("Invalid Json Payload", Status.BAD_REQUEST);
      }
      return gson.fromJson(payload, BulkPayload.class);
    } catch (Exception ex) {
      throw new CrudException("Invalid Json Payload", Status.BAD_REQUEST);
    }
  }

  public List<JsonElement> getObjects() {
    return objects;
  }

  public void setObjects(List<JsonElement> objects) {
    this.objects = objects;
  }

  public List<JsonElement> getRelationships() {
    return relationships;
  }

  public void setRelationships(List<JsonElement> relationships) {
    this.relationships = relationships;
  }

  @Override
  public String toString() {
    return "BulkPayload [objects=" + objects + ", relationships=" + relationships + "]";
  }

  public static void main(String[] args) throws Exception {
    BulkPayload p = new BulkPayload();
    JsonObject root = new JsonObject();
    JsonArray vertices = new JsonArray();
    JsonObject v1 = new JsonObject();
    JsonObject v2 = new JsonObject();
    JsonObject prop = new JsonObject();

    prop.addProperty("p1", "value1");
    prop.addProperty("p2", "value2");
    v1.add("v1", prop);
    v2.add("v2", prop);

    vertices.add(v1);
    vertices.add(v2);

    root.add("objects", vertices);

    String s = "{\"objects\":[{\"v1\":{\"p1\":\"value1\",\"p2\":\"value2\"}},{\"v2\":{\"p1\":\"value1\",\"p2\":\"value2\"}}]}";

    p = BulkPayload.fromJson(s);

    List<JsonElement> po = p.getObjects();
    List<String> ids = new ArrayList<String>();
    for (JsonElement e : po) {
      Set<Map.Entry<String, JsonElement>> entries = e.getAsJsonObject().entrySet();

      for (Map.Entry<String, JsonElement> entry : entries) {
        ids.add(entry.getKey());
      }
    }

    System.out.println("root: " + root.toString());
    System.out.println("payload ids: " + ids.toString());

  }

}