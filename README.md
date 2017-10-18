runsOnPserverrunsOnPserver# CRUD Microservice (Gizmo)

## Overview
The CRUD microservice implements a set of RESTful APIs which allow a client to perform CREATE, UPDATE, GET, and DELETE operations on verticies and edges within the A&AI graph database.

## Getting Started

### Building The Microservice

After cloning the project, execute the following Maven command from the project's top level directory to build the project:

    > mvn clean install

Now, you can build your Docker image:

    > docker build -t openecomp/crud-service target 
    
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
	
### Create Vertex

Vertex payload data is validated against oxm.
* Mandatory attributes are required in payload
* Data type validation is enforced
* Defaults from oxm schema used when not specified in payload

	URL: https://<host>:9520/services/inventory/v8/pserver/
	Method: POST
	Body:
		{   
			"properties":{   
				"ptnii-equip-name":"e-name",
				"equip-type":"server",
				"hostname":"myhost",
				"equip-vendor":"HP",
				"equip-model":"DL380p-nd",
				"fqdn":myhost.onap.net",
				"purpose":"my-purpose",
				"resource-version":"1477013499",
				"ipv4-oam-address":"1.2.3.4"
			}
		}
	Success	Response:
		Code: 201
		Content:
			{   
				"id":"1130672",
				"type":"pserver",
				"url":"services/inventory/v8/pserver/1130672",
				"properties":{   
					"ptnii-equip-name":"e-name",
					"equip-type":"server",
					"hostname":"myhost",
					"equip-vendor":"HP",
					"equip-model":"DL380p-nd",
					"fqdn":myhost.onap.net",
					"purpose":"my-purpose",
					"resource-version":"1477013499",
					"ipv4-oam-address":"1.2.3.4"
				}
			}	
	Error Response:
		Code: 400 (BAD REQUEST)
		Content: Error message describing the bad request failure.
		Situation: Invalid Payload or schema error.

		Code: 403 (FORBIDDEN)
		Content: Error message describing the Authorization failure.
		Situation: Authorization failure.

		Code: 415 (UNSUPPORTED MEDIA TYPE)
		Situation: Unsupported content type .
		
		Code: 500 (Internal Server Error)
		Content: Error message describing the failure.
		Situation: Any scenario not covered by the above error codes.

Optionally, a vertex can be created by posting to an endpoint which doesn't include the vertex type.

	URL: https://<host>:9520/services/inventory/v8/
	Method: POST
	Body:
		{   
			"type":"pserver",
			"properties":{   
				"ptnii-equip-name":"e-name",
				"equip-type":"server",
				"hostname":"myhost",
				"equip-vendor":"HP",
				"equip-model":"DL380p-nd",
				"fqdn":myhost.onap.net",
				"purpose":"my-purpose",
				"resource-version":"1477013499",
				"ipv4-oam-address":"1.2.3.4"
			}
		}
	Success	Response:
		Code: 201
		Content:
			{   
				"id":"1130672",
				"type":"pserver",
				"url":"services/inventory/v8/pserver/1130672",
				"properties":{   
					"ptnii-equip-name":"e-name",
					"equip-type":"server",
					"hostname":"myhost",
					"equip-vendor":"HP",
					"equip-model":"DL380p-nd",
					"fqdn":myhost.onap.net",
					"purpose":"my-purpose",
					"resource-version":"1477013499",
					"ipv4-oam-address":"1.2.3.4"
				}
			}	
	Error Response:
		Code: 400 (BAD REQUEST)
		Content: Error message describing the bad request failure.
		Situation: Invalid Payload or schema error.

		Code: 403 (FORBIDDEN)
		Content: Error message describing the Authorization failure.
		Situation: Authorization failure.

		Code: 415 (UNSUPPORTED MEDIA TYPE)
		Situation: Unsupported content type .
		
		Code: 500 (Internal Server Error)
		Content: Error message describing the failure.
		Situation: Any scenario not covered by the above error codes.
 
### Get Vertex

	URL: https://<host>:9520/services/inventory/v8/pserver/<id>
	Method: GET
	Success	Response:
		Code: 200
		Content:
			{   
				"id":"1130672",
				"type":"pserver",
				"url":"services/inventory/v8/pserver/<id>",
				"properties":{   
					"ptnii-equip-name":"e-name",
					"equip-type":"server",
					"hostname":"myhost",
					"equip-vendor":"HP",
					"equip-model":"DL380p-nd",
					"fqdn":myhost.onap.net",
					"purpose":"my-purpose",
					"resource-version":"1477013499",
					"ipv4-oam-address":"1.2.3.4"
				},
				"in":[   
				],
				"out":[   
					{   
						"id":"1crwnu-6hc-d6vp-oe08g",
						"type":"has",
						"target":"services/inventory/v8/vserver/40964272",
						"url":"services/inventory/relationships/v8/has/1crwnu-6hc-d6vp-oe08g"
					}
				]
			}	
	Error Response:
		Code: 404 (NOT FOUND)
		Situation: Resource Not found

		Code: 403 (FORBIDDEN)
		Content: Error message describing the Authorization failure.
		Situation: Authorization failure.

		Code: 415 (UNSUPPORTED MEDIA TYPE)
		Situation: Unsupported content type .
		
		Code: 500 (Internal Server Error)
		Content: Error message describing the failure.
		Situation: Any scenario not covered by the above error codes.

### Get Vertices

	URL: https://<host>:9520/services/inventory/v8/pserver/
	Optional Query Param: ?equip-vendor=HP
	Method: GET
	Success	Response:
		Code: 200
		Content:
			[   
				{   
					"id":"950296",
					"type":"pserver",
					"url":"services/inventory/v8/pserver/950296"
				},
				{   
					"id":"1126576",
					"type":"pserver",
					"url":"services/inventory/v8/pserver/1126576"
				},
				{   
					"id":"1032384",
					"type":"pserver",
					"url":"services/inventory/v8/pserver/1032384"
				}
			]	
	Error Response:
		Code: 404 (NOT FOUND)
		Situation: Resource Not found

		Code: 403 (FORBIDDEN)
		Content: Error message describing the Authorization failure.
		Situation: Authorization failure.

		Code: 415 (UNSUPPORTED MEDIA TYPE)
		Situation: Unsupported content type .
		
		Code: 500 (Internal Server Error)
		Content: Error message describing the failure.
		Situation: Any scenario not covered by the above error codes.		

### Update Vertex

The PUT command is used to modify an existing vertex.  By default, the vertex data is replaced by the content of the payload.  However, teh following parameter can be added to the header to perform a PATCH instead of a replace:
**X-HTTP-Method-Override=Patch**

	URL: https://<host>:9520/services/inventory/v8/pserver/<id>
	Method: PUT
	Body: Same as POST	
	Success	Response:
		Code: 201
		Content: Same as POST	
	Error Response:
		Code: 400 (BAD REQUEST)
		Content: Error message describing the bad request failure.
		Situation: Invalid Payload or schema error.

		Code: 403 (FORBIDDEN)
		Content: Error message describing the Authorization failure.
		Situation: Authorization failure.

		Code: 415 (UNSUPPORTED MEDIA TYPE)
		Situation: Unsupported content type .
		
		Code: 500 (Internal Server Error)
		Content: Error message describing the failure.
		Situation: Any scenario not covered by the above error codes.

### Patch Vertex

	URL: https://<host>:9520/services/inventory/v8/pserver/<id>
	Method: PATCH (Content-Type header set to application/merge-patch+json)
	Body:
		{   
			"properties":{   
				"ptnii-equip-name":"e-name",
				"resource-version":"1477013499",
				"ipv4-oam-address":"1.2.3.99"
			}
		}
	Success	Response:
		Code: 200
		Content: Same as POST		
	Error Response:
		Code: 400 (BAD REQUEST)
		Content: Error message describing the bad request failure.
		Situation: Invalid Payload or schema error.

		Code: 403 (FORBIDDEN)
		Content: Error message describing the Authorization failure.
		Situation: Authorization failure.

		Code: 415 (UNSUPPORTED MEDIA TYPE)
		Situation: Unsupported content type .
		
		Code: 500 (Internal Server Error)
		Content: Error message describing the failure.
		Situation: Any scenario not covered by the above error codes.
 
 ### Delete Vertex

	URL: https://<host>:9520/services/inventory/v8/pserver/<id>
	Method: DELETE
	Success	Response:
		Code: 200		
	Error Response:
		Code: 404 (NOT FOUND)
		Situation: Resource not found

		Code: 403 (FORBIDDEN)
		Content: Error message describing the Authorization failure.
		Situation: Authorization failure.

		Code: 415 (UNSUPPORTED MEDIA TYPE)
		Situation: Unsupported content type .
		
		Code: 500 (Internal Server Error)
		Content: Error message describing the failure.
		Situation: Any scenario not covered by the above error codes.

### Create Edge

When creating an edge, the CRUD service will validate:
* properties match the defined schema
* relationship is valid between the source and target

	URL: https://<host>:9520/services/inventory/relationships/v8/runsOnPserver/
	Method: POST
	Body:
		{     
			"source":"services/inventory/v8/vserver/0",
			"target":"services/inventory/v8/pserver/7",
			"properties":{   
				"multiplicity":"many",
				"is-parent":true,
				"uses-resource":"true",
				"has-del-target":"true"
			}
		}
	Success	Response:
		Code: 201
		Content:
			{   
				"id":"215x5m-6hc-d6vp-oe08g",
				"type":"runsOnPserver",
				"url":"services/inventory/relationships/v8/has/215x5m-6hc-d6vp-oe08g",
				"source":"services/inventory/v8/vserver/0",
				"target":"services/inventory/v8/pserver/7",
				"properties":{   
					"is-parent":"true",
					"multiplicity":"many",
					"has-del-target":"true",
					"uses-resource":"true"
				}
			}
	Error Response:
		Code: 400 (BAD REQUEST)
		Content: Error message describing the bad request failure.
		Situation: Invalid Payload or schema error.

		Code: 403 (FORBIDDEN)
		Content: Error message describing the Authorization failure.
		Situation: Authorization failure.

		Code: 415 (UNSUPPORTED MEDIA TYPE)
		Situation: Unsupported content type .
		
		Code: 500 (Internal Server Error)
		Content: Error message describing the failure.
		Situation: Any scenario not covered by the above error codes.

Optionally, an edge can be created by posting to an endpoint which doesn't include the edge type.

	URL: https://<host>:9520/services/inventory/relationships/v8/
	Method: POST
	Body:
		{    
			"type":"runsOnPserver",
			"source":"services/inventory/v8/vserver/0",
			"target":"services/inventory/v8/pserver/7",
			"properties":{   
				"multiplicity":"many",
				"is-parent":true,
				"uses-resource":"true",
				"has-del-target":"true"
			}
		}
	Success	Response:
		Code: 201
		Content: Same as above	
	Error Response:
		Code: 400 (BAD REQUEST)
		Content: Error message describing the bad request failure.
		Situation: Invalid Payload or schema error.

		Code: 403 (FORBIDDEN)
		Content: Error message describing the Authorization failure.
		Situation: Authorization failure.

		Code: 415 (UNSUPPORTED MEDIA TYPE)
		Situation: Unsupported content type .
		
		Code: 500 (Internal Server Error)
		Content: Error message describing the failure.
		Situation: Any scenario not covered by the above error codes.
 
### Create Edge With Auto-Population Of Edge Properties
An alternate endpoint exists for creating edges which follows all of the conventions of the above endpoints, with the  addition that properties defined in the db edge rules produced by the A&AI will be automatically populated for the edge.

	URL: https://<host>:9520/services/resources/relationships/runsOnPserver/
	Method: POST
	Body:
		{     
			"source":"services/inventory/v8/vserver/0",
			"target":"services/inventory/v8/pserver/7",
			"properties":{   

			}
		}
	Success	Response:
		Code: 201
		Content:
			{   
				"id":"215x5m-6hc-d6vp-oe08g",
				"type":"runsOnPserver",
				"url":"services/inventory/relationships/v8/has/215x5m-6hc-d6vp-oe08g",
				"source":"services/inventory/v8/vserver/0",
				"target":"services/inventory/v8/pserver/7",
				"properties":{   
			        "contains-other-v": "NONE",
			        "delete-other-v": "NONE",
			        "SVC-INFRA": "OUT",
			        "prevent-delete": "IN"
				}
			}
	Error Response:
		Code: 400 (BAD REQUEST)
		Content: Error message describing the bad request failure.
		Situation: Invalid Payload or schema error.

		Code: 403 (FORBIDDEN)
		Content: Error message describing the Authorization failure.
		Situation: Authorization failure.

		Code: 415 (UNSUPPORTED MEDIA TYPE)
		Situation: Unsupported content type .
		
		Code: 500 (Internal Server Error)
		Content: Error message describing the failure.
		Situation: Any scenario not covered by the above error codes.
 
The same option to POST to an endpoint without specifying a type in the URL exists for this endpoint as well:

	URL: https://<host>:9520/services/resources/relationships/
	Method: POST
	Body:
		{    
			"type":"runsOnPserver",
			"source":"services/inventory/v8/vserver/0",
			"target":"services/inventory/v8/pserver/7",
			"properties":{   
				"multiplicity":"many",
				"is-parent":true,
				"uses-resource":"true",
				"has-del-target":"true"
			}
		}
	Success	Response:
		Code: 201
		Content: Same as above	
	Error Response:
		Code: 400 (BAD REQUEST)
		Content: Error message describing the bad request failure.
		Situation: Invalid Payload or schema error.

		Code: 403 (FORBIDDEN)
		Content: Error message describing the Authorization failure.
		Situation: Authorization failure.

		Code: 415 (UNSUPPORTED MEDIA TYPE)
		Situation: Unsupported content type .
		
		Code: 500 (Internal Server Error)
		Content: Error message describing the failure.
		Situation: Any scenario not covered by the above error codes.
		
### Get Edge

	URL: https://<host>:9520/services/inventory/relationships/v8/runsOnPserver/<id>
	Method: GET
	Success	Response:
		Code: 200
		Content:
			{   
				"id":"215x5m-6hc-d6vp-oe08g",
				"type":"runsOnPserver",
				"url":"services/inventory/relationships/v8/has/215x5m-6hc-d6vp-oe08g",
				"source":"services/inventory/v8/vserver/8400",
				"target":"services/inventory/v8/pserver/40964272",
				"properties":{   
					"is-parent":"true",
					"multiplicity":"many",
					"has-del-target":"true",
					"uses-resource":"true"
				}
			}	
	Error Response:
		Code: 404 (NOT FOUND)
		Situation: Resource Not found

		Code: 403 (FORBIDDEN)
		Content: Error message describing the Authorization failure.
		Situation: Authorization failure.

		Code: 415 (UNSUPPORTED MEDIA TYPE)
		Situation: Unsupported content type .
		
		Code: 500 (Internal Server Error)
		Content: Error message describing the failure.
		Situation: Any scenario not covered by the above error codes.

### Get Edges

	URL: https://<host>:9520/services/inventory/relationships/v8/runsOnPserver
	Optional Query Param: ?multiplicity=many
	Method: GET
	Success	Response:
		Code: 200
		Content:
			[   
				{   
					"id":"1crwnu-6hc-d6vp-oe08g",
					"type":"runsOnPserver",
					"url":"services/inventory/relationships/v8/runsOnPserver/1crwnu-6hc-d6vp-oe08g",
					"source":"services/inventory/v8/vserver/8400",
					"target":"services/inventory/v8/pserver/40964272"
				},
				{   
					"id":"215x5m-6hc-d6vp-oe08g",
					"type":"runsOnPserver",
					"url":"services/inventory/relationships/v8/runsOnPserver/215x5m-6hc-d6vp-oe08g",
					"source":"services/inventory/v8/vserver/8400",
					"target":"services/inventory/v8/pserver/40964272"
				}
			]
	Error Response:
		Code: 404 (NOT FOUND)
		Situation: Resource Not found

		Code: 403 (FORBIDDEN)
		Content: Error message describing the Authorization failure.
		Situation: Authorization failure.

		Code: 415 (UNSUPPORTED MEDIA TYPE)
		Situation: Unsupported content type .
		
		Code: 500 (Internal Server Error)
		Content: Error message describing the failure.
		Situation: Any scenario not covered by the above error codes.		

### Update Edge

The PUT command is used to modify an existing edge.  By default, the edge data is replaced by the content of the payload.  However, the following parameter can be added to the header to perform a PATCH instead of a replace:
**X-HTTP-Method-Override=Patch**

	URL: https://<host>:9520/services/inventory/relationships/v8/runsOnPserver/<id>
	Method: PUT
	Body: (**Note that the source and target can not be modified)
		{     
			"properties":{   
				"multiplicity":"many",
				"is-parent":true,
				"uses-resource":"true",
				"has-del-target":"true"
			}
		}
	Success	Response:
		Code: 200
		Content: Same as POST	
	Error Response:
		Code: 400 (BAD REQUEST)
		Content: Error message describing the bad request failure.
		Situation: Invalid Payload or schema error.

		Code: 403 (FORBIDDEN)
		Content: Error message describing the Authorization failure.
		Situation: Authorization failure.

		Code: 415 (UNSUPPORTED MEDIA TYPE)
		Situation: Unsupported content type .
		
		Code: 500 (Internal Server Error)
		Content: Error message describing the failure.
		Situation: Any scenario not covered by the above error codes.

### Patch Edge

	URL: https://<host>:9520/services/inventory/relationships/v8/runsOnPserver/<id>
	Method: PATCH (Content-Type header set to application/merge-patch+json)
	Body:
		{        
			"properties":{   
				"multiplicity":"many"
			}
		}
	Success	Response:
		Code: 200
		Content: Same as POST		
	Error Response:
		Code: 400 (BAD REQUEST)
		Content: Error message describing the bad request failure.
		Situation: Invalid Payload or schema error.

		Code: 403 (FORBIDDEN)
		Content: Error message describing the Authorization failure.
		Situation: Authorization failure.

		Code: 415 (UNSUPPORTED MEDIA TYPE)
		Situation: Unsupported content type .
		
		Code: 500 (Internal Server Error)
		Content: Error message describing the failure.
		Situation: Any scenario not covered by the above error codes.
 
### Delete Edge

	URL: https://<host>:9520/services/inventory/relationships/v8/runsOnPserver/<id>
	Method: DELETE
	Success	Response:
		Code: 200		
	Error Response:
		Code: 404 (NOT FOUND)
		Situation: Resource not found

		Code: 403 (FORBIDDEN)
		Content: Error message describing the Authorization failure.
		Situation: Authorization failure.

		Code: 415 (UNSUPPORTED MEDIA TYPE)
		Situation: Unsupported content type .
		
		Code: 500 (Internal Server Error)
		Content: Error message describing the failure.
		Situation: Any scenario not covered by the above error codes.
 
 		
 