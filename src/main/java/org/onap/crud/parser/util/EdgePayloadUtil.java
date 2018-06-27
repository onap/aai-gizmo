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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.core.Response.Status;
import org.onap.crud.entity.Edge;
import org.onap.crud.entity.Vertex;
import org.onap.crud.exception.CrudException;

/**
 * Utility Class to extract data from the Edge Payload
 */
public class EdgePayloadUtil {

    private static final Pattern URL_MATCHER = Pattern.compile("services/inventory/(.*)/(.*)/(.*)");

    private static Matcher getVertexMatcher(String vertex) throws CrudException {
        Matcher matcher = URL_MATCHER.matcher(vertex);
        if (!matcher.matches()) {
            throw new CrudException("Invalid Source/Target Urls", Status.BAD_REQUEST);
        }
        return matcher;
    }

    /**
     * Returns the node type from a vertex on the edge payload
     *
     * @param vertex
     * @return
     * @throws CrudException
     */
    public static String getVertexNodeType(String vertex) throws CrudException {
        return getVertexMatcher(vertex).group(2);
    }

    /**
     * Returns the node id from a vertex on the edge payload
     *
     * @param vertex
     * @return
     * @throws CrudException
     */
    public static String getVertexNodeId(String vertex) throws CrudException {
        return getVertexMatcher(vertex).group(3);
    }

    /**
     * Generates a key based on the edge payload content
     *
     * @param source
     * @param target
     * @param type
     * @return
     * @throws CrudException
     */
    public static String generateEdgeKey(String source, String target, String type) throws CrudException {
        return getVertexNodeType(source) + ":" + getVertexNodeType(target) + ":" + type;
    }

    /**
     * Returns an Edge Builder object from the payload properties
     *
     * @param source
     * @param target
     * @param type
     * @return
     * @throws CrudException
     */
    public static Edge.Builder getBuilderFromEdgePayload(String source, String target, String type) throws CrudException {
        Edge.Builder edgeBuilder = new Edge.Builder(type);

        edgeBuilder.source(new Vertex.Builder(getVertexNodeType(source)).id(getVertexNodeId(source)).build());
        edgeBuilder.target(new Vertex.Builder(getVertexNodeType(target)).id(getVertexNodeId(target)).build());

        return edgeBuilder;
    }

    /**
     * Returns an Edge Builder object from an Edge object properties
     *
     * @param edge
     * @return
     */
    public static Edge.Builder getBuilderFromEdge(Edge edge) {
        Edge.Builder edgeBuilder = new Edge.Builder(edge.getType()).id(edge.getId().get());

        edgeBuilder
                .source(new Vertex.Builder(edge.getSource().getType()).id(edge.getSource().getId().get()).build());
        edgeBuilder
                .target(new Vertex.Builder(edge.getTarget().getType()).id(edge.getTarget().getId().get()).build());

        return edgeBuilder;
    }

    /**
     * Filter Edges by its source/target vertex type and the edge type
     *
     * @param sourceTargetType the new Edge source/target type
     * @param type
     * @param edges
     * @return List<Edge>
     */
    public static List<Edge> filterEdgesByRelatedVertexAndType(String sourceTargetType, String type, List<Edge> edges) {
        List<Edge> filteredEdges = new ArrayList<>();
        if (edges != null) {
            for (Edge edge : edges) {
                if (doesEdgeTypeMatch(edge, type) && doesEdgeSourceTargetTypeMatch(edge, sourceTargetType)) {
                    filteredEdges.add(edge);
                }
            }
        }
        return filteredEdges;
    }

    private static boolean doesEdgeTypeMatch(Edge edge, String type) {
        return edge.getType() != null && edge.getType().equals(type);
    }

    private static boolean doesEdgeSourceTargetTypeMatch(Edge edge, String sourceTargetType) {
        return (edge.getSource().getType() != null && edge.getSource().getType().equals(sourceTargetType))
                || ((edge.getTarget().getType() != null && edge.getTarget().getType().equals(sourceTargetType)));
    }
}
