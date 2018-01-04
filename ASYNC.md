# ASYNCHRONOUS MODE

Gizmo has two modes, a Synchoronous (sync) mode and an Asynchronous (async) mode.

In the Async mode, Gizmo uses the consumer/producer model where when a
client makes a request, Gizmo will generate an event payload and
publish it  on the async event stream. It will then wait for a
response for that particular event on a different event stream. Once it recieves a
response, gizmo will send a response back to the client which made the
original request.

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
