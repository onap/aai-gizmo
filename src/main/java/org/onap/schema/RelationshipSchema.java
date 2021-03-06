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


import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response.Status;
import org.codehaus.jackson.map.ObjectMapper;
import org.onap.aai.edges.EdgeRule;
import org.onap.crud.exception.CrudException;
import com.google.common.collect.Multimap;


public class RelationshipSchema {

  public static final String SCHEMA_RELATIONSHIP_TYPE = "label";

  private Map<String, Map<String, Class<?>>> relations = new HashMap<>();
  /**
   * Hashmap of valid relationship types along with properties.
   */
  private Map<String, Map<String, Class<?>>> relationTypes  = new HashMap<>();

  // A map storing the list of valid edge types for a source/target pair
  private Map<String, Set<String>> edgeTypesForNodePair = new HashMap<>();
  

  @SuppressWarnings("unchecked")
  public RelationshipSchema(Multimap<String, EdgeRule> rules, String props) throws CrudException, IOException {
    HashMap<String, String> properties = new ObjectMapper().readValue(props, HashMap.class);

    // hold the true values of the edge rules by key - convert to java 8
    for (EdgeRule rule : rules.values()) {
      
      String nodePairKey = buildNodePairKey(rule.getFrom(), rule.getTo());
      if (edgeTypesForNodePair.get(nodePairKey) == null) {
        Set<String> typeSet = new HashSet<String>();
        typeSet.add(rule.getLabel());
        edgeTypesForNodePair.put(nodePairKey, typeSet);
      }
      else {
        edgeTypesForNodePair.get(nodePairKey).add(rule.getLabel());
      }
    }

    Map<String, Class<?>> edgeProps = properties.entrySet().stream().collect(Collectors.toMap(p -> p.getKey(), p -> {
      try {
        return resolveClass(p.getValue());
      } catch (CrudException | ClassNotFoundException e) {
        e.printStackTrace();
      }
      return null;
    }));

    rules.entries ().forEach ( (kv) -> {
      relationTypes.put(kv.getValue ().getLabel (), edgeProps);
      relations.put (buildRelation ( kv.getValue ().getFrom (), kv.getValue ().getTo (), kv.getValue ().getLabel ()), edgeProps);
    });
  }

  public Map<String, Class<?>> lookupRelation(String key) {
    return this.relations.get(key);
  }

  public Map<String, Class<?>> lookupRelationType(String type) {
    return this.relationTypes.get(type);
  }

  public boolean isValidType(String type) {
    return relationTypes.containsKey(type);
  }

  public Set<String> getValidRelationTypes(String source, String target) {
    Set<String> typeList = edgeTypesForNodePair.get(buildNodePairKey(source, target));

    if (typeList == null) {
      return new HashSet<String>();
    }
    
    return typeList;
  }
  
  private String buildRelation(String source, String target, String relation) {
    return source + ":" + target + ":" + relation;
  }
  
  private String buildNodePairKey(String source, String target) {
    return source + ":" + target;
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