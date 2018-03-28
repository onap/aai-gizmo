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
