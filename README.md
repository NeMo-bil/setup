# local-setup
Helm Umbrella Chart for the Nemo.Bil applications and a local setup of the digital platform.

Requirements:
- [maven](https://maven.apache.org/)

Useful tools:
- [kubectl](https://kubernetes.io/docs/reference/kubectl/)
- [helm](https://helm.sh/)

## Deployment with Helm

The NeMo.Bil chart is a [Helm Umbrella-Chart](https://helm.sh/docs/howto/charts_tips_and_tricks/#complex-charts-with-many-dependencies), containing all the sub-charts of the different components and their dependencies.

The chart is available at the repository ```https://nemo-bil.github.io/setup/```. You can install it via:

```shell
    # add the repo
    helm repo add nemobil https://nemo-bil.github.io/setup/
    # install the chart
    helm install <DeploymentName> nemobil/nemobil -n <Namespace> -f values.yaml
```
**Note,** that due to the app-of-apps structure of the chart and the different dependencies between the components, a deployment without providing any configuration values will not work. Make sure to provide a
`values.yaml` file for the deployment, specifying all necessary parameters. This currently mainly includes setting parameters of the DNS information (providing Ingress or OpenShift Route parameters).

Configurations for all sub-charts (and sub-dependencies) can be managed through the top-level [values.yaml](./charts/nemobil/values.yaml) of the chart. It contains the default values of each component and additional parameter shared between the components. The configuration of the applications can be changed under the key ```<APPLICATION_NAME>```, please see the individual applications and there sub-charts for the available options.  

## Local cluster
### Local cluster for development

> :warning: The local deployment uses [k3s](https://k3s.io/) and is currently only tested on linux.

```
mvn clean deploy -Plocal
export KUBECONFIG=$(pwd)/target/k3s.yaml
```

Starts a local K3S cluster with the components used by the NeMo.Bil digital platform utilizing mocked external services.

The [Helm Values File](k3s/local.yaml) can be adapted to adjust the setup and to disable the used mocks. 

### Integration tests using umbrella chart

```
mvn clean install -Ptest
export KUBECONFIG=$(pwd)/it/target/k3s.yaml
```

The [Cucumber File](it/src/main/resources/it/buchungsanfrage.feature) and the connected test functions can be extended to cover more flows.


## Endpoints
To have the local environment as close to reality as possible, all interaction happens
through [Ingress](https://kubernetes.io/docs/concepts/services-networking/ingress/). The ingress is provided via the
[Traefik-IngressController](https://doc.traefik.io/traefik/providers/kubernetes-ingress/) and configured
here: [k3s/infra/traefik]. Additionally, to ensure access to public endpoints happens
equally from inside the cluster and outside of it, [CoreDNS](https://coredns.io/)(deployed on default with k3s) is
instructed to resolve the ingresses(e.g. *.127.0.0.1.nip.io) directly to the loadbalancer-service of Traefik.

| Component            | Url                                               | User credentials               |
|----------------------|---------------------------------------------------|--------------------------------|
| Grafana              | http://grafana.127.0.0.1.nip.io:8080              | see secret "local/local-grafana" |
| API Gateway          | http://traefik.127.0.0.1.nip.io:8080/dashboard/#/ | -/-                            |
| Kubernetes Dashboard | http://k8s.127.0.0.1.nip.io:8080                  |  `kubectl create token observability-kubernetes-dashboard`                             |



## Useful snippets

### General

Stop and remove an existing k3s instance
```
docker stop k3s-maven-plugin ; docker rm k3s-maven-plugin
```

### Broker

Retrieve the entities of type "Vehicle" 
```
curl -G -iX GET 'http://context-broker.127.0.0.1.nip.io:8080/ngsi-ld/v1/entities' \
-H 'type="application/ld+json"' \
-H 'Link: <http://context-file:9000/context-file/ngsild-context.jsonld>; rel="http://www.w3.org/ns/json-ld#context"; type="application/ld+json"' \
-d 'type=Vehicle'
```

### Useful links

[NGSI-LD Tutorial Suite](https://github.com/FIWARE/tutorials.NGSI-LD)
