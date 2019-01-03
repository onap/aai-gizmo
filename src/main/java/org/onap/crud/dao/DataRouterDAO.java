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
package org.onap.crud.dao;

import org.eclipse.jetty.util.security.Password;
import org.onap.aai.restclient.client.RestClient;
import org.onap.aai.restclient.enums.RestAuthenticationMode;
import org.onap.crud.dao.champ.ChampDao;
import org.onap.crud.util.CrudServiceConstants;

public class DataRouterDAO extends ChampDao {
  public DataRouterDAO(String url, String certPassword) {
    try {
      String deobfuscatedCertPassword = certPassword.startsWith("OBF:")?Password.deobfuscate(certPassword):certPassword;
      client = new RestClient().authenticationMode(RestAuthenticationMode.SSL_CERT).validateServerHostname(false)
          .validateServerCertChain(false).clientCertFile(CrudServiceConstants.CRD_DATAROUTER_AUTH_FILE)
          .clientCertPassword(Password.deobfuscate(deobfuscatedCertPassword));

      baseObjectUrl = url + OBJECT_SUB_URL;
      baseRelationshipUrl = url + RELATIONSHIP_SUB_URL;
     } catch (Exception e) {
      System.out.println("Error setting up datarouter configuration");
      e.printStackTrace();
      System.exit(1);
    }
  }
}
