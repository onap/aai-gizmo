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

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.onap.crud.exception.CrudException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class RelationshipSchemaTest {

    final static Pattern rulesFilePattern = Pattern.compile("DbEdgeRules(.*).json");
    final static Pattern propsFilePattern = Pattern.compile("edge_properties_(.*).json");
    final static Pattern versionPattern = Pattern.compile(".*(v\\d+).json");


    @Test
    public void shouldLoadAllTheVersionsInDirectory() throws Exception {
        Map<String, RelationshipSchema> versionContextMap = new ConcurrentHashMap<>();
        loadRelations(versionContextMap);
        assertTrue(versionContextMap.keySet().size() >= 0);
    }

    @Test
    public void shouldContainValidTypes() throws Exception {
        Map<String, RelationshipSchema> versionContextMap = new ConcurrentHashMap<>();
        loadRelations(versionContextMap);
        assertTrue(versionContextMap.get("v11").isValidType("groupsResourcesIn"));
        assertTrue(versionContextMap.get("v11").isValidType("uses"));
        assertFalse(versionContextMap.get("v11").isValidType("notValidType"));
    }

    @Test
    public void shouldLookUpByRelation() throws Exception {
        Map<String, RelationshipSchema> versionContextMap = new ConcurrentHashMap<>();
        loadRelations(versionContextMap);
        assertNotNull(versionContextMap.get("v11").lookupRelation("availability-zone:complex:groupsResourcesIn"));
        assertTrue(versionContextMap.get("v11")
                .lookupRelation("availability-zone:complex:groupsResourcesIn").containsKey("prevent-delete"));
    }

    @Test
    public void shouldLookUpByRelationType() throws Exception {
        Map<String, RelationshipSchema> versionContextMap = new ConcurrentHashMap<>();
        loadRelations(versionContextMap);
        assertNotNull(versionContextMap.get("v11").lookupRelationType("groupsResourcesIn"));
        assertTrue(versionContextMap.get("v11")
                .lookupRelation("availability-zone:complex:groupsResourcesIn").containsKey("prevent-delete"));
    }

    private void loadRelations(Map<String, RelationshipSchema> map){
        ClassLoader classLoader = getClass().getClassLoader();
        File dir = new File(classLoader.getResource("model").getFile());
        File[] allFiles = dir.listFiles((d, name) ->
                (propsFilePattern.matcher(name).matches() || rulesFilePattern.matcher(name).matches()));

        Arrays.stream(allFiles).sorted(Comparator.comparing(File::getName))
                .collect(Collectors.groupingBy(f -> myMatcher(versionPattern, f.getName())))
                .forEach((e, f) -> map.put(e, jsonFilesLoader(f)));

    }


    private RelationshipSchema jsonFilesLoader (List<File> files) {
        List<String> fileContents = new ArrayList<>();
        RelationshipSchema rsSchema = null;
        for (File f : files) {
            fileContents.add(jsonToString(f));
        }

        try {
            rsSchema = new RelationshipSchema(fileContents);
        } catch (CrudException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rsSchema;
    }

    private String jsonToString (File file) {
        InputStream inputStream = null;
        String content = null;
        HashMap<String,Object> result = null;

        try {
            inputStream = new FileInputStream(file);
            content =  IOUtils.toString(inputStream, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return content;
    }

    private String myMatcher (Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.matches() ? m.group(1) : "";
    }
}