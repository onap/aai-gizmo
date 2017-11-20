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
package org.onap.crud.util;

import org.onap.crud.exception.CrudException;

import javax.ws.rs.core.Response.Status;

public class CrudServiceUtil {


  public static Object validateFieldType(String value, Class clazz) throws CrudException {
    try {
      if (clazz.isAssignableFrom(Integer.class)) {
        return Integer.parseInt(value);
      } else if (clazz.isAssignableFrom(Long.class)) {
        return Long.parseLong(value);
      } else if (clazz.isAssignableFrom(Float.class)) {
        return Float.parseFloat(value);
      } else if (clazz.isAssignableFrom(Double.class)) {
        return Double.parseDouble(value);
      } else if (clazz.isAssignableFrom(Boolean.class)) {
		  
		// If the value is an IN/OUT direction, this gets seen as a boolean, so
        // check for that first.
        if (value.equals("OUT") || value.equals("IN")) {
          return value;
        }
		
        if (!value.equals("true") && !value.equals("false")) {
          throw new CrudException("Invalid propertry value: " + value, Status.BAD_REQUEST);
        }
        return Boolean.parseBoolean(value);
      } else {
        return value;
      }
    } catch (Exception e) {
      throw new CrudException("Invalid property value: " + value, Status.BAD_REQUEST);
    }
  }

}
