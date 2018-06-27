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
package org.onap.schema.validation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Response.Status;
import org.onap.crud.entity.Edge;
import org.onap.crud.entity.Vertex;
import org.onap.crud.exception.CrudException;
import org.onap.crud.parser.EdgePayload;
import org.onap.crud.parser.util.EdgePayloadUtil;
import org.onap.crud.util.CrudServiceUtil;
import org.onap.schema.EdgeRulesLoader;
import org.onap.schema.RelationshipSchema;
import org.onap.schema.validation.OxmModelValidator.Metadata;
import org.radeox.util.logging.Logger;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

public class RelationshipSchemaValidator {

  private static final String SOURCE_LABEL = "Source";
  private static final String TARGET_LABEL = "Target";

  public static Map<String, Object> resolveCollectionfilter(String version, String type, Map<String, String> filter) throws CrudException {
    RelationshipSchema schema = EdgeRulesLoader.getSchemaForVersion(version);
    if (schema == null) {
      throw new CrudException("", Status.NOT_FOUND);
    }

    Map<String, Class<?>> props = schema.lookupRelationType(type);
    Map<String, Object> result = new HashMap<String, Object>();

    for (String key : filter.keySet()) {

      if (props.containsKey(key)) {
        try {
          Object value = CrudServiceUtil.validateFieldType(filter.get(key), props.get(key));
          result.put(key, value);
        } catch (Exception ex) {
          // Skip any exceptions thrown while validating the filter key value
          continue;
        }
      }
    }
    return result;
  }

  public static void validateType(String version, String type) throws CrudException {
    RelationshipSchema schema = EdgeRulesLoader.getSchemaForVersion(version);
    if (!schema.isValidType(type)) {
      throw new CrudException("Invalid " + RelationshipSchema.SCHEMA_RELATIONSHIP_TYPE
          + ": " + type,
          Status.BAD_REQUEST);
    }

  }

  public static Edge validateIncomingAddPayload(String version, String type, EdgePayload payload,
    List<Edge> sourceVertexEdges, List<Edge> targetVertexEdges) throws CrudException {

    //perform standard validation
    Edge edge = validateIncomingAddPayload(version, type, payload);

    // validate payload using multiplicity edge rules
    MultiplicityValidator.validatePayloadMultiplicity(payload, sourceVertexEdges, targetVertexEdges, type, version);

    return edge;
  }

  public static Edge validateIncomingAddPayload(String version, String type, EdgePayload payload)
      throws CrudException {
    RelationshipSchema schema = EdgeRulesLoader.getSchemaForVersion(version);

    try {
      if (payload.getSource() == null || payload.getTarget() == null) {
        throw new CrudException("Source/Target not specified", Status.BAD_REQUEST);
      }

      String key = EdgePayloadUtil.generateEdgeKey(payload.getSource(), payload.getTarget(), type);

      // find the validate the key from the schema
      Map<String, Class<?>> schemaObject = schema.lookupRelation(key);

      if (schemaObject == null) {
        throw new CrudException("Invalid source/target/relationship type: " + key, Status.BAD_REQUEST);
      }

      Edge.Builder modelEdgeBuilder = EdgePayloadUtil.getBuilderFromEdgePayload(payload.getSource(), payload.getTarget(), type);

      // validate it properties
      validateEdgeProps(modelEdgeBuilder, payload.getProperties(), schemaObject);

      return modelEdgeBuilder.build();
    } catch (Exception ex) {
      throw new CrudException(ex.getMessage(), Status.BAD_REQUEST);
    }
  }

  public static Edge validateIncomingPatchPayload(Edge edge, String version, EdgePayload payload)
      throws CrudException {
    RelationshipSchema schema = EdgeRulesLoader.getSchemaForVersion(version);

    try {
      validateEdgeVertexMatchesPayload(edge.getSource(), payload.getSource(), SOURCE_LABEL);
      validateEdgeVertexMatchesPayload(edge.getTarget(), payload.getTarget(), TARGET_LABEL);

      // Remove the timestamp properties from the existing edge, as these should be managed by Champ.
      Map<String,Object> existingProps = edge.getProperties();

      if (existingProps.containsKey(Metadata.CREATED_TS.propertyName())) {
        existingProps.remove(Metadata.CREATED_TS.propertyName());
      }
      if (existingProps.containsKey(Metadata.UPDATED_TS.propertyName())) {
        existingProps.remove(Metadata.UPDATED_TS.propertyName());
      }

      // create key based on source:target:relationshipType
      String key = edge.getSource().getType() + ":" + edge.getTarget().getType()
          + ":" + edge.getType();

      // find the validate the key from the schema
      Map<String, Class<?>> schemaObject = schema.lookupRelation(key);

      if (schemaObject == null) {
        Logger.warn("key :" + key
            + " not found in relationship schema . Skipping the schema validation");
        return edge;
      }

      validateEdgePropertiesFromPayload(edge, payload, schemaObject);

      return edge;
    } catch (Exception ex) {
      throw new CrudException(ex.getMessage(), Status.BAD_REQUEST);
    }
  }

  private static void validateEdgePropertiesFromPayload(Edge edge, EdgePayload payload, Map<String, Class<?>> schemaObject) throws CrudException {
    Set<Map.Entry<String, JsonElement>> entries = payload.getProperties().getAsJsonObject().entrySet();
    for (Map.Entry<String, JsonElement> entry : entries) {

      if (!schemaObject.containsKey(entry.getKey())) {
        throw new CrudException("Invalid property: " + entry.getKey(), Status.BAD_REQUEST);
      } else if (entry.getValue() instanceof JsonNull && edge.getProperties().containsKey(entry.getKey())) {
        edge.getProperties().remove(entry.getKey());
      } else if (!(entry.getValue() instanceof JsonNull)) {
        Object value = CrudServiceUtil.validateFieldType(entry.getValue().getAsString(), schemaObject.get(entry.getKey()));
        edge.getProperties().put(entry.getKey(), value);
      }
    }
  }


  public static Edge validateIncomingUpdatePayload(Edge edge, String version, EdgePayload payload, String type,
            List<Edge> sourceVertexEdges, List<Edge> targetVertexEdges) throws CrudException {

    //perform standard validation
    Edge validatedEdge = validateIncomingUpdatePayload(edge, version, payload);

    // validate payload using multiplicity edge rules
    MultiplicityValidator.validatePayloadMultiplicity(payload, sourceVertexEdges, targetVertexEdges, type, version);

    return validatedEdge;
  }

  public static Edge validateIncomingUpdatePayload(Edge edge, String version, EdgePayload payload)
      throws CrudException {
    RelationshipSchema schema = EdgeRulesLoader.getSchemaForVersion(version);

    try {
      validateEdgeVertexMatchesPayload(edge.getSource(), payload.getSource(), SOURCE_LABEL);
      validateEdgeVertexMatchesPayload(edge.getTarget(), payload.getTarget(), TARGET_LABEL);

      // create key based on source:target:relationshipType
      String key = edge.getSource().getType() + ":" + edge.getTarget().getType()
          + ":" + edge.getType();

      // find the validate the key from the schema
      Map<String, Class<?>> schemaObject = schema.lookupRelation(key);

      if (schemaObject == null) {
        Logger.warn("key :" + key
            + " not found in relationship schema . Skipping the schema validation");
        return edge;
      }

      Edge.Builder updatedEdgeBuilder = EdgePayloadUtil.getBuilderFromEdge(edge);

      validateEdgeProps(updatedEdgeBuilder, payload.getProperties(), schemaObject);

      return updatedEdgeBuilder.build();
    } catch (Exception ex) {
      throw new CrudException(ex.getMessage(), Status.BAD_REQUEST);
    }
  }

  private static void validateEdgeVertexMatchesPayload(Vertex edgeVertex, String payloadVertex, String vertexTypeLabel) throws CrudException {
    if (payloadVertex != null) {
      String sourceNodeId = EdgePayloadUtil.getVertexNodeId(payloadVertex);
      if (!sourceNodeId.equals(edgeVertex.getId().get())) {
        throw new CrudException(vertexTypeLabel + " can't be updated", Status.BAD_REQUEST);
      }
    }
  }

  private static void validateEdgeProps(Edge.Builder builder, JsonElement props, Map<String, Class<?>> schemaObject) throws CrudException {
    Set<Map.Entry<String, JsonElement>> entries = props.getAsJsonObject().entrySet();

    for (Map.Entry<String, JsonElement> entry : entries) {
      if (!schemaObject.containsKey(entry.getKey())) {
        throw new CrudException("Invalid property: " + entry.getKey(), Status.BAD_REQUEST);
      } else {
        Object value = CrudServiceUtil.validateFieldType(entry.getValue().getAsString(),
            schemaObject.get(entry.getKey()));
        builder.property(entry.getKey(), value);
      }
    }
  }

  public static Edge validateOutgoingPayload(String version, Edge edge) throws CrudException {
    Edge.Builder modelEdgeBuilder = new Edge.Builder(edge.getType()).id(edge.getId()
        .get()).source(edge.getSource())
        .target(edge.getTarget());

    RelationshipSchema schema = EdgeRulesLoader.getSchemaForVersion(version);

    String key = edge.getSource().getType() + ":" + edge.getTarget().getType()
        + ":" + edge.getType();
    Map<String, Class<?>> schemaObject = schema.lookupRelation(key);

    if (schemaObject == null || schemaObject.isEmpty()) {
      return edge;
    }

    for (String prop : edge.getProperties().keySet()) {
      if (schemaObject.containsKey(prop)) {
        modelEdgeBuilder.property(prop, edge.getProperties().get(prop));
      }

    }
    return modelEdgeBuilder.build();
  }
}
