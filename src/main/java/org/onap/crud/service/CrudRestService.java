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

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.cxf.jaxrs.ext.PATCH;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aaiauth.auth.Auth;
import org.onap.crud.exception.CrudException;
import org.onap.crud.logging.CrudServiceMsgs;
import org.onap.crud.logging.LoggingUtil;
import org.onap.crud.parser.BulkPayload;
import org.onap.crud.parser.EdgePayload;
import org.onap.crud.parser.VertexPayload;
import org.onap.crud.util.CrudProperties;
import org.onap.crud.util.CrudServiceConstants;
import org.onap.crud.util.CrudServiceUtil;
import org.slf4j.MDC;

import com.google.gson.JsonElement;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/inventory")
public class CrudRestService {

    private AbstractGraphDataService graphDataService;
    Logger logger = LoggerFactory.getInstance().getLogger(CrudRestService.class.getName());
    Logger auditLogger = LoggerFactory.getInstance().getAuditLogger(CrudRestService.class.getName());
    private Auth auth;

    private String mediaType = MediaType.APPLICATION_JSON;
    public static final String HTTP_PATCH_METHOD_OVERRIDE = "X-HTTP-Method-Override";
    public static final String TRANSACTIONID_HEADER = "X-TransactionId";

    public CrudRestService(AbstractGraphDataService graphDataService) throws Exception {
        this.graphDataService = graphDataService;
        this.auth = new Auth(CrudServiceConstants.CRD_AUTH_FILE);
    }

    // For unit testing
    public CrudRestService(AbstractGraphDataService graphDataService, Auth auth) throws Exception {
        this.graphDataService = graphDataService;
        this.auth = auth;
    }

    public enum Action {
        POST, GET, PUT, DELETE, PATCH
    }

    public void startup() {

    }
    
    @ApiOperation(value = "Get Vertex" , notes="For example : https://<host>:9520/services/inventory/v11/pserver/<id>")
    @ApiResponses({    
      @ApiResponse(code = 200, message = "Success"),
      @ApiResponse(code = 403, message = "Forbidden"),    
      @ApiResponse(code = 404, message = "Not Found"),
      @ApiResponse(code = 500, message = "Internal Server Error") })
    @ApiImplicitParams({
      @ApiImplicitParam(name = "X-FromAppId", required = true, dataType = "string", paramType = "header"),
      @ApiImplicitParam(name = "X-TransactionId",  required = true, dataType = "string", paramType = "header"),    
    })
    @GET
    @Path("/{version}/{type}/{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response getVertex(@ApiParam(hidden=true) String content, @PathParam("version") @ApiParam(value="oxm model version",defaultValue="v13") String version, @PathParam("type") String type,
            @PathParam("id") String id, @PathParam("uri") @Encoded @ApiParam(hidden=true) String uri, @Context HttpHeaders headers,
            @Context UriInfo uriInfo, @Context HttpServletRequest req) {

        LoggingUtil.initMdcContext(req, headers);
        logger.debug("Incoming request..." + content);

        ResponseBuilder responseBuilder;
        Map<String, String> params = addParams(uriInfo, false, type, version);

        try {
            if (validateRequest(req, uri, content, Action.GET, CrudServiceConstants.CRD_AUTH_POLICY_NAME, headers)) {
                ImmutablePair<EntityTag, String> result = graphDataService.getVertex(version, id, type, params);
                responseBuilder =
                        Response.status(Status.OK).entity(result.getValue()).tag(result.getKey()).type(mediaType);
            } else {
                responseBuilder = Response.status(Status.FORBIDDEN).entity(content).type(MediaType.APPLICATION_JSON);
            }
        } catch (CrudException ce) {
            responseBuilder = Response.status(ce.getHttpStatus()).entity(ce.getMessage());
        } catch (Exception e) {
            responseBuilder = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage());

        }

        setTxIdOnResponseHeader(headers, responseBuilder);

        Response response = responseBuilder.build();
        LoggingUtil.logRestRequest(logger, auditLogger, req, response);
        return response;
    }

    @ApiOperation(value = "Get Vertices" , notes="For example : https://<host>:9520/services/inventory/v11/pserver/")
    @ApiResponses({    
      @ApiResponse(code = 200, message = "Success"),
      @ApiResponse(code = 403, message = "Forbidden"),    
      @ApiResponse(code = 404, message = "Not Found"),
      @ApiResponse(code = 500, message = "Internal Server Error") })
    @ApiImplicitParams({
      @ApiImplicitParam(name = "X-FromAppId", required = true, dataType = "string", paramType = "header"),    
      @ApiImplicitParam(name = "X-TransactionId",  required = true, dataType = "string", paramType = "header"),    
    })
    @GET
    @Path("/{version}/{type}/")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response getVertices(@ApiParam(hidden=true) String content, @PathParam("version") @ApiParam(value="oxm model version",defaultValue="v13") String version, @PathParam("type") String type,
            @PathParam("uri") @Encoded @ApiParam(hidden=true) String uri, @Context HttpHeaders headers, @Context UriInfo uriInfo,
            @Context HttpServletRequest req) {

        LoggingUtil.initMdcContext(req, headers);
        logger.debug("Incoming request..." + content);

        ResponseBuilder responseBuilder;
        try {
            if (validateRequest(req, uri, content, Action.GET, CrudServiceConstants.CRD_AUTH_POLICY_NAME, headers)) {
                String propertiesKey = CrudProperties.get(CrudServiceConstants.CRD_COLLECTION_PROPERTIES_KEY);
                Map<String, String> filter = addParams(uriInfo, true, type, version);

                HashSet<String> properties;
                if (uriInfo.getQueryParameters().containsKey(propertiesKey)) {
                    properties = new HashSet<>(uriInfo.getQueryParameters().get(propertiesKey));
                } else {
                    properties = new HashSet<>();
                }

                ImmutablePair<EntityTag, String> result =
                        graphDataService.getVertices(version, type, filter, properties);
                responseBuilder =
                        Response.status(Status.OK).entity(result.getValue()).tag(result.getKey()).type(mediaType);
            } else {
                responseBuilder = Response.status(Status.FORBIDDEN).entity(content).type(MediaType.APPLICATION_JSON);
            }
        } catch (CrudException ce) {
            responseBuilder = Response.status(ce.getHttpStatus()).entity(ce.getMessage());
        } catch (Exception e) {
            responseBuilder = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage());

        }

        setTxIdOnResponseHeader(headers, responseBuilder);

        Response response = responseBuilder.build();
        LoggingUtil.logRestRequest(logger, auditLogger, req, response);
        return response;
    }

    @ApiOperation(value = "Get Edge" , notes="For example : https://<host>:9520/services/inventory/relationships/v11/tosca.relationships.HostedOn/<id>")
    @ApiResponses({    
      @ApiResponse(code = 200, message = "Success"),
      @ApiResponse(code = 403, message = "Forbidden"),    
      @ApiResponse(code = 404, message = "Not Found"),
      @ApiResponse(code = 500, message = "Internal Server Error") })
    @ApiImplicitParams({
      @ApiImplicitParam(name = "X-FromAppId", required = true, dataType = "string", paramType = "header"),
      @ApiImplicitParam(name = "X-TransactionId",  required = true, dataType = "string", paramType = "header"),    
    })
    @GET
    @Path("/relationships/{version}/{type}/{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response getEdge(@ApiParam(hidden=true) String content, @PathParam("version") @ApiParam(value="oxm model version",defaultValue="v13") String version, @PathParam("type") String type,
            @PathParam("id") String id, @PathParam("uri") @Encoded @ApiParam(hidden=true) String uri, @Context HttpHeaders headers,
            @Context UriInfo uriInfo, @Context HttpServletRequest req) {

        LoggingUtil.initMdcContext(req, headers);
        logger.debug("Incoming request..." + content);

        ResponseBuilder responseBuilder;
        Map<String, String> params = addParams(uriInfo, false, type, version);

        try {
            if (validateRequest(req, uri, content, Action.GET, CrudServiceConstants.CRD_AUTH_POLICY_NAME, headers)) {

                ImmutablePair<EntityTag, String> result = graphDataService.getEdge(version, id, type, params);
                responseBuilder =
                        Response.status(Status.OK).entity(result.getValue()).tag(result.getKey()).type(mediaType);
            } else {
                responseBuilder = Response.status(Status.FORBIDDEN).entity(content).type(MediaType.APPLICATION_JSON);
            }
        } catch (CrudException ce) {
            responseBuilder = Response.status(ce.getHttpStatus()).entity(ce.getMessage());
        } catch (Exception e) {
            responseBuilder = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage());
        }

        setTxIdOnResponseHeader(headers, responseBuilder);

        Response response = responseBuilder.build();
        LoggingUtil.logRestRequest(logger, auditLogger, req, response);
        return response;
    }

    @ApiOperation(value = "Get Edges" , notes="For example : https://<host>:9520/services/inventory/relationships/v11/tosca.relationships.HostedOn/")
    @ApiResponses({    
      @ApiResponse(code = 200, message = "Success"),
      @ApiResponse(code = 403, message = "Forbidden"),    
      @ApiResponse(code = 404, message = "Not Found"),
      @ApiResponse(code = 500, message = "Internal Server Error") })
    @ApiImplicitParams({
      @ApiImplicitParam(name = "X-FromAppId", required = true, dataType = "string", paramType = "header"),
      @ApiImplicitParam(name = "X-TransactionId",  required = true, dataType = "string", paramType = "header"),    
    })
    @GET
    @Path("/relationships/{version}/{type}/")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response getEdges(@ApiParam(hidden=true) String content, @PathParam("version") @ApiParam(value="oxm model version",defaultValue="v13") String version, @PathParam("type") String type,
            @PathParam("uri") @Encoded @ApiParam(hidden=true) String uri, @Context HttpHeaders headers, @Context UriInfo uriInfo,
            @Context HttpServletRequest req) {

        LoggingUtil.initMdcContext(req, headers);
        logger.debug("Incoming request..." + content);

        ResponseBuilder responseBuilder;
        Map<String, String> filter = addParams(uriInfo, true, type, version);

        try {
            if (validateRequest(req, uri, content, Action.GET, CrudServiceConstants.CRD_AUTH_POLICY_NAME, headers)) {
                ImmutablePair<EntityTag, String> result = graphDataService.getEdges(version, type, filter);
                responseBuilder =
                        Response.status(Status.OK).entity(result.getValue()).tag(result.getKey()).type(mediaType);
            } else {
                responseBuilder = Response.status(Status.FORBIDDEN).entity(content).type(MediaType.APPLICATION_JSON);
            }
        } catch (CrudException ce) {
            responseBuilder = Response.status(ce.getHttpStatus()).entity(ce.getMessage());
        } catch (Exception e) {
            responseBuilder = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage());
        }

        setTxIdOnResponseHeader(headers, responseBuilder);

        Response response = responseBuilder.build();
        LoggingUtil.logRestRequest(logger, auditLogger, req, response);
        return response;
    }

    @ApiOperation(value = "Update Edge" , notes = "# Payload \n"
        + "{  \r\n" + 
        "   \"properties\":{  \r\n" + 
        "        \"prop1\" : \"value\",\r\n" + 
        "        \"prop2\" :\"value\",\r\n" +  
        "    ..}\r\n" + 
        " }")
    @ApiResponses({    
      @ApiResponse(code = 200, message = "Success"),
      @ApiResponse(code = 403, message = "Forbidden"),    
      @ApiResponse(code = 404, message = "Not Found"),
      @ApiResponse(code = 400, message = "Bad Request"),
      @ApiResponse(code = 500, message = "Internal Server Error") })
    @ApiImplicitParams({
      @ApiImplicitParam(name = "X-FromAppId", required = true, dataType = "string", paramType = "header"),
      @ApiImplicitParam(name = "X-TransactionId",  required = true, dataType = "string", paramType = "header"),    
    })
    @PUT
    @Path("/relationships/{version}/{type}/{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateEdge(String content, @PathParam("version") @ApiParam(value="oxm model version",defaultValue="v13") String version, @PathParam("type") String type,
            @PathParam("id") String id, @PathParam("uri") @Encoded @ApiParam(hidden=true) String uri, @Context HttpHeaders headers,
            @Context UriInfo uriInfo, @Context HttpServletRequest req) {

        LoggingUtil.initMdcContext(req, headers);
        logger.debug("Incoming request..." + content);

        ResponseBuilder responseBuilder;

        try {
            if (validateRequest(req, uri, content, Action.PUT, CrudServiceConstants.CRD_AUTH_POLICY_NAME, headers)) {
                EdgePayload payload = EdgePayload.fromJson(content);
                if (payload.getProperties() == null || payload.getProperties().isJsonNull()) {
                    throw new CrudException("Invalid request Payload", Status.BAD_REQUEST);
                }
                if (payload.getId() != null && !payload.getId().equals(id)) {
                    throw new CrudException("ID Mismatch", Status.BAD_REQUEST);
                }
                ImmutablePair<EntityTag, String> result;
                if (headers.getRequestHeaders().getFirst(HTTP_PATCH_METHOD_OVERRIDE) != null
                        && headers.getRequestHeaders().getFirst(HTTP_PATCH_METHOD_OVERRIDE).equalsIgnoreCase("PATCH")) {
                    result = graphDataService.patchEdge(version, id, type, payload);
                    responseBuilder =
                            Response.status(Status.OK).entity(result.getValue()).type(mediaType).tag(result.getKey());
                } else {
                    result = graphDataService.updateEdge(version, id, type, payload);
                    responseBuilder =
                            Response.status(Status.OK).entity(result.getValue()).type(mediaType).tag(result.getKey());
                }

            } else {
                responseBuilder = Response.status(Status.FORBIDDEN).entity(content).type(MediaType.APPLICATION_JSON);
            }
        } catch (CrudException ce) {
            responseBuilder = Response.status(ce.getHttpStatus()).entity(ce.getMessage());
        } catch (Exception e) {
            responseBuilder = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage());
        }

        setTxIdOnResponseHeader(headers, responseBuilder);

        Response response = responseBuilder.build();
        LoggingUtil.logRestRequest(logger, auditLogger, req, response);
        return response;
    }

    @ApiOperation(value = "Patch Edge" , notes = "# Payload \n"
        + "{  \r\n" + 
        "   \"properties\":{  \r\n" + 
        "        \"prop1\" : \"value\",\r\n" + 
        "        \"prop2\" :\"value\",\r\n" +  
        "    ..}\r\n" + 
        " }")
    @ApiResponses({    
      @ApiResponse(code = 200, message = "Success"),
      @ApiResponse(code = 403, message = "Forbidden"), 
      @ApiResponse(code = 400, message = "Bad Request"),
      @ApiResponse(code = 404, message = "Not Found"),
      @ApiResponse(code = 500, message = "Internal Server Error") })
    @ApiImplicitParams({
      @ApiImplicitParam(name = "X-FromAppId", required = true, dataType = "string", paramType = "header"),
      @ApiImplicitParam(name = "X-TransactionId",  required = true, dataType = "string", paramType = "header"),    
    })
    @PATCH
    @Path("/relationships/{version}/{type}/{id}")
    @Consumes({"application/merge-patch+json"})
    @Produces({MediaType.APPLICATION_JSON})
    public Response patchEdge(String content, @PathParam("version") @ApiParam(value="oxm model version",defaultValue="v13") String version, @PathParam("type") String type,
            @PathParam("id") String id, @PathParam("uri") @Encoded @ApiParam(hidden=true) String uri, @Context HttpHeaders headers,
            @Context UriInfo uriInfo, @Context HttpServletRequest req) {

        LoggingUtil.initMdcContext(req, headers);
        logger.debug("Incoming request..." + content);

        ResponseBuilder responseBuilder;

        try {
            if (validateRequest(req, uri, content, Action.PATCH, CrudServiceConstants.CRD_AUTH_POLICY_NAME, headers)) {
                EdgePayload payload = EdgePayload.fromJson(content);
                if (payload.getProperties() == null || payload.getProperties().isJsonNull()) {
                    throw new CrudException("Invalid request Payload", Status.BAD_REQUEST);
                }
                if (payload.getId() != null && !payload.getId().equals(id)) {
                    throw new CrudException("ID Mismatch", Status.BAD_REQUEST);
                }

                ImmutablePair<EntityTag, String> result = graphDataService.patchEdge(version, id, type, payload);
                responseBuilder =
                        Response.status(Status.OK).entity(result.getValue()).type(mediaType).tag(result.getKey());
            } else {
                responseBuilder = Response.status(Status.FORBIDDEN).entity(content).type(MediaType.APPLICATION_JSON);
            }
        } catch (CrudException ce) {
            responseBuilder = Response.status(ce.getHttpStatus()).entity(ce.getMessage());
        } catch (Exception e) {
            responseBuilder = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage());
        }

        setTxIdOnResponseHeader(headers, responseBuilder);

        Response response = responseBuilder.build();
        LoggingUtil.logRestRequest(logger, auditLogger, req, response);
        return response;
    }

    @ApiOperation(value = "Update Vertex" , notes = "# Payload \n"
        + "{\r\n" + 
        "   \"type\" :\"vertex type from oxm like comcast.nodes.sdwan.vpn\",\r\n" + 
        "    \"properties\": {\r\n" + 
        "        \"prop1\" : \"value\",\r\n" + 
        "        \"prop2\" :\"value\",\r\n" +          
        "    ..}\r\n" + 
        " }")
    @ApiResponses({    
      @ApiResponse(code = 200, message = "Success"),
      @ApiResponse(code = 403, message = "Forbidden"), 
      @ApiResponse(code = 400, message = "Bad Request"),
      @ApiResponse(code = 404, message = "Not Found"),
      @ApiResponse(code = 500, message = "Internal Server Error") })
    @ApiImplicitParams({
      @ApiImplicitParam(name = "X-FromAppId", required = true, dataType = "string", paramType = "header"),
      @ApiImplicitParam(name = "X-TransactionId",  required = true, dataType = "string", paramType = "header"),    
    })
    @PUT
    @Path("/{version}/{type}/{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response updateVertex(String content, @PathParam("version") @ApiParam(value="oxm model version",defaultValue="v13") String version, @PathParam("type") String type,
            @PathParam("id") String id, @PathParam("uri") @Encoded @ApiParam(hidden=true) String uri, @Context HttpHeaders headers,
            @Context UriInfo uriInfo, @Context HttpServletRequest req) {

        LoggingUtil.initMdcContext(req, headers);
        logger.debug("Incoming request..." + content);

        ResponseBuilder responseBuilder;

        try {
            if (validateRequest(req, uri, content, Action.PUT, CrudServiceConstants.CRD_AUTH_POLICY_NAME, headers)) {
                VertexPayload payload = VertexPayload.fromJson(content);
                if (payload.getProperties() == null || payload.getProperties().isJsonNull()) {
                    throw new CrudException("Invalid request Payload", Status.BAD_REQUEST);
                }
                if (payload.getId() != null && !payload.getId().equals(id)) {
                    throw new CrudException("ID Mismatch", Status.BAD_REQUEST);
                }

                payload.setProperties(
                        CrudServiceUtil.mergeHeaderInFoToPayload(payload.getProperties(), headers, false));

                ImmutablePair<EntityTag, String> result;
                if (headers.getRequestHeaders().getFirst(HTTP_PATCH_METHOD_OVERRIDE) != null
                        && headers.getRequestHeaders().getFirst(HTTP_PATCH_METHOD_OVERRIDE).equalsIgnoreCase("PATCH")) {
                    result = graphDataService.patchVertex(version, id, type, payload);
                    responseBuilder =
                            Response.status(Status.OK).entity(result.getValue()).type(mediaType).tag(result.getKey());
                } else {
                    result = graphDataService.updateVertex(version, id, type, payload);
                    responseBuilder =
                            Response.status(Status.OK).entity(result.getValue()).type(mediaType).tag(result.getKey());
                }

            } else {
                responseBuilder = Response.status(Status.FORBIDDEN).entity(content).type(MediaType.APPLICATION_JSON);
            }
        } catch (CrudException ce) {
            responseBuilder = Response.status(ce.getHttpStatus()).entity(ce.getMessage());
        } catch (Exception e) {
            responseBuilder = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage());
        }

        setTxIdOnResponseHeader(headers, responseBuilder);

        Response response = responseBuilder.build();
        LoggingUtil.logRestRequest(logger, auditLogger, req, response);
        return response;
    }

    @ApiOperation(value = "Patch Vertex" , notes = "# Payload \n"
        + "{\r\n" + 
        "   \"type\" :\"vertex type from oxm like comcast.nodes.sdwan.vpn\",\r\n" + 
        "    \"properties\": {\r\n" + 
        "        \"prop1\" : \"true\",\r\n" + 
        "        \"prop2\" :\"name1\",\r\n" +         
        "    ..}\r\n" + 
        " }")
    @ApiResponses({    
      @ApiResponse(code = 200, message = "Success"),
      @ApiResponse(code = 403, message = "Forbidden"), 
      @ApiResponse(code = 400, message = "Bad Request"),
      @ApiResponse(code = 404, message = "Not Found"),
      @ApiResponse(code = 500, message = "Internal Server Error") })
    @ApiImplicitParams({
      @ApiImplicitParam(name = "X-FromAppId", required = true, dataType = "string", paramType = "header"),
      @ApiImplicitParam(name = "X-TransactionId",  required = true, dataType = "string", paramType = "header"),    
    })
    @PATCH
    @Path("/{version}/{type}/{id}")
    @Consumes({"application/merge-patch+json"})
    @Produces({MediaType.APPLICATION_JSON})
    public Response patchVertex(String content, @PathParam("version") @ApiParam(value="oxm model version",defaultValue="v13") String version, @PathParam("type") String type,
            @PathParam("id") String id, @PathParam("uri") @Encoded @ApiParam(hidden=true) String uri, @Context HttpHeaders headers,
            @Context UriInfo uriInfo, @Context HttpServletRequest req) {

        LoggingUtil.initMdcContext(req, headers);
        logger.debug("Incoming request..." + content);

        ResponseBuilder responseBuilder;

        try {
            if (validateRequest(req, uri, content, Action.PATCH, CrudServiceConstants.CRD_AUTH_POLICY_NAME, headers)) {
                VertexPayload payload = VertexPayload.fromJson(content);
                if (payload.getProperties() == null || payload.getProperties().isJsonNull()) {
                    throw new CrudException("Invalid request Payload", Status.BAD_REQUEST);
                }
                if (payload.getId() != null && !payload.getId().equals(id)) {
                    throw new CrudException("ID Mismatch", Status.BAD_REQUEST);
                }

                payload.setProperties(
                        CrudServiceUtil.mergeHeaderInFoToPayload(payload.getProperties(), headers, false));

                ImmutablePair<EntityTag, String> result = graphDataService.patchVertex(version, id, type, payload);
                responseBuilder =
                        Response.status(Status.OK).entity(result.getValue()).type(mediaType).tag(result.getKey());
            } else {
                responseBuilder = Response.status(Status.FORBIDDEN).entity(content).type(MediaType.APPLICATION_JSON);
            }
        } catch (CrudException ce) {
            responseBuilder = Response.status(ce.getHttpStatus()).entity(ce.getMessage());
        } catch (Exception e) {
            responseBuilder = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage());
        }

        setTxIdOnResponseHeader(headers, responseBuilder);

        Response response = responseBuilder.build();
        LoggingUtil.logRestRequest(logger, auditLogger, req, response);
        return response;
    }

    @ApiOperation(value = "Create Vertex" , notes = "# Payload \n"
        + "{\r\n" +      
        "    \"properties\": {\r\n" + 
        "        \"prop1\" : \"value\",\r\n" + 
        "        \"prop2\" :\"value\",\r\n" +         
        "    ..}\r\n" + 
        " }")
    @ApiResponses({    
      @ApiResponse(code = 201, message = "Created"),
      @ApiResponse(code = 403, message = "Forbidden"), 
      @ApiResponse(code = 400, message = "Bad Request"),
      @ApiResponse(code = 404, message = "Not Found"),
      @ApiResponse(code = 500, message = "Internal Server Error") })
    @ApiImplicitParams({
      @ApiImplicitParam(name = "X-FromAppId", required = true, dataType = "string", paramType = "header"),
      @ApiImplicitParam(name = "X-TransactionId",  required = true, dataType = "string", paramType = "header"),    
    })
    @POST
    @Path("/{version}/{type}/")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response addVertex(String content, @PathParam("version") @ApiParam(value="oxm model version",defaultValue="v13") String version, @PathParam("type") String type,
            @PathParam("uri") @Encoded @ApiParam(hidden=true) String uri, @Context HttpHeaders headers, @Context UriInfo uriInfo,
            @Context HttpServletRequest req) {

        LoggingUtil.initMdcContext(req, headers);
        logger.debug("Incoming request..." + content);

        ResponseBuilder responseBuilder;

        try {
            if (validateRequest(req, uri, content, Action.POST, CrudServiceConstants.CRD_AUTH_POLICY_NAME, headers)) {
                VertexPayload payload = VertexPayload.fromJson(content);
                if (payload.getProperties() == null || payload.getProperties().isJsonNull()) {
                    throw new CrudException("Invalid request Payload", Status.BAD_REQUEST);
                }
                if (payload.getId() != null) {
                    throw new CrudException("ID specified , use Http PUT to update Vertex", Status.BAD_REQUEST);
                }

                if (payload.getType() != null && !payload.getType().equals(type)) {
                    throw new CrudException("Vertex Type mismatch", Status.BAD_REQUEST);
                }

                payload.setProperties(CrudServiceUtil.mergeHeaderInFoToPayload(payload.getProperties(), headers, true));

                ImmutablePair<EntityTag, String> result = graphDataService.addVertex(version, type, payload);
                responseBuilder =
                        Response.status(Status.CREATED).entity(result.getValue()).tag(result.getKey()).type(mediaType);
            } else {
                responseBuilder = Response.status(Status.FORBIDDEN).entity(content).type(MediaType.APPLICATION_JSON);
            }
        } catch (CrudException ce) {
            responseBuilder = Response.status(ce.getHttpStatus()).entity(ce.getMessage());
        } catch (Exception e) {
            responseBuilder = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage());
        }

        setTxIdOnResponseHeader(headers, responseBuilder);

        Response response = responseBuilder.build();
        LoggingUtil.logRestRequest(logger, auditLogger, req, response);
        return response;
    }

    private void validateBulkPayload(BulkPayload payload) throws CrudException {
        List<String> vertices = new ArrayList<String>();
        List<String> edges = new ArrayList<String>();

        for (JsonElement v : payload.getObjects()) {
            List<Map.Entry<String, JsonElement>> entries =
                    new ArrayList<Map.Entry<String, JsonElement>>(v.getAsJsonObject().entrySet());

            if (entries.size() != 2) {
                throw new CrudException("", Status.BAD_REQUEST);
            }
            Map.Entry<String, JsonElement> opr = entries.get(0);
            Map.Entry<String, JsonElement> item = entries.get(1);

            if (vertices.contains(item.getKey())) {
                throw new CrudException("duplicate vertex in payload: " + item.getKey(), Status.BAD_REQUEST);
            }
            VertexPayload vertexPayload = VertexPayload.fromJson(item.getValue().getAsJsonObject().toString());
            if (vertexPayload.getType() == null) {
                throw new CrudException("Vertex Type cannot be null for: " + item.getKey(), Status.BAD_REQUEST);
            }

            if (!opr.getKey().equalsIgnoreCase("operation")) {
                throw new CrudException("operation missing in item: " + item.getKey(), Status.BAD_REQUEST);
            }

            if (!opr.getValue().getAsString().equalsIgnoreCase("add")
                    && !opr.getValue().getAsString().equalsIgnoreCase("modify")
                    && !opr.getValue().getAsString().equalsIgnoreCase("patch")
                    && !opr.getValue().getAsString().equalsIgnoreCase("delete")) {
                throw new CrudException("Invalid operation at item: " + item.getKey(), Status.BAD_REQUEST);
            }
            // check if ID is populate for modify/patch/delete operation
            if ((opr.getValue().getAsString().equalsIgnoreCase("modify")
                    || opr.getValue().getAsString().equalsIgnoreCase("patch")
                    || opr.getValue().getAsString().equalsIgnoreCase("delete")) && (vertexPayload.getId() == null)) {

                throw new CrudException("Mising ID at item: " + item.getKey(), Status.BAD_REQUEST);

            }

            vertices.add(item.getKey());
        }

        for (JsonElement v : payload.getRelationships()) {
            List<Map.Entry<String, JsonElement>> entries =
                    new ArrayList<Map.Entry<String, JsonElement>>(v.getAsJsonObject().entrySet());

            if (entries.size() != 2) {
                throw new CrudException("", Status.BAD_REQUEST);
            }
            Map.Entry<String, JsonElement> opr = entries.get(0);
            Map.Entry<String, JsonElement> item = entries.get(1);

            if (edges.contains(item.getKey())) {
                throw new CrudException("duplicate Edge in payload: " + item.getKey(), Status.BAD_REQUEST);
            }

            EdgePayload edgePayload = EdgePayload.fromJson(item.getValue().getAsJsonObject().toString());

            if (!opr.getKey().equalsIgnoreCase("operation")) {
                throw new CrudException("operation missing in item: " + item.getKey(), Status.BAD_REQUEST);
            }

            if (!opr.getValue().getAsString().equalsIgnoreCase("add")
                    && !opr.getValue().getAsString().equalsIgnoreCase("modify")
                    && !opr.getValue().getAsString().equalsIgnoreCase("patch")
                    && !opr.getValue().getAsString().equalsIgnoreCase("delete")) {
                throw new CrudException("Invalid operation at item: " + item.getKey(), Status.BAD_REQUEST);
            }
            // check if ID is populate for modify/patch/delete operation
            if ((edgePayload.getId() == null) && (opr.getValue().getAsString().equalsIgnoreCase("modify")
                    || opr.getValue().getAsString().equalsIgnoreCase("patch")
                    || opr.getValue().getAsString().equalsIgnoreCase("delete"))) {

                throw new CrudException("Mising ID at item: " + item.getKey(), Status.BAD_REQUEST);

            }
            if (opr.getValue().getAsString().equalsIgnoreCase("add")) {
                if (edgePayload.getSource() == null || edgePayload.getTarget() == null) {
                    throw new CrudException("Source/Target cannot be null for edge: " + item.getKey(),
                            Status.BAD_REQUEST);
                }
                if (edgePayload.getSource().startsWith("$")
                        && !vertices.contains(edgePayload.getSource().substring(1))) {
                    throw new CrudException("Source Vertex " + edgePayload.getSource().substring(1)
                            + " not found for Edge: " + item.getKey(), Status.BAD_REQUEST);
                }

                if (edgePayload.getTarget().startsWith("$")
                        && !vertices.contains(edgePayload.getTarget().substring(1))) {
                    throw new CrudException("Target Vertex " + edgePayload.getSource().substring(1)
                            + " not found for Edge: " + item.getKey(), Status.BAD_REQUEST);
                }
            }
            edges.add(item.getKey());

        }

    }

    @ApiOperation(value = "Bulk API" , notes="For example : https://<host>:9520/services/inventory/v11/bulk")
    @ApiResponses({    
      @ApiResponse(code = 200, message = "Success"),
      @ApiResponse(code = 403, message = "Forbidden"), 
      @ApiResponse(code = 400, message = "Bad Request"),
      @ApiResponse(code = 404, message = "Not Found"),
      @ApiResponse(code = 500, message = "Internal Server Error") })
    @ApiImplicitParams({
      @ApiImplicitParam(name = "X-FromAppId", required = true, dataType = "string", paramType = "header"),
      @ApiImplicitParam(name = "X-TransactionId",  required = true, dataType = "string", paramType = "header"),    
    })
    @POST
    @Path("/{version}/bulk/")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response addBulk(String content, @PathParam("version") @ApiParam(value="oxm model version",defaultValue="v13") String version, 
            @PathParam("uri") @Encoded @ApiParam(hidden=true) String uri, @Context HttpHeaders headers, @Context UriInfo uriInfo,
            @Context HttpServletRequest req) {

        LoggingUtil.initMdcContext(req, headers);
        logger.debug("Incoming request..." + content);

        ResponseBuilder responseBuilder;

        try {
            if (validateRequest(req, uri, content, Action.POST, CrudServiceConstants.CRD_AUTH_POLICY_NAME, headers)) {
                BulkPayload payload = BulkPayload.fromJson(content);
                if ((payload.getObjects() == null && payload.getRelationships() == null)
                        || (payload.getObjects() != null && payload.getObjects().isEmpty()
                                && payload.getRelationships() != null && payload.getRelationships().isEmpty())) {
                    throw new CrudException("Invalid request Payload", Status.BAD_REQUEST);
                }

                validateBulkPayload(payload);
                String result = graphDataService.addBulk(version, payload, headers);
                responseBuilder = Response.status(Status.OK).entity(result).type(mediaType);
            } else {
                responseBuilder = Response.status(Status.FORBIDDEN).entity(content).type(MediaType.APPLICATION_JSON);
            }
        } catch (CrudException ce) {
            responseBuilder = Response.status(ce.getHttpStatus()).entity(ce.getMessage());
        } catch (Exception e) {
            responseBuilder = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage());
        }

        setTxIdOnResponseHeader(headers, responseBuilder);

        Response response = responseBuilder.build();
        LoggingUtil.logRestRequest(logger, auditLogger, req, response);
        return response;
    }

    @ApiOperation(value = "Create Vertex (typeless endpoint)" , nickname="addVertex-typeless",notes = "# Payload \n"
        + "{\r\n" +  
        "   \"type\" :\"vertex type from oxm like comcast.nodes.sdwan.vpn\",\r\n" +       
        "    \"properties\": {\r\n" + 
        "        \"prop1\" : \"value\",\r\n" + 
        "        \"prop2\" :\"value\",\r\n" +          
        "    ..}\r\n" + 
        " }")
    @ApiResponses({   
      @ApiResponse(code = 201, message = "Created"),
      @ApiResponse(code = 403, message = "Forbidden"), 
      @ApiResponse(code = 400, message = "Bad Request"),
      @ApiResponse(code = 404, message = "Not Found"),
      @ApiResponse(code = 500, message = "Internal Server Error") })
    @ApiImplicitParams({
      @ApiImplicitParam(name = "X-FromAppId", required = true, dataType = "string", paramType = "header"),
      @ApiImplicitParam(name = "X-TransactionId",  required = true, dataType = "string", paramType = "header"),    
    })
    @POST
    @Path("/{version}/")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response addVertex(String content, @PathParam("version") @ApiParam(value="oxm model version",defaultValue="v13") String version,
            @PathParam("uri") @Encoded  @ApiParam(hidden=true) String uri, @Context HttpHeaders headers, @Context UriInfo uriInfo,
            @Context HttpServletRequest req) {

        LoggingUtil.initMdcContext(req, headers);
        logger.debug("Incoming request..." + content);

        ResponseBuilder responseBuilder;

        try {

            if (validateRequest(req, uri, content, Action.POST, CrudServiceConstants.CRD_AUTH_POLICY_NAME, headers)) {
                VertexPayload payload = VertexPayload.fromJson(content);
                if (payload.getProperties() == null || payload.getProperties().isJsonNull()) {
                    throw new CrudException("Invalid request Payload", Status.BAD_REQUEST);
                }
                if (payload.getId() != null) {
                    throw new CrudException("ID specified , use Http PUT to update Vertex", Status.BAD_REQUEST);
                }

                if (payload.getType() == null || payload.getType().isEmpty()) {
                    throw new CrudException("Missing Vertex Type ", Status.BAD_REQUEST);
                }

                payload.setProperties(CrudServiceUtil.mergeHeaderInFoToPayload(payload.getProperties(), headers, true));

                ImmutablePair<EntityTag, String> result =
                        graphDataService.addVertex(version, payload.getType(), payload);
                responseBuilder =
                        Response.status(Status.CREATED).entity(result.getValue()).tag(result.getKey()).type(mediaType);
            } else {
                responseBuilder = Response.status(Status.FORBIDDEN).entity(content).type(MediaType.APPLICATION_JSON);
            }
        } catch (CrudException ce) {
            responseBuilder = Response.status(ce.getHttpStatus()).entity(ce.getMessage());
        } catch (Exception e) {
            responseBuilder = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage());
        }

        setTxIdOnResponseHeader(headers, responseBuilder);

        Response response = responseBuilder.build();
        LoggingUtil.logRestRequest(logger, auditLogger, req, response);
        return response;
    }

    @ApiOperation(value = "Create Edge" , notes = "# Payload \n"
        + "{  \r\n" + 
        "   \"source\":\"source vertex like : services/inventory/v11/vserver/0\",\r\n" + 
        "   \"target\":\"target vertex like : services/inventory/v11/pserver/7\",\r\n" + 
        "   \"properties\":{  \r\n" + 
        "        \"prop1\" : \"value\",\r\n" + 
        "        \"prop2\" :\"value\",\r\n" +  
        "    ..}\r\n" + 
        " }")
    @ApiResponses({   
      @ApiResponse(code = 201, message = "Created"),
      @ApiResponse(code = 403, message = "Forbidden"), 
      @ApiResponse(code = 400, message = "Bad Request"),
      @ApiResponse(code = 404, message = "Not Found"),
      @ApiResponse(code = 500, message = "Internal Server Error") })
   
    @ApiImplicitParams({
      @ApiImplicitParam(name = "X-FromAppId", required = true, dataType = "string", paramType = "header"),
      @ApiImplicitParam(name = "X-TransactionId",  required = true, dataType = "string", paramType = "header"),       
    })
    @POST
    @Path("/relationships/{version}/{type}/")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response addEdge(String content, @PathParam("version") @ApiParam(value="oxm model version",defaultValue="v13") String version, @PathParam("type") String type,
            @PathParam("uri") @Encoded @ApiParam(hidden=true) String uri, @Context HttpHeaders headers, @Context UriInfo uriInfo,
            @Context HttpServletRequest req) {

        LoggingUtil.initMdcContext(req, headers);
        logger.debug("Incoming request..." + content);

        ResponseBuilder responseBuilder;

        try {
            if (validateRequest(req, uri, content, Action.POST, CrudServiceConstants.CRD_AUTH_POLICY_NAME, headers)) {
                EdgePayload payload = EdgePayload.fromJson(content);
                if (payload.getProperties() == null || payload.getProperties().isJsonNull()) {
                    throw new CrudException("Invalid request Payload", Status.BAD_REQUEST);
                }
                if (payload.getId() != null) {
                    throw new CrudException("ID specified , use Http PUT to update Edge", Status.BAD_REQUEST);
                }

                if (payload.getType() != null && !payload.getType().equals(type)) {
                    throw new CrudException("Edge Type mismatch", Status.BAD_REQUEST);
                }
                ImmutablePair<EntityTag, String> result = graphDataService.addEdge(version, type, payload);
                responseBuilder =
                        Response.status(Status.CREATED).entity(result.getValue()).tag(result.getKey()).type(mediaType);
            } else {
                responseBuilder = Response.status(Status.FORBIDDEN).entity(content).type(MediaType.APPLICATION_JSON);
            }
        } catch (CrudException ce) {
            responseBuilder = Response.status(ce.getHttpStatus()).entity(ce.getMessage());
        } catch (Exception e) {
            responseBuilder = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage());
        }

        setTxIdOnResponseHeader(headers, responseBuilder);

        Response response = responseBuilder.build();
        LoggingUtil.logRestRequest(logger, auditLogger, req, response);
        return response;
    }

    @ApiOperation(value = "Create Edge (typeless endpoint)" , nickname="addEdge-typeless",notes = "# Payload \n"
        + "{  \r\n" + 
        "   \"type\":\"edge type like : tosca.relationships.HostedOn\",\r\n" +      
        "   \"source\":\"source vertex like : services/inventory/v11/vserver/0\",\r\n" + 
        "   \"target\":\"target vertex like : services/inventory/v11/pserver/7\",\r\n" + 
        "   \"properties\":{  \r\n" + 
        "        \"prop1\" : \"value\",\r\n" + 
        "        \"prop2\" :\"value\",\r\n" +  
        "    ..}\r\n" + 
        " }")
    @ApiResponses({  
      @ApiResponse(code = 201, message = "Created"),
      @ApiResponse(code = 403, message = "Forbidden"), 
      @ApiResponse(code = 400, message = "Bad Request"),
      @ApiResponse(code = 404, message = "Not Found"),
      @ApiResponse(code = 500, message = "Internal Server Error") })
    @ApiImplicitParams({
      @ApiImplicitParam(name = "X-FromAppId", required = true, dataType = "string", paramType = "header"),
      @ApiImplicitParam(name = "X-TransactionId",  required = true, dataType = "string", paramType = "header"),    
    })
    @POST
    @Path("/relationships/{version}/")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response addEdge(String content, @PathParam("version")  @ApiParam(value="oxm model version",defaultValue="v13") String version, @PathParam("uri") @Encoded @ApiParam(hidden=true) String uri,
            @Context HttpHeaders headers, @Context UriInfo uriInfo, @Context HttpServletRequest req) {

        LoggingUtil.initMdcContext(req, headers);
        logger.debug("Incoming request..." + content);

        ResponseBuilder responseBuilder;

        try {
            if (validateRequest(req, uri, content, Action.POST, CrudServiceConstants.CRD_AUTH_POLICY_NAME, headers)) {
                EdgePayload payload = EdgePayload.fromJson(content);
                if (payload.getProperties() == null || payload.getProperties().isJsonNull()) {
                    throw new CrudException("Invalid request Payload", Status.BAD_REQUEST);
                }
                if (payload.getId() != null) {
                    throw new CrudException("ID specified , use Http PUT to update Edge", Status.BAD_REQUEST);
                }

                if (payload.getType() == null || payload.getType().isEmpty()) {
                    payload.setType(CrudServiceUtil.determineEdgeType(payload, version));
                }

                ImmutablePair<EntityTag, String> result = graphDataService.addEdge(version, payload.getType(), payload);
                responseBuilder =
                        Response.status(Status.CREATED).entity(result.getValue()).tag(result.getKey()).type(mediaType);
            } else {
                responseBuilder = Response.status(Status.FORBIDDEN).entity(content).type(MediaType.APPLICATION_JSON);
            }
        } catch (CrudException ce) {
            responseBuilder = Response.status(ce.getHttpStatus()).entity(ce.getMessage());
        } catch (Exception e) {
            responseBuilder = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage());
        }

        setTxIdOnResponseHeader(headers, responseBuilder);

        Response response = responseBuilder.build();
        LoggingUtil.logRestRequest(logger, auditLogger, req, response);
        return response;
    }

    @ApiOperation(value = "Delete Vertex" , notes="For example : https://<host>:9520/services/inventory/v11/pserver/<id>")
    @ApiResponses({  
      @ApiResponse(code = 200, message = "Success"),
      @ApiResponse(code = 403, message = "Forbidden"), 
      @ApiResponse(code = 400, message = "Bad Request"),
      @ApiResponse(code = 404, message = "Not Found"),
      @ApiResponse(code = 500, message = "Internal Server Error") })
    @ApiImplicitParams({
      @ApiImplicitParam(name = "X-FromAppId", required = true, dataType = "string", paramType = "header"),
      @ApiImplicitParam(name = "X-TransactionId",  required = true, dataType = "string", paramType = "header"),    
    })
    @DELETE
    @Path("/{version}/{type}/{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteVertex(@ApiParam(hidden=true) String content, @PathParam("version") @ApiParam(value="oxm model version",defaultValue="v13") String version, @PathParam("type") String type,
            @PathParam("id") String id, @PathParam("uri") @Encoded @ApiParam(hidden=true)  String uri, @Context HttpHeaders headers,
            @Context UriInfo uriInfo, @Context HttpServletRequest req) {

        LoggingUtil.initMdcContext(req, headers);
        logger.debug("Incoming request..." + content);

        ResponseBuilder responseBuilder;

        try {
            if (validateRequest(req, uri, content, Action.DELETE, CrudServiceConstants.CRD_AUTH_POLICY_NAME, headers)) {
                String result = graphDataService.deleteVertex(version, id, type);
                responseBuilder = Response.status(Status.OK).entity(result).type(mediaType);
            } else {
                responseBuilder = Response.status(Status.FORBIDDEN).entity(content).type(MediaType.APPLICATION_JSON);
            }
        } catch (CrudException ce) {
            responseBuilder = Response.status(ce.getHttpStatus()).entity(ce.getMessage());
        } catch (Exception e) {
            responseBuilder = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage());
        }

        setTxIdOnResponseHeader(headers, responseBuilder);

        Response response = responseBuilder.build();
        LoggingUtil.logRestRequest(logger, auditLogger, req, response);
        return response;
    }

    @ApiOperation(value = "Delete Edge" , notes="For example : https://<host>:9520/services/inventory/v11/pserver/<id>")
    @ApiResponses({ 
      @ApiResponse(code = 200, message = "Success"),
      @ApiResponse(code = 403, message = "Forbidden"), 
      @ApiResponse(code = 400, message = "Bad Request"),
      @ApiResponse(code = 404, message = "Not Found"),
      @ApiResponse(code = 500, message = "Internal Server Error") })
    @ApiImplicitParams({
      @ApiImplicitParam(name = "X-FromAppId", required = true, dataType = "string", paramType = "header"),
      @ApiImplicitParam(name = "X-TransactionId",  required = true, dataType = "string", paramType = "header"),    
    })
    @DELETE
    @Path("/relationships/{version}/{type}/{id}")
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteEdge(@ApiParam(hidden=true) String content, @PathParam("version") @ApiParam(value="oxm model version",defaultValue="v13") String version, @PathParam("type") String type,
            @PathParam("id") String id, @PathParam("uri") @Encoded @ApiParam(hidden=true) String uri, @Context HttpHeaders headers,
            @Context UriInfo uriInfo, @Context HttpServletRequest req) {

        LoggingUtil.initMdcContext(req, headers);
        logger.debug("Incoming request..." + content);

        ResponseBuilder responseBuilder;

        try {
            if (validateRequest(req, uri, content, Action.DELETE, CrudServiceConstants.CRD_AUTH_POLICY_NAME, headers)) {
                String result = graphDataService.deleteEdge(version, id, type);
                responseBuilder = Response.status(Status.OK).entity(result).type(mediaType);
            } else {
                responseBuilder = Response.status(Status.FORBIDDEN).entity(content).type(MediaType.APPLICATION_JSON);
            }
        } catch (CrudException ce) {
            responseBuilder = Response.status(ce.getHttpStatus()).entity(ce.getMessage());
        } catch (Exception e) {
            responseBuilder = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage());
        }

        setTxIdOnResponseHeader(headers, responseBuilder);

        Response response = responseBuilder.build();
        LoggingUtil.logRestRequest(logger, auditLogger, req, response);
        return response;
    }

    protected boolean validateRequest(HttpServletRequest req, String uri, String content, Action action,
            String authPolicyFunctionName, HttpHeaders headers) throws CrudException {
        boolean isValid = false;
        try {
            String cipherSuite = (String) req.getAttribute("javax.servlet.request.cipher_suite");
            String authUser = null;
            if (cipherSuite != null) {
                X509Certificate[] certChain =
                        (X509Certificate[]) req.getAttribute("javax.servlet.request.X509Certificate");
                X509Certificate clientCert = certChain[0];
                X500Principal subjectDn = clientCert.getSubjectX500Principal();
                authUser = subjectDn.toString();
            }
            if (null != authUser) {
                isValid = this.auth.validateRequest(authUser.toLowerCase(),
                        action.toString() + ":" + authPolicyFunctionName);
            }
        } catch (Exception e) {
            logResult(action, uri, e);
            return false;
        }

        validateRequestHeader(headers);

        return isValid;
    }

    public void validateRequestHeader(HttpHeaders headers) throws CrudException {
        String sourceOfTruth = null;
        if (headers.getRequestHeaders().containsKey("X-FromAppId")) {
            sourceOfTruth = headers.getRequestHeaders().getFirst("X-FromAppId");
        }

        if (sourceOfTruth == null || sourceOfTruth.trim() == "") {
            throw new CrudException("Invalid request, Missing X-FromAppId header", Status.BAD_REQUEST);
        }

        String transId = null;
        if (headers.getRequestHeaders().containsKey("X-TransactionId")) {
            transId = headers.getRequestHeaders().getFirst("X-TransactionId");
        }

        if (transId == null || transId.trim() == "") {
            throw new CrudException("Invalid request, Missing X-TransactionId header", Status.BAD_REQUEST);
        }
    }

    void logResult(Action op, String uri, Exception e) {

        logger.error(CrudServiceMsgs.EXCEPTION_DURING_METHOD_CALL, op.toString(), uri,
                Arrays.toString(e.getStackTrace()));

        // Clear the MDC context so that no other transaction inadvertently
        // uses our transaction id.
        MDC.clear();
    }

    private Map<String, String> addParams(UriInfo info, boolean filter, String type, String version) {
        String propertiesKey = CrudProperties.get(CrudServiceConstants.CRD_COLLECTION_PROPERTIES_KEY);
        Map<String, String> params = new HashMap<String, String>();
        params.put(CrudServiceConstants.CRD_RESERVED_VERSION, version);
        params.put(CrudServiceConstants.CRD_RESERVED_NODE_TYPE, type);
        if (filter) {
            for (Map.Entry<String, List<String>> e : info.getQueryParameters().entrySet()) {
                if (!e.getKey().equals(propertiesKey)) {
                    params.put(e.getKey(), e.getValue().get(0));
                }
            }
        } else {
            for (Map.Entry<String, List<String>> e : info.getQueryParameters().entrySet()) {
                params.put(e.getKey(), e.getValue().get(0));
            }
        }
        return params;
    }

    private void setTxIdOnResponseHeader(HttpHeaders headers, ResponseBuilder responseBuilder) {
        String txId = headers.getHeaderString(TRANSACTIONID_HEADER);
        if (txId != null) {
            responseBuilder.header(TRANSACTIONID_HEADER, txId);
        }
    }
}
