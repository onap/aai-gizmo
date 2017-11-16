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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.openecomp.cl.eelf.LoggerFactory;
import org.openecomp.crud.exception.CrudException;
import org.openecomp.crud.logging.CrudServiceMsgs;
import org.openecomp.crud.util.CrudServiceConstants;
import org.openecomp.crud.util.FileWatcher;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

public class RelationshipSchemaLoader {

  private static Map<String, RelationshipSchema> versionContextMap = new ConcurrentHashMap<>();
  private static SortedSet<Integer> versions = new TreeSet<Integer>();
  private static Map<String, Timer> timers = new ConcurrentHashMap<String, Timer>();
  final static String edgePropsFiles = "edge_properties_";
  final static String fileExt = ".json";
  final static Pattern rulesFilePattern = Pattern.compile("DbEdgeRules(.*)" + fileExt);
  final static Pattern propsFilePattern = Pattern.compile(edgePropsFiles + "(.*)" + fileExt);
  final static Pattern versionPattern = Pattern.compile(".*(v\\d+)" + fileExt);


  private static org.openecomp.cl.api.Logger logger = LoggerFactory.getInstance()
          .getLogger(RelationshipSchemaLoader.class.getName());

  public synchronized static void loadModels() throws CrudException {
    load(rulesFilePattern, propsFilePattern);
  }

  public synchronized static void loadModels(String version) throws CrudException {
    String pattern = String.format(".*(%s)" + fileExt, version);
    load(Pattern.compile(pattern), Pattern.compile(edgePropsFiles + version + fileExt));
  }

  public static RelationshipSchema getSchemaForVersion(String version) throws CrudException {
    if (versionContextMap == null || versionContextMap.isEmpty()) {
      loadModels();
    } else if (!versionContextMap.containsKey(version)) {
      try {
        loadModels(version);
      } catch (Exception e) {
        throw new CrudException("", Status.NOT_FOUND);
      }
    }
    RelationshipSchema schema = versionContextMap.get(version);
    if (schema == null) {
      throw new CrudException("", Status.NOT_FOUND);
    } else
      return schema;
  }

  public static String getLatestSchemaVersion() throws CrudException {
    return "v" + versions.last();
  }

  public static Map<String, RelationshipSchema> getVersionContextMap() {
    return versionContextMap;
  }

  public static void setVersionContextMap(Map<String, RelationshipSchema> versionContextMap) {
    RelationshipSchemaLoader.versionContextMap = versionContextMap;
  }

  public static void resetVersionContextMap() {
    RelationshipSchemaLoader.versionContextMap = new ConcurrentHashMap<>();
  }


  private static void load(Pattern rulesPattern, Pattern edgePropsPattern) throws CrudException {
    ClassLoader cl = RelationshipSchemaLoader.class.getClassLoader();
    ResourcePatternResolver rulesResolver = new PathMatchingResourcePatternResolver(cl);
    List<Object> rulesFiles;
    String rulesDir = CrudServiceConstants.CRD_HOME_MODEL;
    try {

      // getResources method returns objects of type "Resource"
      // 1. We are getting all the objects from the classpath which has "DbEdgeRules" in the name.
      // 2. We run them through a filter and return only the objects which match the supplied pattern "p"
      // 3. We then collect the objects in a list. At this point we have a list of the kind of files we require.
      rulesFiles = Arrays.stream(rulesResolver.getResources("classpath*:/dbedgerules/DbEdgeRules*" + fileExt))
              .filter(r -> !myMatcher(rulesPattern, r.getFilename()).isEmpty())
              .collect(Collectors.toList());

      // This gets all the objects of type "File" from external directory (not on the classpath)
      // 1. From an external directory (one not on the classpath) we get all the objects of type "File"
      // 2. We only return the files whose names matched the supplied pattern "p2".
      // 3. We then collect all the objects in a list and add the contents of this list
      //    to the previous collection (rulesFiles)
      rulesFiles.addAll(Arrays.stream(new File(rulesDir).listFiles((d, name) ->
              edgePropsPattern.matcher(name).matches())).collect(Collectors.toList()));

      if (rulesFiles.isEmpty()) {
        logger.error(CrudServiceMsgs.INVALID_OXM_DIR, rulesDir);
        throw new FileNotFoundException("DbEdgeRules and edge_properties files were not found.");
      }

      // Sort and then group the files with their versions, convert them to the schema, and add them to versionContextMap
      // 1. Sort the files. We need the DbEdgeRules files to be before the edgeProperties files.
      // 2. Group the files with their versions. ie. v11 -> ["DbEdgeRule_v11.json", "edgeProperties_v11.json"].
      //    The "group method" returns a HashMap whose key is the version and the value is a list of objects.
      // 3. Go through each version and map the files into one schema using the "jsonFilesLoader" method.
      //      Also update the  "versionContextMap" with the version and it's schema.
      rulesFiles.stream().sorted(Comparator.comparing(RelationshipSchemaLoader::filename))
              .collect(Collectors.groupingBy(f -> myMatcher(versionPattern, filename(f))))
              .forEach((version, resourceAndFile) -> {
                if (resourceAndFile.size() == 2 ) {
                  versionContextMap.put(version, jsonFilesLoader(version, resourceAndFile));
                } else {
                  String filenames = resourceAndFile.stream().map(f-> filename(f)).collect(Collectors.toList()).toString();
                  String errorMsg = "Expecting a rules and a edge_properties files for " + version + ". Found: " + filenames;
                  logger.warn(CrudServiceMsgs.INVALID_OXM_FILE, errorMsg);
                }});
      logger.info(CrudServiceMsgs.LOADED_OXM_FILE, "Relationship Schema and Properties files: " + rulesFiles.stream().map(f -> filename(f)).collect(Collectors.toList()));
    } catch (IOException e) {
      logger.error(CrudServiceMsgs.INVALID_OXM_DIR, rulesDir);
      throw new CrudException("DbEdgeRules or edge_properties files were not found.", new FileNotFoundException());
    }
  }

  private static String filename (Object k) throws ClassCastException {
    if (k instanceof UrlResource){
      return ((UrlResource) k).getFilename();
    } else if (k instanceof File) {
      return ((File) k).getName();
    } else {
      throw new ClassCastException();
    }
  }

  private static RelationshipSchema jsonFilesLoader (String version, List<Object> files) {
    List<String> fileContents = new ArrayList<>();
    RelationshipSchema rsSchema = null;
    if (files.size() == 2) {
      for (Object file : files) {
        fileContents.add(jsonToRelationSchema(version, file));
        versions.add(Integer.parseInt(version.substring(1)));
      }

      try {
        rsSchema = new RelationshipSchema(fileContents);
      } catch (CrudException | IOException e) {
        e.printStackTrace();
        logger.error(CrudServiceMsgs.INVALID_OXM_FILE,
                files.stream().map(f -> filename(f)).collect(Collectors.toList()).toString(), e.getMessage());
      }
      return rsSchema;
    }
    return rsSchema;
  }

  private synchronized static void updateVersionContext(String version, RelationshipSchema rs){
    versionContextMap.put(version, rs);
  }

  private synchronized static String jsonToRelationSchema (String version, Object file) {
    InputStream inputStream = null;
    String content = null;

    try {
      if (file instanceof  UrlResource) {
        inputStream = ((UrlResource) file).getInputStream();
      } else {
        inputStream = new FileInputStream((File) file);
        addtimer(version, file);
      }
      content =  IOUtils.toString(inputStream, "UTF-8");
    } catch (IOException e) {
      e.printStackTrace();
    }
    return content;
  }

  private static void addtimer(String version, Object file) {
    TimerTask task = null;
    task = new FileWatcher(
            (File) file) {
      protected void onChange(File file) {
        // here we implement the onChange
        logger.info(CrudServiceMsgs.OXM_FILE_CHANGED, file.getName());

        try {
          // Cannot use the file object here because we also have to get the edge properties associated with that version.
          // The properties are stored in a different file.
          RelationshipSchemaLoader.loadModels(version);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    };

    if (!timers.containsKey(version)) {
      Timer timer = new Timer("db_edge_rules_" + version);
      timer.schedule(task, new Date(), 10000);
      timers.put(version, timer);

    }
  }

  private static String myMatcher (Pattern p, String s) {
    Matcher m = p.matcher(s);
    return m.matches() ? m.group(1) : "";
  }
}
