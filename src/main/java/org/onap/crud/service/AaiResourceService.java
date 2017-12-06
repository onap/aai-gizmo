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

import java.security.cert.X509Certificate;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.Encoded;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.onap.aai.exceptions.AAIException;
import org.onap.aai.serialization.db.EdgeProperty;
import org.onap.aai.serialization.db.EdgeRule;
import org.onap.aai.serialization.db.EdgeRules;
import org.onap.aai.serialization.db.EdgeType;
import org.onap.aaiauth.auth.Auth;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.crud.exception.CrudException;
import org.onap.crud.logging.CrudServiceMsgs;
import org.onap.crud.logging.LoggingUtil;
import org.onap.crud.service.CrudRestService.Action;
import org.onap.crud.util.CrudServiceConstants;
import org.onap.schema.RelationshipSchemaLoader;
import org.onap.schema.RelationshipSchemaValidator;
import org.slf4j.MDC;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;


/**
 * This defines a set of REST endpoints which allow clients to create or update graph edges
 * where the edge rules defined by the A&AI will be invoked to automatically populate the
 * defined edge properties.
 */
public class AaiResourceService {

  private String mediaType = MediaType.APPLICATION_JSON;
  public static final String HTTP_PATCH_METHOD_OVERRIDE = "X-HTTP-Method-Override";
  
  private Auth auth;
  CrudGraphDataService crudGraphDataService;
  Gson gson = new Gson();
  
  private Logger logger      = LoggerFactory.getInstance().getLogger(AaiResourceService.class.getName());
  private Logger auditLogger = LoggerFactory.getInstance().getAuditLogger(AaiResourceService.class.getName());
 
  public AaiResourceService() {}
  
  /**
   * Creates a new instance of the AaiResourceService.
   * 
   * @param crudGraphDataService - Service used for interacting with the graph.
   * 
   * @throws Exception
   */
  public AaiResourceService(CrudGraphDataService crudGraphDataService) throws Exception {
    this.crudGraphDataService = crudGraphDataService;
    this.auth                 = new Auth(CrudServiceConstants.CRD_AUTH_FILE);
  }
  
  /**
   * Perform any one-time initialization required when starting the service.
   */
  public void startup() {
    
    if(logger.isDebugEnabled()) {
      logger.debug("AaiResourceService started!");
    }
  }
  
  
  /**
   * Creates a new relationship in the graph, automatically populating the edge
   * properties based on the A&AI edge rules.
   * 
   * @param content - Json structure describing the relationship to create.
   * @param type    - Relationship type supplied as a URI parameter.
   * @param uri     - Http request uri
   * @param headers - Http request headers
   * @param uriInfo - Http URI info field
   * @param req     - Http request structure.
   * 
   * @return - Standard HTTP response.
   */
  @POST
  @Path("/relationships/{type}/")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  public Response createRelationship(String content, 
                                     @PathParam("type") String type, 
                                     @PathParam("uri") @Encoded String uri,
                                     @Context HttpHeaders headers, 
                                     @Context UriInfo uriInfo,
                                     @Context HttpServletRequest req) {
    
    LoggingUtil.initMdcContext(req, headers);

    if(logger.isDebugEnabled()) {
      logger.debug("Incoming request..." + content);
    }
    
    Response response = null;

    if (validateRequest(req, uri, content, Action.POST, CrudServiceConstants.CRD_AUTH_POLICY_NAME)) {
      
      try {
        
        // Extract the edge payload from the request.
        EdgePayload payload = EdgePayload.fromJson(content);   
        
        // Do some basic validation on the payload.
        if (payload.getProperties() == null || payload.getProperties().isJsonNull()) {
          throw new CrudException("Invalid request Payload", Status.BAD_REQUEST);
        }
        if (payload.getId() != null) {
          throw new CrudException("ID specified , use Http PUT to update Edge", Status.BAD_REQUEST);
        }
        if (payload.getType() != null && !payload.getType().equals(type)) {
          throw new CrudException("Edge Type mismatch", Status.BAD_REQUEST);
        }
        
        // Apply the edge rules to our edge.
        payload = applyEdgeRulesToPayload(payload);
        
        if(logger.isDebugEnabled()) {
          logger.debug("Creating AAI edge using version " + RelationshipSchemaLoader.getLatestSchemaVersion() );
        }
        
        // Now, create our edge in the graph store.
        String result = crudGraphDataService.addEdge(RelationshipSchemaLoader.getLatestSchemaVersion(), type, payload);
        response = Response.status(Status.CREATED).entity(result).type(mediaType).build();
        
      } catch (CrudException e) {

        response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
      }   
    }
    
    LoggingUtil.logRestRequest(logger, auditLogger, req, response);
    return response;
  }
  
  
  /**
   * Creates a new relationship in the graph, automatically populating the edge
   * properties based on the A&AI edge rules.
   * 
   * @param content - Json structure describing the relationship to create.
   * @param uri     - Http request uri
   * @param headers - Http request headers
   * @param uriInfo - Http URI info field
   * @param req     - Http request structure.
   * 
   * @return - Standard HTTP response.
   *    
   */
  @POST
  @Path("/relationships/")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  public Response createRelationship(String content, 
                                     @PathParam("uri") @Encoded String uri, 
                                     @Context HttpHeaders headers,
                                     @Context UriInfo uriInfo, 
                                     @Context HttpServletRequest req) {

    LoggingUtil.initMdcContext(req, headers);

    logger.debug("Incoming request..." + content);
    Response response = null;

    if (validateRequest(req, uri, content, Action.POST, CrudServiceConstants.CRD_AUTH_POLICY_NAME)) {

      try {
        
        // Extract the edge payload from the request.
        EdgePayload payload = EdgePayload.fromJson(content);
        
        // Do some basic validation on the payload.
        if (payload.getProperties() == null || payload.getProperties().isJsonNull()) {
          throw new CrudException("Invalid request Payload", Status.BAD_REQUEST);
        }
        if (payload.getId() != null) {
          throw new CrudException("ID specified , use Http PUT to update Edge", Status.BAD_REQUEST);
        }
        if (payload.getType() == null || payload.getType().isEmpty()) {
          throw new CrudException("Missing Edge Type ", Status.BAD_REQUEST);
        }
        
        // Apply the edge rules to our edge.
        payload = applyEdgeRulesToPayload(payload);
        
        // Now, create our edge in the graph store.
        String result = crudGraphDataService.addEdge(RelationshipSchemaLoader.getLatestSchemaVersion(), payload.getType(), payload);
        response = Response.status(Status.CREATED).entity(result).type(mediaType).build();
      
      } catch (CrudException ce) {
        response = Response.status(ce.getHttpStatus()).entity(ce.getMessage()).build();
      } catch (Exception e) {
        response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
      }
    } else {
      response = Response.status(Status.FORBIDDEN).entity(content)
          .type(MediaType.APPLICATION_JSON).build();
    }

    LoggingUtil.logRestRequest(logger, auditLogger, req, response);
    return response;
  }

  
  
  /**
   * Upserts a relationship into the graph, automatically populating the edge properties
   * based on the A&AI edge rules.  The behaviour is as follows:
   * <p>
   * <li>If no relationship with the supplied identifier already exists, then a new relationship 
   * is created with that id.<br>
   * <li>If a relationship with the supplied id DOES exist, then it is replaced with the supplied 
   * content.
   * 
   * @param content - Json structure describing the relationship to create.
   * @param type    - Relationship type supplied as a URI parameter.
   * @param id      - Edge identifier.
   * @param uri     - Http request uri
   * @param headers - Http request headers
   * @param uriInfo - Http URI info field
   * @param req     - Http request structure.
   * 
   * @return - Standard HTTP response.
   */
  @PUT
  @Path("/relationships/{type}/{id}")
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  public Response upsertEdge(String content, 
                             @PathParam("type") String type, 
                             @PathParam("id") String id,
                             @PathParam("uri") @Encoded String uri, 
                             @Context HttpHeaders headers,
                             @Context UriInfo uriInfo, 
                             @Context HttpServletRequest req) {
    LoggingUtil.initMdcContext(req, headers);

    logger.debug("Incoming request..." + content);
    Response response = null;

    if (validateRequest(req, uri, content, Action.PUT, CrudServiceConstants.CRD_AUTH_POLICY_NAME)) {
      
      try {
        
        // Extract the edge payload from the request.
        EdgePayload payload = EdgePayload.fromJson(content);
        
        // Do some basic validation on the payload.
        if (payload.getProperties() == null || payload.getProperties().isJsonNull()) {
          throw new CrudException("Invalid request Payload", Status.BAD_REQUEST);
        }
        if (payload.getId() != null && !payload.getId().equals(id)) {
          throw new CrudException("ID Mismatch", Status.BAD_REQUEST);
        }
        
        // Apply the edge rules to our edge.
        payload = applyEdgeRulesToPayload(payload);
        
        String result;
        if (headers.getRequestHeaders().getFirst(HTTP_PATCH_METHOD_OVERRIDE) != null &&
            headers.getRequestHeaders().getFirst(HTTP_PATCH_METHOD_OVERRIDE).equalsIgnoreCase("PATCH")) {
          result = crudGraphDataService.patchEdge(RelationshipSchemaLoader.getLatestSchemaVersion(), id, type, payload);
        } else {

          result = crudGraphDataService.updateEdge(RelationshipSchemaLoader.getLatestSchemaVersion(), id, type, payload);
        }

        response = Response.status(Status.OK).entity(result).type(mediaType).build();
        
      } catch (CrudException ce) {
        response = Response.status(ce.getHttpStatus()).entity(ce.getMessage()).build();
      } catch (Exception e) {
        response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
      }
      
    } else {
      
      response = Response.status(Status.FORBIDDEN).entity(content)
          .type(MediaType.APPLICATION_JSON).build();
    }
    
    LoggingUtil.logRestRequest(logger, auditLogger, req, response);
    return response;
  }
  
  
  /**
   * Retrieves the properties defined in the edge rules for a relationship between the 
   * supplied vertex types.
   * 
   * @param sourceVertexType - Type of source vertex for the relationship.
   * @param targetVertexType - Type of target vertex for the relationship.
   * 
   * @return - The defined properties for the relationship type.
   *  
   * @throws CrudException
   */
  private Map<EdgeProperty, String> getEdgeRuleProperties(String sourceVertexType, String targetVertexType) throws CrudException {

    if(logger.isDebugEnabled()) {
      logger.debug("Lookup db edge rules for " + sourceVertexType + " -> " + targetVertexType);
    }
    
    EdgeRules rules = EdgeRules.getInstance();
    EdgeRule rule;
    try {
      
      if(logger.isDebugEnabled()) {
        logger.debug("Lookup by edge type TREE");
      }
      
      // We have no way of knowing in advance whether our relationship is considered to
      // be a tree or cousing relationship, so try looking it up as a tree type first.
      rule = rules.getEdgeRule(EdgeType.TREE, sourceVertexType, targetVertexType);
      
    } catch (AAIException e) {
      try {
        
        if(logger.isDebugEnabled()) {
          logger.debug("Lookup by edge type COUSIN");
        }
        
        // If we are here, then our lookup by 'tree' type failed, so try looking it up
        // as a 'cousin' relationship.
        rule = rules.getEdgeRule(EdgeType.COUSIN, sourceVertexType, targetVertexType);
        
      } catch (AAIException e1) {
        
        // If we're here then we failed to find edge rules for this relationship.  Time to
        // give up...
        throw new CrudException("No edge rules for " + sourceVertexType + " -> " + targetVertexType, Status.NOT_FOUND);
      }
    } catch (Exception e) {
      
      throw new CrudException("General failure getting edge rule properties - " + 
                              e.getMessage(), Status.INTERNAL_SERVER_ERROR);
    }
    
    return rule.getEdgeProperties();
  }
  
  
  /**
   * This method takes an inbound edge request payload, looks up the edge rules for the
   * sort of relationship defined in the payload, and automatically applies the defined
   * edge properties to it.
   * 
   * @param payload - The original edge request payload
   * 
   * @return - An updated edge request payload, with the properties defined in the edge
   *           rules automatically populated.
   *           
   * @throws CrudException
   */
  public EdgePayload applyEdgeRulesToPayload(EdgePayload payload) throws CrudException {
    
    // Extract the types for both the source and target vertices.
    String srcType = RelationshipSchemaValidator.vertexTypeFromUri(payload.getSource());
    String tgtType = RelationshipSchemaValidator.vertexTypeFromUri(payload.getTarget());

      // Now, get the default properties for this edge based on the edge rules definition...
      Map<EdgeProperty, String> props = getEdgeRuleProperties(srcType, tgtType);
      
      // ...and merge them with any custom properties provided in the request.
      JsonElement mergedProperties = mergeProperties(payload.getProperties(), props);
      payload.setProperties(mergedProperties);
    
    
    if(logger.isDebugEnabled()) {
      logger.debug("Edge properties after applying rules for '" + srcType + " -> " + tgtType + "': " + mergedProperties);
    }
    
    return payload;
  }
  
  
  /**
   * Given a set of edge properties extracted from an edge request payload and a set of properties
   * taken from the db edge rules, this method merges them into one set of properties.
   * <p>
   * If the client has attempted to override the defined value for a property in the db edge rules
   * then the request will be rejected as invalid.
   * 
   * @param propertiesFromRequest - Set of properties from the edge request.
   * @param propertyDefaults      - Set of properties from the db edge rules.
   * 
   * @return - A merged set of properties.
   * 
   * @throws CrudException
   */
  public JsonElement mergeProperties(JsonElement propertiesFromRequest, Map<EdgeProperty, String> propertyDefaults) throws CrudException {
        
    // Convert the properties from the edge payload into something we can
    // manipulate.
    Set<Map.Entry<String, JsonElement>> properties = new HashSet<Map.Entry<String, JsonElement>>();
    properties.addAll(propertiesFromRequest.getAsJsonObject().entrySet());
    
    Set<String> propertyKeys = new HashSet<String>();
    for(Map.Entry<String, JsonElement> property : properties) {
      propertyKeys.add(property.getKey());
    }
    
    // Now, merge in the properties specified in the Db Edge Rules.
    for(EdgeProperty defProperty : propertyDefaults.keySet()) {
      
      // If the edge rules property was explicitly specified by the
      // client then we will reject the request...
      if(!propertyKeys.contains(defProperty.toString())) {
        properties.add(new AbstractMap.SimpleEntry<String, JsonElement>(defProperty.toString(),
            (JsonElement)(new JsonPrimitive(propertyDefaults.get(defProperty)))));
        
      } else {
        throw new CrudException("Property " + defProperty + " defined in db edge rules can not be overriden by the client.", 
                                Status.BAD_REQUEST);
      }    
    }

    Object[] propArray = properties.toArray();
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    boolean first=true;
    for(int i=0; i<propArray.length; i++) {
      
      Map.Entry<String, JsonElement> entry = (Entry<String, JsonElement>) propArray[i];
      if(!first) {
        sb.append(",");
      }
      sb.append("\"").append(entry.getKey()).append("\"").append(":").append(entry.getValue());
      first=false;
    }
    sb.append("}");
    
    // We're done.  Return the result as a JsonElement.
    return gson.fromJson(sb.toString(), JsonElement.class);
  }


  /**
   * Invokes authentication validation on an incoming HTTP request.
   * 
   * @param req                    - The HTTP request.
   * @param uri                    - HTTP URI
   * @param content                - Payload of the HTTP request.
   * @param action                 - What HTTP action is being performed (GET/PUT/POST/PATCH/DELETE)
   * @param authPolicyFunctionName - Policy function being invoked.
   * 
   * @return true  - if the request passes validation,
   *         false - otherwise.
   */
  protected boolean validateRequest(HttpServletRequest req, 
                                    String uri, 
                                    String content,
                                    Action action, 
                                    String authPolicyFunctionName) {
    try {
      String cipherSuite = (String) req.getAttribute("javax.servlet.request.cipher_suite");
      String authUser = null;
      if (cipherSuite != null) {
                
        X509Certificate[] certChain = (X509Certificate[]) req.getAttribute("javax.servlet.request.X509Certificate");
        X509Certificate clientCert = certChain[0];
        X500Principal subjectDn = clientCert.getSubjectX500Principal();
        authUser = subjectDn.toString();
      }
      
      return this.auth.validateRequest(authUser.toLowerCase(), action.toString() + ":" + authPolicyFunctionName);
      
    } catch (Exception e) {
      logResult(action, uri, e);
      return false;
    }
  }
  
  protected void logResult(Action op, String uri, Exception e) {

    logger.error(CrudServiceMsgs.EXCEPTION_DURING_METHOD_CALL, 
                 op.toString(), 
                 uri, 
                 e.getStackTrace().toString());

    // Clear the MDC context so that no other transaction inadvertently
    // uses our transaction id.
    MDC.clear();
  }
}
