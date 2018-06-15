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
package org.onap.crud.util.etag;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import org.junit.Before;
import org.junit.Test;
import org.onap.crud.event.GraphEventEdge;
import org.onap.crud.event.GraphEventVertex;
import org.onap.crud.util.etag.EtagGenerator;
import com.google.gson.JsonObject;

public class EtagGeneratorTest {

    private EtagGenerator etagGenerator;

    @Before
    public void init() throws NoSuchAlgorithmException {
        etagGenerator = new EtagGenerator();
    }

    private GraphEventVertex createVertex(String propKey, String propValue) {
        JsonObject properties = new JsonObject();
        properties.addProperty(propKey, propValue);
        GraphEventVertex vertex = new GraphEventVertex("vertex1", "v11", "pserver", properties);
        return vertex;
    }

    private GraphEventEdge createEdge(String id, GraphEventVertex source, GraphEventVertex target, String propKey,
            String propValue) {
        JsonObject properties = new JsonObject();
        properties.addProperty(propKey, propValue);
        GraphEventEdge edge = new GraphEventEdge(id, "v11", "tosca.relationships.HostedOn", source, target, properties);
        return edge;
    }

    @Test
    public void computeHashForIdenticalVertexObjects() throws IOException {
        // everything is same
        GraphEventVertex sourceVertex1 = createVertex("prop1", "value1");
        GraphEventVertex targetVertex1 = createVertex("prop2", "value2");

        GraphEventEdge edge1 = createEdge("edge1", sourceVertex1, targetVertex1, "prop1", "value1");

        GraphEventVertex sourceVertex2 = createVertex("prop1", "value1");
        GraphEventVertex targetVertex2 = createVertex("prop2", "value2");

        GraphEventEdge edge2 = createEdge("edge1", sourceVertex2, targetVertex2, "prop1", "value1");

        assertThat(etagGenerator.computeHashForEdge(edge1), is(etagGenerator.computeHashForEdge(edge2)));
    }

    @Test
    public void computeHashForVertexObjectsWithDifferentKey() throws IOException {
        // key is different
        GraphEventVertex sourceVertex1 = createVertex("prop1", "value1");
        GraphEventVertex targetVertex1 = createVertex("prop2", "value2");

        GraphEventEdge edge1 = createEdge("edge1", sourceVertex1, targetVertex1, "prop1", "value1");

        GraphEventVertex sourceVertex2 = createVertex("prop1", "value1");
        GraphEventVertex targetVertex2 = createVertex("prop2", "value2");

        GraphEventEdge edge2 = createEdge("edge2", sourceVertex2, targetVertex2, "prop1", "value1");

        assertThat(etagGenerator.computeHashForEdge(edge1), not(etagGenerator.computeHashForEdge(edge2)));
    }

    @Test
    public void computeHashForVertexObjectsWithDifferentEdge() throws IOException {
        // relationship is different
        GraphEventVertex sourceVertex1 = createVertex("prop1", "value1");
        GraphEventVertex targetVertex1 = createVertex("prop2", "value2");

        GraphEventEdge edge1 = createEdge("edge1", sourceVertex1, targetVertex1, "prop1", "value1");

        GraphEventVertex sourceVertex2 = createVertex("prop1", "value1");
        GraphEventVertex targetVertex2 = createVertex("prop2", "value2");

        GraphEventEdge edge2 = createEdge("edge2", sourceVertex2, targetVertex2, "prop1", "value1");
        edge2.setType("tosca.relationships.RelatedTo");

        assertThat(etagGenerator.computeHashForEdge(edge1), not(etagGenerator.computeHashForEdge(edge2)));
    }

    @Test
    public void computeHashForEdgeObjectsWithDifferentVertexObjects() throws IOException {
        // source/target different
        GraphEventVertex sourceVertex1 = createVertex("prop1", "value1");
        GraphEventVertex targetVertex1 = createVertex("prop2", "value2");
        targetVertex1.setId("vertex2");

        GraphEventEdge edge1 = createEdge("edge1", sourceVertex1, targetVertex1, "prop1", "value1");

        GraphEventVertex sourceVertex2 = createVertex("prop1", "value1");
        GraphEventVertex targetVertex2 = createVertex("prop2", "value2");

        GraphEventEdge edge2 = createEdge("edge2", sourceVertex2, targetVertex2, "prop1", "value1");
        edge2.setType("tosca.relationships.RelatedTo");

        assertThat(etagGenerator.computeHashForEdge(edge1), not(etagGenerator.computeHashForEdge(edge2)));
    }

    @Test
    public void computeHashForEdgeObjectsWithDifferentProperties() throws IOException {
        // property different
        GraphEventVertex sourceVertex1 = createVertex("sourceprop1", "value1");
        GraphEventVertex targetVertex1 = createVertex("targetprop2", "value2");

        GraphEventEdge edge1 = createEdge("edge1", sourceVertex1, targetVertex1, "edgeprop1", "value1");

        GraphEventVertex sourceVertex2 = createVertex("sourceprop1", "value1");
        GraphEventVertex targetVertex2 = createVertex("targetprop2", "value2");

        GraphEventEdge edge2 = createEdge("edge1", sourceVertex2, targetVertex2, "edgeprop2", "value2");

        assertThat(etagGenerator.computeHashForEdge(edge1), not(etagGenerator.computeHashForEdge(edge2)));
    }

    @Test
    public void testComputeHashForIdenticalVertexObjects() throws IOException {
        GraphEventVertex sourceVertex1 = createVertex("prop1", "value1");
        GraphEventVertex targetVertex1 = createVertex("prop1", "value1");
        assertThat(etagGenerator.computeHashForVertex(sourceVertex1),
                is(etagGenerator.computeHashForVertex(targetVertex1)));
    }

    @Test
    public void testComputeHashForVertexObjectsWithDifferentProperties() throws IOException {
        GraphEventVertex sourceVertex1 = createVertex("prop1", "value1");
        GraphEventVertex targetVertex1 = createVertex("prop2", "value2");
        assertThat(etagGenerator.computeHashForVertex(sourceVertex1),
                not(etagGenerator.computeHashForVertex(targetVertex1)));
    }

    @Test
    public void testComputeHashForChampObjectsWithDifferentKey() throws IOException {
        GraphEventVertex sourceVertex1 = createVertex("prop1", "value1");
        GraphEventVertex targetVertex1 = createVertex("prop1", "value1");
        targetVertex1.setId("vertex2");
        assertThat(etagGenerator.computeHashForVertex(sourceVertex1),
                not(etagGenerator.computeHashForVertex(targetVertex1)));
    }


}