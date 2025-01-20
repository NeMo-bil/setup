# Demo

## Request a trip

curl -L 'http://context-broker.127.0.0.1.nip.io:8080/ngsi-ld/v1/entities/' \
-H 'Content-Type: application/json' \
-H 'Link: <http://context-file:9000/context-file/ngsild-context.jsonld>; rel="http://www.w3.org/ns/json-ld#context"; type="application/ld+json"' \
-d @tripRequest.json


## Retrieve the proposal(s) provided by the operational planning

curl -G -H 'Link: <http://context-file:9000/context-file/ngsild-context.jsonld>; rel="http://www.w3.org/ns/json-ld#context"; type="application/ld+json"' 'http://context-broker.127.0.0.1.nip.io:8080/ngsi-ld/v1/entities?type=TripProposal&q=request==%22urn:ngsi-ld:TripRequest:f8d4e1e6-10d0-421d-83cb-35e45e207071%22'

```
[
  {
    "id": "urn:ngsi-ld:TripProposal:96d1817a-dd4d-4b40-b354-06551e49febd",
    "type": "TripProposal",
    "cabDropoffLocation": {
      "type": "GeoProperty",
      "value": {
        "type": "Point",
        "coordinates": [
          2.3522,
          48.8566
        ]
      }
    },
    "cabPickupLocation": {
      "type": "GeoProperty",
      "value": {
        "type": "Point",
        "coordinates": [
          13.405,
          52.52
        ]
      }
    },
    "pickupTime": {
      "type": "Property",
      "value": "2024-08-09T12:00:00Z"
    },
    "proposalReleaseTime": {
      "type": "Property",
      "value": "2024-08-08T18:00:00Z"
    },
    "request": {
      "type": "Relationship",
      "object": "urn:ngsi-ld:TripRequest:f8d4e1e6-10d0-421d-83cb-35e45e207071"
    }
  }
]
```

## User accepts a proposal


curl -L 'http://context-broker.127.0.0.1.nip.io:8080/ngsi-ld/v1/entities/' \
-H 'Content-Type: application/json' \
-H 'Link: <http://context-file:9000/context-file/ngsild-context.jsonld>; rel="http://www.w3.org/ns/json-ld#context"; type="application/ld+json"' \
-d @trip.json

## Check if trip was planned

curl -G -H 'Link: <http://context-file:9000/context-file/ngsild-context.jsonld>; rel="http://www.w3.org/ns/json-ld#context"; type="application/ld+json"' 'http://context-broker.127.0.0.1.nip.io:8080/ngsi-ld/v1/entities/urn:ngsi-ld:Trip:ccb1f755-9990-4051-bff0-286c4b5a16f1'

```
{
  "id": "urn:ngsi-ld:Trip:ccb1f755-9990-4051-bff0-286c4b5a16f1",
  "type": "Trip",
  "pickupTime": {
    "type": "Property",
    "value": "2024-08-08T14:33:06Z"
  },
  "cabPickupLocation": {
    "type": "GeoProperty",
    "value": {
      "type": "Point",
      "coordinates": [
        51.729777,
        8.753317
      ]
    }
  },
  "requestedAdults": {
    "type": "Property",
    "value": 1
  },
  "cabDropoffLocation": {
    "type": "GeoProperty",
    "value": {
      "type": "Point",
      "coordinates": [
        52.729777,
        8.753317
      ]
    }
  },
  "user": {
    "type": "Property",
    "value": "urn:ngsi-ld:User:user1"
  },
  "status": {
    "value": "Planned",
    "type": "Property"
  }
}
```



# Postman

## Create TripRequest
- User sends a request via the user application to request transportation

> Reisewith gets a notification regarding a new TripRequest and creates TripProposals that link the Request
> Payment service would add the price to the Proposals ( not implemented yet)

## Retrieve TripProposals
- User retrieves all Proposals created for the request

## Create Trip
- User picks a proposal by creating a trip with state Unplanned
- State and property matching with the proposal is enforced by the AccessControl

> Reisewitz gets a notification regarding the new Trip and updates the status to Planned

## Get Trip
- Reisewitz updated the status to Planned

> Reisewitz will update the nextStopLocation property of a vehicle when the trip is due
> Node Red receives a notification regarding the property change and writes it to the MQTT broker

## Get Vehicle
- The vehicle has the pickupLocation as a nextStopLocation
