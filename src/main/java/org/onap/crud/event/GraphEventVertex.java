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
package org.onap.crud.event;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import org.onap.crud.entity.Vertex;
import org.onap.crud.exception.CrudException;

import java.util.Map;
import javax.ws.rs.core.Response.Status;

/**
 * This class provides a generic representation of a Vertex as provided by the
 * graph data store.
 */
public class GraphEventVertex {

  /**
   * The unique identifier used to identify this vertex in the graph data
   * store.
   */
  @SerializedName("key")
  private String id;

  @SerializedName("schema-version")
  private String modelVersion;

  /**
   * Type label assigned to this vertex.
   */
  private String type;

  /**
   * Map of all of the properties assigned to this vertex.
   */
  private JsonElement properties;

  /**
   * Marshaller/unmarshaller for converting to/from JSON.
   */
  private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  public GraphEventVertex(String id, String modelVersion, String type, JsonElement properties) {
    this.id = id;
    this.modelVersion = modelVersion;
    this.type = type;
    this.properties = properties;
  }

  public GraphEventVertex() {

  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }


  public JsonElement getProperties() {
    return properties;
  }

  public void setProperties(JsonElement properties) {
    this.properties = properties;
  }

  public String getModelVersion() {
    return modelVersion;
  }

  public void setModelVersion(String modelVersion) {
    this.modelVersion = modelVersion;
  }

  /**
   * Unmarshalls this Vertex object into a JSON string.
   *
   * @return - A JSON format string representation of this Vertex.
   */
  public String toJson() {
    return gson.toJson(this);
  }

  /**
   * Marshalls the provided JSON string into a Vertex object.
   *
   * @param json - The JSON string to produce the Vertex from.
   * @return - A Vertex object.
   * @throws SpikeException
   */
  public static GraphEventVertex fromJson(String json) throws CrudException {

    try {

      // Make sure that we were actually provided a non-empty string
      // before we
      // go any further.
      if (json == null || json.isEmpty()) {
        throw new CrudException("Empty or null JSON string.", Status.BAD_REQUEST);
      }

      // Marshall the string into a Vertex object.
      return gson.fromJson(json, GraphEventVertex.class);

    } catch (Exception ex) {
      throw new CrudException("Unable to parse JSON string: ", Status.BAD_REQUEST);
    }
  }

  @Override
  public String toString() {

    return toJson();
  }

  public static GraphEventVertex fromVertex(Vertex vertex, String modelVersion) {

    java.lang.reflect.Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
    JsonObject props = gson.toJsonTree(vertex.getProperties(), mapType).getAsJsonObject();
    GraphEventVertex graphEventVertex = new GraphEventVertex(vertex.getId().orElse(""),
        modelVersion, vertex.getType(), props);
    return graphEventVertex;

  }

  public Vertex toVertex() {
    Vertex.Builder builder = new Vertex.Builder(this.getType()).id(this.getId());

    if (this.getProperties() != null) {
      java.lang.reflect.Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
      Map<String, Object> propertiesMap = gson.fromJson(this.getProperties(), mapType);
      for (String key : propertiesMap.keySet()) {
        builder.property(key, propertiesMap.get(key));
      }
    }

    return builder.build();

  }


}
