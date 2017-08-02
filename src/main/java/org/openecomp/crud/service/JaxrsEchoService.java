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
package org.openecomp.crud.service;

import org.openecomp.cl.api.Logger;
import org.openecomp.cl.eelf.LoggerFactory;
import org.openecomp.crud.logging.LoggingUtil;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;


public class JaxrsEchoService {

  private static Logger logger = LoggerFactory.getInstance()
      .getLogger(JaxrsEchoService.class.getName());
  private static Logger auditLogger = LoggerFactory.getInstance()
      .getAuditLogger(JaxrsEchoService.class.getName());

  @GET
  @Path("echo/{input}")
  @Produces("text/plain")
  public String ping(@PathParam("input") String input,
                     @Context HttpHeaders headers,
                     @Context UriInfo info,
                     @Context HttpServletRequest req) {

    LoggingUtil.initMdcContext(req, headers);
    LoggingUtil.logRestRequest(logger, auditLogger, req, Response.status(Status.OK)
        .entity("OK").build());

    return "Hello, " + input + ".";
  }
}