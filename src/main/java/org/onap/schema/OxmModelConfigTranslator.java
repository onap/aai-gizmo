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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.onap.aai.setup.ConfigTranslator;
import org.onap.aai.setup.SchemaLocationsBean;
import org.onap.aai.setup.Version;

public class OxmModelConfigTranslator extends ConfigTranslator {

    public OxmModelConfigTranslator(SchemaLocationsBean bean) {
        super(bean);
    }

    @Override
    public Map<Version, List<String>> getNodeFiles() {
        String nodeDirectory = bean.getNodeDirectory();
        if (nodeDirectory == null) {
            throw new ServiceConfigurationError(
                    "Node(s) directory is empty in the schema location bean (" + bean.getSchemaConfigLocation() + ")");
        }
        try {
            return getVersionMap(Paths.get(nodeDirectory), "*_v*.xml");
        } catch (IOException e) {
            throw new ServiceConfigurationError("Failed to read node(s) directory " + getPath(nodeDirectory), e);
        }
    }

    @Override
    public Map<Version, List<String>> getEdgeFiles() {
        String edgeDirectory = bean.getEdgeDirectory();
        if (edgeDirectory == null) {
            throw new ServiceConfigurationError(
                    "Edge(s) directory is empty in the schema location bean (" + bean.getSchemaConfigLocation() + ")");
        }
        try {
            return getVersionMap(Paths.get(edgeDirectory), "*_v*.json");
        } catch (IOException e) {
            throw new ServiceConfigurationError("Failed to read edge(s) directory " + getPath(edgeDirectory), e);
        }
    }

    private String getPath(String nodeDirectory) {
        return Paths.get(nodeDirectory).toAbsolutePath().toString();
    }

    /**
     * Creates a map containing each OXM Version and the matching OXM file path(s)
     *
     * @param folderPath the folder/directory containing the OXM files
     * @param fileSuffix
     * @return a new Map object (may be empty)
     * @throws IOException if there is a problem reading the specified directory path
     */
    private Map<Version, List<String>> getVersionMap(Path folderPath, String globPattern) throws IOException {
        final PathMatcher filter = folderPath.getFileSystem().getPathMatcher("glob:**/" + globPattern);
        try (final Stream<Path> stream = Files.list(folderPath)) {
            return stream.filter(filter::matches).map(Path::toString).filter(p -> getVersionFromPath(p) != null)
                    .collect(Collectors.groupingBy(this::getVersionFromPath));
        }
    }

    private Version getVersionFromPath(String pathName) {
        String version = "V" + pathName.replaceAll("^.*\\/", "").replaceAll("\\D+", "");
        try {
            return Version.valueOf(version);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
