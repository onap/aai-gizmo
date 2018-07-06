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
package org.onap.crud.service;

import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.crud.event.GraphEvent;
import org.onap.crud.event.envelope.GraphEventEnvelope;
import org.onap.crud.logging.CrudServiceMsgs;


public class GraphEventUpdater {

    private static Logger logger = LoggerFactory.getInstance().getLogger(GraphEventUpdater
        .class.getName());

    private static Logger auditLogger = LoggerFactory.getInstance()
        .getAuditLogger(GraphEventUpdater.class.getName());

    public void update(String eventAsJson) {
        try {

            GraphEventEnvelope graphEventEnvelope = GraphEventEnvelope.fromJson(eventAsJson);
            GraphEvent graphEvent = graphEventEnvelope.getBody();
            auditLogger.info(CrudServiceMsgs.ASYNC_RESPONSE_CONSUMER_INFO,
                "Event received of type: " + graphEvent.getObjectType() + " with key: "
                    + graphEvent.getObjectKey() + " , transaction-id: "
                    + graphEvent.getTransactionId() + " , operation: "
                    + graphEvent.getOperation().toString());
            logger.info(CrudServiceMsgs.ASYNC_RESPONSE_CONSUMER_INFO,
                "Event received of type: " + graphEvent.getObjectType() + " with key: "
                    + graphEvent.getObjectKey() + " , transaction-id: "
                    + graphEvent.getTransactionId() + " , operation: "
                    + graphEvent.getOperation().toString());
            logger.debug(CrudServiceMsgs.ASYNC_RESPONSE_CONSUMER_INFO,
                "Event received with payload:" + eventAsJson);

            if (CrudAsyncGraphEventCache.get(graphEvent.getTransactionId()) != null) {
                CrudAsyncGraphEventCache.get(graphEvent.getTransactionId())
                    .populateGraphEventEnvelope(graphEventEnvelope);
            } else {
                logger.error(CrudServiceMsgs.ASYNC_DATA_SERVICE_ERROR,
                    "Request timed out. Not sending response for transaction-id: "
                        + graphEvent.getTransactionId());
            }

        } catch (Exception e) {
            logger.error(CrudServiceMsgs.ASYNC_RESPONSE_CONSUMER_ERROR, e.getMessage());
        }
    }
}
