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
package org.openecomp.crud.entity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Edge {
  private static final Gson gson = new GsonBuilder().create();

  private final Optional<String> id;
  private final String type;
  private final Map<String, Object> properties;
  private final Vertex source;
  private final Vertex target;


  private Edge(Builder builder) {
    this.id = builder.id;
    this.type = builder.type;
    this.source = builder.source;
    this.target = builder.target;
    this.properties = builder.properties;
  }

  public static class Builder {
    private Optional<String> id = Optional.empty();
    private final String type;
    private Vertex source;
    private Vertex target;
    private final Map<String, Object> properties = new HashMap<String, Object>();

    public Builder(String type) {
      if (type == null) {
        throw new IllegalArgumentException("Type cannot be null");
      }

      this.type = type;
    }

    public Builder id(String id) {
      if (id == null) {
        throw new IllegalArgumentException("id cannot be null");
      }

      this.id = Optional.of(id);
      return this;
    }

    public Builder property(String key, Object value) {
      if (key == null || value == null) {
        throw new IllegalArgumentException("Property key/value cannot be null");
      }
      properties.put(key, value);
      return this;
    }

    public Builder source(Vertex source) {
      this.source = source;
      return this;
    }

    public Builder target(Vertex target) {
      this.target = target;
      return this;
    }

    public Edge build() {
      return new Edge(this);
    }
  }


  @Override
  public String toString() {
    return "Edge [id=" + id + ", type=" + type + ", properties=" + properties
        + ", source=" + source + ", target=" + target + "]";
  }

  public String toJson() {
    return gson.toJson(this);
  }

  public Optional<String> getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public Map<String, Object> getProperties() {
    return properties;
  }

  public Vertex getSource() {
    return source;
  }

  public Vertex getTarget() {
    return target;
  }


}
