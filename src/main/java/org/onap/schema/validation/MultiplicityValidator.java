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
package org.onap.schema.validation;

import java.util.List;
import javax.ws.rs.core.Response.Status;
import org.onap.crud.entity.Edge;
import org.onap.crud.exception.CrudException;
import org.onap.crud.parser.EdgePayload;
import org.onap.crud.parser.util.EdgePayloadUtil;
import org.onap.schema.EdgeRulesLoader;
import org.onap.schema.RelationshipSchema;

/**
 * Validator to enforce multiplicity rules on the creation of a new Edge
 *
 */
public class MultiplicityValidator {

    public enum MultiplicityType {
        MANY2ONE("Many2One"), MANY2MANY("Many2Many"), ONE2MANY("One2Many"), ONE2ONE("One2One");

        private final String value;

        MultiplicityType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Validates the Edge payload's source and target vertices against multiplicity rule
     *
     * @param payload
     * @param edgesForSourceVertex
     * @param edgesForTargetVertex
     * @param type
     * @param version
     * @throws CrudException
     */
    public static void validatePayloadMultiplicity(EdgePayload payload, List<Edge> edgesForSourceVertex,
            List<Edge> edgesForTargetVertex, String type, String version)
            throws CrudException {
        RelationshipSchema schema = EdgeRulesLoader.getSchemaForVersion(version);
        // find the validate the key from the schema
        String key = EdgePayloadUtil.generateEdgeKey(payload.getSource(), payload.getTarget(), type);

        // get the multiplicity rule for the relationships
        String multiplicityTypeValue = schema.lookupRelationMultiplicity(key);
        if (multiplicityTypeValue != null) {
            MultiplicityType multiplicityType = MultiplicityType.valueOf(multiplicityTypeValue.toUpperCase());

            boolean isVertexValidForMultiplicityType =
                    isVertexValidForMultiplicityType(edgesForSourceVertex, edgesForTargetVertex, multiplicityType);

            if (!isVertexValidForMultiplicityType) {
                throw new CrudException(
                        multiplicityType.toString() + " multiplicity rule broken for Edge:" + key,
                        Status.BAD_REQUEST);
            }
        }
    }

    /**
     * Compare vertex existing relationships to ensure its not in breach of multiplicity rules
     *
     * @param edgesForVertex
     * @param multiplicityType
     * @return
     */
    public static Boolean isVertexValidForMultiplicityType(List<Edge> edgesForSourceVertex,
            List<Edge> edgesForTargetVertex,
            MultiplicityType multiplicityType) {

        switch (multiplicityType) {
            case MANY2MANY:
                return true;
            case MANY2ONE:
                if (edgesForSourceVertex != null && !edgesForSourceVertex.isEmpty()) {
                    return false;
                }
                break;
            case ONE2MANY:
                if (edgesForTargetVertex != null && !edgesForTargetVertex.isEmpty()) {
                    return false;
                }
                break;
            case ONE2ONE:
                if ((edgesForSourceVertex != null && !edgesForSourceVertex.isEmpty())
                        || (edgesForTargetVertex != null && !edgesForTargetVertex.isEmpty())) {
                    return false;
                }
                break;
        }
        return true;
    }

}