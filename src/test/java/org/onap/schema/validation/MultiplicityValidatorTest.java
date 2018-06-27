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
package org.onap.schema.validation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onap.crud.entity.Edge;
import org.onap.crud.entity.Vertex;
import org.onap.crud.exception.CrudException;
import org.onap.crud.parser.EdgePayload;
import org.onap.schema.EdgeRulesLoader;
import org.onap.schema.validation.MultiplicityValidator.MultiplicityType;

public class MultiplicityValidatorTest {

    private final String postEdgePayload = "{" + "\"type\": \"tosca.relationships.HostedOn\","
            + "\"source\": \"services/inventory/v12/vserver/50bdab41-ad1c-4d00-952c-a0aa5d827811\","
            + "\"target\": \"services/inventory/v12/pserver/1d326bc7-b985-492b-9604-0d5d1f06f908\","
            + "\"properties\": {" + "\"prevent-delete\": \"NONE\" } }";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void init() {
        ClassLoader classLoader = getClass().getClassLoader();
        File dir = new File(classLoader.getResource("rules").getFile());
        System.setProperty("CONFIG_HOME", dir.getParent());
        EdgeRulesLoader.resetSchemaVersionContext();
    }

    @Test
    public void testValidPayloadForMultiplicityRule() throws CrudException {
        Map<String, List<Edge>> vertexMap = getEdgesForVertex(MultiplicityType.MANY2ONE, true);
        MultiplicityValidator.validatePayloadMultiplicity(EdgePayload.fromJson(postEdgePayload),
                vertexMap.get("source"), vertexMap.get("target"),
                "tosca.relationships.HostedOn", "v11");
    }

    @Test
    public void testInvalidPayloadForMultiplicityRule() throws CrudException {
        thrown.expect(CrudException.class);
        thrown.expectMessage("MANY2ONE multiplicity rule broken for Edge:vserver:pserver:tosca.relationships.HostedOn");

        Map<String, List<Edge>> vertexMap = getEdgesForVertex(MultiplicityType.MANY2ONE, false);
        MultiplicityValidator.validatePayloadMultiplicity(EdgePayload.fromJson(postEdgePayload),
                vertexMap.get("source"), vertexMap.get("target"),
                "tosca.relationships.HostedOn", "v11");
    }

    @Test
    public void testIsVertexValidForMultiplicityType() throws CrudException {

        Map<String, List<Edge>> vertexMap = getEdgesForVertex(MultiplicityType.MANY2MANY, true);
        Assert.assertTrue(MultiplicityValidator.isVertexValidForMultiplicityType(vertexMap.get("source"),
                vertexMap.get("target"), MultiplicityType.MANY2MANY));

        vertexMap = getEdgesForVertex(MultiplicityType.MANY2ONE, true);
        Assert.assertTrue(MultiplicityValidator.isVertexValidForMultiplicityType(
                vertexMap.get("source"), vertexMap.get("target"), MultiplicityType.MANY2ONE));

        vertexMap = getEdgesForVertex(MultiplicityType.ONE2MANY, true);
        Assert.assertTrue(MultiplicityValidator.isVertexValidForMultiplicityType(
                vertexMap.get("source"), vertexMap.get("target"), MultiplicityType.ONE2MANY));

        vertexMap = getEdgesForVertex(MultiplicityType.ONE2ONE, true);
        Assert.assertTrue(MultiplicityValidator.isVertexValidForMultiplicityType(
                vertexMap.get("source"), vertexMap.get("target"), MultiplicityType.ONE2ONE));

        vertexMap = getEdgesForVertex(MultiplicityType.ONE2MANY, false);
        Assert.assertFalse(MultiplicityValidator.isVertexValidForMultiplicityType(
                vertexMap.get("source"), vertexMap.get("target"), MultiplicityType.ONE2MANY));

        vertexMap = getEdgesForVertex(MultiplicityType.ONE2ONE, false);
        Assert.assertFalse(MultiplicityValidator.isVertexValidForMultiplicityType(
                vertexMap.get("source"), vertexMap.get("target"), MultiplicityType.ONE2ONE));
    }

    private Map<String, List<Edge>> getEdgesForVertex(MultiplicityType multiplicityType, boolean pass) {

        Map<String, List<Edge>> vertexMap = new HashMap<String, List<Edge>>();
        List<Edge> edgesForSourceVertex = new ArrayList<>();
        List<Edge> edgesForTargetVertex = new ArrayList<>();

        switch (multiplicityType) {
            case MANY2MANY:
                if (pass) {
                    Edge edge = new Edge.Builder("type").source(new Vertex.Builder("source").build())
                            .target(new Vertex.Builder("target").build()).build();
                    edgesForSourceVertex.add(edge);
                    edgesForTargetVertex.add(edge);
                }
                break;
            case MANY2ONE:
                if (pass) {
                    Edge edge = new Edge.Builder("type").source(new Vertex.Builder("source").build())
                            .target(new Vertex.Builder("target").build()).build();
                    edgesForTargetVertex.add(edge);
                } else {
                    Edge edge = new Edge.Builder("type").source(new Vertex.Builder("source").build())
                            .target(new Vertex.Builder("target").build()).build();
                    edgesForSourceVertex.add(edge);
                    edgesForTargetVertex.add(edge);
                }
                break;
            case ONE2MANY:
                if (pass) {
                    Edge edge = new Edge.Builder("type").source(new Vertex.Builder("source").build())
                            .target(new Vertex.Builder("target").build()).build();
                    edgesForSourceVertex.add(edge);
                } else {
                    Edge edge = new Edge.Builder("type").source(new Vertex.Builder("source").build())
                            .target(new Vertex.Builder("target").build()).build();
                    edgesForSourceVertex.add(edge);
                    edgesForTargetVertex.add(edge);
                }
                break;
            case ONE2ONE:
                if (!pass) {
                    Edge edge = new Edge.Builder("type").source(new Vertex.Builder("source").build())
                            .target(new Vertex.Builder("target").build()).build();
                    edgesForSourceVertex.add(edge);
                    edgesForTargetVertex.add(edge);
                }
                break;
        }
        vertexMap.put("source", edgesForSourceVertex);
        vertexMap.put("target", edgesForTargetVertex);

        return vertexMap;
    }

}
