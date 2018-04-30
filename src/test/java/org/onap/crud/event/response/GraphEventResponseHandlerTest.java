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
package org.onap.crud.event.response;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.onap.crud.event.envelope.GraphEventEnvelope;
import org.onap.crud.test.util.TestUtil;
import com.google.gson.Gson;

public class GraphEventResponseHandlerTest {

    @Test
    public void testPolicyViolationsNotDetected() throws Exception {
        String expectedEnvelope = TestUtil.getFileAsString("event/event-envelope-sentinel-no-violations.json");
        Gson gson = new Gson();
        GraphEventEnvelope envelope = gson.fromJson(expectedEnvelope, GraphEventEnvelope.class);

        GraphEventResponseHandler graphEventResponseHandler = new GraphEventResponseHandler();
        assertThat(graphEventResponseHandler.hasPolicyViolations(envelope), is(false));
    }

    @Test
    public void testPolicyViolationsDetected() throws Exception {
        String expectedEnvelope = TestUtil.getFileAsString("event/event-envelope-sentinel.json");
        Gson gson = new Gson();
        GraphEventEnvelope envelope = gson.fromJson(expectedEnvelope, GraphEventEnvelope.class);

        GraphEventResponseHandler graphEventResponseHandler = new GraphEventResponseHandler();
        assertThat(graphEventResponseHandler.hasPolicyViolations(envelope), is(true));
    }
}
