package org.openecomp.crud.dao.champ;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openecomp.crud.dao.GraphDao;
import org.openecomp.crud.entity.Edge;
import org.openecomp.crud.entity.Vertex;
import org.openecomp.crud.exception.CrudException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;


/**
 * This suite of tests validates the basic functionality of the {@link ChampDao}.
 */
public class ChampDaoTest {

  private static final String GRAPH_NAME = "my_test_graph";

  private GraphDao champDao = null;


  /**
   * Perform setup steps that must be done prior to executing each test.
   */
  @Before
  public void setup() {

    // Create an instance of the Champ DAO, backed by the Champ library's in-memory back end
    // for testing purposes.
    Properties champDaoProperties = new Properties();
    champDaoProperties.put(ChampDao.CONFIG_STORAGE_BACKEND, "in-memory");
    champDaoProperties.put(ChampDao.CONFIG_GRAPH_NAME, GRAPH_NAME);
    champDao = new ChampDao(champDaoProperties);
  }


  /**
   * Perform tear down steps that must be done after executing each test.
   */
  @After
  public void tearDown() {

    // Release the Champ DAO instance that we were using for the test.
    if (champDao != null) {
      ((ChampDao) champDao).close();
    }
  }


  /**
   * Tests the ability of the {@link ChampDao} to create a vertex.
   *
   * @throws CrudException
   */
  @Test
  public void createVertexTest() throws CrudException {

    String VERTEX_TYPE = "Test_Vertex";

    Map<String, Object> properties = new HashMap<String, Object>();
    properties.put("property1", "something");
    properties.put("property2", "something else");

    // Create the vertex.
    Vertex createdVertex = champDao.addVertex(VERTEX_TYPE, properties);

    // Validate that the returned {@link Vertex} has the right label assigned to it.
    assertTrue("Unexpected vertex type '" + createdVertex.getType() + "' returned from DAO",
        createdVertex.getType().equals(VERTEX_TYPE));

    // Validate that all of the properties that we provided to the DAO are in fact assigned
    // to the {@link Vertex} that we got back.
    assertTrue("Vertex property list returned from DAO did not contain all expected properties - expected: " +
            properties.keySet() + " actual: " + createdVertex.getProperties().keySet(),
        createdVertex.getProperties().keySet().containsAll(properties.keySet()));

    // Validate that the values assigned to the properties in the returned {@link Vertex}
    // match the ones that we provided.
    for (String propertyKey : properties.keySet()) {

      assertTrue(createdVertex.getProperties().get(propertyKey).equals(properties.get(propertyKey)));
    }
  }


  /**
   * Tests the ability of the {@link ChampDao} to retrieve a vertex from the graph data store
   * by its unique identifier.
   *
   * @throws CrudException
   */
  @Test
  public void getVertexByIdTest() throws CrudException {

    String VERTEX_TYPE = "Test_Vertex";

    Map<String, Object> properties = new HashMap<String, Object>();
    properties.put("property1", "something");
    properties.put("property2", "something else");

    // Create the vertex.
    Vertex createdVertex = champDao.addVertex(VERTEX_TYPE, properties);

    // Make sure the {@link Vertex} returned from the create method includes an id that we can
    // use to retrieve it.
    assertTrue("No valid id returned for the created vertex", createdVertex.getId().isPresent());

    // Now, retrieve the {@link Vertex} by its identifier.
    Vertex retrievedVertex = champDao.getVertex(createdVertex.getId().get(), VERTEX_TYPE);

    // Validate that the retrieved {@link Vertex} has the right label assigned to it.
    assertTrue("Unexpected vertex type '" + retrievedVertex.getType() + "' returned from DAO",
        retrievedVertex.getType().equals(VERTEX_TYPE));

    // Validate that all of the properties that we provided when we created the {@link Vertex}
    // are present in the {@link Vertex} that we retrieved.
    assertTrue("Vertex property list returned from DAO did not contain all expected properties - expected: " +
            properties.keySet() + " actual: " + retrievedVertex.getProperties().keySet(),
        retrievedVertex.getProperties().keySet().containsAll(properties.keySet()));

    // Validate that the values assigned to the properties in the retrieved {@link Vertex}
    // match the ones that we provided when we created it.
    for (String propertyKey : properties.keySet()) {

      assertTrue(retrievedVertex.getProperties().get(propertyKey).equals(properties.get(propertyKey)));
    }
  }


  /**
   * Tests the ability of the {@link ChampDao} to update an already existing vertex.
   *
   * @throws CrudException
   */
  @Test
  public void updateVertexTest() throws CrudException {

    final String VERTEX_TYPE = "Test_Vertex";

    Map<String, Object> properties = new HashMap<String, Object>();
    properties.put("property1", "something");
    properties.put("property2", "something else");

    // Create the vertex.
    Vertex createdVertex = champDao.addVertex(VERTEX_TYPE, properties);

    // Make sure the {@link Vertex} returned from the create method includes an id that we can
    // use to retrieve it.
    assertTrue("No valid id returned for the created vertex", createdVertex.getId().isPresent());

    // Modify the properties list...
    properties.put("property3", "a new property");
    properties.remove("property1");

    // ...and apply it to our vertex.
    Vertex updatedVertex = champDao.updateVertex(createdVertex.getId().get(), createdVertex.getType(), properties);

    assertTrue("Vertex property list returned from DAO update operation did not contain all expected properties - expected: " +
            properties.keySet() + " actual: " + updatedVertex.getProperties().keySet(),
        updatedVertex.getProperties().keySet().containsAll(properties.keySet()));

    // Validate that the values assigned to the properties in the updated {@link Vertex}
    // match the ones that we provided when we created it.
    for (String propertyKey : properties.keySet()) {

      assertTrue("Unexpected value for property '" + propertyKey + "' - Expected: " +
              properties.get(propertyKey) + "  Actual: " +
              updatedVertex.getProperties().get(propertyKey),
          updatedVertex.getProperties().get(propertyKey).equals(properties.get(propertyKey)));
    }

    // Validate that the property that we removed is NOT in the set of properties from our
    // updated {@link Vertex}.
    assertFalse("Property 'property1' should no longer be associated with updated vertex",
        updatedVertex.getProperties().containsKey("property1"));
  }


  /**
   * Tests the ability of the {@link ChampDao} to retrieve multiple vertices which match
   * a particular set of supplied properties.
   *
   * @throws CrudException
   */
  @Test
  public void getVerticesTest() throws CrudException {

    final String FIRST_VERTEX_TYPE = "pserver";
    final String SECOND_VERTEX_TYPE = "complex";

    // Create some vertices.

    Map<String, Object> vertex1Properties = new HashMap<String, Object>();
    vertex1Properties.put("O/S", "Linux");
    vertex1Properties.put("version", "6.5");
    vertex1Properties.put("hostname", "kll0001");
    champDao.addVertex(FIRST_VERTEX_TYPE, vertex1Properties);

    Map<String, Object> vertex2Properties = new HashMap<String, Object>();
    vertex2Properties.put("O/S", "Linux");
    vertex2Properties.put("version", "6.5");
    vertex2Properties.put("hostname", "kll0002");
    champDao.addVertex(FIRST_VERTEX_TYPE, vertex2Properties);

    Map<String, Object> vertex3Properties = new HashMap<String, Object>();
    vertex3Properties.put("O/S", "Linux");
    vertex3Properties.put("version", "7.2");
    vertex3Properties.put("hostname", "kll0003");
    champDao.addVertex(FIRST_VERTEX_TYPE, vertex3Properties);

    Map<String, Object> vertex4Properties = new HashMap<String, Object>();
    vertex4Properties.put("O/S", "Windows");
    vertex4Properties.put("version", "10");
    vertex4Properties.put("hostname", "Dev Laptop");
    champDao.addVertex(FIRST_VERTEX_TYPE, vertex4Properties);

    Map<String, Object> vertex5Properties = new HashMap<String, Object>();
    vertex5Properties.put("Street", "Baker");
    vertex5Properties.put("Number", "222B");
    champDao.addVertex(SECOND_VERTEX_TYPE, vertex5Properties);

    // Create a set of properties to use for our query.
    Map<String, Object> queryProperties = new HashMap<String, Object>();
    queryProperties.put("O/S", "Linux");
    queryProperties.put("version", "6.5");

    // Validate that we filter our 'get vertices' results by type
    List<Vertex> allVerticesByType = champDao.getVertices(FIRST_VERTEX_TYPE, MapBuilder.builder().build());
    for (Vertex v : allVerticesByType) {
      assertTrue("Unexpected vertex type returned from query.  Expected: " +
              FIRST_VERTEX_TYPE + " Actual: " + v.getType(),
          v.getType().equals(FIRST_VERTEX_TYPE));
    }

    // Now, request the vertices that match our parameters.
    List<Vertex> vertices = champDao.getVertices(FIRST_VERTEX_TYPE, queryProperties);

    // Validate that got back the expected number of vertices.
    assertEquals(vertices.size(), 2);

    // Validate that the vertices we got back contain the expected parameters.
    for (Vertex v : vertices) {

      assertTrue("Vertex from query result does not contain expected vertex 'O/S'",
          v.getProperties().containsKey("O/S"));
      assertTrue("Vertex from query result contains unexpected value for 'O/S' parameter - Expected: 'Linux'  Actual: '" +
              v.getProperties().get("O/S") + "'",
          v.getProperties().get("O/S").equals("Linux"));

      assertTrue("Vertex from query result does not contain expected vertex 'O/S'",
          v.getProperties().containsKey("version"));
      assertTrue("Vertex from query result contains unexpected value for 'O/S' parameter - Expected: 'Linux'  Actual: '" +
              v.getProperties().get("O/S") + "'",
          v.getProperties().get("version").equals("6.5"));
    }
  }

  @Test
  public void deleteVertexTest() throws CrudException {

    boolean deletedVertexNotFound = false;

    // Create a vertex.
    Vertex createdVertex = champDao.addVertex("test_type", MapBuilder.builder()
        .withKeyValue("O/S", "Linux")
        .withKeyValue("version", "6.5")
        .withKeyValue("hostname", "kll0001")
        .build());

    // Verify that we can retrieve the vertex from the graph data base.
    Vertex retrievedVertex = champDao.getVertex(createdVertex.getId().get(), "test_type");

    // Now, delete the vertex.
    champDao.deleteVertex(createdVertex.getId().get(), "test_type");

    // Now, try to retrieve it again.  This time we should fail to find it.
    try {
      champDao.getVertex(createdVertex.getId().get(), "test_type");

    } catch (CrudException e) {
      assertTrue(e.getMessage().contains("No vertex with id"));
      deletedVertexNotFound = true;
    }

    assertTrue("Should not have been able to retrieve deleted vertex", deletedVertexNotFound);
  }

  @Test
  public void createEdgeTest() throws CrudException {

    String EDGE_TYPE = "has";

    // Create the source vertex for the edge.
    Map<String, Object> srcVertexProperties = new HashMap<String, Object>();
    srcVertexProperties.put("O/S", "Linux");
    srcVertexProperties.put("version", "6.5");
    srcVertexProperties.put("hostname", "kll0001");
    Vertex sourceVertex = champDao.addVertex("vserver", srcVertexProperties);

    // Create the target vertex for the edge.
    Map<String, Object> dstVertexProperties = new HashMap<String, Object>();
    dstVertexProperties.put("O/S", "Linux");
    dstVertexProperties.put("version", "6.5");
    dstVertexProperties.put("hostname", "kll0002");
    Vertex destVertex = champDao.addVertex("VNF", dstVertexProperties);

    // Now, create the edge itself.
    Map<String, Object> edgeProperties = new HashMap<String, Object>();
    edgeProperties.put("prop", "val");
    Edge createdEdge = champDao.addEdge("has", sourceVertex, destVertex, edgeProperties);

    // Validate that the Edge object returned from the create method matches what we were
    // trying to create.
    assertTrue("Unexpected type for Edge returned from create method.  Expected: " + EDGE_TYPE
            + " Actual: " + createdEdge.getType(),
        createdEdge.getType().equals("has"));
    assertTrue("Unexpected properties for Edge returned from create method.  Expected: " + edgeProperties
            + " Actual: " + createdEdge.getProperties(),
        createdEdge.getProperties().equals(edgeProperties));

  }

  @Test
  public void createEdgeWithMissingSrcOrTargetTest() throws CrudException {

    String EDGE_TYPE = "has";

    // Create the source vertex for the edge.
    Map<String, Object> srcVertexProperties = new HashMap<String, Object>();
    srcVertexProperties.put("O/S", "Linux");
    srcVertexProperties.put("version", "6.5");
    srcVertexProperties.put("hostname", "kll0001");
    Vertex sourceVertex = champDao.addVertex("vserver", srcVertexProperties);

    // Create the target vertex for the edge.
    Map<String, Object> dstVertexProperties = new HashMap<String, Object>();
    dstVertexProperties.put("O/S", "Linux");
    dstVertexProperties.put("version", "6.5");
    dstVertexProperties.put("hostname", "kll0002");
    Vertex destVertex = champDao.addVertex("VNF", dstVertexProperties);

    // Now, try creating the Edge but specify an id for the source vertex that does
    // not exist.
    Map<String, Object> edgeProperties = new HashMap<String, Object>();
    edgeProperties.put("prop", "val");
    try {
      champDao.addEdge(EDGE_TYPE, new Vertex.Builder("miss").id("99").build(), destVertex, edgeProperties);
    } catch (CrudException e) {
      assertTrue(e.getMessage().contains("Error creating edge - source vertex"));
    }

    // Now, try created the Edge with a valid source vertex, but specify an id for the
    // target vertex that does not exist.
    try {
      champDao.addEdge(EDGE_TYPE, sourceVertex, new Vertex.Builder("miss").id("99").build(), edgeProperties);
    } catch (CrudException e) {
      assertTrue(e.getMessage().contains("Error creating edge - target vertex"));
    }

  }

  @Test
  public void getEdgeByIdTest() throws CrudException {

    String EDGE_TYPE = "has";

    // Create the source vertex for the edge.
    Map<String, Object> srcVertexProperties = new HashMap<String, Object>();
    srcVertexProperties.put("O/S", "Linux");
    srcVertexProperties.put("version", "6.5");
    srcVertexProperties.put("hostname", "kll0001");
    Vertex sourceVertex = champDao.addVertex("vserver", srcVertexProperties);

    // Create the target vertex for the edge.
    Map<String, Object> dstVertexProperties = new HashMap<String, Object>();
    dstVertexProperties.put("O/S", "Linux");
    dstVertexProperties.put("version", "6.5");
    dstVertexProperties.put("hostname", "kll0002");
    Vertex destVertex = champDao.addVertex("VNF", dstVertexProperties);

    // Now, create the edge itself.
    Map<String, Object> edgeProperties = new HashMap<String, Object>();
    edgeProperties.put("prop", "val");
    Edge createdEdge = champDao.addEdge("has", sourceVertex, destVertex, edgeProperties);

    // Retrieve the edge we just created by specifying its unique identifier.
    Edge retrievedEdge = champDao.getEdge(createdEdge.getId().get(), "has");

    // Validate that the contents of the object that we got back matches what we thought we
    // created.
    assertTrue("Unexpected type for Edge returned from get method.  Expected: " + EDGE_TYPE
            + " Actual: " + retrievedEdge.getType(),
        retrievedEdge.getType().equals(EDGE_TYPE));
    assertTrue("Unexpected properties for Edge returned from get method.  Expected: " + edgeProperties
            + " Actual: " + retrievedEdge.getProperties(),
        retrievedEdge.getProperties().equals(edgeProperties));
  }

  @Test
  public void getEdgesTest() throws CrudException {

    final String EDGE_TYPE_HAS = "has";
    final String EDGE_TYPE_RUNS = "runs";

    // Create some vertices and edges that we can query agains.
    Vertex complex = champDao.addVertex("complex", MapBuilder.builder()
        .withKeyValue("Province", "Ontario")
        .withKeyValue("City", "Ottawa")
        .withKeyValue("Street", "303 Terry Fox")
        .build());

    Vertex vserver = champDao.addVertex("vserver", MapBuilder.builder()
        .withKeyValue("O/S", "Linux")
        .withKeyValue("version", "6.5")
        .withKeyValue("hostname", "kll0001")
        .build());

    Vertex vnf1 = champDao.addVertex("vserver", MapBuilder.builder()
        .withKeyValue("Application", "OpenDaylight")
        .build());

    Vertex vnf2 = champDao.addVertex("vserver", MapBuilder.builder()
        .withKeyValue("Application", "Cammunda")
        .build());

    Edge edge1 = champDao.addEdge(EDGE_TYPE_HAS, complex, vserver,
        MapBuilder.builder()
            .withKeyValue("usesResource", "false")
            .withKeyValue("hasDelTarget", "false")
            .build());

    Edge edge2 = champDao.addEdge(EDGE_TYPE_RUNS, vserver, vnf1,
        MapBuilder.builder()
            .withKeyValue("usesResource", "false")
            .withKeyValue("hasDelTarget", "true")
            .build());

    Edge edge3 = champDao.addEdge(EDGE_TYPE_RUNS, vserver, vnf2,
        MapBuilder.builder()
            .withKeyValue("usesResource", "false")
            .withKeyValue("hasDelTarget", "false")
            .build());

    // Query for all HAS edges.
    List<Edge> hasEdges = champDao.getEdges(EDGE_TYPE_HAS, new HashMap<String, Object>());

    assertEquals("Unexpected number of edges of type 'has' found.  Expected: 1 Actual: " + hasEdges.size(),
        hasEdges.size(), 1);
    assertTrue("Result of query for 'has' type edges does not contain the expected results",
        containsEdge(edge1, hasEdges));

    // Query for all RUNS edges.
    List<Edge> runsEdges = champDao.getEdges(EDGE_TYPE_RUNS, new HashMap<String, Object>());

    assertEquals("Unexpected number of edges of type 'runs' found.  Expected: 2 Actual: " + runsEdges.size(),
        runsEdges.size(), 2);
    assertTrue("Result of query for 'runs' type edges does not contain the expected results",
        containsEdge(edge2, runsEdges));
    assertTrue("Result of query for 'runs' type edges does not contain the expected results",
        containsEdge(edge2, runsEdges));

    // Query for all HAS edges with the property 'hasDelTarget' equal to 'true'.
    List<Edge> runsEdgesWithDelTargetTrue =
        champDao.getEdges(EDGE_TYPE_RUNS, MapBuilder.builder()
            .withKeyValue("hasDelTarget", "true")
            .build());

    assertEquals("Unexpected number of edges of type 'has' with 'hasDelTarget=true' found.  Expected: 1 Actual: "
            + runsEdgesWithDelTargetTrue.size(),
        runsEdgesWithDelTargetTrue.size(), 1);
    assertTrue("Result of query for 'runs' type edges with delTarget set to TRUE does not contain the expected results",
        containsEdge(edge2, runsEdgesWithDelTargetTrue));
  }

  @Test
  @Ignore   // For now - pending some expected fixes to the Champ library.
  public void updateEdgeTest() throws CrudException {

    // Create the source vertex for the edge.
    Vertex sourceVertex = champDao.addVertex("vserver", MapBuilder.builder()
        .withKeyValue("O/S", "Linux")
        .withKeyValue("version", "6.5")
        .withKeyValue("hostname", "kll0001")
        .build());

    // Create the target vertex for the edge.
    Vertex destVertex = champDao.addVertex("VNF", MapBuilder.builder()
        .withKeyValue("O/S", "Linux")
        .withKeyValue("version", "6.5")
        .withKeyValue("hostname", "kll0002")
        .build());

    // Now, create the edge itself.
    Edge createdEdge = champDao.addEdge("has",
        sourceVertex,
        destVertex,
        MapBuilder.builder()
            .withKeyValue("key1", "value1")
            .withKeyValue("key2", "value2")
            .withKeyValue("key3", "value3")
            .build());

    // Make sure the Edge returned from the create method includes an id that we can
    // use to retrieve it.
    assertTrue("No valid id returned for the created edge", createdEdge.getId().isPresent());

    // Retrieve the properties map for our edge and make some changes.
    Map<String, Object> properties = createdEdge.getProperties();
    properties.put("key4", "value4");
    properties.remove("key2");

    // Now update the edge with the new properties map.
    Edge updatedEdge = champDao.updateEdge(createdEdge);

    assertTrue("Edge property list returned from DAO update operation did not contain all expected properties - expected: " +
            properties.keySet() + " actual: " + updatedEdge.getProperties().keySet(),
        updatedEdge.getProperties().keySet().containsAll(properties.keySet()));

    // Validate that the values assigned to the properties in the updated Edge
    // match the ones that we provided when we created it.
    for (String propertyKey : properties.keySet()) {

      assertTrue("Unexpected value for property '" + propertyKey + "' - Expected: " +
              properties.get(propertyKey) + "  Actual: " +
              updatedEdge.getProperties().get(propertyKey),
          updatedEdge.getProperties().get(propertyKey).equals(properties.get(propertyKey)));
    }

    // Validate that the property that we removed is NOT in the set of properties from our
    // updated edge.
    // *** We will leave this validation commented out for now, as the Champ library actually
    //     merges update properties instead of replacing them...
//		assertFalse("Property 'key2' should no longer be associated with updated edge",
//				    updatedEdge.getProperties().containsKey("key2"));
  }

  @Test
  public void deleteEdgeTest() throws CrudException {

    boolean deletedEdgeNotFound = false;

    // Create the source vertex for the edge.
    Vertex sourceVertex = champDao.addVertex("vserver", MapBuilder.builder()
        .withKeyValue("O/S", "Linux")
        .withKeyValue("version", "6.5")
        .withKeyValue("hostname", "kll0001")
        .build());

    // Create the target vertex for the edge.
    Vertex destVertex = champDao.addVertex("VNF", MapBuilder.builder()
        .withKeyValue("O/S", "Linux")
        .withKeyValue("version", "6.5")
        .withKeyValue("hostname", "kll0002")
        .build());

    // Now, create the edge itself.
    Edge createdEdge = champDao.addEdge("has",
        sourceVertex,
        destVertex,
        MapBuilder.builder()
            .withKeyValue("key1", "value1")
            .withKeyValue("key2", "value2")
            .withKeyValue("key3", "value3")
            .build());

    // Verify that we can retrieve the edge that we just created.
    Edge retrievedEdge = champDao.getEdge(createdEdge.getId().get(), "has");

    // Now, delete it.
    champDao.deleteEdge(createdEdge.getId().get(), "has");

    // Try retrieving it again.  This time we should not find it.
    try {
      champDao.getEdge(createdEdge.getId().get(), "has");
    } catch (CrudException e) {

      assertTrue(e.getMessage().contains("No edge with id"));
      deletedEdgeNotFound = true;
    }

    assertTrue("Should not have been able to retrieve deleted edge.", deletedEdgeNotFound);
  }

  private boolean containsEdge(Edge anEdge, List<Edge> edges) {

    for (Edge e : edges) {
      if (e.getId().isPresent() && anEdge.getId().isPresent() && (e.getId().get().equals(anEdge.getId().get()))) {
        return true;
      }

    }
    return false;
  }

  public static class MapBuilder {

    private Map<String, Object> map;

    private MapBuilder() {
      map = new HashMap<String, Object>();
    }

    public static MapBuilder builder() {
      return new MapBuilder();
    }

    public MapBuilder withKeyValue(String key, Object value) {
      map.put(key, value);
      return this;
    }

    public Map<String, Object> build() {
      return map;
    }
  }
}
