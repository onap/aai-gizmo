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
package org.onap.schema;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.Response.Status;

import org.onap.crud.entity.Edge;
import org.onap.crud.entity.Vertex;
import org.onap.crud.exception.CrudException;
import org.onap.crud.service.EdgePayload;
import org.onap.crud.util.CrudServiceUtil;
import org.onap.schema.OxmModelValidator.Metadata;
import org.radeox.util.logging.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

public class RelationshipSchemaValidator {

	public static final String SOURCE_NODE = "source";
	public static final String TARGET_NODE = "target";
	
	final static Pattern urlPattern = Pattern.compile("services/inventory/(.*)/(.*)/(.*)");
	
	public static Map<String, Object> resolveCollectionfilter(String version, String type,Map<String, String> filter)  throws CrudException {
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

  public static Edge validateIncomingAddPayload(String version, String type, Vertex sourceNode, Vertex targetNode, JsonElement properties) throws CrudException {
	EdgePayload payload = new EdgePayload();
	payload.setSource("services/inventory/" + version + "/" + sourceNode.getType() + "/" + sourceNode.getId().get());
	payload.setTarget("services/inventory/" + version + "/" + targetNode.getType() + "/" + targetNode.getId().get());
	payload.setType(type);
	payload.setProperties(properties);
	
	return validateIncomingAddPayload(version, type, payload);
  }

  public static Edge validateIncomingAddPayload(String version, String type, EdgePayload payload)
      throws CrudException {
    RelationshipSchema schema = EdgeRulesLoader.getSchemaForVersion(version);

    try {
      if (payload.getSource() == null || payload.getTarget() == null) {
        throw new CrudException("Source/Target not specified", Status.BAD_REQUEST);
      }

      Matcher sourceMatcher = urlPattern.matcher(payload.getSource());
      Matcher targetMatcher = urlPattern.matcher(payload.getTarget());
      
      if (!sourceMatcher.matches() || !targetMatcher.matches()) {
    	  throw new CrudException("Invalid Source/Target Urls", Status.BAD_REQUEST);
      }
      
      // create key based on source:target:relationshipType
      String sourceNodeType = sourceMatcher.group(2);
      String targetNodeType = targetMatcher.group(2);
      
      String sourceNodeId = sourceMatcher.group(3);
      String targetNodeId = targetMatcher.group(3);
      
      String key = sourceNodeType + ":" + targetNodeType + ":" + type;

      // find the validate the key from the schema
      Map<String, Class<?>> schemaObject = schema.lookupRelation(key);

      if (schemaObject == null) {
        throw new CrudException("Invalid source/target/relationship type: " + key, Status.BAD_REQUEST);
      }

      Edge.Builder modelEdgeBuilder = new Edge.Builder(type);
      
      modelEdgeBuilder.source(new Vertex.Builder(sourceNodeType).id(sourceNodeId).build());
      modelEdgeBuilder.target(new Vertex.Builder(targetNodeType).id(targetNodeId).build());

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
	    	if (payload.getSource() != null) {
	    		Matcher sourceMatcher = urlPattern.matcher(payload.getSource());
	    		
	    		if (!sourceMatcher.matches()) {
	    			throw new CrudException("Invalid Source Urls", Status.BAD_REQUEST);
	    		}
	    		String sourceNodeId = sourceMatcher.group(3);
	    		if (!sourceNodeId.equals(edge.getSource().getId().get())) {
	    			throw new CrudException("Source can't be updated", Status.BAD_REQUEST);
	    		}
	    	}
	    	
	    	if (payload.getTarget() != null) {
	    		Matcher targetMatcher = urlPattern.matcher(payload.getTarget());
	    		 
	    		if (!targetMatcher.matches()) {
	    			throw new CrudException("Invalid Target Urls", Status.BAD_REQUEST);
	    		}
	    		String sourceNodeId = targetMatcher.group(3);
	    		if (!sourceNodeId.equals(edge.getTarget().getId().get())) {
	    			throw new CrudException("Target can't be updated", Status.BAD_REQUEST);
	    		}
	    	}

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
	      
    	  return edge;
      } catch (Exception ex) {
    	  throw new CrudException(ex.getMessage(), Status.BAD_REQUEST);
      }
  }


  public static Edge validateIncomingUpdatePayload(Edge edge, String version, Vertex sourceNode, Vertex targetNode, JsonElement properties) 
		  throws CrudException {
	  EdgePayload payload = new EdgePayload();
	  payload.setSource("services/inventory/" + version + "/" + sourceNode.getType() + "/" + sourceNode.getId().get());
	  payload.setTarget("services/inventory/" + version + "/" + targetNode.getType() + "/" + targetNode.getId().get());
	  payload.setType(edge.getType());
	  payload.setProperties(properties);
	  return validateIncomingUpdatePayload(edge, version, payload);
  }

  public static Edge validateIncomingUpdatePayload(Edge edge, String version, EdgePayload payload)
      throws CrudException {
    RelationshipSchema schema = EdgeRulesLoader.getSchemaForVersion(version);

    try {
    	if (payload.getSource() != null) {
    		Matcher sourceMatcher = urlPattern.matcher(payload.getSource());
    		
    		if (!sourceMatcher.matches()) {
    			throw new CrudException("Invalid Source Urls", Status.BAD_REQUEST);
    		}
    		String sourceNodeId = sourceMatcher.group(3);
    		if (!sourceNodeId.equals(edge.getSource().getId().get())) {
    			throw new CrudException("Source can't be updated", Status.BAD_REQUEST);
    		}
    	}
    	
    	if (payload.getTarget() != null) {
    		Matcher targetMatcher = urlPattern.matcher(payload.getTarget());
    		
    		if (!targetMatcher.matches()) {
    			throw new CrudException("Invalid Target Urls", Status.BAD_REQUEST);
    		}
    		String sourceNodeId = targetMatcher.group(3);
    		if (!sourceNodeId.equals(edge.getTarget().getId().get())) {
    			throw new CrudException("Target can't be updated", Status.BAD_REQUEST);
    		}
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

      Edge.Builder updatedEdgeBuilder = new Edge.Builder(edge.getType()).id(edge.getId().get());
      
      updatedEdgeBuilder.source(new Vertex.Builder(edge.getSource().getType()).id(edge.getSource().getId().get()).build());
      updatedEdgeBuilder.target(new Vertex.Builder(edge.getTarget().getType()).id(edge.getTarget().getId().get()).build());

      validateEdgeProps(updatedEdgeBuilder, payload.getProperties(), schemaObject);

      return updatedEdgeBuilder.build();
    } catch (Exception ex) {
      throw new CrudException(ex.getMessage(), Status.BAD_REQUEST);
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
  
  public static String vertexTypeFromUri(String uri) throws CrudException {

	  Matcher matcher = urlPattern.matcher(uri);
	  
	  if (!matcher.matches()) {
		  throw new CrudException("Invalid Source/Target Urls", Status.BAD_REQUEST);
	  }
	  
	  return matcher.group(2);
  }
}
