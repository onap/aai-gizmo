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
package org.openecomp.schema;

import org.eclipse.persistence.jaxb.JAXBContextProperties;
import org.eclipse.persistence.jaxb.dynamic.DynamicJAXBContext;
import org.eclipse.persistence.jaxb.dynamic.DynamicJAXBContextFactory;
import org.openecomp.cl.eelf.LoggerFactory;
import org.openecomp.crud.exception.CrudException;
import org.openecomp.crud.logging.CrudServiceMsgs;
import org.openecomp.crud.util.CrudServiceConstants;
import org.openecomp.crud.util.FileWatcher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBException;


public class OxmModelLoader {

  private static Map<String, DynamicJAXBContext> versionContextMap
      = new ConcurrentHashMap<String, DynamicJAXBContext>();
  private static Map<String, Timer> timers = new ConcurrentHashMap<String, Timer>();

  final static Pattern p = Pattern.compile("aai_oxm_(.*).xml");

  private static org.openecomp.cl.api.Logger logger = LoggerFactory.getInstance()
      .getLogger(OxmModelLoader.class.getName());

  public synchronized static void loadModels() throws CrudException {
    ClassLoader cl = OxmModelLoader.class.getClassLoader();
    ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
    Resource[] resources;
    try {
      resources = resolver.getResources("classpath*:/oxm/aai_oxm*.xml");
    } catch (IOException ex) {
      logger.error(CrudServiceMsgs.OXM_LOAD_ERROR, ex.getMessage());
      throw new CrudException("", Status.NOT_FOUND);
    }

    if (resources.length == 0) {
      logger.error(CrudServiceMsgs.OXM_LOAD_ERROR, "No OXM schema files found on classpath");
      throw new CrudException("Failed to load schema", Status.NOT_FOUND);
    }

    for (Resource resource : resources) {
      Matcher matcher = p.matcher(resource.getFilename());

      if (matcher.matches()) {
        try {
          OxmModelLoader.loadModel(matcher.group(1), resource);
        } catch (Exception e) {
          logger.error(CrudServiceMsgs.OXM_LOAD_ERROR, "Failed to load " + resource.getFilename()
              + ": " + e.getMessage());
          throw new CrudException("Failed to load schema", Status.NOT_FOUND);
        }
      }
    }
  }

  private static void addtimer(String version, File file) {
    TimerTask task = null;
    task = new FileWatcher(
        file) {
      protected void onChange(File file) {
        // here we implement the onChange
        logger.info(CrudServiceMsgs.OXM_FILE_CHANGED, file.getName());

        try {
          OxmModelLoader.loadModel(version, file);
        } catch (Exception e) {
          e.printStackTrace();
        }

      }
    };

    if (!timers.containsKey(version)) {
      Timer timer = new Timer("oxm-" + version);
      timer.schedule(task, new Date(), 10000);
      timers.put(version, timer);

    }
  }

  private synchronized static void loadModel(String version, File file)
      throws JAXBException, IOException {
    InputStream inputStream = new FileInputStream(file);
    loadModel(version, file.getName(), inputStream);
    addtimer(version, file);
  }

  private synchronized static void loadModel(String version, Resource resource)
      throws JAXBException, IOException {
    InputStream inputStream = resource.getInputStream();
    loadModel(version, resource.getFilename(), inputStream);
  }

  private synchronized static void loadModel(String version, String resourceName,
                                             InputStream inputStream)
      throws JAXBException, IOException {
    Map<String, Object> properties = new HashMap<String, Object>();
    properties.put(JAXBContextProperties.OXM_METADATA_SOURCE, inputStream);
    final DynamicJAXBContext jaxbContext = DynamicJAXBContextFactory
        .createContextFromOXM(Thread.currentThread().getContextClassLoader(), properties);
    versionContextMap.put(version, jaxbContext);
    logger.info(CrudServiceMsgs.LOADED_OXM_FILE, resourceName);
  }

  public static DynamicJAXBContext getContextForVersion(String version) throws CrudException {
    if (versionContextMap == null || versionContextMap.isEmpty()) {
      loadModels();
    } else if (!versionContextMap.containsKey(version)) {
      try {
        loadModel(version, new File(CrudServiceConstants.CRD_HOME_MODEL + "aai_oxm_"
            + version + ".xml"));
      } catch (Exception e) {
        throw new CrudException("", Status.NOT_FOUND);
      }
    }

    return versionContextMap.get(version);
  }

  public static Map<String, DynamicJAXBContext> getVersionContextMap() {
    return versionContextMap;
  }

  public static void setVersionContextMap(Map<String, DynamicJAXBContext> versionContextMap) {
    OxmModelLoader.versionContextMap = versionContextMap;
  }

}
