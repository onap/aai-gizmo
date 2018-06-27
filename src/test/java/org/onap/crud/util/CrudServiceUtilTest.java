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

import org.junit.Assert;
import org.junit.Test;
import org.onap.crud.parser.VertexPayload;
import org.onap.crud.service.util.TestHeaders;
import com.google.gson.JsonElement;

public class CrudServiceUtilTest {

    private final String putVertexPayload = "{" + "\"id\": \"test-uuid\"," + "\"type\": \"pserver\","
            + "\"properties\": {" + "fqdn: myhost.onap.com," + "hostname: myhost } }";

    @Test
    public void testMergeHeaderInFoToPayload() throws Exception {
        TestHeaders headers = new TestHeaders();
        // X-FromAppId is used to set the source of truth
        VertexPayload payload = VertexPayload.fromJson(putVertexPayload);

        JsonElement properties = CrudServiceUtil.mergeHeaderInFoToPayload(payload.getProperties(), headers, false);
        Assert.assertEquals("myhost.onap.com", properties.getAsJsonObject().get("fqdn").getAsString());
        Assert.assertEquals("myhost", properties.getAsJsonObject().get("hostname").getAsString());
        Assert.assertEquals("source-of-truth",
                properties.getAsJsonObject().get("last-mod-source-of-truth").getAsString());

        properties = CrudServiceUtil.mergeHeaderInFoToPayload(payload.getProperties(), headers, true);
        Assert.assertEquals("myhost.onap.com", properties.getAsJsonObject().get("fqdn").getAsString());
        Assert.assertEquals("myhost", properties.getAsJsonObject().get("hostname").getAsString());
        Assert.assertEquals("source-of-truth",
                properties.getAsJsonObject().get("last-mod-source-of-truth").getAsString());
        Assert.assertEquals("source-of-truth", properties.getAsJsonObject().get("source-of-truth").getAsString());
    }

}
