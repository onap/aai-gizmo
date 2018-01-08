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
package org.onap.crud.util;

import org.onap.aai.db.props.AAIProperties;
import org.onap.aaiutils.oxm.OxmModelLoader;
import org.onap.crud.exception.CrudException;
import org.onap.schema.RelationshipSchemaLoader;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

public class CrudServiceUtil {

  private static Gson gson = new Gson();
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

  public static void loadModels() throws CrudException {
    // load the schemas
    try {
      OxmModelLoader.loadModels();
    } catch (Exception e) {
      throw new CrudException(e);
    }
    RelationshipSchemaLoader.loadModels();
  }
  
  public static JsonElement mergeHeaderInFoToPayload(JsonElement propertiesFromRequest,  HttpHeaders headers, boolean isAdd) {
    if(!headers.getRequestHeaders().containsKey("X-FromAppId"))  
        return propertiesFromRequest;
    
    String sourceOfTruth = headers.getRequestHeaders().getFirst("X-FromAppId");  
    Set<Map.Entry<String, JsonElement>> properties = new HashSet<Map.Entry<String, JsonElement>>();
    properties.addAll(propertiesFromRequest.getAsJsonObject().entrySet());
    
    Set<String> propertyKeys = new HashSet<String>();
    for(Map.Entry<String, JsonElement> property : properties) {
      propertyKeys.add(property.getKey());
    }
    
    if(!propertyKeys.contains(AAIProperties.LAST_MOD_SOURCE_OF_TRUTH)) {
        properties.add(new AbstractMap.SimpleEntry<String, JsonElement>(AAIProperties.LAST_MOD_SOURCE_OF_TRUTH,
            (JsonElement)(new JsonPrimitive(sourceOfTruth))));
    }
   
    if(isAdd && !propertyKeys.contains(AAIProperties.SOURCE_OF_TRUTH)) {
        properties.add(new AbstractMap.SimpleEntry<String, JsonElement>(AAIProperties.SOURCE_OF_TRUTH,
            (JsonElement)(new JsonPrimitive(sourceOfTruth))));
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
  
}
