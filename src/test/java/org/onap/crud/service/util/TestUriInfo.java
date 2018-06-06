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

import java.net.URI;
import java.util.List;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

public class TestUriInfo implements UriInfo {

  @Override
  public URI getAbsolutePath() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public UriBuilder getAbsolutePathBuilder() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public URI getBaseUri() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public UriBuilder getBaseUriBuilder() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<Object> getMatchedResources() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<String> getMatchedURIs() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<String> getMatchedURIs(boolean arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getPath() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getPath(boolean arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public MultivaluedMap<String, String> getPathParameters() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public MultivaluedMap<String, String> getPathParameters(boolean arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<PathSegment> getPathSegments() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<PathSegment> getPathSegments(boolean arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public MultivaluedMap<String, String> getQueryParameters() {
    MultivaluedMap<String, String> map = new MultivaluedHashMap<String, String>();
    map.add("hostname", "myhost");
    return map;
  }

  @Override
  public MultivaluedMap<String, String> getQueryParameters(boolean arg0) {
    return null;
  }

  @Override
  public URI getRequestUri() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public UriBuilder getRequestUriBuilder() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public URI relativize(URI arg0) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public URI resolve(URI arg0) {
    // TODO Auto-generated method stub
    return null;
  }

}
