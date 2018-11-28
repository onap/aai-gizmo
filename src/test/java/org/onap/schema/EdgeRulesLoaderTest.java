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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;
import org.onap.crud.exception.CrudException;
import org.onap.crud.parser.EdgePayload;
import org.onap.crud.util.CrudServiceUtil;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.crud.OXMModelLoaderSetup;

@RunWith(MockitoJUnitRunner.Silent.class)
public class EdgeRulesLoaderTest extends OXMModelLoaderSetup{

    @Test
    public void loadModels() throws Exception {
        EdgeRulesLoader.loadModels();
        assertTrue(EdgeRulesLoader.getSchemaForVersion ( "v11" ).isValidType ( "org.onap.relationships.inventory.groupsResourcesIn" ));
    }

    @Test
    public void loadModelsWithAVersion() throws Exception {
        EdgeRulesLoader.resetSchemaVersionContext ();
        EdgeRulesLoader.loadModels("V11");
        assertEquals(1, EdgeRulesLoader.getSchemas ().size ());
        assertEquals("v11", EdgeRulesLoader.getLatestSchemaVersion ());
    }

    @Test
    public void getSchemaForVersion() throws Exception {
        EdgeRulesLoader.resetSchemaVersionContext ();
        EdgeRulesLoader.loadModels("v11");
        String version = EdgeRulesLoader.getLatestSchemaVersion();
        RelationshipSchema g = EdgeRulesLoader.getSchemaForVersion(version);
        assertNotNull(g.lookupRelationType("org.onap.relationships.inventory.groupsResourcesIn"));
        assertNotNull(g.lookupRelation("U:V:org.onap.relationships.inventory.groupsResourcesIn"));
        assertNull(g.lookupRelation("U:W:org.onap.relationships.inventory.groupsResourcesIn"));
    }

    @Test
    public void getRelationshipTypeForNodePair() throws Exception {
        EdgeRulesLoader.resetSchemaVersionContext();
        EdgeRulesLoader.loadModels("v11");
        RelationshipSchema schema = EdgeRulesLoader.getSchemaForVersion("v11");
        
        EdgePayload payload1 = new EdgePayload();
        payload1.setSource("services/inventory/v11/availability-zone/xxx");
        payload1.setTarget("services/inventory/v11/cloud-region/xxx");

        EdgePayload payload2 = new EdgePayload();
        payload2.setSource("services/inventory/v11/image/xxx");
        payload2.setTarget("services/inventory/v11/pserver/xxx");
        
        EdgePayload payload3 = new EdgePayload();
        payload3.setSource("services/inventory/v11/allotted-resource/xxx");
        payload3.setTarget("services/inventory/v11/instance-group/xxx");
        
        // Get edge types for node pair with a single possible edge between them
        Set<String> typeList = schema.getValidRelationTypes("availability-zone", "cloud-region");
        assertEquals(1, typeList.size());
        assertTrue(typeList.contains("org.onap.relationships.inventory.BelongsTo"));
        assertEquals(CrudServiceUtil.determineEdgeType(payload1, "v11"), "org.onap.relationships.inventory.BelongsTo");
        
        // Get edge types for node pair with no possible edge between them
        typeList = schema.getValidRelationTypes("image", "pserver");
        assertEquals(0, typeList.size());
        typeList = schema.getValidRelationTypes("cloud-region", "availability-zone");
        assertEquals(0, typeList.size());
        
        try {
          // Should throw an exception here
          CrudServiceUtil.determineEdgeType(payload2, "v11");
          assertTrue(false);
        }
        catch (CrudException ex) {
          System.out.println(ex.getMessage());
        }
        
        typeList = schema.getValidRelationTypes("allotted-resource", "instance-group");
        assertEquals(2, typeList.size());
        assertTrue(typeList.contains("org.onap.relationships.inventory.TestEdge"));
        assertTrue(typeList.contains("org.onap.relationships.inventory.MemberOf"));
        
        for (String type : typeList) {
          System.out.println(type);
        }
        
        try {
          // Should throw an exception here
          CrudServiceUtil.determineEdgeType(payload3, "v11");
          assertTrue(false);
        }
        catch (CrudException ex) {
          System.out.println(ex.getMessage());
        }
    }

    @Test
    public void getSchemaForVersionFail() throws Exception {
        EdgeRulesLoader.loadModels();
        try {
            EdgeRulesLoader.getSchemaForVersion("v1");
        } catch (CrudException e) {
            assertEquals(404, e.getHttpStatus().getStatusCode());
        }
    }
}