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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.io.IOUtils;
import org.onap.aai.cl.eelf.LoggerFactory;
import org.onap.aai.edges.EdgeIngestor;
import org.onap.aai.edges.EdgeRule;
import org.onap.aai.edges.exceptions.EdgeRuleNotFoundException;
import org.onap.aai.setup.ConfigTranslator;
import org.onap.aai.setup.SchemaLocationsBean;
import org.onap.aai.setup.Version;
import org.onap.crud.exception.CrudException;
import org.onap.crud.logging.CrudServiceMsgs;
import org.onap.schema.util.SchemaIngestPropertyReader;
import com.google.common.collect.Multimap;

public class EdgeRulesLoader {

    private static Map<String, RelationshipSchema> versionContextMap =
            new ConcurrentHashMap<> ();

    static final Pattern versionPattern = Pattern.compile ( "V(\\d*)" );
    static final String propsPrefix = "edge_properties_";
    static final String propsSuffix = ".json";
    final static Pattern propsFilePattern = Pattern.compile ( propsPrefix + "(.*)" + propsSuffix );
    final static Pattern propsVersionPattern = Pattern.compile ( "v\\d*" );

    private static org.onap.aai.cl.api.Logger logger =
            LoggerFactory.getInstance ().getLogger ( EdgeRulesLoader.class.getName () );

    private EdgeRulesLoader () {
    }

    /**
     * Finds all DB Edge Rules and Edge Properties files for all OXM models.
     *
     * @throws CrudException
     */
    public static synchronized void loadModels () throws CrudException {
        SchemaIngestPropertyReader schemaIngestPropertyReader = new SchemaIngestPropertyReader ();
        SchemaLocationsBean schemaLocationsBean = new SchemaLocationsBean ();
        schemaLocationsBean.setEdgeDirectory ( schemaIngestPropertyReader.getEdgeDir () );
        ConfigTranslator configTranslator = new OxmModelConfigTranslator ( schemaLocationsBean );
        EdgeIngestor edgeIngestor = new EdgeIngestor ( configTranslator );
        Map<String, File> propFiles = edgePropertyFiles(schemaIngestPropertyReader);

        if (logger.isDebugEnabled ()) {
            logger.debug ( "Loading DB Edge Rules" );
        }

        for (String version : OxmModelLoader.getLoadedOXMVersions ()) {
            try {
                loadModel ( Version.valueOf ( version ), edgeIngestor, propFiles );
            } catch (IOException | EdgeRuleNotFoundException e) {
                throw new CrudException(e.getMessage (), e);
            }
        }
    }

    /**
     * Loads DB Edge Rules and Edge Properties for a given version.
     *
     * @throws CrudException
     */

    public static synchronized void loadModels ( String v ) throws CrudException {
        SchemaIngestPropertyReader schemaIngestPropertyReader = new SchemaIngestPropertyReader ();
        SchemaLocationsBean schemaLocationsBean = new SchemaLocationsBean ();
        schemaLocationsBean.setEdgeDirectory ( schemaIngestPropertyReader.getEdgeDir () );
        ConfigTranslator configTranslator = new OxmModelConfigTranslator ( schemaLocationsBean );
        EdgeIngestor edgeIngestor = new EdgeIngestor ( configTranslator );
        String version = v.toUpperCase ();
        Map<String, File> propFiles = edgePropertyFiles(schemaIngestPropertyReader);

        if (logger.isDebugEnabled ()) {
            logger.debug ( "Loading DB Edge Rules " );
        }

        try {
            loadModel ( Version.valueOf ( version ), edgeIngestor, propFiles );
        } catch (IOException | EdgeRuleNotFoundException e) {
            throw new CrudException(e.getMessage (), Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Retrieves the DB Edge Rule relationship schema for a given version.
     *
     * @param version - The OXM version that we want the DB Edge Rule for.
     * @return - A RelationshipSchema of the DB Edge Rule for the OXM version.
     * @throws CrudException
     */
    public static RelationshipSchema getSchemaForVersion ( String version ) throws CrudException {

        // If we haven't already loaded in the available OXM models, then do so now.
        if (versionContextMap == null || versionContextMap.isEmpty ()) {
            loadModels ();
        } else if (!versionContextMap.containsKey ( version )) {
            logger.error ( CrudServiceMsgs.OXM_LOAD_ERROR, "Error loading DB Edge Rules for: " + version );
            throw new CrudException ( "Error loading DB Edge Rules for: " + version, Status.NOT_FOUND );
        }

        return versionContextMap.get ( version );
    }

    /**
     * Retrieves the DB Edge Rule relationship schema for all loaded OXM versions.
     *
     * @return - A Map of the OXM version and it's corresponding RelationshipSchema of the DB Edge Rule.
     * @throws CrudException
     */
    public static Map<String, RelationshipSchema> getSchemas () throws CrudException {

        // If we haven't already loaded in the available OXM models, then do so now.
        if (versionContextMap == null || versionContextMap.isEmpty ()) {
            loadModels ();
        }
        return versionContextMap;
    }

    /**
     * Returns the latest available DB Edge Rule version.
     *
     * @return - A Map of the OXM version and it's corresponding RelationshipSchema of the DB Edge Rule.
     * @throws CrudException
     */
    public static String getLatestSchemaVersion () throws CrudException {

        // If we haven't already loaded in the available OXM models, then do so now.
        if (versionContextMap == null || versionContextMap.isEmpty ()) {
            loadModels ();
        }

        // If there are still no models available, then there's not much we can do...
        if (versionContextMap.isEmpty ()) {
            logger.error ( CrudServiceMsgs.OXM_LOAD_ERROR, "No available DB Edge Rules to get latest version for." );
            throw new CrudException ( "No available DB Edge Rules to get latest version for.",
                    Status.INTERNAL_SERVER_ERROR );
        }

        // Iterate over the available model versions to determine which is the most
        // recent.
        Integer latestVersion = null;
        String latestVersionStr = null;
        for (String versionKey : versionContextMap.keySet ()) {

            Matcher matcher = versionPattern.matcher ( versionKey.toUpperCase () );
            if (matcher.find ()) {

                int currentVersion = Integer.parseInt ( matcher.group ( 1 ) );

                if ((latestVersion == null) || (currentVersion > latestVersion)) {
                    latestVersion = currentVersion;
                    latestVersionStr = versionKey;
                }
            }
        }

        return latestVersionStr;
    }

    /**
     * Reset the loaded DB Edge Rule schemas
     *
     */

    public static void resetSchemaVersionContext () {
        versionContextMap = new ConcurrentHashMap<> ();
    }

    private static synchronized void loadModel ( Version version, EdgeIngestor edgeIngestor, Map<String, File> props)
            throws IOException, CrudException, EdgeRuleNotFoundException {

        Multimap<String, EdgeRule> edges = edgeIngestor.getAllRules ( version );
        String edgeProps;
        if (props.get ( version.toString().toLowerCase () ) != null) {
            edgeProps = IOUtils.toString ( new FileInputStream ( props.get ( version.toString().toLowerCase () ) ), "UTF-8" );
        } else {
            throw new FileNotFoundException ( "The Edge Properties file for OXM version " + version + "was not found." );
        }
        if (edges != null) {
            RelationshipSchema rs = new RelationshipSchema ( edges, edgeProps );
            versionContextMap.put ( version.toString ().toLowerCase (), rs );
            logger.info ( CrudServiceMsgs.LOADED_DB_RULE_FILE, version.toString () );
        }
    }

    private static Map<String, File> edgePropertyFiles ( SchemaIngestPropertyReader dir ) throws CrudException {
        Map<String, File> propsFiles = Arrays.stream ( new File ( dir.getEdgePropsDir () )
                .listFiles ( ( d, name ) -> propsFilePattern.matcher ( name ).matches () ) )
                .collect ( Collectors.toMap ( new Function<File, String> () {
                    public String apply ( File f ) {
                        Matcher m1 = propsVersionPattern.matcher ( f.getName () );
                        m1.find ();
                        return m1.group ( 0 );
                    }
                }, f -> f ) );
        return  propsFiles;
    }

}
