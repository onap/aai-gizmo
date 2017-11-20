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
package org.onap.crud.dao.champion;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.onap.crud.entity.Vertex;

import java.lang.reflect.Type;

public class ChampionVertexSerializer implements JsonSerializer<Vertex> {
  @Override
  public JsonElement serialize(Vertex vertex, Type type, JsonSerializationContext jsonSerializationContext) {
    final JsonObject vertexObj = new JsonObject();
    if (vertex.getId().isPresent()) {
      vertexObj.add("key", jsonSerializationContext.serialize(vertex.getId().get()));
    }
    vertexObj.add("type", jsonSerializationContext.serialize(vertex.getType()));
    vertexObj.add("properties", jsonSerializationContext.serialize(vertex.getProperties()));

    return vertexObj;
  }
}
