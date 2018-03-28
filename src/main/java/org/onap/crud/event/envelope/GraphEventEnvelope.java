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
package org.onap.crud.event.envelope;

import javax.ws.rs.core.Response;
import org.onap.crud.event.GraphEvent;
import org.onap.crud.event.GraphEventEdge;
import org.onap.crud.event.GraphEventVertex;
import org.onap.crud.exception.CrudException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/**
 * Defines the top-level structure of the Gizmo events. Primarily serves as a wrapper for the graph details and includes
 * an event header to describe and identify the event.
 */
public class GraphEventEnvelope {

    private GraphEventHeader header;
    private GraphEvent body;
    private JsonElement policyViolations;

    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    /**
     * Construct the event.
     *
     * @param header the event header to describe the event
     * @param body the body of the event with graph details
     */
    public GraphEventEnvelope(GraphEventHeader header, GraphEvent body) {
        this.header = header;
        this.body = body;
    }

    /**
     * Construct the envelope header from the provided GraphEvent.
     *
     * @param event the graph event for a vertex or edge operation
     */
    public GraphEventEnvelope(GraphEvent event) {
        this.header = new GraphEventHeader.Builder().requestId(event.getTransactionId())
                .validationEntityType(getType(event)).build();
        this.body = event;
    }

    public GraphEvent getBody() {
        return body;
    }

    public GraphEventHeader getHeader() {
        return header;
    }

    public JsonElement getPolicyViolations() {
        return policyViolations != null ? policyViolations : new JsonArray();
    }

    /**
     * Serializes this object into a JSON string representation.
     *
     * @return a JSON format string representation of this object.
     */
    public String toJson() {
        return gson.toJson(this);
    }

    /**
     * Deserializes a JSON string into a GraphEventEnvelope object.
     *
     * @param json the JSON string
     * @return a GraphEventEnvelope object
     * @throws CrudException
     */
    public static GraphEventEnvelope fromJson(String json) throws CrudException {
        try {
            if (json == null || json.isEmpty()) {
                throw new CrudException("Empty or null JSON string.", Response.Status.BAD_REQUEST);
            }
            return gson.fromJson(json, GraphEventEnvelope.class);
        } catch (Exception ex) {
            throw new CrudException("Unable to parse JSON string: " + json, Response.Status.BAD_REQUEST);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.toJson();
    }

    private String getType(GraphEvent event) {
        GraphEventVertex vertex = event.getVertex();
        GraphEventEdge edge = event.getEdge();
        if (vertex != null) {
            return vertex.getType();
        } else if (edge != null) {
            return edge.getType();
        }
        return null;
    }

}
