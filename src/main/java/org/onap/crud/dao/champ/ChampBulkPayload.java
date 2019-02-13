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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

import org.onap.aai.restclient.client.OperationResult;
import org.onap.crud.dao.GraphDao;
import org.onap.crud.entity.Edge;
import org.onap.crud.entity.Vertex;
import org.onap.crud.exception.CrudException;
import org.onap.crud.service.BulkPayload;
import org.onap.crud.service.EdgePayload;
import org.onap.crud.service.VertexPayload;
import org.onap.crud.util.CrudServiceUtil;
import org.onap.schema.OxmModelValidator;
import org.onap.schema.RelationshipSchemaValidator;

public class ChampBulkPayload {

  public static String ADD_OP = "add";
  public static String UPDATE_OP = "modify";
  public static String DELETE_OP = "delete";
  public static String PATCH_OP = "patch";

  private List<ChampBulkOp> edgeDeleteOps = new ArrayList<ChampBulkOp>();
  private List<ChampBulkOp> vertexDeleteOps = new ArrayList<ChampBulkOp>();
  private List<ChampBulkOp> vertexAddModifyOps = new ArrayList<ChampBulkOp>();
  private List<ChampBulkOp> edgeAddModifyOps = new ArrayList<ChampBulkOp>();

  private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

  public String toJson() {
    return  gson.toJson(this);
  }

  public static ChampBulkPayload fromJson(String payload) {
    return gson.fromJson(payload, ChampBulkPayload.class);
  }

  public List<ChampBulkOp> getEdgeDeleteOps() {
    return edgeDeleteOps;
  }

  public void setEdgeDeleteOps(List<ChampBulkOp> ops) {
    this.edgeDeleteOps = ops;
  }

  public List<ChampBulkOp> getVertexDeleteOps() {
    return vertexDeleteOps;
  }

  public void setVertexDeleteOps(List<ChampBulkOp> ops) {
    this.vertexDeleteOps = ops;
  }

  public List<ChampBulkOp> getVertexAddModifyOps() {
    return vertexAddModifyOps;
  }

  public void setVertexAddModifyOps(List<ChampBulkOp> ops) {
    this.vertexAddModifyOps = ops;
  }

  public List<ChampBulkOp> getEdgeAddModifyOps() {
    return edgeAddModifyOps;
  }

  public void setEdgeAddModifyOps(List<ChampBulkOp> ops) {
    this.edgeAddModifyOps = ops;
  }

  public void fromGizmoPayload(BulkPayload gizmoPayload, String version, HttpHeaders headers, GraphDao champDao) throws CrudException {
    edgeDeleteOps = new ArrayList<ChampBulkOp>();
    vertexDeleteOps = new ArrayList<ChampBulkOp>();
    vertexAddModifyOps = new ArrayList<ChampBulkOp>();
    edgeAddModifyOps = new ArrayList<ChampBulkOp>();

    Map<String,String> addedVertexes = new HashMap<String,String>();

    // Step 1. Extract edge deletes
    for (JsonElement v : gizmoPayload.getRelationships()) {
      List<Map.Entry<String, JsonElement>> entries = new ArrayList<Map.Entry<String, JsonElement>>(
              v.getAsJsonObject().entrySet());

      if (entries.size() != 2) {
        throw new CrudException("", Status.BAD_REQUEST);
      }
      Map.Entry<String, JsonElement> opr = entries.get(0);
      Map.Entry<String, JsonElement> item = entries.get(1);
      EdgePayload edgePayload = EdgePayload.fromJson(item.getValue().getAsJsonObject().toString());

      if (opr.getValue().getAsString().equalsIgnoreCase("delete")) {
        ChampBulkOp champOp = new ChampBulkOp();
        champOp.setId(edgePayload.getId());
        champOp.setOperation(DELETE_OP);
        edgeDeleteOps.add(champOp);
      }
    }

    // Step 2: Extract vertex deletes
    for (JsonElement v : gizmoPayload.getObjects()) {
      List<Map.Entry<String, JsonElement>> entries = new ArrayList<Map.Entry<String, JsonElement>>(
              v.getAsJsonObject().entrySet());

      if (entries.size() != 2) {
        throw new CrudException("", Status.BAD_REQUEST);
      }

      Map.Entry<String, JsonElement> opr = entries.get(0);
      Map.Entry<String, JsonElement> item = entries.get(1);
      VertexPayload vertexPayload = VertexPayload.fromJson(item.getValue().getAsJsonObject().toString());

      if (opr.getValue().getAsString().equalsIgnoreCase("delete")) {
        String type = OxmModelValidator.resolveCollectionType(version, vertexPayload.getType());
        ChampBulkOp champOp = new ChampBulkOp();
        champOp.setId(vertexPayload.getId());
        champOp.setOperation(DELETE_OP);
        champOp.setType(type);
        vertexDeleteOps.add(champOp);
      }
    }

    // Step 3: Extract vertex add/modify
    for (JsonElement v : gizmoPayload.getObjects()) {
      List<Map.Entry<String, JsonElement>> entries = new ArrayList<Map.Entry<String, JsonElement>>(
              v.getAsJsonObject().entrySet());

      if (entries.size() != 2) {
        throw new CrudException("", Status.BAD_REQUEST);
      }
      Map.Entry<String, JsonElement> opr = entries.get(0);
      Map.Entry<String, JsonElement> item = entries.get(1);
      VertexPayload vertexPayload = VertexPayload.fromJson(item.getValue().getAsJsonObject().toString());

      // Add vertex
      if (opr.getValue().getAsString().equalsIgnoreCase("add")) {
        vertexPayload.setProperties(CrudServiceUtil.mergeHeaderInFoToPayload(vertexPayload.getProperties(),
                headers, true));
        Vertex validatedVertex = OxmModelValidator.validateIncomingUpsertPayload(null, version, vertexPayload.getType(),
                vertexPayload.getProperties());
        validatedVertex.getProperties().put(OxmModelValidator.Metadata.NODE_TYPE.propertyName(), vertexPayload.getType());

        ChampBulkOp champOp = new ChampBulkOp();
        champOp.setLabel(item.getKey());
        champOp.setOperation(ADD_OP);
        champOp.setType(vertexPayload.getType());
        champOp.setProperties(validatedVertex.getProperties());
        vertexAddModifyOps.add(champOp);
      }

      // Update vertex
      else if (opr.getValue().getAsString().equalsIgnoreCase("modify")) {
        vertexPayload.setProperties(CrudServiceUtil.mergeHeaderInFoToPayload(vertexPayload.getProperties(),
                headers, false));
        Vertex validatedVertex = OxmModelValidator.validateIncomingUpsertPayload(vertexPayload.getId(), version,
                vertexPayload.getType(), vertexPayload.getProperties());
        validatedVertex.getProperties().put(OxmModelValidator.Metadata.NODE_TYPE.propertyName(), vertexPayload.getType());

        ChampBulkOp champOp = new ChampBulkOp();
        champOp.setLabel(item.getKey());
        champOp.setId(vertexPayload.getId());
        champOp.setOperation(UPDATE_OP);
        champOp.setType(vertexPayload.getType());
        champOp.setProperties(validatedVertex.getProperties());
        vertexAddModifyOps.add(champOp);
      }

      // Patch vertex
      else if (opr.getValue().getAsString().equalsIgnoreCase("patch")) {
        if ( (vertexPayload.getId() == null) || (vertexPayload.getType() == null) ) {
          throw new CrudException("id and type must be specified for patch request", Status.BAD_REQUEST);
        }

        vertexPayload.setProperties(CrudServiceUtil.mergeHeaderInFoToPayload(vertexPayload.getProperties(),
                headers, false));

        OperationResult existingVertexOpResult =
                champDao.getVertex(vertexPayload.getId(),
                        OxmModelValidator.resolveCollectionType(version, vertexPayload.getType()),
                        version, new HashMap<String, String>());

        Vertex existingVertex = Vertex.fromJson(existingVertexOpResult.getResult(), version);
        Vertex validatedVertex = OxmModelValidator.validateIncomingPatchPayload(vertexPayload.getId(),
                version, vertexPayload.getType(), vertexPayload.getProperties(), existingVertex);
        validatedVertex.getProperties().put(OxmModelValidator.Metadata.NODE_TYPE.propertyName(), vertexPayload.getType());

        ChampBulkOp champOp = new ChampBulkOp();
        champOp.setLabel(item.getKey());
        champOp.setId(vertexPayload.getId());
        champOp.setOperation(UPDATE_OP);
        champOp.setType(vertexPayload.getType());
        champOp.setProperties(validatedVertex.getProperties());
        vertexAddModifyOps.add(champOp);
      }

      addedVertexes.put(item.getKey(), "services/inventory/" + version + "/" + vertexPayload.getType()+ "/" + item.getKey());
    }

    // Step 4: Extract edge add/modify
    for (JsonElement v : gizmoPayload.getRelationships()) {
      List<Map.Entry<String, JsonElement>> entries = new ArrayList<Map.Entry<String, JsonElement>>(
              v.getAsJsonObject().entrySet());

      if (entries.size() != 2) {
        throw new CrudException("", Status.BAD_REQUEST);
      }
      Map.Entry<String, JsonElement> opr = entries.get(0);
      Map.Entry<String, JsonElement> item = entries.get(1);
      EdgePayload edgePayload = EdgePayload.fromJson(item.getValue().getAsJsonObject().toString());

      // Add/Update edge
      if (opr.getValue().getAsString().equalsIgnoreCase("add")
              || opr.getValue().getAsString().equalsIgnoreCase("modify")
              || opr.getValue().getAsString().equalsIgnoreCase("patch")) {




        ChampBulkOp champOp = new ChampBulkOp();
        champOp.setLabel(item.getKey());

        if (opr.getValue().getAsString().equalsIgnoreCase("add")) {
          // If the source/target is a vertex that hasn't been created yet, get the types from the map
          String sourceUrl = edgePayload.getSource();
          String targetUrl = edgePayload.getTarget();
          edgePayload.setSource(resolveUrl(edgePayload.getSource(), addedVertexes));
          edgePayload.setTarget(resolveUrl(edgePayload.getTarget(), addedVertexes));

          // If the type isn't set, resolve it based on on the source and target vertex types
          if (edgePayload.getType() == null || edgePayload.getType().isEmpty()) {
            edgePayload.setType(CrudServiceUtil.determineEdgeType(edgePayload, version));
          }

          champOp.setType(edgePayload.getType());
          Edge validatedEdge = RelationshipSchemaValidator.validateIncomingAddPayload(version, edgePayload.getType(), edgePayload);
          champOp.setOperation(ADD_OP);
          champOp.setProperties(validatedEdge.getProperties());
          champOp.setSource(sourceUrl.substring(sourceUrl.lastIndexOf('/') + 1));
          champOp.setTarget(targetUrl.substring(targetUrl.lastIndexOf('/') + 1));
        } else if (opr.getValue().getAsString().equalsIgnoreCase("modify")) {
          Edge edge = champDao.getEdge(edgePayload.getId());
          if (edgePayload.getType() == null || edgePayload.getType().isEmpty()) {
            edgePayload.setType(edge.getType());
          }
          champOp.setType(edgePayload.getType());
          Edge validatedEdge = RelationshipSchemaValidator.validateIncomingUpdatePayload(edge, version, edgePayload);
          champOp.setOperation(UPDATE_OP);
          champOp.setId(edgePayload.getId());
          champOp.setProperties(validatedEdge.getProperties());
          champOp.setSource(edge.getSource().getId().get());
          champOp.setTarget(edge.getTarget().getId().get());
        } else {
          if (edgePayload.getId() == null) {
            throw new CrudException("id must be specified for patch request", Status.BAD_REQUEST);
          }
          Edge existingEdge = champDao.getEdge(edgePayload.getId());
          if (edgePayload.getType() == null || edgePayload.getType().isEmpty()) {
            edgePayload.setType(existingEdge.getType());
          }
          champOp.setType(edgePayload.getType());
          Edge patchedEdge = RelationshipSchemaValidator.validateIncomingPatchPayload(existingEdge, version, edgePayload);
          champOp.setOperation(UPDATE_OP);
          champOp.setId(edgePayload.getId());
          champOp.setProperties(patchedEdge.getProperties());
          champOp.setSource(existingEdge.getSource().getId().get());
          champOp.setTarget(existingEdge.getTarget().getId().get());
        }

        edgeAddModifyOps.add(champOp);
      }
    }
  }

  private String resolveUrl(String vertexUrl, Map<String, String> addedVertexes) throws CrudException {
    if (vertexUrl.startsWith("$")) {
      String key = vertexUrl.substring(1);
      if (addedVertexes.get(key) != null) {
        return addedVertexes.get(key);
      }

      throw new CrudException("Unable to resolve vertex " + key, Status.BAD_REQUEST);
    }

    return vertexUrl;
  }
}
