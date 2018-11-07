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
package org.onap.schema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.Response.Status;

import org.eclipse.persistence.dynamic.DynamicType;
import org.eclipse.persistence.internal.oxm.mappings.Descriptor;
import org.eclipse.persistence.jaxb.dynamic.DynamicJAXBContext;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.nodes.NodeIngestor;
import org.onap.aai.setup.ConfigTranslator;
import org.onap.aai.setup.SchemaLocationsBean;
import org.onap.aai.setup.Version;
import org.onap.crud.exception.CrudException;
import org.onap.crud.logging.CrudServiceMsgs;
import org.onap.schema.util.SchemaIngestPropertyReader;

import com.google.common.base.CaseFormat;

public class OxmModelLoader {

    private static Map<String, DynamicJAXBContext> versionContextMap =
            new ConcurrentHashMap<>();
    
    private static Map<String, HashMap<String, DynamicType>> xmlElementLookup = new ConcurrentHashMap<String, HashMap<String, DynamicType>>();

    static final Pattern p = Pattern.compile("aai_oxm_(.*).xml");
    static final Pattern versionPattern = Pattern.compile("V(\\d*)");

    private static org.onap.aai.cl.api.Logger logger =
            LoggerFactory.getInstance().getLogger(OxmModelLoader.class.getName());

    private OxmModelLoader() {
    }

    /**
     * Finds all OXM model files
     *
     * @throws SpikeException
     * @throws IOException
     *
     */
    public static synchronized void loadModels() throws CrudException {
        SchemaIngestPropertyReader schemaIngestPropertyReader = new SchemaIngestPropertyReader();

        SchemaLocationsBean schemaLocationsBean = new SchemaLocationsBean();
        schemaLocationsBean.setNodeDirectory(schemaIngestPropertyReader.getNodeDir());
        ConfigTranslator configTranslator = new OxmModelConfigTranslator(schemaLocationsBean);
        NodeIngestor nodeIngestor = new NodeIngestor(configTranslator);

        if (logger.isDebugEnabled()) {
            logger.debug("Loading OXM Models");
        }

        for (Version oxmVersion : Version.values()) {
            DynamicJAXBContext jaxbContext = nodeIngestor.getContextForVersion(oxmVersion);
            if (jaxbContext != null) {
                loadModel(oxmVersion.toString().toLowerCase(), jaxbContext);
            }
        }
    }


    private static synchronized void loadModel(String oxmVersion, DynamicJAXBContext jaxbContext) {
        versionContextMap.put(oxmVersion, jaxbContext);
        loadXmlLookupMap(oxmVersion, jaxbContext);
        logger.info(CrudServiceMsgs.LOADED_OXM_FILE, oxmVersion);
    }

    /**
     * Retrieves the JAXB context for the specified OXM model version.
     *
     * @param version - The OXM version that we want the JAXB context for.
     *
     * @return - A JAXB context derived from the OXM model schema.
     *
     * @throws SpikeException
     */
    public static DynamicJAXBContext getContextForVersion(String version) throws CrudException {

        // If we haven't already loaded in the available OXM models, then do so now.
        if (versionContextMap == null || versionContextMap.isEmpty()) {
            loadModels();
        } else if (!versionContextMap.containsKey(version)) {
			logger.error(CrudServiceMsgs.OXM_LOAD_ERROR, "Error loading oxm model: " + version);
            throw new CrudException("Error loading oxm model: " + version, Status.INTERNAL_SERVER_ERROR);
        }

        return versionContextMap.get(version);
    }

    public static String getLatestVersion() throws CrudException {

        // If we haven't already loaded in the available OXM models, then do so now.
        if (versionContextMap == null || versionContextMap.isEmpty()) {
            loadModels();
        }

        // If there are still no models available, then there's not much we can do...
        if (versionContextMap.isEmpty()) {
			logger.error(CrudServiceMsgs.OXM_LOAD_ERROR, "No available OXM schemas to get latest version for.");
            throw new CrudException("No available OXM schemas to get latest version for.",
                    Status.INTERNAL_SERVER_ERROR);
        }

        // Iterate over the available model versions to determine which is the most
        // recent.
        Integer latestVersion = null;
        String latestVersionStr = null;
        for (String versionKey : versionContextMap.keySet()) {

            Matcher matcher = versionPattern.matcher(versionKey.toUpperCase());
            if (matcher.find()) {

                int currentVersion = Integer.parseInt(matcher.group(1));

                if ((latestVersion == null) || (currentVersion > latestVersion)) {
                    latestVersion = currentVersion;
                    latestVersionStr = versionKey;
                }
            }
        }

        return latestVersionStr;
    }

    /**
     * Retrieves the list of all Loaded OXM versions.
     *
     * @return - A List of Strings of all loaded OXM versions.
     *
     * @throws CrudException
     */
    public static List<String> getLoadedOXMVersions() throws CrudException {

        // If we haven't already loaded in the available OXM models, then do so now.
        if (versionContextMap == null || versionContextMap.isEmpty()) {
            loadModels();
        }

        // If there are still no models available, then there's not much we can do...
        if (versionContextMap.isEmpty()) {
            logger.error(CrudServiceMsgs.OXM_LOAD_ERROR, "No available OXM schemas to get versions for.");
            throw new CrudException("No available OXM schemas to get versions for.",
                    Status.INTERNAL_SERVER_ERROR);
        }

        List<String> versions = new ArrayList<String> ();
        for (String versionKey : versionContextMap.keySet()) {

            Matcher matcher = versionPattern.matcher(versionKey.toUpperCase());
            if (matcher.find()) {
                versions.add ( "V" + matcher.group ( 1 ) );
            }
        }
        return versions;
    }

    /**
     * Retrieves the map of all JAXB context objects that have been created by importing the
     * available OXM model schemas.
     *
     * @return - Map of JAXB context objects.
     */
    public static Map<String, DynamicJAXBContext> getVersionContextMap() {
        return versionContextMap;
    }

    /**
     * Assigns the map of all JAXB context objects.
     *
     * @param versionContextMap
     */
    public static void setVersionContextMap(Map<String, DynamicJAXBContext> versionContextMap) {
        OxmModelLoader.versionContextMap = versionContextMap;
    }
    
   
    public static void loadXmlLookupMap(String version, DynamicJAXBContext jaxbContext )  {
      
      @SuppressWarnings("rawtypes")
      List<Descriptor> descriptorsList = jaxbContext.getXMLContext().getDescriptors();     
      HashMap<String,DynamicType> types = new HashMap<String,DynamicType>();

      for (@SuppressWarnings("rawtypes")
      Descriptor desc : descriptorsList) {

        DynamicType entity = jaxbContext.getDynamicType(desc.getAlias());
        String entityName = desc.getDefaultRootElement();
        types.put(entityName, entity);
      }
      xmlElementLookup.put(version, types);
    }
    
    
  public static DynamicType getDynamicTypeForVersion(String version, String type) throws CrudException {

    DynamicType dynamicType;
    // If we haven't already loaded in the available OXM models, then do so now.
    if (versionContextMap == null || versionContextMap.isEmpty()) {
      loadModels();
    } else if (!versionContextMap.containsKey(version)) {
      logger.error(CrudServiceMsgs.OXM_LOAD_ERROR, "Error loading oxm model: " + version);
      throw new CrudException("Error loading oxm model: " + version, Status.INTERNAL_SERVER_ERROR);
    }

    // First try to match the Java-type based on hyphen to camel case translation
    String javaTypeName = CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, type);
    dynamicType = versionContextMap.get(version).getDynamicType(javaTypeName);

    //Attempt to lookup in xml elements
    if (xmlElementLookup.containsKey(version)) {
      if (dynamicType == null) {
        // Try to lookup by xml root element by exact match
        dynamicType = xmlElementLookup.get(version).get(type);
      }

      if (dynamicType == null) {
        // Try to lookup by xml root element by lowercase
        dynamicType = xmlElementLookup.get(version).get(type.toLowerCase());
      }

      if (dynamicType == null) {
        // Direct lookup as java-type name
        dynamicType = versionContextMap.get(version).getDynamicType(type);
      }
    }

    return dynamicType;

  }
}
