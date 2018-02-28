# ASYNCHRONOUS MODE

Gizmo has two modes, a Synchoronous (sync) mode and an Asynchronous (async) mode.

In the Async mode, Gizmo uses the consumer/producer model where when a
client makes a request, Gizmo will generate an event payload and
publish it  on the async event stream. It will then wait for a
response for that particular event on a different event stream. Once it recieves a
response, gizmo will send a response back to the client which made the
original request.

Note: In the examples below, there is a database-transaction-id and a
transaction-id. A database-transaction-id is only present in the
payload when it's part of bulk operation.

## Here are a few examples of the events published by Gizmo

### Vertex

#### Adding a Vertex

    {
        "timestamp": 1514927928167,
        "operation": "CREATE",
        "vertex": {
            "properties": {
                "ipv4-oam-address": "1.2.3.4",
                "resource-version": "1477013499",
                "purpose": "my-purpose",
                "fqdn": "myhost.onap.net",
                "in-maint": false,
                "equip-model": "DL380p-nd",
                "equip-vendor": "HP",
                "equip-type": "server",
                "hostname": "myhost",
                "ptnii-equip-name": "e-name"
            },
            "key": "",
            "type": "pserver",
            "schema-version": "vX"
        },
        "transaction-id": "c0a81fa7-5ef4-49cd-ab39-e42c53c9b9a4",
        "database-transaction-id": "b3e2853e-f643-47a3-a0c3-cb54cc997ad3"
    }

#### Updating a Vertex

    {
        "timestamp": 1514929776429,
        "operation": "UPDATE",
        "vertex": {
            "properties": {
                "ipv4-oam-address": "1.2.3.4",
                "resource-version": "1477013499",
                "purpose": "my-purpose",
                "fqdn": "updated.myhost.onap.net",
                "in-maint": false,
                "equip-model": "DELL380p-nd",
                "equip-vendor": "DELL",
                "equip-type": "updated-server",
                "hostname": "updated-myhost",
                "ptnii-equip-name": "name-has-been-updated"
            },
            "key": "84bf7b3f-81f5-4c34-ab5c-207281cb71bd",
            "type": "pserver",
            "schema-version": "vX"
        },
        "transaction-id": "3b8df1d5-4c51-47e3-bbef-c27b47e11149",
        "database-transaction-id": "b3e2853e-f643-47a3-a0c3-cb54cc997ad3"
    }

#### Deleting a Vertex

    {
        "timestamp": 1514930052327,
        "operation": "DELETE",
        "vertex": {
            "key": "84bf7b3f-81f5-4c34-ab5c-207281cb71bd",
            "type": "pserver",
            "schema-version": "vX"
        },
        "transaction-id": "6bb7a27b-a942-4cac-9b2b-0fa1f3897b8c",
        "database-transaction-id": "b3e2853e-f643-47a3-a0c3-cb54cc997ad3"
    }


#### Adding an Edge

    {
      "timestamp": 1515005153863,
      "operation": "CREATE",
      "edge": {
        "target": {
          "key": "febd8996-62ec-4ce6-ba8e-d2fa1609e13b",
          "type": "pserver"
        },
        "properties": {
          "contains-other-v": "NONE",
          "delete-other-v": "NONE",
          "prevent-delete": "IN",
          "SVC-INFRA": "OUT"
        },
        "key": "",
        "type": "tosca.relationships.HostedOn",
        "schema-version": "v11",
        "source": {
          "key": "7beade35-19f1-4c1d-a1bd-bfba59e0b582",
          "type": "vserver"
        }
      },
      "transaction-id": "63a8994d-1118-4e65-ab06-fff40f6f48ef",
      "database-transaction-id": "b3e2853e-f643-47a3-a0c3-cb54cc997ad3"
    }

#### Replace an Edge

    {
      "timestamp": 1515005301622,
      "operation": "UPDATE",
      "edge": {
        "target": {
          "key": "febd8996-62ec-4ce6-ba8e-d2fa1609e13b",
          "type": "pserver"
        },
        "properties": {
          "contains-other-v": "NOPE",
          "delete-other-v": "YES",
          "prevent-delete": "MAYBE",
          "SVC-INFRA": "OUT"
        },
        "key": "9727a0ea-559e-497c-98e4-0cbdaede0346",
        "type": "tosca.relationships.HostedOn",
        "schema-version": "v11",
        "source": {
          "key": "7beade35-19f1-4c1d-a1bd-bfba59e0b582",
          "type": "vserver"
        }
      },
      "transaction-id": "ed284991-6c2f-4c94-a592-76fed17a2f14",
      "database-transaction-id": "b3e2853e-f643-47a3-a0c3-cb54cc997ad3"
    }


#### Deleting an Edge

    {
      "timestamp": 1515005579837,
      "operation": "DELETE",
      "edge": {
        "key": "9727a0ea-559e-497c-98e4-0cbdaede0346",
        "type": "tosca.relationships.HostedOn",
        "schema-version": "v11"
      },
      "transaction-id": "b4583bc9-dd96-483f-ab2d-20c1c6e5622f",
      "database-transaction-id": "b3e2853e-f643-47a3-a0c3-cb54cc997ad3"
    }


## Gizmo expects a response event in the following formats

### Vertex

#### Adding Vertex Response

    {
      "timestamp": 1519832579046,
      "operation": "CREATE",
      "vertex": {
        "properties": {
          "last-mod-source-of-truth": "champ-response",
          "resource-version": "1477013499",
          "aai-uuid": "12a0ca27-25b1-41c2-b9ff-4bf54a3caa8a",
          "in-maint": false,
          "equip-model": "DELL380p-nd",
          "hostname": "updated-myhost",
          "source-of-truth": "champ-response",
          "ipv4-oam-address": "1.2.3.4",
          "aai-created-ts": 1519832577709,
          "purpose": "my-purpose",
          "fqdn": "updated.myhost.onap.net",
          "equip-vendor": "DELL",
          "equip-type": "updated-server",
          "aai-last-mod-ts": 1519832577709,
          "ptnii-equip-name": "testing"
        },
        "key": "12a0ca27-25b1-41c2-b9ff-4bf54a3caa8a",
        "type": "pserver",
        "schema-version": "v11"
      },
      "transaction-id": "d5c15dd3-2492-4ab9-86a3-e1cd39221e4a",
      "result": "SUCCESS"
    }

#### Updating Vertex Response

    {
      "timestamp": 1519833774425,
      "operation": "UPDATE",
      "vertex": {
        "properties": {
          "last-mod-source-of-truth": "shwetank-updating",
          "resource-version": "shwetank-resource",
          "aai-uuid": "12a0ca27-25b1-41c2-b9ff-4bf54a3caa8a",
          "in-maint": false,
          "equip-model": "testing-update",
          "hostname": "sdave",
          "source-of-truth": "champ-response",
          "ipv4-oam-address": "1.2.3.4",
          "aai-created-ts": 1519832577709,
          "purpose": "my-purpose-testing-update",
          "fqdn": "created.myhost.onap.net",
          "equip-vendor": "testing-update",
          "equip-type": "shwetank",
          "aai-last-mod-ts": 1519833774407,
          "ptnii-equip-name": "testing-update"
        },
        "key": "12a0ca27-25b1-41c2-b9ff-4bf54a3caa8a",
        "type": "pserver",
        "schema-version": "v11"
      },
      "transaction-id": "2981a1b0-30d6-4323-8b36-c80636a1639f",
      "result": "SUCCESS"
    }


#### Deleting Vertex Response

    {
      "timestamp": 1519839126416,
      "operation": "DELETE",
      "vertex": {
        "key": "12a0ca27-25b1-41c2-b9ff-4bf54a3caa8a",
        "type": "pserver",
        "schema-version": "v11"
      },
      "transaction-id": "78650070-a500-47bb-98e5-e46aba77612a",
      "result": "SUCCESS"
    }


### Edge

#### Adding Edge Response

    {
      "timestamp": 1519835679492,
      "operation": "CREATE",
      "edge": {
        "target": {
          "key": "2674b073-2eb5-484b-a21c-7e36983b7cd4",
          "type": "pserver"
        },
        "properties": {
          "contains-other-v": "NONE",
          "delete-other-v": "NONE",
          "aai-created-ts": 1519835679447,
          "prevent-delete": "IN",
          "SVC-INFRA": "OUT",
          "aai-uuid": "149620ae-2382-41a2-b626-1dd321fad4bb",
          "aai-last-mod-ts": 1519835679447
        },
        "key": "149620ae-2382-41a2-b626-1dd321fad4bb",
        "type": "tosca.relationships.HostedOn",
        "schema-version": "v11",
        "source": {
          "key": "b945d923-b596-4339-a80f-f72ca9a4fe1f",
          "type": "vserver"
        }
      },
      "transaction-id": "83c7776d-88c9-4fbe-b129-11f40d20348b",
      "result": "SUCCESS"
    }

#### Update Edge Response

    {
      "timestamp": 1519838804681,
      "operation": "UPDATE",
      "edge": {
        "target": {
          "key": "2674b073-2eb5-484b-a21c-7e36983b7cd4",
          "type": "pserver"
        },
        "properties": {
          "contains-other-v": "NONE",
          "delete-other-v": "NONE",
          "aai-created-ts": 1519835679447,
          "SVC-INFRA": "OUT",
          "prevent-delete": "TRUE",
          "aai-uuid": "149620ae-2382-41a2-b626-1dd321fad4bb",
          "aai-last-mod-ts": 1519838804671
        },
        "key": "149620ae-2382-41a2-b626-1dd321fad4bb",
        "type": "tosca.relationships.HostedOn",
        "schema-version": "v11",
        "source": {
          "key": "b945d923-b596-4339-a80f-f72ca9a4fe1f",
          "type": "vserver"
        }
      },
      "transaction-id": "644839f0-bb6d-490f-8f79-bb8227198ed2",
      "result": "SUCCESS"
    }

#### Delete Edge Response

    {
      "timestamp": 1519838964576,
      "operation": "DELETE",
      "edge": {
        "key": "149620ae-2382-41a2-b626-1dd321fad4bb",
        "type": "tosca.relationships.HostedOn",
        "schema-version": "v11"
      },
      "transaction-id": "db3e67ae-28e3-4c1d-bf63-ec398cf28d98",
      "result": "SUCCESS"
    }


### Misc

#### Failed Operation Response


    {
      "timestamp": 1519843867529,
      "operation": "UPDATE",
      "httpErrorStatus": "BAD_REQUEST",
      "error-message": "aai-created-ts can't be updated",
      "vertex": {
        "properties": {
          "last-mod-source-of-truth": "shwetank-creating-for-update",
          "ipv4-oam-address": "1.2.3.4",
          "aai-created-ts": 123456789,
          "resource-version": "shwetank-resource",
          "purpose": "my-purpose-creating-for-patching",
          "fqdn": "created.myhost.onap.net",
          "in-maint": false,
          "equip-model": "creating-for-patching",
          "equip-vendor": "creating-for-patching",
          "equip-type": "testing-2",
          "hostname": "sdave",
          "ptnii-equip-name": "creating-for-patching"
        },
        "key": "10572385-3211-4c80-a7c5-cea648513271",
        "type": "pserver",
        "schema-version": "v11"
      },
      "transaction-id": "25d4c3e9-d4ab-41ec-b2a9-832286a726db",
      "result": "FAILURE"
    }
