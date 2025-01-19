package bil.nemo.it.vehicle;

import bil.nemo.it.LocalSetupEnvironment;
import bil.nemo.it.model.CabProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;

import java.util.Map;
import java.util.UUID;

import static bil.nemo.it.LocalSetupEnvironment.MQTT_TOPIC_TEMPLATE;
import static bil.nemo.it.TestUtils.createEntity;

/**
 *
 */
public class Cab {
    private static final int MQTT_QOS_AT_LEAST_ONCE = 1;

    private final IMqttClient client;
    private CabProperties properties;
    private final ObjectMapper mapper;
    private final String id;

    public Cab() throws Exception {
        mapper = new ObjectMapper();
        String publisherId = UUID.randomUUID().toString();
        client = new MqttClient("tcp://%s".formatted(LocalSetupEnvironment.MQTT_BROKER_ADDRESS), publisherId);
        id = "urn:ngsi-ld:vehicle:" + UUID.randomUUID().toString();
        createEntity(Map.of("id", id, "type", "Vehicle"));
    }

    public void connect() throws MqttException {
        if (!client.isConnected()) {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            client.connect(options);
        }
    }

    public void updateProperties(CabProperties properties) throws Exception {
        this.properties = properties;
        send();
    }

    private void send() throws Exception {
        connect();
        String propertiesJson = mapper.writeValueAsString(properties);
        MqttMessage msg = new MqttMessage(propertiesJson.getBytes());
        msg.setQos(MQTT_QOS_AT_LEAST_ONCE);
        msg.setRetained(true);
        client.publish(MQTT_TOPIC_TEMPLATE.formatted(id), msg);
    }
}
