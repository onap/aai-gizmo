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
package org.onap.crud.event.response;

import javax.ws.rs.core.Response.Status;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.crud.event.GraphEvent;
import org.onap.crud.event.GraphEvent.GraphEventResult;
import org.onap.crud.event.envelope.GraphEventEnvelope;
import org.onap.crud.exception.CrudException;
import org.onap.crud.logging.CrudServiceMsgs;
import org.onap.crud.parser.CrudResponseBuilder;
import org.onap.schema.validation.OxmModelValidator;
import org.onap.schema.validation.RelationshipSchemaValidator;

/**
 * Reads event responses, logs and generates exceptions if errors are found.
 *
 */
public class GraphEventResponseHandler {

    private static Logger logger = LoggerFactory.getInstance().getLogger(GraphEventResponseHandler.class.getName());

    public String handleVertexResponse(String version, GraphEvent event, GraphEventEnvelope response)
            throws CrudException {
        handlePolicyViolations(event, response);
        logResponse(event, response.getBody());

        if (isErrorResponse(response.getBody())) {
            throwOperationException(response);
        }

        return CrudResponseBuilder.buildUpsertVertexResponse(
                OxmModelValidator.validateOutgoingPayload(version, response.getBody().getVertex().toVertex()), version);
    }

    public String handleEdgeResponse(String version, GraphEvent event, GraphEventEnvelope response)
            throws CrudException {
        handlePolicyViolations(event, response);
        logResponse(event, response.getBody());

        if (isErrorResponse(response.getBody())) {
            throwOperationException(response);
        }

        return CrudResponseBuilder.buildUpsertEdgeResponse(
                RelationshipSchemaValidator.validateOutgoingPayload(version, response.getBody().getEdge().toEdge()),
                version);
    }

    public String handleDeletionResponse(GraphEvent event, GraphEventEnvelope response) throws CrudException {
        handlePolicyViolations(event, response);
        logResponse(event, response.getBody());

        if (isErrorResponse(response.getBody())) {
            throwOperationException(response);
        }

        return "";
    }

    public void handleBulkEventResponse(GraphEvent event, GraphEventEnvelope response) throws CrudException {
        handlePolicyViolations(event, response);
        logResponse(event, response.getBody());

        if (isErrorResponse(response.getBody())) {
            throwOperationException(response);
        }
    }

    public boolean hasPolicyViolations(GraphEventEnvelope event) {
        return event.getPolicyViolations() != null && event.getPolicyViolations().isJsonArray()
                && event.getPolicyViolations().getAsJsonArray().size() != 0;
    }

    private void handlePolicyViolations(GraphEvent event, GraphEventEnvelope response) throws CrudException {
        if (hasPolicyViolations(response)) {
            logPolicyViolation(event, response);
            throw new CrudException(GraphEventResponseMessage.POLICY_VIOLATION_EXCEPTION_MESSAGE.getMessage(
                    response.getBody().getTransactionId(), response.getPolicyViolations()), Status.BAD_REQUEST);
        }
    }

    private void logResponse(GraphEvent event, GraphEvent response) {
        String message = GraphEventResponseMessage.BASE_OPERATION_LOG_MESSAGE.getMessage(response.getObjectType(),
                response.getObjectKey(), response.getTransactionId(), event.getOperation().toString(),
                response.getResult());
        if (isErrorResponse(response)) {
            message = GraphEventResponseMessage.OPERATION_ERROR_LOG_MESSAGE.getMessage(message,
                    response.getErrorMessage());
        }

        logger.info(CrudServiceMsgs.ASYNC_DATA_SERVICE_INFO, message);
    }

    private void logPolicyViolation(GraphEvent event, GraphEventEnvelope response) {
        //@formatter:off
        logger.info(CrudServiceMsgs.ASYNC_DATA_SERVICE_INFO,
                GraphEventResponseMessage.POLICY_VIOLATION_LOG_MESSAGE.getMessage(
                        response.getBody().getTransactionId(),
                        response.getHeader().getSourceName(),
                        response.getHeader().getEventType(),
                        response.getBody().getObjectKey(),
                        response.getBody().getObjectType(),
                        event.getOperation().toString(),
                        response.getPolicyViolations().toString()));
        //@formatter:on
    }

    private void throwOperationException(GraphEventEnvelope response) throws CrudException {
        throw new CrudException(
                GraphEventResponseMessage.OPERATION_ERROR_EXCEPTION_MESSAGE
                        .getMessage(response.getBody().getTransactionId(), response.getBody().getErrorMessage()),
                response.getBody().getHttpErrorStatus());
    }

    private boolean isErrorResponse(GraphEvent response) {
        return GraphEventResult.FAILURE.equals(response.getResult());
    }
}
