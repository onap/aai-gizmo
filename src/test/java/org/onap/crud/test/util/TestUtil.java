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
package org.onap.crud.test.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Helper methods for locating and reading test data files.
 *
 */
public class TestUtil {

    public static Path getPath(String resourceFilename) throws URISyntaxException {
        URL resource = ClassLoader.getSystemResource(resourceFilename);
        if (resource != null) {
            return Paths.get(resource.toURI());
        }

        // If the resource is not found relative to the classpath, try to get it from the file system directly.
        File file = new File(resourceFilename);
        if (!file.exists()) {
            throw new RuntimeException("Resource does not exist: " + resourceFilename);
        }
        return file.toPath();
    }

    public static String getContentUtf8(Path filePath) throws IOException {
        return new String(Files.readAllBytes(filePath));
    }

    public static String getFileAsString(String resourceFilename) throws IOException, URISyntaxException {
        return getContentUtf8(getPath(resourceFilename));
    }
}
