## Bulk API

The bulk API allows a client to add/modify/patch/delete multiple vertexes and/or edges within a single request.  This request will be treated as an atomic transaction in that all operations within the bulk request will either fail or succeed together. This is often useful when attempting to add an entire subgraph.

The following example shows how a client could create 2 vertexes (pserver and vserver) and link them with an edge. In the JSON body, the order in which vertex and edge operations are defined within the `objects` and `relationships` arrays is not important. The bulk API will handle the operations requested in a logical order to preserve data integrity.

    URL: https://<host>:9520/services/inventory/v11/bulk
    Method: POST
    Body:
        {
            "objects":[
                {
                    "operation":"add",
                    "v1":{
                        "type":"vserver",
                        "properties":{
                            "in-maint":false,
                            "vserver-name":"vserver1",
                            "prov-status":"Provisioned",
                            "vserver-id":"Vserver-AMT-002-HSGW",
                            "vserver-name2":"Vs2-HSGW-OTT",
                            "vserver-selflink":"AMT VserverLink",
                            "is-closed-loop-disabled":false
                        }
                    }
                },
                {
                    "operation":"add",
                    "v2":{
                        "type":"pserver",
                        "properties":{
                            "ptnii-equip-name":"ps1993",
                            "hostname":"pserver1",
                            "equip-type":"server",
                            "equip-vendor":"HP",
                            "equip-model":"DL380p-nd",
                            "in-maint":false,
                            "fqdn":"pserver1.lab.com",
                            "ipv4-oam-address":"199.1.138.60"
                        }
                    }
                }
            ],
            "relationships":[
                {
                    "operation":"add",
                    "e1":{
                        "type":"tosca.relationships.HostedOn",
                        "source":"$v1",
                        "target":"$v2",
                        "properties":{
                            "contains-other-v": "NONE",
                            "delete-other-v": "NONE",
                            "SVC-INFRA": "OUT",
                            "prevent-delete": "IN"
                        }
                    }
                }
            ]
        }

    Success Response:
        Code: 200
        Content:
            {
                "objects": [
                    {
                        "operation": "add",
                        "v1": {
                            "id": "1024143488",
                            "type": "vserver",
                            "url": "services/inventory/v11/vserver/1024143488",
                            "properties": {
                                "in-maint":false,
                                "vserver-name":"vserver1",
                                "prov-status":"Provisioned",
                                "vserver-id":"Vserver-AMT-002-HSGW",
                                "vserver-name2":"Vs2-HSGW-OTT",
                                "vserver-selflink":"AMT VserverLink",
                                "is-closed-loop-disabled":false
                            },
                            "in": [],
                            "out": []
                        }
                    },
                    {
                        "operation": "add",
                        "v2": {
                            "id": "1228865600",
                            "type": "pserver",
                            "url": "services/inventory/v11/pserver/1228865600",
                            "properties": {
                                "ptnii-equip-name":"ps1993",
                                "hostname":"pserver1",
                                "equip-type":"server",
                                "equip-vendor":"HP",
                                "equip-model":"DL380p-nd",
                                "in-maint":false,
                                "fqdn":"pserver1.lab.com",
                                "ipv4-oam-address":"199.1.138.60"
                            },
                            "in": [],
                            "out": []
                        }
                    }
                ],
                "relationships": [
                    {
                        "operation": "add",
                        "e1": {
                            "id": "kbrs40-gxqy68-108id-kbmurk",
                            "type": "tosca.relationships.HostedOn",
                            "url": "services/inventory/relationships/v11/tosca.relationships.HostedOn/kbrs40-gxqy68-108id-kbmurk",
                            "source": "services/inventory/v11/vserver/1024143488",
                            "target": "services/inventory/v11/pserver/1228865600",
                            "properties": {
                                "SVC-INFRA": "OUT",
                                "prevent-delete": "IN",
                                "delete-other-v": "NONE",
                                "contains-other-v": "NONE"
                            }
                        }
                    }
                ]
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
