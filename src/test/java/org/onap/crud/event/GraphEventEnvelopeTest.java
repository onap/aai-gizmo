package org.onap.crud.event;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.onap.crud.entity.Vertex;
import org.onap.crud.event.GraphEvent.GraphEventOperation;
import org.onap.crud.event.envelope.GraphEventEnvelope;
import org.onap.crud.test.util.TestUtil;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class GraphEventEnvelopeTest {

    @Test
    public void testPublishedEventFormat() throws Exception {
        String expectedEnvelope = TestUtil.getFileAsString("event/event-envelope.json");

        GraphEvent body = GraphEvent.builder(GraphEventOperation.CREATE)
                .vertex(GraphEventVertex.fromVertex(new Vertex.Builder("pserver").build(), "v13")).build();
        String graphEventEnvelope = new GraphEventEnvelope(body).toJson();

        JSONAssert.assertEquals(expectedEnvelope, graphEventEnvelope,
                new CustomComparator(JSONCompareMode.STRICT, new Customization("header.request-id", (o1, o2) -> true),
                        new Customization("header.timestamp", (o1, o2) -> true),
                        new Customization("body.timestamp", (o1, o2) -> true),
                        new Customization("body.transaction-id", (o1, o2) -> true)));

        Gson gson = new Gson();
        GraphEventEnvelope envelope = gson.fromJson(graphEventEnvelope, GraphEventEnvelope.class);
        assertThat(envelope.getHeader().getRequestId(), is(envelope.getBody().getTransactionId()));
    }

    @Test
    public void testConsumedEventFormat() throws Exception {
        String expectedEnvelope = TestUtil.getFileAsString("event/event-envelope-sentinel.json");
        Gson gson = new Gson();
        GraphEventEnvelope envelope = gson.fromJson(expectedEnvelope, GraphEventEnvelope.class);
        JsonElement jsonElement = envelope.getPolicyViolations().getAsJsonArray().get(0);

        assertThat(jsonElement.getAsJsonObject().get("summary").getAsString(), is("a summary"));
        assertThat(jsonElement.getAsJsonObject().get("policyName").getAsString(), is("a policy name"));
    }
}
