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
package org.onap.schema;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.onap.aai.exceptions.AAIException;
import org.onap.aai.serialization.db.EdgeProperty;
import org.onap.aai.serialization.db.EdgeRule;
import org.onap.aai.serialization.db.EdgeRules;
import org.onap.aai.serialization.db.EdgeType;
import org.onap.crud.exception.CrudException;
import org.onap.crud.service.AaiResourceService;
import org.onap.crud.service.EdgePayload;

import com.google.gson.JsonElement;

public class AaiResourceServiceTest {

  public AaiResourceService aaiResSvc = null;
  
  
  @Before
  public void setup() {
    System.setProperty("AJSC_HOME", ".");
    System.setProperty("BUNDLECONFIG_DIR", "src/test/resources/bundleconfig-local");
    
    aaiResSvc = new AaiResourceService();
  }
  
  
  /**
   * This test validates that we can apply db edge rules against an edge request
   * payload and have the properties defined in the edge rules merged into the
   * payload.
   * 
   * @throws CrudException
   * @throws AAIException
   */
  @Test
  public void applyEdgeRulesToPayloadTest() throws CrudException, AAIException {
    
    String content = "{" +
        "\"source\": \"services/inventory/v8/l-interface/369553424\", " +
        "\"target\": \"services/inventory/v8/logical-link/573444128\"," +
        "\"properties\": {" +
        "}" +
     "}";
    
    // Convert our simulated payload to an EdgePayload object.
    EdgePayload payload = EdgePayload.fromJson(content);
    
    // Now, apply the db edge rules against our edge payload.
    EdgePayload payloadAfterEdgeRules = aaiResSvc.applyEdgeRulesToPayload(payload);
    
    EdgeRules rules = EdgeRules.getInstance();
    EdgeRule rule = rules.getEdgeRule(EdgeType.COUSIN, "l-interface", "logical-link");
    Map<EdgeProperty, String> edgeProps = rule.getEdgeProperties();
    
    // Validate that the properties defined in the DB edge rules show up in our
    // final payload.
    for(EdgeProperty key : edgeProps.keySet()) {
      assertTrue(payloadAfterEdgeRules.toString().contains(key.toString()));
    }
  }
  
  
  /**
   * This test validates that trying to apply edge rules where there is no
   * db edge rules entry for the supplied source and target vertex types
   * produces an exception.
   * 
   * @throws CrudException
   */
  @Test
  public void noRuleForEdgeTest() throws CrudException {
        
    String content = "{" +
        "\"source\": \"services/inventory/v8/commodore-64/12345\", " +
        "\"target\": \"services/inventory/v8/jumpman/67890\"," +
        "\"properties\": {" +
        "}" +
     "}";
    
    // Convert our simulated payload to an EdgePayload object.
    EdgePayload payload = EdgePayload.fromJson(content);
    
    // Now, apply the db edge rules against our edge payload.
    try {
      aaiResSvc.applyEdgeRulesToPayload(payload);
      
    } catch (CrudException e) {
      
      // We expected an exception since there is no rule for our made up vertices..
      assertTrue(e.getMessage().contains("No edge rules for"));
      return;
    }
    
    // If we're here then something unexpected happened...
    fail();
  }
  
  
  /**
   * This test validates that it is possible to merge client supplied and edge rule
   * supplied properties into one edge property list.
   * 
   * @throws Exception
   */
  @Test
  public void mergeEdgePropertiesTest() throws Exception {
        
    String content = "{" +
        "\"source\": \"services/inventory/v8/l-interface/369553424\", " +
        "\"target\": \"services/inventory/v8/logical-link/573444128\"," +
        "\"properties\": {" +
          "\"multiplicity\": \"many\"," +
          "\"is-parent\": true," +
          "\"uses-resource\": \"true\"," +
          "\"has-del-target\": \"true\"" +
        "}" +
     "}";
    
    EdgePayload payload = EdgePayload.fromJson(content);
    EdgeRules rules = EdgeRules.getInstance();
    EdgeRule rule = rules.getEdgeRule(EdgeType.COUSIN, "l-interface", "logical-link");
    Map<EdgeProperty, String> edgeProps = rule.getEdgeProperties();

    // Merge the client supplied properties with the properties defined in the DB edge rules.
    JsonElement mergedProperties = 
        aaiResSvc.mergeProperties(payload.getProperties(), rule.getEdgeProperties());
    
    // Now, validate that the resulting set of properties contains both the client and edge
    // rule supplied properties.
    String mergedPropertiesString = mergedProperties.toString();
    assertTrue("Client supplied property 'multiplicity' is missing from merged properties set",
               mergedPropertiesString.contains("multiplicity"));
    assertTrue("Client supplied property 'is-parent' is missing from merged properties set",
               mergedPropertiesString.contains("is-parent"));
    assertTrue("Client supplied property 'uses-resource' is missing from merged properties set",
               mergedPropertiesString.contains("uses-resource"));
    assertTrue("Client supplied property 'has-del-target' is missing from merged properties set",
               mergedPropertiesString.contains("has-del-target"));
    
    for(EdgeProperty key : edgeProps.keySet()) {
      assertTrue("Edge rule supplied property '" + key.toString() + "' is missing from merged properties set",
                 mergedPropertiesString.contains(key.toString()));
    }
  }
  
  /**
   * This test validates that if we try to merge client supplied edge properties
   * with the properties defined in the db edge rules, and there is a conflict,
   * then the merge will fail.
   * 
   * @throws Exception
   */
  @Test
  public void mergeEdgePropertiesConflictTest() throws Exception {
        
    String content = "{" +
        "\"source\": \"services/inventory/v8/l-interface/369553424\", " +
        "\"target\": \"services/inventory/v8/logical-link/573444128\"," +
        "\"properties\": {" +
          "\"contains-other-v\": \"OUT\"" +
        "}" +
     "}";
    
    EdgePayload payload = EdgePayload.fromJson(content);
    EdgeRules rules = EdgeRules.getInstance();
    EdgeRule rule = rules.getEdgeRule(EdgeType.COUSIN, "l-interface", "logical-link");

    try {
      
      // Try to merge our client supplied properties with the properties defined
      // in the db edge rules.
      aaiResSvc.mergeProperties(payload.getProperties(), rule.getEdgeProperties());
    
    } catch (CrudException e) {
      
      // We should have gotten an exception because we are trying to set a parameter which is
      // already defined in the db edge rules, so if we're here then we are good.
      return;
    }

    // If we made it here then we were allowed to set a property that is already defined
    // in the db edge rules, which we should not have...
    fail();
  }
  



}
