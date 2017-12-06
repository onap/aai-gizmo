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
import com.google.gson.annotations.SerializedName;

import org.onap.crud.exception.CrudException;

import javax.ws.rs.core.Response.Status;

public class GraphEvent {

  public enum GraphEventOperation {
    CREATE, UPDATE, DELETE
  }

  public enum GraphEventResult {
    SUCCESS, FAILURE
  }

  private GraphEventOperation operation;

  @SerializedName("transaction-id")
  private String transactionId;

  private long timestamp;

  private GraphEventVertex vertex;

  private GraphEventEdge edge;

  private GraphEventResult result;

  @SerializedName("error-message")
  private String errorMessage;

  private Status httpErrorStatus;

  /**
   * Marshaller/unmarshaller for converting to/from JSON.
   */
  private static final Gson gson = new GsonBuilder().disableHtmlEscaping()
      .setPrettyPrinting().create();

  public static Builder builder(GraphEventOperation operation) {
    return new Builder(operation);
  }

  public GraphEventOperation getOperation() {
    return operation;
  }

  public String getTransactionId() {
    return transactionId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public GraphEventVertex getVertex() {
    return vertex;
  }

  public GraphEventEdge getEdge() {
    return edge;
  }

  public GraphEventResult getResult() {
    return result;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setResult(GraphEventResult result) {
    this.result = result;
  }


  public Status getHttpErrorStatus() {
    return httpErrorStatus;
  }

  public void setHttpErrorStatus(Status httpErrorStatus) {
    this.httpErrorStatus = httpErrorStatus;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public void setVertex(GraphEventVertex vertex) {
    this.vertex = vertex;
  }

  public void setEdge(GraphEventEdge edge) {
    this.edge = edge;
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
  public static GraphEvent fromJson(String json) throws CrudException {

    try {

      // Make sure that we were actually provided a non-empty string
      // before we
      // go any further.
      if (json == null || json.isEmpty()) {
        throw new CrudException("Empty or null JSON string.", Status.BAD_REQUEST);
      }

      // Marshall the string into a Vertex object.
      return gson.fromJson(json, GraphEvent.class);

    } catch (Exception ex) {
      throw new CrudException("Unable to parse JSON string: "+json, Status.BAD_REQUEST);
    }
  }

  @Override
  public String toString() {

    return toJson();
  }

  public String getObjectKey() {
    if (this.getVertex() != null) {
      return this.getVertex().getId();
    } else if (this.getEdge() != null) {
      return this.getEdge().getId();
    }

    return null;

  }

  public String getObjectType() {
    if (this.getVertex() != null) {
      return "vertex->" + this.getVertex().getType();
    } else if (this.getEdge() != null) {
      return "edge->" + this.getEdge().getType();
    }

    return null;

  }

  public static class Builder {

    GraphEvent event = null;

    public Builder(GraphEventOperation operation) {
      event = new GraphEvent();
      event.operation = operation;
    }

    public Builder vertex(GraphEventVertex vertex) {
      event.vertex = vertex;
      return this;
    }

    public Builder edge(GraphEventEdge edge) {
      event.edge = edge;
      return this;
    }

    public Builder result(GraphEventResult result) {
      event.result = result;
      return this;
    }

    public Builder errorMessage(String errorMessage) {
      event.errorMessage = errorMessage;
      return this;
    }

    public Builder httpErrorStatus(Status httpErrorStatus) {
      event.httpErrorStatus = httpErrorStatus;
      return this;
    }

    public GraphEvent build() {

      event.timestamp = System.currentTimeMillis();
      event.transactionId = java.util.UUID.randomUUID().toString();

      return event;
    }
  }

}
