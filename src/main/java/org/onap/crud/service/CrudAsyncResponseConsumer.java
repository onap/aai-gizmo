/**
 * ﻿============LICENSE_START=======================================================
 * Gizmo
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property.
 * Copyright © 2017 Amdocs
 * All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 *
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.onap.crud.service;

import java.util.TimerTask;

import javax.naming.OperationNotSupportedException;

import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.crud.event.GraphEvent;
import org.onap.crud.logging.CrudServiceMsgs;

import com.att.ecomp.event.api.EventConsumer;

public class CrudAsyncResponseConsumer extends TimerTask {

  private static Logger logger = LoggerFactory.getInstance().getLogger(CrudAsyncResponseConsumer
                                                                       .class.getName());

  private static Logger auditLogger = LoggerFactory.getInstance()
    .getAuditLogger(CrudAsyncResponseConsumer.class.getName());

  private EventConsumer asyncResponseConsumer;

 
  public CrudAsyncResponseConsumer(EventConsumer asyncResponseConsumer) {
    this.asyncResponseConsumer = asyncResponseConsumer;
    logger.info(CrudServiceMsgs.ASYNC_RESPONSE_CONSUMER_INFO,
                "CrudAsyncResponseConsumer initialized SUCCESSFULLY! with event consumer "
                + asyncResponseConsumer.getClass().getName());
  }


  @Override
  public void run() {

    logger.info(CrudServiceMsgs.ASYNC_RESPONSE_CONSUMER_INFO, "Listening for graph events");

    if (asyncResponseConsumer == null) {
      logger.error(CrudServiceMsgs.ASYNC_RESPONSE_CONSUMER_ERROR,
                   "Unable to initialize CrudAsyncRequestProcessor");
    }

    Iterable<String> events = null;
    try {
      events = asyncResponseConsumer.consume();
    } catch (Exception e) {
      logger.error(CrudServiceMsgs.ASYNC_RESPONSE_CONSUMER_ERROR, e.getMessage());
      return;
    }

    if (events == null || !events.iterator().hasNext()) {
      logger.info(CrudServiceMsgs.ASYNC_RESPONSE_CONSUMER_INFO, "No events recieved");

    }

    for (String event : events) {
      try {

        GraphEvent graphEvent = GraphEvent.fromJson(event);
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
                       "Event received with payload:" + event);

        if (CrudAsyncGraphEventCache.get(graphEvent.getTransactionId()) != null) {
          CrudAsyncGraphEventCache.get(graphEvent.getTransactionId())
            .populateGraphEvent(graphEvent);
        } else {
          logger.error(CrudServiceMsgs.ASYNC_DATA_SERVICE_ERROR,
                       "Request timed out. Not sending response for transaction-id: "
                       + graphEvent.getTransactionId());
        }

      } catch (Exception e) {
        logger.error(CrudServiceMsgs.ASYNC_RESPONSE_CONSUMER_ERROR, e.getMessage());
      }
    }

    try {
      asyncResponseConsumer.commitOffsets();
    }
    catch(OperationNotSupportedException e) {
        //Dmaap doesnt support commit with offset    
        logger.debug(CrudServiceMsgs.ASYNC_RESPONSE_CONSUMER_ERROR, e.getMessage());
    }
    catch (Exception e) {
      logger.error(CrudServiceMsgs.ASYNC_RESPONSE_CONSUMER_ERROR, e.getMessage());
    }

  }

}