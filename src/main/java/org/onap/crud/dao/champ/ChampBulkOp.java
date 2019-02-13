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
package org.onap.crud.dao.champ;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ChampBulkOp {
  private static final Gson gson = new GsonBuilder().create();

  private String operation;
  private String id;
  private String type;
  private String label;
  private String source;
  private String target;
  private Map<String, Object> properties;


  public String toJson() {
    return gson.toJson(this);
  }

  public static ChampBulkOp fromJson(String jsonString) {
    return gson.fromJson(jsonString, ChampBulkOp.class);
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

  public Map<String, Object> getProperties() {
    return properties;
  }

  public Object getProperty(String key) {
    return properties.get(key);
  }

  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
  }

  public void setProperty(String key, String value) {
    if (properties == null) {
      properties = new HashMap<String,Object>();
    }

    properties.put(key, value);
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }
}
