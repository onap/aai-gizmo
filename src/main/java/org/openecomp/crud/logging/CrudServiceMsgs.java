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
package org.openecomp.crud.logging;

import com.att.eelf.i18n.EELFResourceManager;

import org.openecomp.cl.eelf.LogMessageEnum;

public enum CrudServiceMsgs implements LogMessageEnum {

  /**
   * Received request {0} {1} from {2}.  Sending response: {3}
   *
   * <p>Arguments:
   * {0} = operation
   * {1} = target URL
   * {2} = source
   * {3} = response code
   */
  PROCESS_REST_REQUEST,

  INVALID_OXM_FILE,
  INVALID_OXM_DIR,
  OXM_FILE_CHANGED,

  /**
   * Successfully loaded schema: {0}
   *
   * <p>Arguments:
   * {0} = oxm filename
   */
  LOADED_OXM_FILE,

  /**
   * Unable to load OXM schema: {0}
   *
   * <p>Arguments:
   * {0} = error
   */
  OXM_LOAD_ERROR,

  /**
   * Instantiate data access layer for graph data store type: {0} graph: {1} using hosts: {2}
   *
   * <p>Arguments:
   * {0} = Graph data store technology type
   * {1} = Graph name
   * {2} = Hosts list
   */
  INSTANTIATE_GRAPH_DAO,

  /**
   * Stopping ChampDAO...
   *
   * <p>Arguments:
   */
  STOPPING_CHAMP_DAO,

  /**
   * Unsupported graph database {0} specified.
   *
   * <p>Arguments:
   * {0} = Graph database back end.
   */
  INVALID_GRAPH_BACKEND,

  /**
   * Failure instantiating {0} graph database backend.  Cause: {1}
   *
   * <p>Arguments:
   * {0} - Graph database type.
   * {1} - Failure cause.
   */
  INSTANTIATE_GRAPH_BACKEND_ERR,

  /**
   * Failure instantiating CRUD Rest Service.  Cause: {0}
   *
   * <p>Arguments:
   * {0} - Failure cause.
   */
  INSTANTIATE_AUTH_ERR,

  /**
   * Any info log related to titan graph
   *
   * <p>Arguments:
   * {0} - Info.
   */
  TITAN_GRAPH_INFO,
  
  /**
   * Arguments:
   * {0} Opertaion
   * {1} URI
   * {2} = Exception
   */
  EXCEPTION_DURING_METHOD_CALL;


  /**
   * Static initializer to ensure the resource bundles for this class are loaded...
   */
  static {
    EELFResourceManager.loadMessageBundle("logging/CrudServiceMsgs");
  }
}
