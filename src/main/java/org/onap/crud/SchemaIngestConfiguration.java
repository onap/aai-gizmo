/**
 * ============LICENSE_START=======================================================
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

import org.onap.aai.edges.EdgeIngestor;
import org.onap.aai.nodes.NodeIngestor;
import org.onap.aai.setup.AAIConfigTranslator;
import org.onap.aai.setup.ConfigTranslator;
import org.onap.aai.setup.SchemaLocationsBean;
import org.onap.aai.setup.SchemaVersions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "file:${schema.ingest.file}", ignoreResourceNotFound = true)
public class SchemaIngestConfiguration {

    @Bean
    public SchemaVersions schemaVersions() {
        return new SchemaVersions();
    }

    @Bean
    public SchemaLocationsBean schemaLocationsBean() {
        return new SchemaLocationsBean();
    }

    @Bean
    public ConfigTranslator configTranslator() {
        return new AAIConfigTranslator(schemaLocationsBean(), schemaVersions());
    }

    @Bean
    public NodeIngestor nodeIngestor() {
        return new NodeIngestor(configTranslator());
    }

    @Bean
    public EdgeIngestor edgeIngestor() {
        return new EdgeIngestor(configTranslator(), schemaVersions());
    }

}