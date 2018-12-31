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
package org.onap.crud;

import java.util.Collections;
import java.util.HashMap;
import javax.annotation.PostConstruct;
import org.eclipse.jetty.util.security.Password;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.env.Environment;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Crud application class - SpringApplication.run
 */
@SpringBootApplication
@EnableSwagger2
@ImportResource({"file:${SERVICE_BEANS}/*.xml"})
public class CrudApplication extends SpringBootServletInitializer{// NOSONAR
    @Autowired
    private Environment env;

    public static void main(String[] args) {// NOSONAR
        String keyStorePassword = System.getProperty("KEY_STORE_PASSWORD");
        if(keyStorePassword==null || keyStorePassword.isEmpty()){
          throw new RuntimeException("Env property KEY_STORE_PASSWORD not set");
        }
        HashMap<String, Object> props = new HashMap<>();
        String deobfuscatedKeyStorePassword = keyStorePassword.startsWith("OBF:")?Password.deobfuscate(keyStorePassword):keyStorePassword;
        props.put("server.ssl.key-store-password", deobfuscatedKeyStorePassword);
        
        String trustStoreLocation = System.getProperty("TRUST_STORE_LOCATION");
        String trustStorePassword = System.getProperty("TRUST_STORE_PASSWORD");
        if(trustStoreLocation!=null && trustStorePassword !=null){
            trustStorePassword = trustStorePassword.startsWith("OBF:")?Password.deobfuscate(trustStorePassword):trustStorePassword;
            props.put("server.ssl.trust-store", trustStoreLocation);
            props.put("server.ssl.trust-store-password", trustStorePassword);
        } 
        
        props.put("schema.service.ssl.key-store-password", deobfuscatedKeyStorePassword);
        props.put("schema.service.ssl.trust-store-password", deobfuscatedKeyStorePassword);
        
       
        
        new CrudApplication()
            .configure(new SpringApplicationBuilder(CrudApplication.class).properties(props))
            .run(args);
    }

    /**
     * Set required trust store system properties using values from application.properties
     */
    @PostConstruct
    public void setSystemProperties() {
        String trustStorePath = env.getProperty("server.ssl.key-store");
        if (trustStorePath != null) {
            String trustStorePassword = env.getProperty("server.ssl.key-store-password");

            if (trustStorePassword != null) {
                System.setProperty("javax.net.ssl.trustStore", trustStorePath);
                System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
            } else {
                throw new IllegalArgumentException("Env property server.ssl.key-store-password not set");
            }
        }
    }
    public static final Contact DEFAULT_CONTACT = new Contact("Amdocs", "http://www.amdocs.com", "noreply@amdocs.com");

    public static final ApiInfo DEFAULT_API_INFO = new ApiInfo("AAI NCSO Adapter Service", "AAI NCSO Adapter Service.",
        "1.0", "urn:tos", DEFAULT_CONTACT, "Apache 2.0", "API license URL", Collections.emptyList());

    public Docket api() {
      return new Docket(DocumentationType.SWAGGER_2).apiInfo(DEFAULT_API_INFO).select().paths(PathSelectors.any())
          .apis(RequestHandlerSelectors.basePackage("org.onap.crud")).build();
    }
    
   
}
