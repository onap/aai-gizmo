# CRUD Microservice (Gizmo)

## Overview
The CRUD microservice implements a set of RESTful APIs which allow a client to perform CREATE, UPDATE, GET, and DELETE operations on verticies and edges within the A&AI graph database.

## Getting Started

### Building The Microservice

After cloning the project, execute the following Maven command from the project's top level directory to build the project:

    > mvn clean install

Now, you can build your Docker image:

    > docker build -t onap/crud-service target

### Deploying The Microservice

Push the Docker image that you have built to your Docker repository and pull it down to the location from which you will be running the service.

**Create the following directories on the host machine:**

    ../logs
    ../appconfig
	../appconfig/auth

You will be mounting these as data volumes when you start the Docker container.

#### Configuring the Microservice

Create configuration file **../appconfig/crud-api.properties**

	# List of hostnames/addresses of the graph database
	crud.graph.host=graphhost1.onap.com,graphhost2.onap.com

	# Port on which to connect to the graph database
	crud.graph.port=2181

	# Name of the graph on which this service will operate
	crud.graph.name=aaigraphautomation

	# Backend storage type for the graph.  Types currently supported:
	#  - cassandra
	#  - hbase
	crud.storage.backend.db=cassandra

	# List of hostnames/addresses of the DMaaP/Kafka cluster on which to post notification events
	event.stream.hosts=kafkahost1.onap.com,kafkahost2.onap.com

	# Number of events to bath up before posting to DMaaP/Kafka
	event.stream.batch-size=100

	# Amount of time (in ms) to wait before sending batch of events (when batch does not reach batch-size)
	event.stream.batch-timeout=60000

Create configuration file **../appconfig/auth/crud-policy.json**

This policy file defines which client certificates are authorized to use the service's APIs.  An example policy file follows:

    {
        "roles": [
            {
                "name": "admin",
                "functions": [
                    {
                        "name": "search", "methods": [ { "name": "GET" },{ "name": "DELETE" }, { "name": "PUT" }, { "name": "POST" } ]
                    }
                ],
                "users": [
                    {
                        "username": "CN=admin, OU=My Organization Unit, O=, L=Sometown, ST=SomeProvince, C=CA"
                    }
                ]
            }
        ]
    }

Create keystore file **../appconfig/auth/tomcat\_keystore**
_tomcat\_keystore_

Create a keystore with this name containing whatever CA certificates that you want your instance of the CRUD service to accept for HTTPS traffic.

#### Start the service

You can now start the Docker container in the following manner:

	docker run -d \
	    -p 9520:9520 \
		-e CONFIG_HOME=/opt/app/crud-service/config/ \
		-e KEY_STORE_PASSWORD={{obfuscated password}} \
		-e KEY_MANAGER_PASSWORD=OBF:{{obfuscated password}} \
	    -v /<path>/logs:/opt/aai/logroot/AAI-CRUD \
	    -v /<path>/appconfig:/opt/app/crud-service/config \
	    --name crud-service \
	    {{your docker repo}}/crud-service

Where,

    {{your docker repo}} = The Docker repository you have published your CRUD Service image to.
    {{obfuscated password}} = The password for your key store/key manager after running it through the Jetty obfuscation tool.

## API Definitions

### Echo API

	URL: https://<host>:9520/services/crud-api/v1/echo-service/echo/<input>
	Method: GET
	Success Response: 200

### Vertex APIs
Gizmo exposes a set of APIs to operate on verticies within the graph.
[Vertex APIs](./VERTEX.md)

### Edge APIs
Gizmo exposes a set of APIs to operate on edges within the graph.
[Edge APIs](./EDGE.md)

### Bulk API
Gizmo exposes a bulk API to operate on multiple graph entities within a single request.
[Bulk API](./BULK.md)

## ASYNC PIPELINE
Gizmo is capable of working Synchronously and Asynchronously. Asynchronous Pipeline is explained
here: [Async Pipeline](./ASYNC.md)
