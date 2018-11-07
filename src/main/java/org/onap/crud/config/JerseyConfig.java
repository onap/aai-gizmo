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
package org.onap.crud.config;

import javax.annotation.PostConstruct;
import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.onap.crud.service.CrudRestService;
import org.onap.crud.service.JaxrsEchoService;
import org.springframework.stereotype.Component;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;

/**
 * Registers Crud Rest interface as JAX-RS endpoints.
 */

@ApplicationPath("/services")
@Component
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig(CrudRestService crudRestService, JaxrsEchoService jaxrsEchoService) {
        register(crudRestService);
        register(jaxrsEchoService);
    }
    
    @PostConstruct
    public void init() {
      // Register components where DI is needed
      this.SwaggerConfig();
    }
  private void SwaggerConfig() {
      this.register(ApiListingResource.class);
      this.register(SwaggerSerializers.class);

      BeanConfig swaggerConfigBean = new BeanConfig();
      swaggerConfigBean.setConfigId("Gizmo");
      swaggerConfigBean.setTitle("Gizmo Rest API ");
      swaggerConfigBean.setVersion("v1");
      swaggerConfigBean.setContact("Amdocs Inc.");
      swaggerConfigBean.setSchemes(new String[] { "https" });
      swaggerConfigBean.setBasePath("/services");
      swaggerConfigBean.setResourcePackage("org.onap.crud");
      swaggerConfigBean.setPrettyPrint(true);
      swaggerConfigBean.setScan(true);
    }

}
