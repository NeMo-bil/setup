package bil.nemo.it;

public abstract class LocalSetupEnvironment {
    public static final String BROKER_ADDRESS = "http://context-broker.127.0.0.1.nip.io:8080";
    public static final String OPERATIONAL_PLANNING_ADDRESS = "http://reisewitz.127.0.0.1.nip.io:8080";
    public static final String VISUALISATION_ADDRESS = "http://grafana.127.0.0.1.nip.io:8080";
    public static final String CAB_ID = "urn:ngsi-ld:vehicle:someCab";
    public static final String CONTEXT_FILE = "http://context-file:9000/context-file/ngsild-context.jsonld";

}
