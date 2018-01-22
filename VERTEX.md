## Vertex APIs

### Create Vertex

Vertex payload data is validated against oxm.
* Mandatory attributes are required in payload
* Data type validation is enforced
* Defaults from oxm schema used when not specified in payload

	URL: https://<host>:9520/services/inventory/v11/pserver/
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
				"url":"services/inventory/v11/pserver/1130672",
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

	URL: https://<host>:9520/services/inventory/v11/
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
				"url":"services/inventory/v11/pserver/1130672",
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

	URL: https://<host>:9520/services/inventory/v11/pserver/<id>
	Method: GET
	Success	Response:
		Code: 200
		Content:
			{   
				"id":"1130672",
				"type":"pserver",
				"url":"services/inventory/v11/pserver/<id>",
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
						"target":"services/inventory/v11/vserver/40964272",
						"url":"services/inventory/relationships/v11/has/1crwnu-6hc-d6vp-oe08g"
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

	URL: https://<host>:9520/services/inventory/v11/pserver/
	Optional Query Param: ?equip-vendor=HP
	Method: GET
	Success	Response:
		Code: 200
		Content:
			[   
				{   
					"id":"950296",
					"type":"pserver",
					"url":"services/inventory/v11/pserver/950296"
				},
				{   
					"id":"1126576",
					"type":"pserver",
					"url":"services/inventory/v11/pserver/1126576"
				},
				{   
					"id":"1032384",
					"type":"pserver",
					"url":"services/inventory/v11/pserver/1032384"
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
		
### Get Vertices with Properties
Note: Adding query param of properties=all will return all properties

	URL: https://<host>:9520/services/inventory/v11/pserver/
	Optional Query Param: ?equip-vendor=HP
	Optional Query Param: ?properties=hostname&properties=equip-vendor
	Method: GET
	Success	Response:
		Code: 200
		Content:
			[
                {
                    "idfdsa": "1263346e-372b-4681-8ce4-d40411620487",
                    "type": "pserver",
                    "url": "services/inventory/v11/pserver/1263346e-372b-4681-8ce4-d40411620487",
                    "properties": {
                        "equip-vendor": "HP",
                        "hostname": "mtanjasdf119snd"
                    }
                },
                {
                    "idfdsa": "b57a9e54-bbb5-4e11-b537-aaa7bc8fd726",
                    "type": "pserver",
                    "url": "services/inventory/v11/pserver/b57a9e54-bbb5-4e11-b537-aaa7bc8fd726",
                    "properties": {
                        "equip-vendor": "HP",
                        "hostname": "mtanjasdf119snd"
                    }
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

	URL: https://<host>:9520/services/inventory/v11/pserver/<id>
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

	URL: https://<host>:9520/services/inventory/v11/pserver/<id>
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

	URL: https://<host>:9520/services/inventory/v11/pserver/<id>
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
