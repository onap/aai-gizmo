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
package org.onap.crud.logging;

import com.att.eelf.i18n.EELFResourceManager;

import org.onap.aai.cl.eelf.LogMessageEnum;

public enum CrudServiceMsgs implements LogMessageEnum {

  /**
   * Received request {0} {1} from {2}. Sending response: {3}
   *
   * <p>
   * Arguments: {0} = operation {1} = target URL {2} = source {3} = response
   * code
   */
  PROCESS_REST_REQUEST,

  INVALID_OXM_FILE, INVALID_OXM_DIR, OXM_FILE_CHANGED, TRANSACTION,

  /**
   * Successfully loaded schema: {0}
   *
   * <p>
   * Arguments: {0} = oxm filename
   */
  LOADED_OXM_FILE,

  /**
   * Successfully loaded Edge Properties Files: {0}
   *
   * <p>
   * Arguments: {0} = oxm filename
   */
  LOADED_DB_RULE_FILE,

  /**
   * Unable to load OXM schema: {0}
   *
   * <p>
   * Arguments: {0} = error
   */
  OXM_LOAD_ERROR,

  /**
   * Stopping ChampDAO...
   *
   * <p>
   * Arguments:
   */
  STOPPING_CHAMP_DAO,

  /**
   * Failure instantiating CRUD Rest Service. Cause: {0}
   *
   * <p>
   * Arguments: {0} - Failure cause.
   */
  INSTANTIATE_AUTH_ERR,

  /**
   * Any info log related to ASYNC_DATA_SERVICE_INFO
   *
   * <p>Arguments:
   * {0} - Info.
   */
  ASYNC_DATA_SERVICE_INFO,
  
  /**
   * Any error log related to ASYNC_DATA_SERVICE_ERROR
   *
   * <p>Arguments:
   * {0} - Error.
   */
  ASYNC_DATA_SERVICE_ERROR,
  
  /**
   * Any info log related to CHAMP_BULK_OP_INFO
   *
   * <p>Arguments:
   * {0} - Info.
   */
  CHAMP_BULK_OP_INFO,
  
  /**
   * Any info log related to ASYNC_DATA_CACHE_INFO
   *
   * <p>Arguments:
   * {0} - Info.
   */
  ASYNC_DATA_CACHE_INFO,

  /**
   * Any error log related to ASYNC_DATA_CACHE_ERROR
   *
   * <p>Arguments:
   * {0} - Error.
   */
  ASYNC_DATA_CACHE_ERROR,
  
  /**
   * Any info log related to ASYNC_RESPONSE_CONSUMER_INFO
   *
   * <p>Arguments:
   * {0} - Info.
   */
  ASYNC_RESPONSE_CONSUMER_INFO,

  /**
   * Any error log related to ASYNC_RESPONSE_CONSUMER_ERROR
   *
   * <p>Arguments:
   * {0} - Error.
   */
  ASYNC_RESPONSE_CONSUMER_ERROR,

  /**
   * Arguments: {0} Opertaion {1} URI {2} = Exception
   */
  EXCEPTION_DURING_METHOD_CALL,
    
  /**
   * Schema Ingest properties file was not loaded properly
   */
  SCHEMA_INGEST_LOAD_ERROR;

  /**
   * Static initializer to ensure the resource bundles for this class are
   * loaded...
   */
  static {
    EELFResourceManager.loadMessageBundle("logging/CrudServiceMsgs");
  }
}
