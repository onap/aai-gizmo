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

import com.att.ecomp.event.api.EventConsumer;
import com.att.ecomp.event.api.EventPublisher;

import org.onap.aai.cl.api.LogFields;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.cl.mdc.MdcContext;
import org.onap.aai.cl.mdc.MdcOverride;
import org.onap.crud.dao.GraphDao;
import org.onap.crud.entity.Edge;
import org.onap.crud.entity.Vertex;
import org.onap.crud.event.GraphEvent;
import org.onap.crud.event.GraphEvent.GraphEventOperation;
import org.onap.crud.event.GraphEvent.GraphEventResult;
import org.onap.crud.event.GraphEventEdge;
import org.onap.crud.event.GraphEventVertex;
import org.onap.crud.exception.CrudException;
import org.onap.crud.logging.CrudServiceMsgs;
import org.onap.crud.parser.CrudResponseBuilder;
import org.onap.crud.util.CrudProperties;
import org.onap.crud.util.CrudServiceConstants;
import org.onap.schema.OxmModelValidator;
import org.onap.schema.RelationshipSchemaValidator;

import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.PreDestroy;
import javax.ws.rs.core.Response.Status;

public class CrudAsyncGraphDataService extends AbstractGraphDataService {

  private static Integer requestTimeOut;

  private EventPublisher asyncRequestPublisher;

  private Timer timer;

  public static final Integer DEFAULT_REQUEST_TIMEOUT = 30000;
  private static final Integer DEFAULT_ASYNC_RESPONSE_PROCESS_POLL_INTERVAL = 1000;

  private static Logger logger = LoggerFactory.getInstance()
    .getLogger(CrudAsyncGraphDataService.class.getName());
  private static Logger metricsLogger = LoggerFactory.getInstance()
    .getMetricsLogger(CrudAsyncGraphDataService.class.getName());
  private static LogFields OK_FIELDS = new LogFields();

  static {
		OK_FIELDS.setField(Status.OK, Status.OK.toString());
  }

  public static Integer getRequestTimeOut() {
    return requestTimeOut;
  }

  public CrudAsyncGraphDataService(GraphDao dao, 
		  EventPublisher asyncRequestPublisher,
		  EventConsumer asyncResponseConsumer) throws CrudException {

     super(dao);
     
    requestTimeOut = DEFAULT_REQUEST_TIMEOUT;
    try {
      requestTimeOut
        = Integer.parseInt(CrudProperties.get(CrudServiceConstants.CRD_ASYNC_REQUEST_TIMEOUT));
    } catch (NumberFormatException ex) {
      // Leave it as the default
    }

    Integer responsePollInterval = DEFAULT_ASYNC_RESPONSE_PROCESS_POLL_INTERVAL;
    try {
      responsePollInterval = Integer
        .parseInt(CrudProperties
                  .get(CrudServiceConstants.CRD_ASYNC_RESPONSE_PROCESS_POLL_INTERVAL));
    } catch (Exception ex) {
      logger.error(CrudServiceMsgs.ASYNC_DATA_SERVICE_ERROR, "Unable to parse "
                   + CrudServiceConstants.CRD_ASYNC_RESPONSE_PROCESS_POLL_INTERVAL
                   + " error: " + ex.getMessage());
    }

    // Start the Response Consumer timer
    CrudAsyncResponseConsumer crudAsyncResponseConsumer
      = new CrudAsyncResponseConsumer(asyncResponseConsumer);
    timer = new Timer("crudAsyncResponseConsumer-1");
    timer.schedule(crudAsyncResponseConsumer, responsePollInterval, responsePollInterval);

    this.asyncRequestPublisher = asyncRequestPublisher;
    
    logger.info(CrudServiceMsgs.ASYNC_DATA_SERVICE_INFO,
                "CrudAsyncGraphDataService initialized SUCCESSFULLY!");
  }

  public class CollectGraphResponse implements Callable<GraphEvent> {
    private volatile GraphEvent graphEvent;
    private volatile CountDownLatch latch = new CountDownLatch(1);

    @Override
    public GraphEvent call() throws TimeoutException {
      try {
        // Wait until graphEvent is available
        latch.await(CrudAsyncGraphDataService.getRequestTimeOut(), TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        latch.countDown();
        if (this.graphEvent != null) {
          return this.graphEvent;
        } else {
          throw new TimeoutException();
        }
      }
      return this.graphEvent;
    }

    public void populateGraphEvent(GraphEvent event) {
      this.graphEvent = event;
      latch.countDown();
    }
  }

  private GraphEvent sendAndWait(GraphEvent event) throws CrudException {

    long startTimeInMs = System.currentTimeMillis();
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    MdcOverride override = new MdcOverride();
    override.addAttribute(MdcContext.MDC_START_TIME, formatter.format(startTimeInMs));

    // publish to request queue
    try {
      asyncRequestPublisher.sendSync(event.toJson());
    } catch (Exception e) {
      throw new CrudException("Error publishing request " + event.getTransactionId() + "  Cause: " + e.getMessage(), Status.INTERNAL_SERVER_ERROR);
    }
    
    logger.debug(CrudServiceMsgs.ASYNC_DATA_SERVICE_INFO, "Event Sent ="+event.toJson());

    logger.info(CrudServiceMsgs.ASYNC_DATA_SERVICE_INFO,
                "Event submitted of type: " + event.getObjectType() + " with key: " + event.getObjectKey()
                + " , transaction-id: " + event.getTransactionId() + " , operation: "
                + event.getOperation().toString());

    ExecutorService executor = Executors
      .newSingleThreadExecutor(new CrudThreadFactory("TX-" + event.getTransactionId()));
    CollectGraphResponse collector = new CollectGraphResponse();
    CrudAsyncGraphEventCache.put(event.getTransactionId(), collector);
    GraphEvent response;
    Future<GraphEvent> future = executor.submit(collector);
    try {
      response = future.get(requestTimeOut, TimeUnit.MILLISECONDS);

    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      CrudAsyncGraphEventCache.invalidate(event.getTransactionId());
      logger.error(CrudServiceMsgs.ASYNC_DATA_SERVICE_ERROR,
                   "Request timed out for transactionId: " + event.getTransactionId());
      future.cancel(true);
      throw new CrudException("Timed out , transactionId: " + event.getTransactionId()
                              + " , operation: " + event.getOperation().toString(), Status.INTERNAL_SERVER_ERROR);
    } finally {      
      //Kill the thread as the work is completed
      executor.shutdownNow();
    }
    metricsLogger.info(CrudServiceMsgs.ASYNC_DATA_SERVICE_INFO, OK_FIELDS, override,
            "Total elapsed time for operation: " + event.getOperation().toString()
            + " , transactionId: " + event.getTransactionId() + " is "
            + Long.toString(System.currentTimeMillis() - startTimeInMs) + " ms");
    return response;
  }

  public String addVertex(String version, String type, VertexPayload payload) throws CrudException {
    // Validate the incoming payload
    Vertex vertex = OxmModelValidator.validateIncomingUpsertPayload(null, version,
                                                                    type, payload.getProperties());
    // Create graph request event
    GraphEvent event = GraphEvent.builder(GraphEventOperation.CREATE)
      .vertex(GraphEventVertex.fromVertex(vertex, version)).build();

    GraphEvent response = sendAndWait(event);
    if (response.getResult().equals(GraphEventResult.SUCCESS)) {
      logger.info(CrudServiceMsgs.ASYNC_DATA_SERVICE_INFO,
                  "Event response received: " + response.getObjectType() + " with key: "
                  + response.getObjectKey() + " , transaction-id: " + response.getTransactionId()
                  + " , operation: " + event.getOperation().toString() + " , result: "
                  + response.getResult());
      return CrudResponseBuilder.buildUpsertVertexResponse(
                                                           OxmModelValidator.validateOutgoingPayload(version,
                                                                                                     response.getVertex().toVertex()), version);
    } else {
      logger.info(CrudServiceMsgs.ASYNC_DATA_SERVICE_INFO,
                  "Event response received: " + response.getObjectType() + " with key: "
                  + response.getObjectKey() + " , transaction-id: " + response.getTransactionId()
                  + " , operation: " + event.getOperation().toString() + " , result: "
                  + response.getResult() + " , error: " + response.getErrorMessage());
      throw new CrudException("Operation Failed with transaction-id: " + response.getTransactionId()
                              + " Error: " + response.getErrorMessage(), response.getHttpErrorStatus());
    }

  }

  public String addEdge(String version, String type, EdgePayload payload) throws CrudException {
    Edge edge = RelationshipSchemaValidator.validateIncomingAddPayload(version, type, payload);
    // Create graph request event
    GraphEvent event = GraphEvent.builder(GraphEventOperation.CREATE)
      .edge(GraphEventEdge.fromEdge(edge, version)).build();

    GraphEvent response = sendAndWait(event);
    if (response.getResult().equals(GraphEventResult.SUCCESS)) {
      logger.info(CrudServiceMsgs.ASYNC_DATA_SERVICE_INFO,
                  "Event response received: " + response.getObjectType() + " with key: "
                  + response.getObjectKey() + " , transaction-id: " + response.getTransactionId()
                  + " , operation: " + event.getOperation().toString() + " , result: "
                  + response.getResult());
      return CrudResponseBuilder.buildUpsertEdgeResponse(
                                                         RelationshipSchemaValidator.validateOutgoingPayload(version, response.getEdge().toEdge()),
                                                         version);
    } else {
      logger.info(CrudServiceMsgs.ASYNC_DATA_SERVICE_INFO,
                  "Event response received: " + response.getObjectType() + " with key: "
                  + response.getObjectKey() + " , transaction-id: " + response.getTransactionId()
                  + " , operation: " + event.getOperation().toString() + " , result: "
                  + response.getResult() + " , error: " + response.getErrorMessage());
      throw new CrudException("Operation Failed with transaction-id: " + response.getTransactionId()
                              + " Error: " + response.getErrorMessage(), response.getHttpErrorStatus());
    }
  }

  public String updateVertex(String version, String id, String type, VertexPayload payload)
    throws CrudException {
    Vertex vertex = OxmModelValidator.validateIncomingUpsertPayload(id, version,
                                                                    type, payload.getProperties());
    GraphEvent event = GraphEvent.builder(GraphEventOperation.UPDATE)
      .vertex(GraphEventVertex.fromVertex(vertex, version)).build();

    GraphEvent response = sendAndWait(event);
    if (response.getResult().equals(GraphEventResult.SUCCESS)) {
      logger.info(CrudServiceMsgs.ASYNC_DATA_SERVICE_INFO,
                  "Event response received: " + response.getObjectType() + " with key: "
                  + response.getObjectKey() + " , transaction-id: " + response.getTransactionId()
                  + " , operation: " + event.getOperation().toString() + " , result: "
                  + response.getResult());
      return CrudResponseBuilder.buildUpsertVertexResponse(
                                                           OxmModelValidator.validateOutgoingPayload(version, response.getVertex().toVertex()),
                                                           version);
    } else {
      logger.info(CrudServiceMsgs.ASYNC_DATA_SERVICE_INFO,
                  "Event response received: " + response.getObjectType() + " with key: "
                  + response.getObjectKey() + " , transaction-id: " + response.getTransactionId()
                  + " , operation: " + event.getOperation().toString() + " , result: "
                  + response.getResult() + " , error: " + response.getErrorMessage());
      throw new CrudException("Operation Failed with transaction-id: " + response.getTransactionId()
                              + " Error: " + response.getErrorMessage(), response.getHttpErrorStatus());
    }

  }

  public String patchVertex(String version, String id, String type, VertexPayload payload)
    throws CrudException {
    Vertex existingVertex
      = dao.getVertex(id, OxmModelValidator.resolveCollectionType(version, type));
    Vertex patchedVertex = OxmModelValidator.validateIncomingPatchPayload(id, version,
                                                                          type, payload.getProperties(),
                                                                          existingVertex);
    GraphEvent event = GraphEvent.builder(GraphEventOperation.UPDATE)
      .vertex(GraphEventVertex.fromVertex(patchedVertex, version)).build();

    GraphEvent response = sendAndWait(event);
    if (response.getResult().equals(GraphEventResult.SUCCESS)) {
      logger.info(CrudServiceMsgs.ASYNC_DATA_SERVICE_INFO,
                  "Event response received: " + response.getObjectType() + " with key: "
                  + response.getObjectKey() + " , transaction-id: " + response.getTransactionId()
                  + " , operation: " + event.getOperation().toString() + " , result: "
                  + response.getResult());
      return CrudResponseBuilder.buildUpsertVertexResponse(
                                                           OxmModelValidator.validateOutgoingPayload(version, response.getVertex().toVertex()),
                                                           version);
    } else {
      logger.info(CrudServiceMsgs.ASYNC_DATA_SERVICE_INFO,
                  "Event response received: " + response.getObjectType() + " with key: "
                  + response.getObjectKey() + " , transaction-id: " + response.getTransactionId()
                  + " , operation: " + event.getOperation().toString() + " , result: "
                  + response.getResult() + " , error: " + response.getErrorMessage());
      throw new CrudException("Operation Failed with transaction-id: " + response.getTransactionId()
                              + " Error: " + response.getErrorMessage(), response.getHttpErrorStatus());
    }

  }

  public String deleteVertex(String version, String id, String type) throws CrudException {
    type = OxmModelValidator.resolveCollectionType(version, type);
    GraphEvent event = GraphEvent.builder(GraphEventOperation.DELETE)
      .vertex(new GraphEventVertex(id, version, type, null)).build();

    GraphEvent response = sendAndWait(event);
    if (response.getResult().equals(GraphEventResult.SUCCESS)) {
      logger.info(CrudServiceMsgs.ASYNC_DATA_SERVICE_INFO,
                  "Event response received: " + response.getObjectType() + " with key: "
                  + response.getObjectKey() + " , transaction-id: " + response.getTransactionId()
                  + " , operation: " + event.getOperation().toString() + " , result: "
                  + response.getResult());
      return "";
    } else {
      logger.info(CrudServiceMsgs.ASYNC_DATA_SERVICE_INFO,
                  "Event response received: " + response.getObjectType() + " with key: "
                  + response.getObjectKey() + " , transaction-id: " + response.getTransactionId()
                  + " , operation: " + event.getOperation().toString() + " , result: "
                  + response.getResult() + " , error: " + response.getErrorMessage());
      throw new CrudException("Operation Failed with transaction-id: " + response.getTransactionId()
                              + " Error: " + response.getErrorMessage(), response.getHttpErrorStatus());
    }

  }

  public String deleteEdge(String version, String id, String type) throws CrudException {
    RelationshipSchemaValidator.validateType(version, type);
    GraphEvent event = GraphEvent.builder(GraphEventOperation.DELETE)
      .edge(new GraphEventEdge(id, version, type, null, null, null)).build();

    GraphEvent response = sendAndWait(event);
    if (response.getResult().equals(GraphEventResult.SUCCESS)) {
      logger.info(CrudServiceMsgs.ASYNC_DATA_SERVICE_INFO,
                  "Event response received: " + response.getObjectType() + " with key: "
                  + response.getObjectKey() + " , transaction-id: " + response.getTransactionId()
                  + " , operation: " + event.getOperation().toString() + " , result: "
                  + response.getResult());
      return "";
    } else {
      logger.info(CrudServiceMsgs.ASYNC_DATA_SERVICE_INFO,
                  "Event response received: " + response.getObjectType() + " with key: "
                  + response.getObjectKey() + " , transaction-id: " + response.getTransactionId()
                  + " , operation: " + event.getOperation().toString() + " , result: "
                  + response.getResult() + " , error: " + response.getErrorMessage());
      throw new CrudException("Operation Failed with transaction-id: " + response.getTransactionId()
                              + " Error: " + response.getErrorMessage(), response.getHttpErrorStatus());
    }

  }

  public String updateEdge(String version, String id, String type, EdgePayload payload)
    throws CrudException {
    Edge edge = dao.getEdge(id, type);
    Edge validatedEdge = RelationshipSchemaValidator.validateIncomingUpdatePayload(edge, version,
                                                                                   payload);
    GraphEvent event = GraphEvent.builder(GraphEventOperation.UPDATE)
      .edge(GraphEventEdge.fromEdge(validatedEdge, version)).build();

    GraphEvent response = sendAndWait(event);
    if (response.getResult().equals(GraphEventResult.SUCCESS)) {
      logger.info(CrudServiceMsgs.ASYNC_DATA_SERVICE_INFO,
                  "Event response received: " + response.getObjectType() + " with key: "
                  + response.getObjectKey() + " , transaction-id: " + response.getTransactionId()
                  + " , operation: " + event.getOperation().toString() + " , result: "
                  + response.getResult());
      return CrudResponseBuilder.buildUpsertEdgeResponse(
                                                         RelationshipSchemaValidator.validateOutgoingPayload(version,
                                                                                                             response.getEdge().toEdge()), version);
    } else {
      logger.info(CrudServiceMsgs.ASYNC_DATA_SERVICE_INFO,
                  "Event response received: " + response.getObjectType() + " with key: "
                  + response.getObjectKey() + " , transaction-id: " + response.getTransactionId()
                  + " , operation: " + event.getOperation().toString() + " , result: "
                  + response.getResult() + " , error: " + response.getErrorMessage());
      throw new CrudException("Operation Failed with transaction-id: " + response.getTransactionId()
                              + " Error: " + response.getErrorMessage(), response.getHttpErrorStatus());
    }

  }

  public String patchEdge(String version, String id, String type, EdgePayload payload)
    throws CrudException {
    Edge edge = dao.getEdge(id, type);
    Edge patchedEdge = RelationshipSchemaValidator.validateIncomingPatchPayload(edge, version,
                                                                                payload);
    GraphEvent event = GraphEvent.builder(GraphEventOperation.UPDATE)
      .edge(GraphEventEdge.fromEdge(patchedEdge, version)).build();

    GraphEvent response = sendAndWait(event);
    if (response.getResult().equals(GraphEventResult.SUCCESS)) {
      logger.info(CrudServiceMsgs.ASYNC_DATA_SERVICE_INFO,
                  "Event response received: " + response.getObjectType() + " with key: "
                  + response.getObjectKey() + " , transaction-id: " + response.getTransactionId()
                  + " , operation: " + event.getOperation().toString() + " , result: "
                  + response.getResult());
      return CrudResponseBuilder.buildUpsertEdgeResponse(
                                                         RelationshipSchemaValidator.validateOutgoingPayload(version, response.getEdge().toEdge()),
                                                         version);
    } else {
      logger.info(CrudServiceMsgs.ASYNC_DATA_SERVICE_INFO,
                  "Event response received: " + response.getObjectType() + " with key: "
                  + response.getObjectKey() + " , transaction-id: " + response.getTransactionId()
                  + " , operation: " + event.getOperation().toString() + " , result: "
                  + response.getResult() + " , error: " + response.getErrorMessage());
      throw new CrudException("Operation Failed with transaction-id: " + response.getTransactionId()
                              + " Error: " + response.getErrorMessage(), response.getHttpErrorStatus());
    }

  }

  @PreDestroy
  protected void preShutdown() {
    timer.cancel();

  }

  @Override
  public String addBulk(String version, BulkPayload payload) throws CrudException {
    throw new CrudException("Bulk operation not supported in async mode", Status.BAD_REQUEST);
  }


}