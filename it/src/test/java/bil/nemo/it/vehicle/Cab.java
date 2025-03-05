package bil.nemo.it.vehicle;

import bil.nemo.it.LocalSetupEnvironment;
import bil.nemo.it.model.CabProperties;
import bil.nemo.it.model.GeoLocation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.eclipse.paho.client.mqttv3.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static bil.nemo.it.LocalSetupEnvironment.MQTT_COMMAND_TOPIC_TEMPLATE;
import static bil.nemo.it.LocalSetupEnvironment.MQTT_TELEMETRY_TOPIC_TEMPLATE;
import static bil.nemo.it.TestUtils.createEntity;
import static bil.nemo.it.UserApplication.ORIGINAL_PICKUP_LOCATION;

/**
 *
 */
public class Cab implements IMqttMessageListener, Runnable {
    private static final int MQTT_QOS_AT_LEAST_ONCE = 1;

    private final IMqttClient client;
    @Getter
    private CabProperties properties;
    private final ObjectMapper mapper;
    @Getter
    private final String id;

    private GeoLocation currentLocation = new GeoLocation(48.0, 10.0);
    @Getter
    private GeoLocation nextStopLocation = ORIGINAL_PICKUP_LOCATION;


    public Cab() throws Exception {
        this("urn:ngsi-ld:vehicle:" + UUID.randomUUID().toString());
    }


    public Cab(String id) throws Exception {
        this.id = id;
        mapper = new ObjectMapper();
        String publisherId = UUID.randomUUID().toString();
        client = new MqttClient("tcp://%s".formatted(LocalSetupEnvironment.MQTT_BROKER_ADDRESS), publisherId);
        createEntity(Map.of("id", id, "type", "Vehicle", "nextStopLocation", Map.of("type", "GeoProperty", "value", Map.of("type", "Point",
                "coordinates", List.of(nextStopLocation.latitude(), nextStopLocation.longitude()))), "location", Map.of("type", "GeoProperty", "value", Map.of("type", "Point",
                "coordinates", List.of(currentLocation.latitude(), currentLocation.longitude()))),
                "speed", Map.of("type", "Property", "value",1.0),
                "bearing", Map.of("type", "Property", "value",1.0),
                "consumption", Map.of("type", "Property", "value",1.0),
                "batteryLevel", Map.of("type", "Property", "value",1.0)));
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(this, 10, 10, TimeUnit.SECONDS);
    }

    public void connect() throws MqttException {
        if (!client.isConnected()) {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            client.connect(options);
        }
        client.subscribe(MQTT_COMMAND_TOPIC_TEMPLATE.formatted(id), this);
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
        client.publish(MQTT_TELEMETRY_TOPIC_TEMPLATE.formatted(id), msg);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        System.out.println("Message " + message.toString() + " arrived on topic " + topic);
        JsonNode coordinatesNode = mapper.readTree(message.toString()).path("nextStopLocation").path("coordinates");
        if (coordinatesNode.isArray() && coordinatesNode.size() == 2) {
            double latitude = coordinatesNode.get(0).asDouble();
            double longitude = coordinatesNode.get(1).asDouble();
            nextStopLocation = new GeoLocation(latitude, longitude);
        } else {
            System.err.println("Invalid data" + message.toString());
        }
    }

    private double calculateNewPosition(double current, double target) {
        double difference = target - current;
        double step = difference * 0.1;
        if (step < 0.01) {
            step = Math.min(0.01, difference);
        }
        return current + step;
    }

    @Override
    public void run() {
        try {
            currentLocation = new GeoLocation(calculateNewPosition(currentLocation.latitude(), nextStopLocation.latitude()), calculateNewPosition(currentLocation.longitude(), nextStopLocation.longitude()));
            this.properties.setLocation(currentLocation);
            send();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
