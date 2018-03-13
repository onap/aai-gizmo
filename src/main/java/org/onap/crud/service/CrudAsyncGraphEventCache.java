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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.TimeUnit;

import org.onap.aai.cl.api.Logger;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.crud.logging.CrudServiceMsgs;
import org.onap.crud.service.CrudAsyncGraphDataService.CollectGraphResponse;
import org.onap.crud.util.CrudProperties;
import org.onap.crud.util.CrudServiceConstants;

/**
 * Self expiring Cache to hold request transactionIds . Events are expired
 * automatically after 2 seconds of request time out
 */
public class CrudAsyncGraphEventCache {
  private static Logger logger = LoggerFactory.getInstance().getLogger(CrudAsyncGraphEventCache
      .class.getName());

  private static Integer interval;

  static {
    // Set the cache eviction timeout = request timeout + 2 sec for the
    // buffer
    interval = CrudAsyncGraphDataService.DEFAULT_REQUEST_TIMEOUT + 2000;
    try {
      interval = Integer
          .parseInt(CrudProperties.get(CrudServiceConstants.CRD_ASYNC_REQUEST_TIMEOUT) + 2000);
    } catch (Exception ex) {
      logger.error(CrudServiceMsgs.ASYNC_DATA_CACHE_ERROR, "Unable to parse "
          + CrudServiceConstants.CRD_ASYNC_REQUEST_TIMEOUT + " error: "
          + ex.getMessage());
    }
  }

  private final static Cache<String, CollectGraphResponse> cache = CacheBuilder.newBuilder()
      .expireAfterWrite(interval, TimeUnit.MILLISECONDS).build();


  public static void put(String uuid, CollectGraphResponse collector) {
    cache.put(uuid, collector);

  }

  public static CollectGraphResponse get(String uuid) {
    return cache.getIfPresent(uuid);
  }

  public static void invalidate(String uuid) {
    cache.invalidate(uuid);
  }

}