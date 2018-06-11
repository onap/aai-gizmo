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

import org.eclipse.persistence.dynamic.DynamicType;
import org.eclipse.persistence.internal.helper.DatabaseField;
import org.eclipse.persistence.jaxb.dynamic.DynamicJAXBContext;
import org.eclipse.persistence.mappings.DatabaseMapping;
import org.junit.Assert;
import org.junit.Test;

public class OxmModelLoaderTest {

    @Test
    public void testLoadingMultipleOxmFiles() throws Exception {
        OxmModelLoader.loadModels();

        DynamicJAXBContext jaxbContext = OxmModelLoader.getContextForVersion(OxmModelLoader.getLatestVersion());

        DynamicType pserver = jaxbContext.getDynamicType("Pserver");
        DynamicType genericVnf = jaxbContext.getDynamicType("GenericVnf");

        Assert.assertNotNull(pserver);
        Assert.assertNotNull(genericVnf);

        DatabaseMapping mapping = pserver.getDescriptor().getMappings().firstElement();
        if (mapping.isAbstractDirectMapping()) {
            DatabaseField f = mapping.getField();
            String keyName = f.getName().substring(0, f.getName().indexOf("/"));
            Assert.assertEquals(keyName, "hostname");
        }

        mapping = genericVnf.getDescriptor().getMappings().firstElement();
        if (mapping.isAbstractDirectMapping()) {
            DatabaseField f = mapping.getField();
            String keyName = f.getName().substring(0, f.getName().indexOf("/"));
            Assert.assertEquals(keyName, "vnf-id");
        }

    }
}