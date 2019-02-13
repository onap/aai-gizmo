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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.onap.crud.OXMModelLoaderSetup;
import org.onap.crud.service.BulkPayload;
import org.onap.crud.service.util.TestHeaders;
import org.onap.schema.EdgeRulesLoader;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ChampBulkPayloadTest extends OXMModelLoaderSetup {
    private ChampDaoTest testDao = new ChampDaoTest();
    
    @Before
    public void init() throws Exception {
        System.setProperty("CONFIG_HOME", "src/test/resources");
        EdgeRulesLoader.resetSchemaVersionContext();
    }

    @Test
    public void testBulk() {
        try {
            File bulkFile = new File("src/test/resources/payloads/bulk2.json");
            String payloadStr = readFileToString(bulkFile);
            BulkPayload gizmoPayload = BulkPayload.fromJson(payloadStr);
            System.out.println("Input Gizmo Payload:\n" + gizmoPayload.toJson());

            ChampBulkPayload champBulk = new ChampBulkPayload();
            champBulk.fromGizmoPayload(gizmoPayload, "v13", new TestHeaders(), testDao);
            System.out.println("Output Champ Payload:\n" + champBulk.toJson());
            
            assertTrue(champBulk.getEdgeDeleteOps().size() == 1);
            assertTrue(champBulk.getEdgeDeleteOps().get(0).getId().equalsIgnoreCase("50bdab41-ad1c-4d00-952c-a0aa5d827811"));
            
            assertTrue(champBulk.getVertexDeleteOps().size() == 1);
            assertTrue(champBulk.getVertexDeleteOps().get(0).getId().equalsIgnoreCase("50bdab41-ad1c-4d00-952c-a0aa5d827811"));
            assertTrue(champBulk.getVertexDeleteOps().get(0).getType().equalsIgnoreCase("pserver"));
            
            assertTrue(champBulk.getVertexAddModifyOps().size() == 3);
            assertTrue(champBulk.getVertexAddModifyOps().get(0).getOperation().equalsIgnoreCase("add"));
            assertTrue(champBulk.getVertexAddModifyOps().get(0).getType().equalsIgnoreCase("vserver"));
            assertTrue(champBulk.getVertexAddModifyOps().get(0).getLabel().equalsIgnoreCase("v1"));
            assertTrue(champBulk.getVertexAddModifyOps().get(0).getProperty("vserver-id").equals("VSER1"));
            assertTrue(champBulk.getVertexAddModifyOps().get(0).getProperty("aai-node-type").equals("vserver"));
            
            assertTrue(champBulk.getVertexAddModifyOps().get(1).getOperation().equalsIgnoreCase("modify"));
            assertTrue(champBulk.getVertexAddModifyOps().get(1).getId().equalsIgnoreCase("50bdab41-ad1c-4d00-952c-a0aa5d827811"));
            assertTrue(champBulk.getVertexAddModifyOps().get(1).getType().equalsIgnoreCase("pserver"));
            assertTrue(champBulk.getVertexAddModifyOps().get(1).getLabel().equalsIgnoreCase("v2"));
            assertTrue(champBulk.getVertexAddModifyOps().get(1).getProperty("hostname").equals("steve-host2"));
            assertTrue(champBulk.getVertexAddModifyOps().get(1).getProperty("aai-node-type").equals("pserver"));
            
            assertTrue(champBulk.getVertexAddModifyOps().get(2).getOperation().equalsIgnoreCase("modify"));
            assertTrue(champBulk.getVertexAddModifyOps().get(2).getId().equalsIgnoreCase("50bdab41-ad1c-4d00-952c-a0aa5d827811"));
            assertTrue(champBulk.getVertexAddModifyOps().get(2).getType().equalsIgnoreCase("pserver"));
            assertTrue(champBulk.getVertexAddModifyOps().get(2).getLabel().equalsIgnoreCase("v3"));
            assertTrue(champBulk.getVertexAddModifyOps().get(2).getProperty("purpose").equals("new-purpose"));
            assertTrue(champBulk.getVertexAddModifyOps().get(2).getProperty("hostname").equals("oldhost"));
            assertTrue(champBulk.getVertexAddModifyOps().get(2).getProperty("aai-node-type").equals("pserver"));
            
            assertTrue(champBulk.getEdgeAddModifyOps().size() == 2);
            assertTrue(champBulk.getEdgeAddModifyOps().get(0).getOperation().equalsIgnoreCase("add"));
            assertTrue(champBulk.getEdgeAddModifyOps().get(0).getType().equalsIgnoreCase("tosca.relationships.HostedOn"));
            assertTrue(champBulk.getEdgeAddModifyOps().get(0).getLabel().equalsIgnoreCase("e1"));
            assertTrue(champBulk.getEdgeAddModifyOps().get(0).getProperty("contains-other-v").equals("NONE"));
            assertTrue(champBulk.getEdgeAddModifyOps().get(0).getSource().equalsIgnoreCase("$v1"));
            assertTrue(champBulk.getEdgeAddModifyOps().get(0).getTarget().equalsIgnoreCase("1d326bc7-b985-492b-9604-0d5d1f06f908"));
            
            assertTrue(champBulk.getEdgeAddModifyOps().get(1).getOperation().equalsIgnoreCase("modify"));
            assertTrue(champBulk.getEdgeAddModifyOps().get(1).getType().equalsIgnoreCase("tosca.relationships.HostedOn"));
            assertTrue(champBulk.getEdgeAddModifyOps().get(1).getLabel().equalsIgnoreCase("e2"));
            assertTrue(champBulk.getEdgeAddModifyOps().get(1).getProperty("contains-other-v").equals("NONE"));
        }
        catch (Exception ex) {
            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            ex.printStackTrace(printWriter);
            printWriter.flush();
            System.out.println(writer.toString());
            assertTrue(false);
        }
    }

    public static String readFileToString(File aFile) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(aFile));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }

            return sb.toString().replaceAll("\\s+", "");
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                fail("Unexpected IOException: " + e.getMessage());
            }
        }
    }

}
