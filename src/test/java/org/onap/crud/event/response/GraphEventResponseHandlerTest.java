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
package org.onap.crud.event.response;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onap.crud.event.GraphEvent;
import org.onap.crud.event.GraphEvent.GraphEventOperation;
import org.onap.crud.event.envelope.GraphEventEnvelope;
import org.onap.crud.exception.CrudException;
import org.onap.crud.util.TestUtil;
import org.onap.schema.OxmModelLoader;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.crud.OXMModelLoaderSetup;

@RunWith(MockitoJUnitRunner.Silent.class)
public class GraphEventResponseHandlerTest extends OXMModelLoaderSetup {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        System.setProperty("CONFIG_HOME", "src/test/resources");
        System.setProperty("AJSC_HOME", ".");
        System.setProperty("BUNDLECONFIG_DIR", "src/test/resources/bundleconfig-local");

        OxmModelLoader.loadModels();
    }

    @Test
    public void testPolicyViolationsNotDetected() throws Exception {
        String expectedEnvelope = TestUtil.getFileAsString("event/event-envelope-sentinel-no-violations.json");
        Gson gson = new Gson();
        GraphEventEnvelope envelope = gson.fromJson(expectedEnvelope, GraphEventEnvelope.class);

        GraphEventResponseHandler graphEventResponseHandler = new GraphEventResponseHandler();
        assertThat(graphEventResponseHandler.hasPolicyViolations(envelope)).isFalse();
    }

    @Test
    public void testPolicyViolationsDetected() throws Exception {
        String expectedEnvelope = TestUtil.getFileAsString("event/event-envelope-sentinel.json");
        Gson gson = new Gson();
        GraphEventEnvelope envelope = gson.fromJson(expectedEnvelope, GraphEventEnvelope.class);

        GraphEventResponseHandler graphEventResponseHandler = new GraphEventResponseHandler();
        assertThat(graphEventResponseHandler.hasPolicyViolations(envelope)).isTrue();
    }

    @Test
    public void testHandleVertexResponse() throws Exception {
        String graphEvent = TestUtil.getFileAsString("event/graph-vertex-event.json");
        String champResult = TestUtil.getFileAsString("event/champ-vertex-event.json");
        Gson gson = new Gson();
        GraphEvent event = gson.fromJson(graphEvent, GraphEvent.class);
        GraphEventEnvelope result = gson.fromJson(champResult, GraphEventEnvelope.class);

        GraphEventResponseHandler graphEventResponseHandler = new GraphEventResponseHandler();
        String response = graphEventResponseHandler.handleVertexResponse("v13", event, result);

        assertThat(new JsonParser().parse(response).getAsJsonObject().get("url").getAsString())
                .isEqualTo("services/inventory/v13/pserver/890c8b3f-892f-48e3-85cd-748ebf0426a5");
    }

    @Test
    public void testHandleVertexResponseWithError() throws Exception {
        expectedException.expect(CrudException.class);
        expectedException.expectMessage("test error");

        String graphEvent = TestUtil.getFileAsString("event/graph-vertex-event.json");
        String champResult = TestUtil.getFileAsString("event/champ-vertex-event-error.json");
        Gson gson = new Gson();
        GraphEvent event = gson.fromJson(graphEvent, GraphEvent.class);
        GraphEventEnvelope result = gson.fromJson(champResult, GraphEventEnvelope.class);

        GraphEventResponseHandler graphEventResponseHandler = new GraphEventResponseHandler();
        graphEventResponseHandler.handleVertexResponse("v13", event, result);
    }

    @Test(expected = CrudException.class)
    public void testHandleVertexResponseWithViolations() throws Exception {

        String graphEvent = TestUtil.getFileAsString("event/graph-vertex-event.json");
        String champResult = TestUtil.getFileAsString("event/champ-vertex-event-violations.json");
        Gson gson = new Gson();
        GraphEvent event = gson.fromJson(graphEvent, GraphEvent.class);
        GraphEventEnvelope result = gson.fromJson(champResult, GraphEventEnvelope.class);

        GraphEventResponseHandler graphEventResponseHandler = new GraphEventResponseHandler();
        graphEventResponseHandler.handleVertexResponse("v13", event, result);
    }

    @Test
    public void testHandleEdgeResponse() throws Exception {
        String graphEvent = TestUtil.getFileAsString("event/graph-edge-event.json");
        String champResult = TestUtil.getFileAsString("event/champ-edge-event.json");
        Gson gson = new Gson();
        GraphEvent event = gson.fromJson(graphEvent, GraphEvent.class);
        GraphEventEnvelope result = gson.fromJson(champResult, GraphEventEnvelope.class);

        GraphEventResponseHandler graphEventResponseHandler = new GraphEventResponseHandler();
        String response = graphEventResponseHandler.handleEdgeResponse("v10", event, result);

        String id = new JsonParser().parse(response).getAsJsonObject().get("id").getAsString();
        assertThat(id).isEqualTo("test-key");
    }

    @Test
    public void testHandleDeletionResponse() throws Exception {
        GraphEventResponseHandler graphEventResponseHandler = new GraphEventResponseHandler();
        GraphEvent event = GraphEvent.builder(GraphEventOperation.DELETE).build();
        String response = graphEventResponseHandler.handleDeletionResponse(event, new GraphEventEnvelope(event));
        assertThat(response).isEqualTo("");
    }

    @Test
    public void testHandleBulkEventResponse() throws Exception {
        GraphEventResponseHandler graphEventResponseHandler = new GraphEventResponseHandler();
        GraphEvent event = GraphEvent.builder(GraphEventOperation.CREATE).build();
        graphEventResponseHandler.handleBulkEventResponse(event, new GraphEventEnvelope(event));
    }
}
