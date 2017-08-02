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

import org.apache.commons.io.IOUtils;
import org.openecomp.cl.eelf.LoggerFactory;
import org.openecomp.crud.exception.CrudException;
import org.openecomp.crud.logging.CrudServiceMsgs;
import org.openecomp.crud.util.CrudServiceConstants;
import org.openecomp.crud.util.FileWatcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBException;


public class RelationshipSchemaLoader {

  private static Map<String, RelationshipSchema> versionContextMap
      = new ConcurrentHashMap<String, RelationshipSchema>();
  private static SortedSet<Integer> versions = new TreeSet<Integer>();
  private static Map<String, Timer> timers = new ConcurrentHashMap<String, Timer>();

  final static Pattern filePattern = Pattern.compile("aai_relationship_(.*).json");


  private static org.openecomp.cl.api.Logger logger = LoggerFactory.getInstance()
      .getLogger(RelationshipSchemaLoader.class.getName());

  public synchronized static void loadModels() {

    File[] listOfFiles = new File(CrudServiceConstants.CRD_HOME_MODEL).listFiles();

    if (listOfFiles != null) {
      for (File file : listOfFiles) {
        if (file.isFile()) {
          Matcher matcher = filePattern.matcher(file.getName());
          if (matcher.matches()) {
            try {
              RelationshipSchemaLoader.loadModel(matcher.group(1), file);
            } catch (Exception e) {
              logger.error(CrudServiceMsgs.INVALID_OXM_FILE, file.getName(), e.getMessage());
            }
          }

        }
      }
    } else {
      logger.error(CrudServiceMsgs.INVALID_OXM_DIR, CrudServiceConstants.CRD_HOME_MODEL);
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
          RelationshipSchemaLoader.loadModel(version, file);
        } catch (Exception e) {
          e.printStackTrace();
        }

      }
    };

    if (!timers.containsKey(version)) {
      Timer timer = new Timer("aai_relationship_" + version);
      timer.schedule(task, new Date(), 10000);
      timers.put(version, timer);

    }
  }

  private synchronized static void loadModel(String version, File file)
      throws JAXBException, IOException, CrudException {

    InputStream inputStream = new FileInputStream(file);
    String content = IOUtils.toString(inputStream, "UTF-8");
    versionContextMap.put(version, new RelationshipSchema(content));
    addtimer(version, file);
    versions.add(Integer.parseInt(version.substring(1)));
  }

  public static RelationshipSchema getSchemaForVersion(String version) throws CrudException {
    if (versionContextMap == null || versionContextMap.isEmpty()) {
      loadModels();
    } else if (!versionContextMap.containsKey(version)) {
      try {
        loadModel(version, new File(CrudServiceConstants.CRD_HOME_MODEL + "aai_relationship_"
            + version + ".json"));
      } catch (Exception e) {
        throw new CrudException("", Status.NOT_FOUND);
      }
    }

    return versionContextMap.get(version);
  }

  public static String getLatestSchemaVersion() throws CrudException {
    return "v" + versions.last();
  }

  public static Map<String, RelationshipSchema> getVersionContextMap() {
    return versionContextMap;
  }

  public static void setVersionContextMap(HashMap<String, RelationshipSchema> versionContextMap) {
    RelationshipSchemaLoader.versionContextMap = versionContextMap;
  }

  public static void main(String[] args) throws FileNotFoundException, Exception {
    File initialFile = new File("C:\\Software\\gizmo\\src\\main\\java\\org\\openecomp\\schema\\vio.json");

    loadModel("v8", initialFile);
  }

}
