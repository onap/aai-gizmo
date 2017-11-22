## Edge APIs

### Create Edge

When creating an edge, the CRUD service will validate:
* properties match the defined schema
* relationship is valid between the source and target

	URL: https://<host>:9520/services/inventory/relationships/v11/tosca.relationships.HostedOn/
	Method: POST
	Body:
		{     
			"source":"services/inventory/v11/vserver/0",
			"target":"services/inventory/v11/pserver/7",
			"properties":{   
				"SVC-INFRA": "OUT",
                "prevent-delete": "IN",
                "delete-other-v": "NONE",
                "contains-other-v": "NONE"
			}
		}
	Success	Response:
		Code: 201
		Content:
			{   
				"id":"215x5m-6hc-d6vp-oe08g",
				"type":"tosca.relationships.HostedOn",
				"url":"services/inventory/relationships/v11/has/215x5m-6hc-d6vp-oe08g",
				"source":"services/inventory/v11/vserver/0",
				"target":"services/inventory/v11/pserver/7",
				"properties":{   
					"SVC-INFRA": "OUT",
                    "prevent-delete": "IN",
                    "delete-other-v": "NONE",
                    "contains-other-v": "NONE"
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

	URL: https://<host>:9520/services/inventory/relationships/v11/
	Method: POST
	Body:
		{    
			"type":"tosca.relationships.HostedOn",
			"source":"services/inventory/v11/vserver/0",
			"target":"services/inventory/v11/pserver/7",
			"properties":{   
				"SVC-INFRA": "OUT",
                "prevent-delete": "IN",
                "delete-other-v": "NONE",
                "contains-other-v": "NONE"
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

	URL: https://<host>:9520/services/resources/relationships/tosca.relationships.HostedOn/
	Method: POST
	Body:
		{     
			"source":"services/inventory/v11/vserver/0",
			"target":"services/inventory/v11/pserver/7",
			"properties":{   

			}
		}
	Success	Response:
		Code: 201
		Content:
			{   
				"id":"215x5m-6hc-d6vp-oe08g",
				"type":"tosca.relationships.HostedOn",
				"url":"services/inventory/relationships/v11/has/215x5m-6hc-d6vp-oe08g",
				"source":"services/inventory/v11/vserver/0",
				"target":"services/inventory/v11/pserver/7",
				"properties":{   
                    "SVC-INFRA": "OUT",
                    "prevent-delete": "IN",
                    "delete-other-v": "NONE",
                    "contains-other-v": "NONE"
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
			"type":"tosca.relationships.HostedOn",
			"source":"services/inventory/v11/vserver/0",
			"target":"services/inventory/v11/pserver/7",
			"properties":{   
				"SVC-INFRA": "OUT",
                "prevent-delete": "IN",
                "delete-other-v": "NONE",
                "contains-other-v": "NONE"
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

	URL: https://<host>:9520/services/inventory/relationships/v11/tosca.relationships.HostedOn/<id>
	Method: GET
	Success	Response:
		Code: 200
		Content:
			{   
				"id":"215x5m-6hc-d6vp-oe08g",
				"type":"tosca.relationships.HostedOn",
				"url":"services/inventory/relationships/tosca.relationships.HostedOn/has/215x5m-6hc-d6vp-oe08g",
				"source":"services/inventory/v11/vserver/8400",
				"target":"services/inventory/v11/pserver/40964272",
				"properties":{   
                    "SVC-INFRA": "OUT",
                    "prevent-delete": "IN",
                    "delete-other-v": "NONE",
                    "contains-other-v": "NONE"
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

	URL: https://<host>:9520/services/inventory/relationships/v11/tosca.relationships.HostedOn
	Optional Query Param: ?multiplicity=many
	Method: GET
	Success	Response:
		Code: 200
		Content:
			[   
				{   
					"id":"1crwnu-6hc-d6vp-oe08g",
					"type":"tosca.relationships.HostedOn",
					"url":"services/inventory/relationships/v11/tosca.relationships.HostedOn/1crwnu-6hc-d6vp-oe08g",
					"source":"services/inventory/v11/vserver/8400",
					"target":"services/inventory/v11/pserver/40964272"
				},
				{   
					"id":"215x5m-6hc-d6vp-oe08g",
					"type":"tosca.relationships.HostedOn",
					"url":"services/inventory/relationships/v11/tosca.relationships.HostedOn/215x5m-6hc-d6vp-oe08g",
					"source":"services/inventory/v11/vserver/8400",
					"target":"services/inventory/v11/pserver/40964272"
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

	URL: https://<host>:9520/services/inventory/relationships/v11/tosca.relationships.HostedOn/<id>
	Method: PUT
	Body: (**Note that the source and target can not be modified)
		{     
			"properties":{   
                "SVC-INFRA": "OUT",
                "prevent-delete": "IN",
                "delete-other-v": "NONE",
                "contains-other-v": "NONE"
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

	URL: https://<host>:9520/services/inventory/relationships/v11/tosca.relationships.HostedOn/<id>
	Method: PATCH (Content-Type header set to application/merge-patch+json)
	Body:
		{        
			"properties":{   
				"prevent-delete":"OUT"
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

	URL: https://<host>:9520/services/inventory/relationships/v11/tosca.relationships.HostedOn/<id>
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
 