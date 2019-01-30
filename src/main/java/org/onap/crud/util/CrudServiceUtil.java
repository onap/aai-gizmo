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
package org.onap.crud.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;
import org.onap.crud.exception.CrudException;
import org.onap.crud.service.EdgePayload;
import org.onap.schema.EdgeRulesLoader;
import org.onap.schema.RelationshipSchema;
import org.onap.schema.RelationshipSchemaValidator;

public class CrudServiceUtil {

  private static Gson gson = new Gson();
  public static final java.lang.String LAST_MOD_SOURCE_OF_TRUTH = "last-mod-source-of-truth";
  public static final java.lang.String SOURCE_OF_TRUTH = "source-of-truth";

  @SuppressWarnings({"unchecked", "rawtypes"})
  public static Object validateFieldType(String value, Class clazz) throws CrudException {
    try {
      if (clazz.isAssignableFrom(Integer.class)) {
        return Integer.parseInt(value);
      } else if (clazz.isAssignableFrom(Long.class)) {
        return Long.parseLong(value);
      } else if (clazz.isAssignableFrom(Float.class)) {
        return Float.parseFloat(value);
      } else if (clazz.isAssignableFrom(Double.class)) {
        return Double.parseDouble(value);
      } else if (clazz.isAssignableFrom(Boolean.class)) {

		// If the value is an IN/OUT direction, this gets seen as a boolean, so
        // check for that first.
        if (value.equals("OUT") || value.equals("IN")) {
          return value;
        }

        if (!value.equals("true") && !value.equals("false")) {
          throw new CrudException("Invalid propertry value: " + value, Status.BAD_REQUEST);
        }
        return Boolean.parseBoolean(value);
      } else {
        return value;
      }
    } catch (Exception e) {
      throw new CrudException("Invalid property value: " + value, Status.BAD_REQUEST);
    }
  }

  /**
   * This method will merge header property from app id in request payload if not already populated
   * @param propertiesFromRequest
   * @param headers
   * @param isAdd
   * @return
   */
    @SuppressWarnings("unchecked")
    public static JsonElement mergeHeaderInFoToPayload(JsonElement propertiesFromRequest, HttpHeaders headers,
            boolean isAdd) {
    String sourceOfTruth = headers.getRequestHeaders().getFirst("X-FromAppId");
    Set<Map.Entry<String, JsonElement>> properties = new HashSet<Map.Entry<String, JsonElement>>();
    properties.addAll(propertiesFromRequest.getAsJsonObject().entrySet());

    Set<String> propertyKeys = new HashSet<String>();
    for(Map.Entry<String, JsonElement> property : properties) {
      propertyKeys.add(property.getKey());
    }

    if(!propertyKeys.contains(LAST_MOD_SOURCE_OF_TRUTH)) {
        properties.add(new AbstractMap.SimpleEntry<String, JsonElement>(LAST_MOD_SOURCE_OF_TRUTH,
            (new JsonPrimitive(sourceOfTruth))));
    }

    if(isAdd && !propertyKeys.contains(SOURCE_OF_TRUTH)) {
        properties.add(new AbstractMap.SimpleEntry<String, JsonElement>(SOURCE_OF_TRUTH,
            (new JsonPrimitive(sourceOfTruth))));
    }

    Object[] propArray = properties.toArray();
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    boolean first=true;
    for(int i=0; i<propArray.length; i++) {

      Map.Entry<String, JsonElement> entry = (Entry<String, JsonElement>) propArray[i];
      if(!first) {
        sb.append(",");
      }
      sb.append("\"").append(entry.getKey()).append("\"").append(":").append(entry.getValue());
      first=false;
    }
    sb.append("}");

    return gson.fromJson(sb.toString(), JsonElement.class);
  }

  public static EntityTag getETagFromHeader(MultivaluedMap<String, String> headers) {
    EntityTag entityTag = null;
    if (headers != null && headers.containsKey(CrudServiceConstants.CRD_HEADER_ETAG)) {
      String value = headers.getFirst(CrudServiceConstants.CRD_HEADER_ETAG);
      entityTag = new EntityTag(value.replace("\"", ""));
    }
    return entityTag;
  }

  public static String determineEdgeType(EdgePayload payload, String version) throws CrudException {
    RelationshipSchema schema = EdgeRulesLoader.getSchemaForVersion(version);

    if (payload.getSource() == null || payload.getTarget() == null) {
      throw new CrudException("Source/Target not specified", Status.BAD_REQUEST);
    }

    Set<String> edgeTypes = schema.getValidRelationTypes(RelationshipSchemaValidator.vertexTypeFromUri(payload.getSource()),
    		RelationshipSchemaValidator.vertexTypeFromUri(payload.getTarget()));

    if (edgeTypes.size() == 0) {
      throw new CrudException("No valid relationship types from " + payload.getSource() + " to " + payload.getTarget(), Status.BAD_REQUEST);
    }

    if (edgeTypes.size() > 1) {
      throw new CrudException("Multiple possible relationship types from " + payload.getSource() + " to " + payload.getTarget(), Status.BAD_REQUEST);
    }

    return edgeTypes.iterator().next();
  }
}
