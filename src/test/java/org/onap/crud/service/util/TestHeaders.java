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
package org.onap.crud.service.util;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

public class TestHeaders implements HttpHeaders {

  private MultivaluedMap<String, String> headers;

  public TestHeaders() {
    headers = new MultivaluedHashMap<String, String>();
    headers.add("X-FromMsId", "sending-service");
    headers.add("X-FromAppId", "source-of-truth");
    headers.add("X-TransactionId", "1234567890");
  }

  @Override
  public List<Locale> getAcceptableLanguages() {
    return null;
  }

  @Override
  public List<MediaType> getAcceptableMediaTypes() {
    return null;
  }

  @Override
  public Map<String, Cookie> getCookies() {
    return null;
  }

  @Override
  public Date getDate() {
    return null;
  }

  @Override
  public String getHeaderString(String arg0) {
    return null;
  }

  @Override
  public Locale getLanguage() {
    return null;
  }

  @Override
  public int getLength() {
    return 0;
  }

  @Override
  public MediaType getMediaType() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<String> getRequestHeader(String arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public MultivaluedMap<String, String> getRequestHeaders() {
    return headers;
  }

  public void clearRequestHeader(String... keys) {
      for (String key : keys) {
          headers.remove(key);
      }
  }

}
