# local-setup
Local setup of the NeMo.Bil project's digital platform.


Endpoints:

| Component            | Url | User credentials               |
|----------------------|-----|--------------------------------|
| Grafana              | grafana.127.0.0.1.nip.io:8080    | see secret "local/local-grafana" |
| API Gateway          | http://traefik.127.0.0.1.nip.io:8080/dashboard/#/    | -/-                            |
| Kubernetes Dashboard | http://k8s.127.0.0.1.nip.io:8080    |  `kubectl create token observability-kubernetes-dashboard`                             |



## Broker

```
curl -G -iX GET 'http://context-broker.127.0.0.1.nip.io:8080/ngsi-ld/v1/entities' \
-H 'type="application/ld+json"' \
-H 'Link: <http://context-file:9000/context-file/ngsild-context.jsonld>; rel="http://www.w3.org/ns/json-ld#context"; type="application/ld+json"' \
-d 'type=Vehicle'
```
