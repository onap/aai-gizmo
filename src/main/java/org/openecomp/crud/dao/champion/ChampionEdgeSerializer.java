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
package org.openecomp.crud.dao.champion;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.openecomp.crud.entity.Edge;
import org.openecomp.crud.entity.Vertex;

import java.lang.reflect.Type;

public class ChampionEdgeSerializer implements JsonSerializer<Edge> {
  @Override
  public JsonElement serialize(Edge edge, Type type, JsonSerializationContext jsonSerializationContext) {
    final JsonObject edgeObj = new JsonObject();
    if (edge.getId().isPresent()) {
      edgeObj.add("key", jsonSerializationContext.serialize(edge.getId().get()));
    }
    edgeObj.add("type", jsonSerializationContext.serialize(edge.getType()));
    edgeObj.add("properties", jsonSerializationContext.serialize(edge.getProperties()));
    edgeObj.add("source", jsonSerializationContext.serialize(edge.getSource()));
    edgeObj.add("target", jsonSerializationContext.serialize(edge.getTarget()));
    return edgeObj;
  }
}
