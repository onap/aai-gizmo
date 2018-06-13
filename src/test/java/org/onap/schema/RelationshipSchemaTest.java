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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.junit.Test;
import org.onap.aai.edges.EdgeRule;
import org.onap.aai.edges.exceptions.EdgeRuleNotFoundException;
import org.onap.crud.exception.CrudException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class RelationshipSchemaTest {

    @Test
    public void shouldLoadAllTheVersionsInDirectory() throws Exception {
        RelationshipSchema rs = loadRelations();
        assertTrue(!rs.lookupRelationType ("org.onap.some-relation"  ).isEmpty ());
    }

    @Test
    public void shouldContainValidTypes() throws Exception {
        RelationshipSchema rs = loadRelations();
        assertTrue(rs.lookupRelationType ("org.onap.some-relation") != null);
        assertTrue(rs.lookupRelationType("notValidType") == null);
    }

    @Test
    public void shouldLookUpByRelation() throws Exception {
        RelationshipSchema rs = loadRelations();
        assertNotNull(rs.lookupRelation("service-instance:customer:org.onap.some-relation"));
    }

    @Test
    public void shouldLookUpByRelationType() throws Exception {
        RelationshipSchema rs = loadRelations();
        assertNotNull(rs.lookupRelationType("org.onap.groupsResourcesIn"));
        assertTrue(rs.lookupRelation("availability-zone:complex:org.onap.groupsResourcesIn").containsKey("prevent-delete"));
    }

    private RelationshipSchema loadRelations() throws CrudException, EdgeRuleNotFoundException, IOException {
        String defaultEdgeProps = "{" +
                "\"contains-other-v\": \"java.lang.String\"," +
                "\"delete-other-v\": \"java.lang.String\"," +
                "\"SVC-INFRA\": \"java.lang.String\"," +
                "\"prevent-delete\": \"java.lang.String\"" +
                "}";

        Map<String, String> ruleOne = new HashMap<> (  );
        Map<String, String> ruleTwo = new HashMap<> (  );

        ruleOne.put("label", "org.onap.some-relation");
        ruleOne.put("direction", "OUT");
        ruleOne.put("contains-other-v", "NONE");
        ruleOne.put("delete-other-v", "NONE");
        ruleOne.put("prevent-delete", "NONE");
        ruleOne.put("from", "service-instance");
        ruleOne.put("to", "customer");
        ruleOne.put("multiplicity", "MANY2MANY");
        ruleOne.put("default", "true");
        ruleOne.put("description", "");

        ruleTwo.put("label", "org.onap.groupsResourcesIn");
        ruleTwo.put("direction", "OUT");
        ruleTwo.put("contains-other-v", "NONE");
        ruleTwo.put("delete-other-v", "NONE");
        ruleTwo.put("prevent-delete", "NONE");
        ruleTwo.put("from", "availability-zone");
        ruleTwo.put("to", "complex");
        ruleTwo.put("multiplicity", "MANY2MANY");
        ruleTwo.put("default", "true");
        ruleTwo.put("description", "");

        EdgeRule erOne = new EdgeRule ( ruleOne );
        EdgeRule erTwo = new EdgeRule ( ruleTwo );
        Multimap<String, EdgeRule> relationship = ArrayListMultimap.create();
        relationship.put ( "customer|service-instane", erOne );
        relationship.put ( "availability-zone|complex", erTwo );
        return new RelationshipSchema ( relationship, defaultEdgeProps );

    }
}