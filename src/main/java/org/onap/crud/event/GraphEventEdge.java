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
package org.onap.crud.event;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import org.onap.crud.entity.Edge;
import org.onap.crud.entity.Vertex;
import org.onap.crud.exception.CrudException;

import java.util.Map;
import javax.ws.rs.core.Response.Status;

/**
 * This class provides a generic representation of an Edge as provided by the
 * graph data store.
 */
public class GraphEventEdge {

  /**
   * The unique identifier used to identify this edge in the graph data store.
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
   * Source vertex for our edge.
   */
  private GraphEventVertex source;

  /**
   * Target vertex for our edge.
   */
  private GraphEventVertex target;

  /**
   * Map of all of the properties assigned to this vertex.
   */
  private JsonElement properties;

  /**
   * Marshaller/unmarshaller for converting to/from JSON.
   */
  private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  public GraphEventEdge(String id, String modelVersion, String type, GraphEventVertex source,
                        GraphEventVertex target, JsonElement properties) {
    this.id = id;
    this.modelVersion = modelVersion;
    this.type = type;
    this.source = source;
    this.target = target;
    this.properties = properties;
  }

  public GraphEventEdge() {

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

  public GraphEventVertex getSource() {
    return source;
  }

  public void setSource(GraphEventVertex source) {
    this.source = source;
  }

  public GraphEventVertex getTarget() {
    return target;
  }

  public void setTarget(GraphEventVertex target) {
    this.target = target;
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
   * Unmarshalls this Edge object into a JSON string.
   *
   * @return - A JSON format string representation of this Edge.
   */
  public String toJson() {
    return gson.toJson(this);
  }

  /**
   * Marshalls the provided JSON string into a Edge object.
   *
   * @param json - The JSON string to produce the Edge from.
   * @return - A Edge object.
   * @throws SpikeException
   */
  public static GraphEventEdge fromJson(String json) throws CrudException {

    try {

      // Make sure that we were actually provided a non-empty string
      // before we
      // go any further.
      if (json == null || json.isEmpty()) {
        throw new CrudException("Unable to parse JSON string: ", Status.BAD_REQUEST);
      }

      // Marshall the string into an Edge object.
      return gson.fromJson(json, GraphEventEdge.class);

    } catch (Exception ex) {
      throw new CrudException("Unable to parse JSON string: ", Status.BAD_REQUEST);
    }
  }

  public static GraphEventEdge fromEdge(Edge edge, String modelVersion) {

    java.lang.reflect.Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
    JsonObject props = gson.toJsonTree(edge.getProperties(), mapType).getAsJsonObject();

    GraphEventEdge graphEventEdge = new GraphEventEdge(edge.getId().orElse(""), modelVersion,
        edge.getType(), new GraphEventVertex(edge.getSource().getId().orElse(""), null,
        edge.getSource().getType(), null), new GraphEventVertex(edge.getTarget().getId().orElse(""),
        null, edge.getTarget().getType(), null), props);

    return graphEventEdge;

  }

  public Edge toEdge() {
    Edge.Builder builder = new Edge.Builder(this.getType()).id(this.getId());
    if (this.getSource() != null) {
      builder.source(new Vertex.Builder(this.getSource().getType()).id(this.getSource().getId())
          .build());
    }
    if (this.getTarget() != null) {
      builder.target(new Vertex.Builder(this.getTarget().getType()).id(this.getTarget().getId())
          .build());
    }

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
