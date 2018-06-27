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
package org.onap.crud.parser.util;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.onap.crud.entity.Edge;
import org.onap.crud.entity.Vertex;
import org.onap.crud.exception.CrudException;

public class EdgePayloadUtilTest {

    @Test
    public void testGetVertexNodeType() throws CrudException {
        Assert.assertEquals("vserver", EdgePayloadUtil
                .getVertexNodeType("services/inventory/v12/vserver/50bdab41-ad1c-4d00-952c-a0aa5d827811"));
    }

    @Test
    public void testGetVertexNodeId() throws CrudException {
        Assert.assertEquals("50bdab41-ad1c-4d00-952c-a0aa5d827811",
                EdgePayloadUtil.getVertexNodeId("services/inventory/v12/vserver/50bdab41-ad1c-4d00-952c-a0aa5d827811"));
    }

    @Test
    public void testGenerateEdgeKey() throws CrudException {
        Assert.assertEquals("vserver:pserver:tosca.relationships.HostedOn",
                EdgePayloadUtil.generateEdgeKey("services/inventory/v12/vserver/50bdab41-ad1c-4d00-952c-a0aa5d827811",
                        "services/inventory/v12/pserver/1d326bc7-b985-492b-9604-0d5d1f06f908",
                        "tosca.relationships.HostedOn"));
    }

    @Test
    public void testKeyGenerationEnforcesSourceAndTargetOrder() throws Exception {
        String generateEdgeKey1 = EdgePayloadUtil.generateEdgeKey(
                "services/inventory/v12/vserver/50bdab41-ad1c-4d00-952c-a0aa5d827811",
                "services/inventory/v12/pserver/1d326bc7-b985-492b-9604-0d5d1f06f908", "tosca.relationships.HostedOn");

        String generateEdgeKey2 = EdgePayloadUtil.generateEdgeKey(
                "services/inventory/v12/pserver/1d326bc7-b985-492b-9604-0d5d1f06f908",
                "services/inventory/v12/vserver/50bdab41-ad1c-4d00-952c-a0aa5d827811", "tosca.relationships.HostedOn");

        Assert.assertNotEquals(generateEdgeKey1, generateEdgeKey2);
    }


    @Test
    public void testGetBuilderFromEdgePayload() throws CrudException {
        Edge.Builder builder = EdgePayloadUtil.getBuilderFromEdgePayload(
                "services/inventory/v12/vserver/50bdab41-ad1c-4d00-952c-a0aa5d827811",
                "services/inventory/v12/pserver/1d326bc7-b985-492b-9604-0d5d1f06f908", "tosca.relationships.HostedOn");

        Edge edge = builder.build();
        Assert.assertEquals("tosca.relationships.HostedOn", edge.getType());
        Assert.assertEquals("vserver", edge.getSource().getType());
        Assert.assertEquals("50bdab41-ad1c-4d00-952c-a0aa5d827811", edge.getSource().getId().get());
        Assert.assertEquals("pserver", edge.getTarget().getType());
        Assert.assertEquals("1d326bc7-b985-492b-9604-0d5d1f06f908", edge.getTarget().getId().get());
    }

    @Test
    public void tesGetBuilderFromEdge() throws CrudException {
        Edge edge = createEdge("vserver", "pserver");

        Assert.assertNotNull(edge);
        Assert.assertNotNull(EdgePayloadUtil.getBuilderFromEdge(edge));
    }

    @Test
    public void testFilterEdgesByRelatedVertexAndType() throws CrudException {
        List<Edge> edges = new ArrayList<>();
        edges.add(createEdge("vserver", "pserver"));
        edges.add(createEdge("vce", "pserver"));
        edges.add(createEdge("snapshot", "pserver"));
        edges.add(createEdge("vserver", "pserver"));

        List<Edge> filteredEdges =
                EdgePayloadUtil.filterEdgesByRelatedVertexAndType("vserver", "tosca.relationships.HostedOn", edges);

        Assert.assertNotNull(filteredEdges);
        Assert.assertEquals(2, filteredEdges.size());
    }

    private Edge createEdge(String sourceVertexType, String targetVertexType) {
        return new Edge.Builder("tosca.relationships.HostedOn").id("test")
                .source(new Vertex.Builder(sourceVertexType).id("50bdab41-ad1c-4d00-952c-a0aa5d827811").build())
                .target(new Vertex.Builder(targetVertexType).id("1d326bc7-b985-492b-9604-0d5d1f06f908").build())
                .build();
    }
}
