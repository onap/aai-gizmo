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
package org.onap.crud.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import org.onap.crud.exception.CrudException;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response.Status;

public class VertexPayload {

  private String id;
  private String type;
  private String url;
  private JsonElement properties;
  private List<EdgePayload> in = new ArrayList<EdgePayload>();
  private List<EdgePayload> out = new ArrayList<EdgePayload>();

  private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  public String toJson() {
    return gson.toJson(this);
  }

  public static VertexPayload fromJson(String payload) throws CrudException {
    try {
      if (payload == null || payload.isEmpty()) {
        throw new CrudException("Invalid Json Payload", Status.BAD_REQUEST);
      }
      return gson.fromJson(payload, VertexPayload.class);
    } catch (Exception ex) {
      throw new CrudException("Invalid Json Payload", Status.BAD_REQUEST);
    }
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

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public JsonElement getProperties() {
    return properties;
  }

  public void setProperties(JsonElement properties) {
    this.properties = properties;
  }

  public List<EdgePayload> getIn() {
    return in;
  }

  public void setIn(List<EdgePayload> in) {
    this.in = in;
  }

  public List<EdgePayload> getOut() {
    return out;
  }

  public void setOut(List<EdgePayload> out) {
    this.out = out;
  }


  @Override
  public String toString() {
    return "VertexPayload [id=" + id + ", type=" + type + ", url=" + url + ", properties="
        + properties + ", in=" + in + ", out=" + out + "]";
  }

}