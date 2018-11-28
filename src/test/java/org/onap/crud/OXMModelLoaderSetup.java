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
package org.onap.crud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.onap.aai.edges.EdgeIngestor;
import org.onap.aai.nodes.NodeIngestor;
import org.onap.aai.setup.AAIConfigTranslator;
import org.onap.aai.setup.SchemaLocationsBean;
import org.onap.aai.setup.SchemaVersion;
import org.onap.aai.setup.SchemaVersions;
import org.onap.schema.EdgePropsConfiguration;
import org.onap.schema.EdgeRulesLoader;
import org.onap.schema.OxmModelLoader;

public class OXMModelLoaderSetup {

    private EdgeRulesLoader edgeRulesLoader;

    private OxmModelLoader oxmModelLoader;

    @Mock
    private SchemaLocationsBean schemaLocationsBean;

    @Mock
    private SchemaVersions schemaVersions;

    @Mock
    private EdgePropsConfiguration edgePropsConfiguration;

    private List<SchemaVersion> schemaVersionList = new ArrayList<>();

    @Before
    public void schemaBeanMockSetup() throws Exception {

    	schemaVersionList.add(new SchemaVersion("v8"));
    	schemaVersionList.add(new SchemaVersion("v9"));
    	schemaVersionList.add(new SchemaVersion("v10"));
    	schemaVersionList.add(new SchemaVersion("v11"));
        schemaVersionList.add(new SchemaVersion("v13"));

        Mockito.when(schemaVersions.getVersions()).thenReturn(schemaVersionList);
        Mockito.when(schemaLocationsBean.getNodesInclusionPattern()).thenReturn(Arrays.asList(".*oxm(.*).xml"));
        Mockito.when(schemaLocationsBean.getEdgesInclusionPattern()).thenReturn(Arrays.asList("DbEdgeRules_.*.json"));
        Mockito.when(schemaLocationsBean.getNodeDirectory()).thenReturn("src/test/resources/multi-oxm/");
        Mockito.when(schemaLocationsBean.getEdgeDirectory()).thenReturn("src/test/resources/rules");
        Mockito.when(edgePropsConfiguration.getEdgePropsDir()).thenReturn("src/test/resources/edgeProps/");

        AAIConfigTranslator aaiConfigTranslator = new AAIConfigTranslator(schemaLocationsBean, schemaVersions);
        NodeIngestor nodeIngestor = new NodeIngestor(aaiConfigTranslator);
        EdgeIngestor edgeIngestor = new EdgeIngestor(aaiConfigTranslator, schemaVersions);
        edgeRulesLoader = new EdgeRulesLoader(aaiConfigTranslator, edgeIngestor, edgePropsConfiguration);
        oxmModelLoader = new OxmModelLoader(aaiConfigTranslator, nodeIngestor);
    }
}