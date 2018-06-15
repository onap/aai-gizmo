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
package org.onap.crud.util.etag;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.onap.crud.event.GraphEventEdge;
import org.onap.crud.event.GraphEventVertex;
import org.onap.crud.util.HashGenerator;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Computes hash for GraphEventVertex and GraphEventEdge
 */
public class EtagGenerator {

    private static final String AAI_LAST_MOD_TS = "aai-last-mod-ts";
    private final HashGenerator hashGenerator;

    public EtagGenerator() throws NoSuchAlgorithmException {
        this.hashGenerator = new HashGenerator();
    }

    /**
     * Takes in the GraphEventVertex for which the hash is to be computed.
     * @param GraphEventVertex
     * @return hash for the GraphEventVertex
     * @throws IOException
     */
    public String computeHashForVertex(GraphEventVertex graphEventVertex) throws IOException {
        return hashGenerator.generateSHA256AsHex(graphEventVertex.getId(), graphEventVertex.getType(), convertPropertiesToMap(graphEventVertex.getProperties()));
    }

    /**
     * Takes in the GraphEventEdge for which the hash is to be computed.
     * @param GraphEventEdge
     * @return hash for the GraphEventEdge
     * @throws IOException
     */
    public String computeHashForEdge(GraphEventEdge graphEventEdge) throws IOException {
        return hashGenerator.generateSHA256AsHex(graphEventEdge.getId(), graphEventEdge.getType(),
                convertPropertiesToMap(graphEventEdge.getProperties()),
                computeHashForVertex(graphEventEdge.getSource()), computeHashForVertex(graphEventEdge.getTarget()));
    }

    private Map<String, Object> convertPropertiesToMap(JsonElement properties) {
        Map<String, Object> propertiesMap = new HashMap<>();
        if (null != properties) {
            JsonObject propsObject = properties.getAsJsonObject();
            for (Entry<String, JsonElement> props : propsObject.entrySet()) {
                String key = props.getKey();
                String value = props.getValue().getAsString();
                propertiesMap.put(key, value);
            }
        }
        return filterAndSortProperties(propertiesMap);
    }

    private Map<String, Object> filterAndSortProperties(Map<String, Object> properties) {
        return properties
                .entrySet()
                .stream()
                .filter(x -> !x.getKey().equals(AAI_LAST_MOD_TS))
                .sorted((x, y) -> x.getKey().compareTo(y.getKey()))
                .collect(LinkedHashMap::new,
                        (m, e) -> m.put(e.getKey(), e.getValue()),
                        Map::putAll);
    }
}