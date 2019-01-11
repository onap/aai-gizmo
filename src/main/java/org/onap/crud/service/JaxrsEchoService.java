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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.crud.logging.LoggingUtil;
import org.springframework.stereotype.Component;

@Component
@Path("/gizmo/v1/echo-service/")
public class JaxrsEchoService {

    private static Logger logger = LoggerFactory.getInstance().getLogger(JaxrsEchoService.class.getName());
    private static Logger auditLogger = LoggerFactory.getInstance().getAuditLogger(JaxrsEchoService.class.getName());

    public static final String TRANSACTIONID_HEADER = "X-TransactionId";

    @GET
    @Path("echo/{input}")
    @Produces("text/plain")
    public Response ping(@PathParam("input") String input, @Context HttpHeaders headers, @Context UriInfo info,
            @Context HttpServletRequest req) {

        LoggingUtil.initMdcContext(req, headers);

        ResponseBuilder responseBuilder = Response.status(Status.OK).entity("Hello, " + input + ".");

        String txId = headers.getHeaderString(TRANSACTIONID_HEADER);
        if (txId != null) {
            responseBuilder.header(TRANSACTIONID_HEADER, txId);
        }

        Response response = responseBuilder.build();
        LoggingUtil.logRestRequest(logger, auditLogger, req, response);

        return response;
    }
}
