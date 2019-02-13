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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.onap.aai.restclient.client.OperationResult;
import org.onap.crud.dao.GraphDao;
import org.onap.crud.entity.Edge;
import org.onap.crud.entity.Vertex;
import org.onap.crud.exception.CrudException;

public class ChampDaoTest implements GraphDao {

    @Override
    public Vertex getVertex(String id, String version) throws CrudException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OperationResult getVertex(String id, String type, String version, Map<String, String> queryParams)
            throws CrudException {
        OperationResult res = new OperationResult();
        Vertex v = new Vertex.Builder("pserver").id("50bdab41-ad1c-4d00-952c-a0aa5d827811").property("hostname", "oldhost").build();
        res.setResult(200, v.toJson().replace("\"id\"", "\"key\""));
        return res;
    }

    @Override
    public List<Edge> getVertexEdges(String id, Map<String, String> queryParams, String txId) throws CrudException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OperationResult getVertices(String type, Map<String, Object> filter, String version) throws CrudException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OperationResult getVertices(String type, Map<String, Object> filter, Set<String> properties, String version)
            throws CrudException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OperationResult getEdge(String id, String type, Map<String, String> queryParams) throws CrudException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OperationResult getEdges(String type, Map<String, Object> filter) throws CrudException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OperationResult addVertex(String type, Map<String, Object> properties, String version) throws CrudException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OperationResult updateVertex(String id, String type, Map<String, Object> properties, String version)
            throws CrudException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteVertex(String id, String type) throws CrudException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public OperationResult addEdge(String type, Vertex source, Vertex target, Map<String, Object> properties,
            String version) throws CrudException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public OperationResult updateEdge(Edge edge) throws CrudException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteEdge(String id) throws CrudException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String openTransaction() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void commitTransaction(String id) throws CrudException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void rollbackTransaction(String id) throws CrudException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean transactionExists(String id) throws CrudException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Vertex addVertex(String type, Map<String, Object> properties, String version, String txId)
            throws CrudException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Edge addEdge(String type, Vertex source, Vertex target, Map<String, Object> properties, String version,
            String txId) throws CrudException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Vertex updateVertex(String id, String type, Map<String, Object> properties, String version, String txId)
            throws CrudException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Edge updateEdge(Edge edge, String txId) throws CrudException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteVertex(String id, String type, String txId) throws CrudException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void deleteEdge(String id, String txId) throws CrudException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Edge getEdge(String id, String txId) throws CrudException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Edge getEdge(String id) throws CrudException {
        Edge edge = new Edge.Builder("tosca.relationships.HostedOn").id("xxx-yyy-zzz").source(new Vertex.Builder("vserver").id("50bdab41-ad1c-4d00-952c-a0aa5d827811").build())
                .target(new Vertex.Builder("pserver").id("1d326bc7-b985-492b-9604-0d5d1f06f908").build()).build();
        
        return edge;
    }

    @Override
    public OperationResult bulkOperation(ChampBulkPayload champPayload) throws CrudException {
        // TODO Auto-generated method stub
        return null;
    }

}
