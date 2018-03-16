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

import static org.junit.Assert.*;
import edu.emory.mathcs.backport.java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.onap.crud.exception.CrudException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class RelationshipSchemaLoaderTest {

    @Before
    public void init() {
        ClassLoader classLoader = getClass().getClassLoader();
        File dir = new File(classLoader.getResource( "model").getFile());
        System.setProperty("CONFIG_HOME", dir.getParent());
        RelationshipSchemaLoader.resetVersionContextMap();
    }

    @Test
    public void loadModels() throws Exception {
        RelationshipSchemaLoader.resetVersionContextMap();
        RelationshipSchemaLoader.loadModels();
        assertFalse( RelationshipSchemaLoader.getVersionContextMap().keySet().isEmpty());
    }

    @Test
    public void loadModelsWithAVersion() throws Exception {
        RelationshipSchemaLoader.resetVersionContextMap();
        RelationshipSchemaLoader.loadModels("v11");
        assertEquals(1, RelationshipSchemaLoader.getVersionContextMap().keySet().size());
        assertEquals("v11",  RelationshipSchemaLoader.getLatestSchemaVersion());
    }

    @Test
    public void getSchemaForVersion() throws Exception {
        RelationshipSchemaLoader.resetVersionContextMap();
        RelationshipSchemaLoader.loadModels("v11");
        String version = RelationshipSchemaLoader.getLatestSchemaVersion();
        RelationshipSchema g = RelationshipSchemaLoader.getSchemaForVersion(version);
        assertNotNull(g.lookupRelationType("org.onap.relationships.inventory.BelongsTo"));
    }

    public void getSchemaForVersionManualFile() throws Exception {
      RelationshipSchemaLoader.resetVersionContextMap();
      RelationshipSchemaLoader.loadModels("v10");
      String version = RelationshipSchemaLoader.getLatestSchemaVersion();
      RelationshipSchema g = RelationshipSchemaLoader.getSchemaForVersion(version);
      assertNotNull(g.lookupRelationType("locatedIn"));
    }


    @Test
    public void getSchemaForVersionFail() throws Exception {
        RelationshipSchemaLoader.resetVersionContextMap();
        RelationshipSchemaLoader.loadModels();
        try {
            RelationshipSchemaLoader.getSchemaForVersion("v1");
        } catch (CrudException e) {
            assertEquals(404, e.getHttpStatus().getStatusCode());
        }
    }

    @Test
    public void setVersionContextMap() throws Exception {
        RelationshipSchemaLoader.resetVersionContextMap();
        ArrayList<String> jsonString = new ArrayList<String>();
        String rules = "{" +
                "\"rules\": [" +
                "{" +
                "\"from\": \"availability-zone\"," +
                "\"to\": \"complex\"," +
                "\"label\": \"groupsResourcesIn\"," +
                "\"direction\": \"OUT\"," +
                "\"multiplicity\": \"Many2Many\"," +
                "\"contains-other-v\": \"NONE\"," +
                "\"delete-other-v\": \"NONE\"," +
                "\"SVC-INFRA\": \"NONE\"," +
                "\"prevent-delete\": \"!${direction}\"" +
                "}]}";
        String props = "{" +
                "  \"isParent\":\"java.lang.Boolean\"," +
                "  \"isParent-REV\":\"java.lang.Boolean\"," +
                "  \"usesResource\":\"java.lang.Boolean\"," +
                "  \"usesResource-REV\":\"java.lang.Boolean\"," +
                "  \"SVC-INFRA\":\"java.lang.Boolean\"," +
                "  \"SVC-INFRA-REV\":\"java.lang.Boolean\"," +
                "  \"hasDelTarget\":\"java.lang.Boolean\"," +
                "  \"hasDelTarget-REV\":\"java.lang.Boolean\"" +
                "}";
        jsonString.add(rules);
        jsonString.add(props);
        RelationshipSchema nRs = new RelationshipSchema(jsonString);
        Map<String, RelationshipSchema> versionMap = new HashMap<>();
        versionMap.put("v1", nRs);
        RelationshipSchemaLoader.setVersionContextMap(versionMap);
        assertNotNull(RelationshipSchemaLoader.getSchemaForVersion("v1").lookupRelationType("groupsResourcesIn"));
    }
}
