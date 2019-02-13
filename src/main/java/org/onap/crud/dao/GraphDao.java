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
package org.onap.crud.dao;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.onap.aai.restclient.client.OperationResult;
import org.onap.crud.dao.champ.ChampBulkPayload;
import org.onap.crud.entity.Edge;
import org.onap.crud.entity.Vertex;
import org.onap.crud.exception.CrudException;

public interface GraphDao {

  public Vertex getVertex(String id, String version) throws CrudException;

  public OperationResult getVertex(String id, String type, String version, Map<String, String> queryParams) throws CrudException;

  /**
   * Retrieve all of the edges which are incident to the vertex with the
   * specified identifier.
   *
   * @param id
   *          - The unique identifier of the vertex to retrieve the edges for.
   * @param queryParams
   * 		  - query parameters to be passed
   * @param txid
   *      - a DB transaction ID to use (if null, no transactionId is used)
   * @return - A collection of edges.
   * @throws CrudException
   */
  public List<Edge> getVertexEdges(String id, Map<String, String> queryParams, String txId) throws CrudException;

  /**
   * Retrieve a collection of {@link Vertex} objects which match the supplied
   * type label and filter properties.
   *
   * @param type
   *          - The vertex type that we want to retrieve.
   * @param filter
   *          - The parameters to filter our results by.
   * @return - The {@link OperationResult} OperationResult
   * @throws CrudException
   */
  public OperationResult getVertices(String type, Map<String, Object> filter, String version) throws CrudException;

  /**
   * Retrieve a collection of {@link Vertex} objects which match the supplied
   * type label and filter properties.
   *
   * @param type
   *          - The vertex type that we want to retrieve.
   * @param filter
   *          - The parameters to filter our results by.
   * @param properties
   *          - The properties to retrieve with the vertex
   * @return - The {@link OperationResult} OperationResult
   * @throws CrudException
   */
  public OperationResult getVertices(String type, Map<String, Object> filter, Set<String> properties, String version) throws CrudException;

  /**
   * Retrieve an {@link Edge} from the graph database by specifying its unique
   * identifier.
   *
   * @param id
   *          - The unique identifier for the Edge to be retrieved.
   * @param type
   *          - The type that we want to retrieve.
   * @param queryParams
   * 		  - query parameters to be passed
   * @return - The {@link OperationResult} OperationResult corresponding to the specified identifier.
   * @throws CrudException
   */
  public OperationResult getEdge(String id, String type, Map<String, String> queryParams) throws CrudException;

  /**
   * Retrieve a collection of {@link Edge} objects with a given type and which
   * match a set of supplied filter parameters.
   *
   * @param type
   *          - The type of edges that we are interested in.
   * @param filter
   *          - The parameters that we want to filter our edges by.
   * @return - The {@link OperationResult} OperationResult
   * @throws CrudException
   */
  public OperationResult getEdges(String type, Map<String, Object> filter) throws CrudException;

  /**
   * Insert a new {@link Vertex} into the graph data store.
   *
   * @param type
   *          - The type label to assign to the vertex.
   * @param properties
   *          - The properties to associated with this vertex.
   * @return - The result of the Vertex creation.
   * @throws CrudException
   */
  public OperationResult addVertex(String type, Map<String, Object> properties, String version) throws CrudException;

  /**
   * Updates an existing {@link Vertex}.
   *
   * @param id
   *          - The unique identifier of the vertex to be updated.
   * @param properties
   *          - The properties to associate with the vertex.
   * @return - The result of the update OperationResult.
   * @throws CrudException
   */
  public OperationResult updateVertex(String id, String type, Map<String, Object> properties, String version) throws CrudException;

  /**
   * Removes the specified vertex from the graph data base.
   *
   * <p>
   * NOTE: The vertex MUST contain NO incident edges before it can be deleted.
   *
   * @param id
   *          - The unique identifier of the vertex to be deleted.
   * @throws CrudException
   */
  public void deleteVertex(String id, String type) throws CrudException;

  /**
   * Adds an edge to the graph database.
   *
   * @param type
   *          - The 'type' label to apply to the edge.
   * @param source
   *          - The source vertex for this edge.
   * @param target
   *          - The target vertex for this edge.
   * @param properties
   *          - The properties map to associate with this edge.
   * @return - The {@link OperationResult} OperationResult containing the Edge that was created.
   * @throws CrudException
   */
  public OperationResult addEdge(String type, Vertex source, Vertex target, Map<String, Object> properties, String version) throws CrudException;

  /**
   * Updates an existing {@link Edge}.
   *
   * @param edge
   *          - The edge to be updated.
   * @return - The result of the update OperationResult.
   * @throws CrudException
   */
  public OperationResult updateEdge(Edge edge) throws CrudException;

  /**
   * Remove the specified edge from the graph data base.
   *
   * @param id
   *          - The unique identifier of the edge to be deleted.
   * @throws CrudException
   */
  public void deleteEdge(String id) throws CrudException;

  public String openTransaction();

  public void commitTransaction(String id) throws CrudException;

  public void rollbackTransaction(String id) throws CrudException;

  public boolean transactionExists(String id) throws CrudException;

  public Vertex addVertex(String type, Map<String, Object> properties, String version, String txId) throws CrudException;

  public Edge addEdge(String type, Vertex source, Vertex target, Map<String, Object> properties, String version, String txId)
          throws CrudException;

  public Vertex updateVertex(String id, String type, Map<String, Object> properties, String version, String txId) throws CrudException;

  public Edge updateEdge(Edge edge, String txId) throws CrudException;

  public void deleteVertex(String id, String type, String txId) throws CrudException;

  public void deleteEdge(String id, String txId) throws CrudException;

  public Edge getEdge(String id, String txId) throws CrudException;

  public Edge getEdge(String id) throws CrudException;

  public OperationResult bulkOperation(ChampBulkPayload champPayload) throws CrudException;
}
