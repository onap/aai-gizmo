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
package org.onap.schema;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.onap.crud.entity.Edge;
import org.onap.crud.exception.CrudException;
import org.onap.crud.parser.EdgePayload;
import org.onap.schema.validation.RelationshipSchemaValidator;

public class RelationshipSchemaValidatorTest {
    // @formatter:off
	  private final String edgePayload = "{" +
		      "\"type\": \"tosca.relationships.HostedOn\"," +
		      "\"source\": \"services/inventory/v12/vserver/50bdab41-ad1c-4d00-952c-a0aa5d827811\"," +
		      "\"target\": \"services/inventory/v12/pserver/1d326bc7-b985-492b-9604-0d5d1f06f908\"," +
		      "\"properties\": {" +
		      "\"prevent-delete\": \"NONE\" } }";
	  
	  private final String champEdge = "{" +
		      "\"key\": \"test-uuid\"," +
		      "\"type\": \"edgeType\"," +
		      "\"properties\": {" +
		      "\"prevent-delete\": \"NONE\" }," +
		      "\"source\": {" +
		      "\"key\": \"50bdab41-ad1c-4d00-952c-a0aa5d827811\", \"type\": \"vserver\"}," +
		      "\"target\": {" +
		      "\"key\": \"1d326bc7-b985-492b-9604-0d5d1f06f908\", \"type\": \"pserver\"}" +
		      " }";
	// @formatter:on

    @Before
    public void init() throws Exception {
        System.setProperty("CONFIG_HOME", "src/test/resources");
    }

    @Test
    public void testValidateIncomingUpdatePayloadMissingSource() throws CrudException {
        String type = "tosca.relationships.HostedOn";
        String version = "v11";
        String jsonString;
        EdgePayload payload;

        String champJson = champEdge.replace("edgeType", type);
        Edge edge = Edge.fromJson(champJson);

        jsonString = edgePayload.replace("services/inventory/v12/vserver/50bdab41-ad1c-4d00-952c-a0aa5d827811", "");
        payload = EdgePayload.fromJson(jsonString);

        try {
            RelationshipSchemaValidator.validateIncomingUpdatePayload(edge, version, payload);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), is("Invalid Source/Target Urls"));
        }
    }

    @Test
    public void testValidateIncomingUpdatePayloadInvalidSource() throws CrudException {
        String type = "tosca.relationships.HostedOn";
        String version = "v11";
        String jsonString;
        EdgePayload payload;

        String champJson = champEdge.replace("edgeType", type);
        Edge edge = Edge.fromJson(champJson);

        jsonString = edgePayload.replace("1d326bc7-b985-492b-9604-0d5d1f06f908", "invalidId");
        payload = EdgePayload.fromJson(jsonString);

        try {
            RelationshipSchemaValidator.validateIncomingUpdatePayload(edge, version, payload);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), is("Target can't be updated"));
        }
    }

    @Test
    public void testValidateIncomingUpdatePayloadMissingTarget() throws CrudException {
        String type = "tosca.relationships.HostedOn";
        String version = "v11";
        String jsonString;
        EdgePayload payload;

        String champJson = champEdge.replace("edgeType", type);
        Edge edge = Edge.fromJson(champJson);

        jsonString = edgePayload.replace("services/inventory/v12/pserver/1d326bc7-b985-492b-9604-0d5d1f06f908", "");
        payload = EdgePayload.fromJson(jsonString);

        try {
            RelationshipSchemaValidator.validateIncomingUpdatePayload(edge, version, payload);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), is("Invalid Source/Target Urls"));
        }
    }

    @Test
    public void testValidateIncomingUpdatePayloadInvalidTarget() throws CrudException {
        String type = "tosca.relationships.HostedOn";
        String version = "v11";
        String jsonString;
        EdgePayload payload;

        String champJson = champEdge.replace("edgeType", type);
        Edge edge = Edge.fromJson(champJson);

        jsonString = edgePayload.replace("50bdab41-ad1c-4d00-952c-a0aa5d827811", "invalidId");
        payload = EdgePayload.fromJson(jsonString);

        try {
            RelationshipSchemaValidator.validateIncomingUpdatePayload(edge, version, payload);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), is("Source can't be updated"));
        }
    }

    @Test
    public void testValidateIncomingAddPayloadExceptionHandling() throws CrudException {
        String type = "tosca.relationships.HostedOn";
        String version = "v11";
        String jsonString;

        EdgePayload payload;
        jsonString = edgePayload.replace("services/inventory/v12/vserver/50bdab41-ad1c-4d00-952c-a0aa5d827811", "");
        payload = EdgePayload.fromJson(jsonString);

        try {
            RelationshipSchemaValidator.validateIncomingAddPayload(version, type, payload);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), is("Invalid Source/Target Urls"));
        }

        jsonString = edgePayload;
        payload = EdgePayload.fromJson(jsonString);
        type = "tosca.relationships.invalidType";
        try {
            RelationshipSchemaValidator.validateIncomingAddPayload(version, type, payload);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), is("Invalid source/target/relationship type: vserver:pserver:" + type));
        }
    }

    @Test
    public void testValidateIncomingPatchPayloadMissingSource() throws CrudException {
        String type = "tosca.relationships.HostedOn";
        String version = "v11";
        String jsonString;

        String champJson = champEdge.replace("edgeType", type);
        Edge edge = Edge.fromJson(champJson);

        EdgePayload payload;

        jsonString = edgePayload.replace("services/inventory/v12/vserver/50bdab41-ad1c-4d00-952c-a0aa5d827811", "");
        payload = EdgePayload.fromJson(jsonString);

        try {
            RelationshipSchemaValidator.validateIncomingPatchPayload(edge, version, payload);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), is("Invalid Source/Target Urls"));
        }
    }

    @Test
    public void testValidateIncomingPatchPayloadInvalidSource() throws CrudException {
        String type = "tosca.relationships.HostedOn";
        String version = "v11";
        String jsonString;

        String champJson = champEdge.replace("edgeType", type);
        Edge edge = Edge.fromJson(champJson);

        EdgePayload payload;

        jsonString = edgePayload.replace("50bdab41-ad1c-4d00-952c-a0aa5d827811", "invalidId");
        payload = EdgePayload.fromJson(jsonString);
        try {
            RelationshipSchemaValidator.validateIncomingPatchPayload(edge, version, payload);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), is("Source can't be updated"));
        }
    }

    @Test
    public void testValidateIncomingPatchPayloadMissingTarget() throws CrudException {
        String type = "tosca.relationships.HostedOn";
        String version = "v11";
        String jsonString;

        String champJson = champEdge.replace("edgeType", type);
        Edge edge = Edge.fromJson(champJson);

        EdgePayload payload;

        jsonString = edgePayload.replace("services/inventory/v12/pserver/1d326bc7-b985-492b-9604-0d5d1f06f908", "");
        payload = EdgePayload.fromJson(jsonString);

        try {
            RelationshipSchemaValidator.validateIncomingPatchPayload(edge, version, payload);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), is("Invalid Source/Target Urls"));
        }
    }

    @Test
    public void testValidateIncomingPatchPayloadInvalidTarget() throws CrudException {
        String type = "tosca.relationships.HostedOn";
        String version = "v11";
        String jsonString;

        String champJson = champEdge.replace("edgeType", type);
        Edge edge = Edge.fromJson(champJson);

        EdgePayload payload;

        jsonString = edgePayload.replace("1d326bc7-b985-492b-9604-0d5d1f06f908", "invalidId");
        payload = EdgePayload.fromJson(jsonString);
        try {
            RelationshipSchemaValidator.validateIncomingPatchPayload(edge, version, payload);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), is("Target can't be updated"));
        }
    }

    @Test
    public void testValidateTypeExceptionHandling() {
        String version = "v11";
        String type = "tosca.relationships.invalidType";

        try {
            RelationshipSchemaValidator.validateType(version, type);
        } catch (CrudException e) {
            assertEquals(400, e.getHttpStatus().getStatusCode());
            assertThat(e.getMessage(), is("Invalid " + RelationshipSchema.SCHEMA_RELATIONSHIP_TYPE + ": " + type));
        }
    }
}
