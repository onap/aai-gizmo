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
package org.onap.crud.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class CrudProperties {

  private static Properties properties;

  static {
    properties = new Properties();
    File file = new File(CrudServiceConstants.CRD_CONFIG_FILE);
    try {
      properties.load(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static String get(String key) {
    return properties.getProperty(key);
  }

  public static String get(String key, String defaultValue) {
    return properties.getProperty(key, defaultValue);
  }

  public static void put(String key, String value) {
    properties.setProperty(key, value);
    try (FileOutputStream fileOut = new FileOutputStream(new File(CrudServiceConstants.CRD_CONFIG_FILE))) {
      properties.store(fileOut, "Added property: " + key);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


}
