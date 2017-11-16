/**
 * ﻿============LICENSE_START=======================================================
 * Gizmo
 * ================================================================================
 * Copyright © 2017 AT&T Intellectual Property.
 * Copyright © 2017 Amdocs
 * All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 *
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.openecomp.crud.dao.champ;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openecomp.aai.champcore.ChampGraph;
import org.openecomp.aai.champcore.ChampTransaction;
import org.openecomp.aai.champcore.exceptions.ChampMarshallingException;
import org.openecomp.aai.champcore.exceptions.ChampObjectNotExistsException;
import org.openecomp.aai.champcore.exceptions.ChampRelationshipNotExistsException;
import org.openecomp.aai.champcore.exceptions.ChampSchemaViolationException;
import org.openecomp.aai.champcore.exceptions.ChampTransactionException;
import org.openecomp.aai.champcore.exceptions.ChampUnmarshallingException;
import org.openecomp.aai.champcore.model.ChampObject;
import org.openecomp.aai.champcore.model.ChampRelationship;
import org.openecomp.aai.champcore.model.fluent.object.ObjectBuildOrPropertiesStep;
import org.openecomp.cl.api.Logger;
import org.openecomp.cl.eelf.LoggerFactory;
import org.openecomp.crud.dao.GraphDao;
import org.openecomp.crud.entity.Edge;
import org.openecomp.crud.entity.Vertex;
import org.openecomp.crud.exception.CrudException;
import org.openecomp.crud.logging.CrudServiceMsgs;

/**
 * This is the integration layer between the CRUD API service and the low level Champ library for graph database
 * interaction.
 */
public class ChampDao implements GraphDao {

	public static final String CONFIG_STORAGE_BACKEND = "storage.backend";
	public static final String CONFIG_STORAGE_BACKEND_DB = "storage.backend.db";
	public static final String STORAGE_HBASE_DB = "hbase";
	public static final String STORAGE_CASSANDRA_DB = "cassandra";
	public static final String CONFIG_STORAGE_HOSTNAMES = "storage.hostnames";
	public static final String CONFIG_STORAGE_PORT = "storage.port";
	public static final String CONFIG_HBASE_ZNODE_PARENT = "storage.hbase.ext.zookeeper.znode.parent";
	public static final String CONFIG_GRAPH_NAME = "graph.name";
	public static final String GRAPH_UNQ_INSTANCE_ID_SUFFIX = "graph.unique-instance-id-suffix";

	public static final String CONFIG_EVENT_STREAM_PUBLISHER = "event.stream.publisher";
	public static final String CONFIG_EVENT_STREAM_NUM_PUBLISHERS = "event.stream.num-publishers";

	private static Map<String, ChampTransaction> transactions = new ConcurrentHashMap<String, ChampTransaction>();
	public static final String DEFAULT_GRAPH_NAME = "default_graph";

	private enum GraphType {
		IN_MEMORY, TITAN, DSE
	}

	/**
	 * Instance of the API used for interacting with the Champ library.
	 */
	private ChampGraph champApi = null;

	private Logger logger = LoggerFactory.getInstance().getLogger(ChampDao.class.getName());

	/**
	 * Creates a new instance of the ChampDao.
	 *
	 * @param champGraph
	 *            - Concrete implementation of the graph dao layer
	 */
	public ChampDao(ChampGraph champGraph) {
		this.champApi = champGraph;
	}

	@Override
	public Vertex getVertex(String id) throws CrudException {

		try {

			if (logger.isDebugEnabled()) {
				logger.debug("getVertex with id: " + id);
			}

			long idAsLong = Long.parseLong(id);

			Optional<ChampObject> retrievedVertex = champApi.retrieveObject(idAsLong);

			String nodeType = org.openecomp.schema.OxmModelValidator.Metadata.NODE_TYPE.propertyName();
			if (retrievedVertex.isPresent() && retrievedVertex.get().getProperties().get(nodeType) != null) {
				return vertexFromChampObject(retrievedVertex.get(), retrievedVertex.get().getProperties().get(nodeType).toString());
			} else {

				// We didn't find a vertex with the supplied id, so just throw an
				// exception.
				throw new CrudException("No vertex with id " + id + " found in graph", javax.ws.rs.core.Response.Status.NOT_FOUND);
			}

		} catch (ChampUnmarshallingException | ChampTransactionException e) {

			// Something went wrong - throw an exception.
			throw new CrudException(e.getMessage(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public Vertex getVertex(String id, String type) throws CrudException {

		try {

			if (logger.isDebugEnabled()) {
				logger.debug("getVertex with id: " + id);
			}

			long idAsLong = Long.parseLong(id);

			// Request the vertex from the graph db.
			Optional<ChampObject> retrievedVertex = champApi.retrieveObject(idAsLong);

			// Did we find it?
			if (retrievedVertex.isPresent()
					&& retrievedVertex.get().getProperties().get(org.openecomp.schema.OxmModelValidator.Metadata.NODE_TYPE.propertyName()) != null
					&& retrievedVertex.get().getProperties().get(org.openecomp.schema.OxmModelValidator.Metadata.NODE_TYPE.propertyName()).toString()
							.equalsIgnoreCase(type)) {

				// Yup, convert it to a Vector object and return it.
				return vertexFromChampObject(retrievedVertex.get(), type);

			} else {

				// We didn't find a vertex with the supplied id, so just throw an
				// exception.
				throw new CrudException("No vertex with id " + id + " found in graph", javax.ws.rs.core.Response.Status.NOT_FOUND);
			}

		} catch (ChampUnmarshallingException | ChampTransactionException e) {

			// Something went wrong - throw an exception.
			throw new CrudException(e.getMessage(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public List<Edge> getVertexEdges(String id) throws CrudException {

		if (logger.isDebugEnabled()) {
			logger.debug("get Edges incident to vertex with id: " + id + " from graph");
		}

		try {
			long idAsLong = Long.parseLong(id); // GDF - what to do about id???

			// Request the vertex from the graph db.
			Optional<ChampObject> retrievedVertex = champApi.retrieveObject(idAsLong);

			// Did we find it?
			if (retrievedVertex.isPresent()) {

				// Query the Champ library for the edges which are incident to the specified
				// vertex.
				Stream<ChampRelationship> relationships = champApi.retrieveRelationships(retrievedVertex.get());

				// Build an edge list from the result stream.
				List<Edge> edges = new ArrayList<Edge>();
				relationships.forEach(r -> edges.add(edgeFromChampRelationship(r)));

				return edges;

			} else {

				// We couldn't find the specified vertex, so throw an exception.
				throw new CrudException("No vertex with id " + id + " found in graph", javax.ws.rs.core.Response.Status.NOT_FOUND);
			}

		} catch (ChampUnmarshallingException e) {

			// Something went wrong, so throw an exception.
			throw new CrudException(e.getMessage(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);

		} catch (ChampObjectNotExistsException e) {

			// We couldn't find the specified vertex, so throw an exception.
			throw new CrudException("No vertex with id " + id + " found in graph", javax.ws.rs.core.Response.Status.NOT_FOUND);
		} catch (ChampTransactionException e) {
			throw new CrudException("Transaction error occured", javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public Vertex addVertex(String type, Map<String, Object> properties) throws CrudException {

		if (logger.isDebugEnabled()) {
			logger.debug("Add/update vertex: {label: " + type + " properties:" + propertiesMapToString(properties));
		}

		// Add the aai_node_type so that AAI can read the data created by gizmo
		properties.put(org.openecomp.schema.OxmModelValidator.Metadata.NODE_TYPE.propertyName(), type);

		// Create an object to represent our vertex in the format expected by the Champ library.
		ChampObject objectToCreate = buildChampObject(type, properties);

		try {

			// Ask the Champ library to store our vertex, placing the returned object into a
			// list so that we can easily put that into our result object.
			return vertexFromChampObject(champApi.storeObject(objectToCreate), type);

		} catch (ChampMarshallingException | ChampSchemaViolationException | ChampObjectNotExistsException | ChampTransactionException e) {

			// Something went wrong - throw an exception.
			throw new CrudException(e.getMessage(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public Vertex updateVertex(String id, String type, Map<String, Object> properties) throws CrudException {

		if (logger.isDebugEnabled()) {
			logger.debug("Update vertex with id: " + id + " with properties: " + propertiesMapToString(properties));
		}
		// Add the aai_node_type so that AAI can read the data created by gizmo
		properties.put(org.openecomp.schema.OxmModelValidator.Metadata.NODE_TYPE.propertyName(), type);

		try {
			// Now, build the updated version of the Champ Object...
			ChampObject updateObject = buildChampObject(id, type, properties);
			// ...and send it to the Champ library.
			return vertexFromChampObject(champApi.replaceObject(updateObject), type);

		} catch (ChampObjectNotExistsException e) {
			throw new CrudException("Not Found", javax.ws.rs.core.Response.Status.NOT_FOUND);
		} catch (NumberFormatException | ChampMarshallingException | ChampSchemaViolationException e) {
			throw new CrudException(e.getMessage(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		} catch (ChampTransactionException e) {
			throw new CrudException("Transaction error occured", javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}

	}

	@Override
	public List<Vertex> getVertices(String type, Map<String, Object> filter) throws CrudException {

		if (logger.isDebugEnabled()) {
			logger.debug("Retrieve vertices with type label: " + type + " which map query parameters: " + propertiesMapToString(filter));
		}

		filter.put(org.openecomp.schema.OxmModelValidator.Metadata.NODE_TYPE.propertyName(), type);

		Stream<ChampObject> retrievedVertices;
		try {
			retrievedVertices = champApi.queryObjects(filter);

		} catch (ChampTransactionException e) {
			throw new CrudException("Transaction error occured", javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}

		List<Vertex> vertices = retrievedVertices.map(v -> vertexFromChampObject(v, type)).collect(Collectors.toList());

		if (logger.isDebugEnabled()) {
			logger.debug("Resulting vertex list: " + retrievedVertices);
		}

		// ...and return it to the caller.
		return vertices;
	}

	private Object getRelKey(String id) {
		Object key = id;
		// convert into Long if applicable . TODO : revisit in story NUC-304
		try {
			key = Long.parseLong(id);
		} catch (NumberFormatException e) {
			// The id isn't a Long, leave it as a string
		}

		return key;
	}

	@Override
	public Edge getEdge(String id, String type) throws CrudException {

		if (logger.isDebugEnabled()) {
			logger.debug("Get edge with id: " + id);
		}

		try {

			// Request the edge from the graph db.
			Optional<ChampRelationship> relationship = champApi.retrieveRelationship(getRelKey(id));

			// Did we find it?
			if (relationship.isPresent() && relationship.get().getType().equals(type)) {

				// Yup - return the result.
				return edgeFromChampRelationship(relationship.get());

			} else {

				// We didn't find an edge with the supplied id, so throw an exception.
				throw new CrudException("No edge with id " + id + " found in graph", javax.ws.rs.core.Response.Status.NOT_FOUND);
			}

		} catch (ChampUnmarshallingException | ChampTransactionException e) {

			// Something went wrong, so throw an exception.
			throw new CrudException(e.getMessage(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public Edge addEdge(String type, Vertex source, Vertex target, Map<String, Object> properties) throws CrudException {

		// For now, assume source and target are straight ids...
		try {

			Optional<ChampObject> sourceObject = champApi.retrieveObject(Long.parseLong(source.getId().get()));
			if (!sourceObject.isPresent() || !sourceObject.get().getType().equals(source.getType())) {
				throw new CrudException("Error creating edge - source vertex with id " + source + " does not exist in graph data base",
						javax.ws.rs.core.Response.Status.BAD_REQUEST);
			}

			Optional<ChampObject> targetObject = champApi.retrieveObject(Long.parseLong(target.getId().get()));
			if (!targetObject.isPresent() || !targetObject.get().getType().equals(target.getType())) {
				throw new CrudException("Error creating edge - target vertex with id " + target + " does not exist in graph data base",
						javax.ws.rs.core.Response.Status.BAD_REQUEST);
			}

			// Now, create the ChampRelationship object for our edge and store it in
			// the graph database.
			return edgeFromChampRelationship(
					champApi.storeRelationship(new ChampRelationship.Builder(sourceObject.get(), targetObject.get(), type).properties(properties).build()));

		} catch (ChampMarshallingException | ChampObjectNotExistsException | ChampSchemaViolationException | ChampRelationshipNotExistsException
				| ChampUnmarshallingException | NumberFormatException | ChampTransactionException e) {

			throw new CrudException("Error creating edge: " + e.getMessage(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public List<Edge> getEdges(String type, Map<String, Object> filter) throws CrudException {

		filter.put(ChampRelationship.ReservedPropertyKeys.CHAMP_RELATIONSHIP_TYPE.toString(), type);

		Stream<ChampRelationship> retrievedRelationships;
		try {
			retrievedRelationships = champApi.queryRelationships(filter);

		} catch (ChampTransactionException e) {
			throw new CrudException("Transaction error occured", javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}

		// Process the result stream from the Champ library into an Edge list, keeping only
		// edges of the specified type.
		List<Edge> edges = retrievedRelationships.map(r -> edgeFromChampRelationship(r)).collect(Collectors.toList());

		return edges;
	}

	@Override
	public Edge updateEdge(Edge edge) throws CrudException {

		if (logger.isDebugEnabled()) {
			logger.debug("Update edge with id: " + edge.getId() + " with properties: " + propertiesMapToString(edge.getProperties()));
		}

		try {
			// Now, build the updated version of the Champ Relationship...
			ChampRelationship updateRelationship = new ChampRelationship.Builder(
					buildChampObject(edge.getSource().getId().get(), edge.getSource().getType(), edge.getSource().getProperties()),
					buildChampObject(edge.getTarget().getId().get(), edge.getTarget().getType(), edge.getTarget().getProperties()), edge.getType())
							.key(getRelKey(edge.getId().get())).properties(edge.getProperties()).build();
			// ...and send it to the Champ library.
			return edgeFromChampRelationship(champApi.replaceRelationship(updateRelationship));

		} catch (ChampRelationshipNotExistsException ex) {
			throw new CrudException("Not Found", javax.ws.rs.core.Response.Status.NOT_FOUND);
		} catch (NumberFormatException | ChampUnmarshallingException | ChampMarshallingException | ChampSchemaViolationException
				| ChampTransactionException ex) {

			throw new CrudException(ex.getMessage(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public void deleteVertex(String id, String type) throws CrudException {

		try {

			// First, retrieve the vertex that we intend to delete.
			Optional<ChampObject> retrievedVertex = champApi.retrieveObject(Long.parseLong(id));

			// Did we find it?
			if (!retrievedVertex.isPresent() || !retrievedVertex.get().getType().equals(type)) {
				throw new CrudException("Failed to delete vertex with id: " + id + " - vertex does not exist.", javax.ws.rs.core.Response.Status.NOT_FOUND);
			}

			// Now, verify that there are no edges incident to the vertex (they must be deleted
			// first if so).
			Stream<ChampRelationship> relationships = champApi.retrieveRelationships(retrievedVertex.get());

			if (relationships.count() > 0) {
				throw new CrudException("Attempt to delete vertex with id " + id + " which has incident edges.", javax.ws.rs.core.Response.Status.BAD_REQUEST);
			}

			// Finally, we can attempt to delete our vertex.
			champApi.deleteObject(Long.parseLong(id));

		} catch (NumberFormatException | ChampUnmarshallingException | ChampObjectNotExistsException | ChampTransactionException e) {

			throw new CrudException(e.getMessage(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public void deleteEdge(String id, String type) throws CrudException {

		try {

			// First, retrieve the edge that we want to delete.
			Optional<ChampRelationship> relationshipToDelete = champApi.retrieveRelationship(getRelKey(id));

			// Did we find it?
			if (!relationshipToDelete.isPresent() || !relationshipToDelete.get().getType().equals(type)) {
				throw new CrudException("Failed to delete edge with id: " + id + " - edge does not exist", javax.ws.rs.core.Response.Status.NOT_FOUND);
			}

			// Now we can delete the edge.
			champApi.deleteRelationship(relationshipToDelete.get());

		} catch (ChampRelationshipNotExistsException | NumberFormatException | ChampUnmarshallingException | ChampTransactionException e) {

			throw new CrudException(e.getMessage(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * This helper method generates a string representation of a properties map for logging purposes.
	 *
	 * @param properties
	 *            - The properties map to be converted.
	 * @return - The log statement friendly conversion of the properties map.
	 */
	private String propertiesMapToString(Map<String, Object> properties) {

		StringBuilder sb = new StringBuilder();
		sb.append("{");

		for (String key : properties.keySet()) {
			sb.append("(").append(key).append(" -> ").append(properties.get(key)).append(") ");
		}

		sb.append("}");

		return sb.toString();
	}

	/**
	 * This helper method constructs a {@link ChampObject} suitable for passing to the Champ library.
	 *
	 * @param type
	 *            - The type to assign to our ChampObject
	 * @param properties
	 *            - The set of properties to assign to our ChampObject
	 * @return - A populated ChampObject
	 */
	private ChampObject buildChampObject(String type, Map<String, Object> properties) {

		ObjectBuildOrPropertiesStep objectInProgress = ChampObject.create().ofType(type).withoutKey();

		for (String key : properties.keySet()) {
			objectInProgress.withProperty(key, properties.get(key));
		}
		return objectInProgress.build();
	}

	/**
	 * This helper method constructs a {@link ChampObject} suitable for passing to the Champ library.
	 *
	 * @param id
	 *            - Unique identifier for this object.
	 * @param type
	 *            - The type to assign to our ChampObject
	 * @param properties
	 *            - The set of properties to assign to our ChampObject
	 * @return - A populated ChampObject
	 */
	private ChampObject buildChampObject(String id, String type, Map<String, Object> properties) {

		ObjectBuildOrPropertiesStep objectInProgress = ChampObject.create().ofType(type).withKey(Long.parseLong(id));

		for (String key : properties.keySet()) {
			objectInProgress.withProperty(key, properties.get(key));
		}
		return objectInProgress.build();
	}

	private Vertex vertexFromChampObject(ChampObject champObject, String type) {

		// Get the identifier for this vertex from the Champ object.
		Object id = champObject.getKey().orElse("");

		// Start building our {@link Vertex} object.
		Vertex.Builder vertexBuilder = new Vertex.Builder(type);
		vertexBuilder.id(id.toString());

		// Convert the properties associated with the Champ object into the form expected for
		// a Vertex object.
		for (String key : champObject.getProperties().keySet()) {
			vertexBuilder.property(key, champObject.getProperties().get(key));
		}

		// ...and return it.
		return vertexBuilder.build();
	}

	/**
	 * This helper method converts a {@link ChampRelationship} from the Champ library into an equivalent {@link Edge}
	 * object that is understood by the CRUD Service.
	 *
	 * @param relationship
	 *            - The ChampRelationship object to be converted.
	 * @return - An Edge object corresponding to the supplied ChampRelationship
	 */
	private Edge edgeFromChampRelationship(ChampRelationship relationship) {

		// Populate the edge's id, if available.
		Object relationshipId = relationship.getKey().orElse("");

		Edge.Builder edgeBuilder = new Edge.Builder(relationship.getType()).id(relationshipId.toString());
		edgeBuilder.source(vertexFromChampObject(relationship.getSource(),
				relationship.getSource().getProperties().get(org.openecomp.schema.OxmModelValidator.Metadata.NODE_TYPE.propertyName()) == null
						? relationship.getSource().getType()
						: relationship.getSource().getProperties().get(org.openecomp.schema.OxmModelValidator.Metadata.NODE_TYPE.propertyName()).toString()));
		edgeBuilder.target(vertexFromChampObject(relationship.getTarget(),
				relationship.getTarget().getProperties().get(org.openecomp.schema.OxmModelValidator.Metadata.NODE_TYPE.propertyName()) == null
						? relationship.getTarget().getType()
						: relationship.getTarget().getProperties().get(org.openecomp.schema.OxmModelValidator.Metadata.NODE_TYPE.propertyName()).toString()));

		for (String key : relationship.getProperties().keySet()) {
			edgeBuilder.property(key, relationship.getProperties().get(key).toString());
		}

		return edgeBuilder.build();
	}

	/**
	 * Performs any necessary shut down operations when the DAO is no longer needed.
	 */
	public void close() {

		if (champApi != null) {

			logger.info(CrudServiceMsgs.STOPPING_CHAMP_DAO);

			champApi.shutdown();
		}
	}

	@Override
	public String openTransaction() {

		ChampTransaction transaction = champApi.openTransaction();

		transactions.put(transaction.id(), transaction);
		logger.info(CrudServiceMsgs.TRANSACTION, "Stored transaction " + transaction.id() + " in hashmap");
		logger.info(CrudServiceMsgs.TRANSACTION, "Hash map contents:");
		for (String key : transactions.keySet()) {
			logger.info(CrudServiceMsgs.TRANSACTION, key);
		}
		return transaction.id();
	}

	@Override
	public void commitTransaction(String id) throws CrudException {

		try {
			champApi.commitTransaction(getTransaction(id));
		} catch (ChampTransactionException e) {
			throw new CrudException("Error while commiting transaction " + id, javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}
		transactions.remove(id);
	}

	@Override
	public void rollbackTransaction(String id) throws CrudException {

		try {
			champApi.rollbackTransaction(getTransaction(id));
		} catch (ChampTransactionException e) {
			throw new CrudException("Error while transaction rollback " + id, javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}
		transactions.remove(id);
	}

	private ChampTransaction getTransaction(String id) throws CrudException {

		logger.info(CrudServiceMsgs.TRANSACTION, "Looking up transaction " + id);
		if (transactions.containsKey(id)) {
			logger.info(CrudServiceMsgs.TRANSACTION, "Found it!");
			return (transactions.get(id));
		} else {
			logger.info(CrudServiceMsgs.TRANSACTION, "Didn't find transaction id " + id + ".  Hash map contains: ");
			for (String key : transactions.keySet()) {
				logger.info(CrudServiceMsgs.TRANSACTION, key);
			}
			throw new CrudException("No open transaction with id: " + id, javax.ws.rs.core.Response.Status.NOT_FOUND);
		}
	}

	@Override
	public Vertex addVertex(String type, Map<String, Object> properties, String txId) throws CrudException {
		if (logger.isDebugEnabled()) {
			logger.debug("Add/update vertex: {label: " + type + " properties:" + propertiesMapToString(properties));
		}

		// Add the aai_node_type so that AAI can read the data created by gizmo
		properties.put(org.openecomp.schema.OxmModelValidator.Metadata.NODE_TYPE.propertyName(), type);

		// Create an object to represent our vertex in the format expected by the Champ library.
		ChampObject objectToCreate = buildChampObject(type, properties);

		try {

			// Ask the Champ library to store our vertex, placing the returned object into a
			// list so that we can easily put that into our result object.
			return vertexFromChampObject(champApi.storeObject(objectToCreate, Optional.of(getTransaction(txId))), type);

		} catch (ChampMarshallingException | ChampSchemaViolationException | ChampObjectNotExistsException | ChampTransactionException e) {

			// Something went wrong - throw an exception.
			throw new CrudException(e.getMessage(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public Edge addEdge(String type, Vertex source, Vertex target, Map<String, Object> properties, String txId) throws CrudException {
		// For now, assume source and target are straight ids...
		try {

			Optional<ChampObject> sourceObject = champApi.retrieveObject(Long.parseLong(source.getId().get()), Optional.of(getTransaction(txId)));
			if (!sourceObject.isPresent() || !sourceObject.get().getType().equals(source.getType())) {
				throw new CrudException("Error creating edge - source vertex with id " + source + " does not exist in graph data base",
						javax.ws.rs.core.Response.Status.BAD_REQUEST);
			}

			Optional<ChampObject> targetObject = champApi.retrieveObject(Long.parseLong(target.getId().get()), Optional.of(getTransaction(txId)));
			if (!targetObject.isPresent() || !targetObject.get().getType().equals(target.getType())) {
				throw new CrudException("Error creating edge - target vertex with id " + target + " does not exist in graph data base",
						javax.ws.rs.core.Response.Status.BAD_REQUEST);
			}

			// Now, create the ChampRelationship object for our edge and store it in
			// the graph database.
			return edgeFromChampRelationship(
					champApi.storeRelationship(new ChampRelationship.Builder(sourceObject.get(), targetObject.get(), type).properties(properties).build(),
							Optional.of(getTransaction(txId))));

		} catch (ChampMarshallingException | ChampObjectNotExistsException | ChampSchemaViolationException | ChampTransactionException
				| ChampRelationshipNotExistsException | ChampUnmarshallingException e) {

			throw new CrudException("Error creating edge: " + e.getMessage(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}

	}

	@Override
	public Vertex updateVertex(String id, String type, Map<String, Object> properties, String txId) throws CrudException {
		if (logger.isDebugEnabled()) {
			logger.debug("Update vertex with id: " + id + " with properties: " + propertiesMapToString(properties));
		}
		// Add the aai_node_type so that AAI can read the data created by gizmo
		properties.put(org.openecomp.schema.OxmModelValidator.Metadata.NODE_TYPE.propertyName(), type);

		try {
			// Now, build the updated version of the Champ Object...
			ChampObject updateObject = buildChampObject(id, type, properties);
			// ...and send it to the Champ library.
			return vertexFromChampObject(champApi.replaceObject(updateObject, Optional.of(getTransaction(txId))), type);

		} catch (ChampObjectNotExistsException e) {
			throw new CrudException("Not Found", javax.ws.rs.core.Response.Status.NOT_FOUND);
		} catch (NumberFormatException | ChampMarshallingException | ChampTransactionException | ChampSchemaViolationException e) {
			throw new CrudException(e.getMessage(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public boolean transactionExists(String id) throws CrudException {
		return transactions.containsKey(id);
	}

	@Override
	public void deleteVertex(String id, String type, String txId) throws CrudException {
		try {

			// First, retrieve the vertex that we intend to delete.
			Optional<ChampObject> retrievedVertex = champApi.retrieveObject(Long.parseLong(id), Optional.of(getTransaction(txId)));

			// Did we find it?
			if (!retrievedVertex.isPresent() || !retrievedVertex.get().getType().equals(type)) {
				throw new CrudException("Failed to delete vertex with id: " + id + " - vertex does not exist.", javax.ws.rs.core.Response.Status.NOT_FOUND);
			}

			// Now, verify that there are no edges incident to the vertex (they must be deleted
			// first if so).
			Stream<ChampRelationship> relationships = champApi.retrieveRelationships(retrievedVertex.get(), Optional.of(getTransaction(txId)));

			if (relationships.count() > 0) {
				throw new CrudException("Attempt to delete vertex with id " + id + " which has incident edges.", javax.ws.rs.core.Response.Status.BAD_REQUEST);
			}

			// Finally, we can attempt to delete our vertex.
			champApi.deleteObject(Long.parseLong(id), Optional.of(getTransaction(txId)));

		} catch (NumberFormatException | ChampUnmarshallingException | ChampObjectNotExistsException | ChampTransactionException e) {

			throw new CrudException(e.getMessage(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}

	}

	@Override
	public Edge updateEdge(Edge edge, String txId) throws CrudException {
		if (logger.isDebugEnabled()) {
			logger.debug("Update edge with id: " + edge.getId() + " with properties: " + propertiesMapToString(edge.getProperties()));
		}

		try {
			// Now, build the updated version of the Champ Relationship...
			ChampRelationship updateRelationship = new ChampRelationship.Builder(
					buildChampObject(edge.getSource().getId().get(), edge.getSource().getType(), edge.getSource().getProperties()),
					buildChampObject(edge.getTarget().getId().get(), edge.getTarget().getType(), edge.getTarget().getProperties()), edge.getType())
							.key(getRelKey(edge.getId().get())).properties(edge.getProperties()).build();
			// ...and send it to the Champ library.
			return edgeFromChampRelationship(champApi.replaceRelationship(updateRelationship, Optional.of(getTransaction(txId))));

		} catch (ChampRelationshipNotExistsException ex) {
			throw new CrudException("Not Found", javax.ws.rs.core.Response.Status.NOT_FOUND);
		} catch (NumberFormatException | ChampUnmarshallingException | ChampMarshallingException | ChampSchemaViolationException
				| ChampTransactionException ex) {

			throw new CrudException(ex.getMessage(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public void deleteEdge(String id, String type, String txId) throws CrudException {
		try {

			// First, retrieve the edge that we want to delete.
			Optional<ChampRelationship> relationshipToDelete = champApi.retrieveRelationship(getRelKey(id), Optional.of(getTransaction(txId)));

			// Did we find it?
			if (!relationshipToDelete.isPresent() || !relationshipToDelete.get().getType().equals(type)) {
				throw new CrudException("Failed to delete edge with id: " + id + " - edge does not exist", javax.ws.rs.core.Response.Status.NOT_FOUND);
			}

			// Now we can delete the edge.
			champApi.deleteRelationship(relationshipToDelete.get(), Optional.of(getTransaction(txId)));

		} catch (ChampRelationshipNotExistsException | NumberFormatException | ChampUnmarshallingException | ChampTransactionException e) {

			throw new CrudException(e.getMessage(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}

	}

	@Override
	public Edge getEdge(String id, String type, String txId) throws CrudException {
		if (logger.isDebugEnabled()) {
			logger.debug("Get edge with id: " + id);
		}

		try {

			// Request the edge from the graph db.
			Optional<ChampRelationship> relationship = champApi.retrieveRelationship(getRelKey(id), Optional.of(getTransaction(txId)));

			// Did we find it?
			if (relationship.isPresent() && relationship.get().getType().equals(type)) {

				// Yup - return the result.
				return edgeFromChampRelationship(relationship.get());

			} else {

				// We didn't find an edge with the supplied id, so throw an exception.
				throw new CrudException("No edge with id " + id + " found in graph", javax.ws.rs.core.Response.Status.NOT_FOUND);
			}

		} catch (ChampUnmarshallingException | ChampTransactionException e) {

			// Something went wrong, so throw an exception.
			throw new CrudException(e.getMessage(), javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

}
