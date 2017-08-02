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
package org.openecomp.schema;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.openecomp.crud.exception.CrudException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Response.Status;

public class RelationshipSchema {
  private static final Gson gson = new GsonBuilder().create();

  public static final String SCHEMA_SOURCE_NODE_TYPE = "source-node-type";
  public static final String SCHEMA_TARGET_NODE_TYPE = "target-node-type";
  public static final String SCHEMA_RELATIONSHIP_TYPE = "relationship-type";
  public static final String SCHEMA_RELATIONSHIP_TYPES_ARRAY = "relationship-types";
  public static final String SCHEMA_RELATIONSHIP_PROPERTIES = "properties";
  public static final String SCHEMA_RELATIONS_ARRAY = "relations";

  /**
   * key = source-node-type:target-node-type:relationship-type value = map of properties with name
   * and type . Like propertyName:PropertyType
   */
  private HashMap<String, HashMap<String, Class<?>>> relations
      = new HashMap<String, HashMap<String, Class<?>>>();
  /**
   * Hashmap of valid relationship types alongwith properrties.
   */
  private HashMap<String, HashMap<String, Class<?>>> relationTypes
      = new HashMap<String, HashMap<String, Class<?>>>();


  public RelationshipSchema(String json) throws CrudException {

    JsonParser parser = new JsonParser();
    try {
      JsonObject root = parser.parse(json).getAsJsonObject();
      JsonArray relationshipTypesArray = root.getAsJsonArray(SCHEMA_RELATIONSHIP_TYPES_ARRAY);
      JsonArray relationsArray = root.getAsJsonArray(SCHEMA_RELATIONS_ARRAY);

      //First load all the relationship-types
      for (JsonElement item : relationshipTypesArray) {
        JsonObject obj = item.getAsJsonObject();
        String type = obj.get(SCHEMA_RELATIONSHIP_TYPE).getAsString();


        HashMap<String, Class<?>> props = new HashMap<String, Class<?>>();
        Set<Map.Entry<String, JsonElement>> entries = obj.get(SCHEMA_RELATIONSHIP_PROPERTIES)
            .getAsJsonObject().entrySet();

        for (Map.Entry<String, JsonElement> entry : entries) {
          props.put(entry.getKey(), resolveClass(entry.getValue().getAsString()));

        }
        relationTypes.put(type, props);

      }

      for (JsonElement item : relationsArray) {
        JsonObject obj = item.getAsJsonObject();
        // Parse the Source/Taget nodeTypes

        String relationType = obj.get(SCHEMA_RELATIONSHIP_TYPE).getAsString();
        String key = obj.get(SCHEMA_SOURCE_NODE_TYPE).getAsString() + ":"
            + obj.get(SCHEMA_TARGET_NODE_TYPE).getAsString() + ":" + relationType;


        if (!relationTypes.containsKey(relationType)) {
          throw new CrudException(SCHEMA_RELATIONSHIP_TYPE + ": " + relationType + " not found",
              Status.BAD_REQUEST);
        }

        relations.put(key, relationTypes.get(relationType));
      }
    } catch (Exception e) {
      throw new CrudException(e.getMessage(), Status.BAD_REQUEST);
    }

  }


  public HashMap<String, Class<?>> lookupRelation(String key) {
    return this.relations.get(key);
  }

  public HashMap<String, Class<?>> lookupRelationType(String type) {
    return this.relationTypes.get(type);
  }

  public boolean isValidType(String type) {
    return relationTypes.containsKey(type);
  }

  private Class<?> resolveClass(String type) throws CrudException, ClassNotFoundException {
    Class<?> clazz = Class.forName(type);
    validateClassTypes(clazz);
    return clazz;
  }

  private void validateClassTypes(Class<?> clazz) throws CrudException {
    if (!clazz.isAssignableFrom(Integer.class) && !clazz.isAssignableFrom(Double.class)
        && !clazz.isAssignableFrom(Boolean.class) && !clazz.isAssignableFrom(String.class)) {
      throw new CrudException("", Status.BAD_REQUEST);
    }
  }


}
