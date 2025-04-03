package bil.nemo.it;

import bil.nemo.it.model.GeoLocation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.squareup.okhttp.*;
import io.github.wistefan.mapping.CacheSerdeableObjectMapper;
import org.apache.http.HttpStatus;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static bil.nemo.it.TestUtils.getLinkHeader;
import static org.junit.jupiter.api.Assertions.*;

public class UserApplication {
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private static final ObjectMapper OBJECT_MAPPER = new CacheSerdeableObjectMapper();
    public static final String USER_ID = "urn:ngsi-ld:User:user1";
    public static final GeoLocation ORIGINAL_PICKUP_LOCATION = new GeoLocation(48.0, 10.0);

    private String lastTripRequest;
    private String lastTrip;
    private List<JsonNode> lastTripProposals;


    public String sendUserRequest() throws Exception {

        lastTripRequest = "urn:ngsi-ld:TripRequest:%s".formatted(UUID.randomUUID());

        Map requestEntity = Map.of("type", "TripRequest",
                "id", lastTripRequest,
                "user", Map.of("type", "Property", "value", "urn:ngsi-ld:User:user1"),
                "startLocation", Map.of("type", "GeoProperty", "value", Map.of("type", "Point",
                        "coordinates", List.of(48.0540141182173,10.890079039102002))),
                "targetLocation", Map.of("type", "GeoProperty", "value", Map.of("type", "Point",
                        "coordinates", List.of(49.0540141182173,11.890079039102002))),
                "requestedAdults", Map.of("type", "Property", "value", 1),
                "pickupTime", Map.of("type", "Property", "value", "2024-08-08T14:33:06Z"));
        TestUtils.createEntity(requestEntity);
        return lastTripRequest;
    }

    public List<JsonNode> getTripProposals() throws IOException {

        HttpUrl.Builder urlBuilder = HttpUrl.parse(LocalSetupEnvironment.BROKER_ADDRESS + "/ngsi-ld/v1/entities").newBuilder();
        urlBuilder.addQueryParameter("type", "TripProposal");
        urlBuilder.addQueryParameter("q", "request==%s".formatted(lastTripRequest));

        String url = urlBuilder.build().toString();

        // Anfrage erstellen
        Request request = new Request.Builder()
                .url(url)
                .get()
                //.addHeader("Link", getLinkHeader())
                //.addHeader("Accept", "application/ld+json")
                .build();

        Response response = HTTP_CLIENT.newCall(request).execute();
        assertEquals(HttpStatus.SC_OK, response.code(), "The entity should be present.");

        JsonNode jsonNode = OBJECT_MAPPER.readTree(response.body().string());
        assertTrue(jsonNode.isArray() && !jsonNode.isEmpty(), "At least one Trip proposal should be present.");

        // TODO parse NGSI-LD format properly and return entity
        lastTripProposals = ImmutableList.copyOf(jsonNode.iterator());
        return lastTripProposals;
    }

    public String acceptTripProposal() throws Exception {

        String id = "urn:ngsi-ld:Trip:%s".formatted(UUID.randomUUID());

        Map requestEntity = Map.of("type", "Trip",
                "id", id,
                "user", Map.of("type", "Property", "value", USER_ID),
                "pickupLocation", Map.of("type", "GeoProperty", "value", Map.of("type", "Point",
                        "coordinates", List.of(48.0540141182173,10.890079039102002))),
                "dropoffLocation", Map.of("type", "GeoProperty", "value", Map.of("type", "Point",
                        "coordinates", List.of(49.0540141182173,11.890079039102002))),
                "requestedAdults", Map.of("type", "Property", "value", 1),
                "pickupTime", Map.of("type", "Property", "value", "2024-08-08T14:33:06Z"),
                "status", Map.of("type", "Property", "value", List.of("Unplanned")));
        TestUtils.createEntity(requestEntity);

        lastTrip = id;
        return lastTrip;
    }

    public boolean checkTripAccepted() throws IOException {

        HttpUrl.Builder urlBuilder = HttpUrl.parse(LocalSetupEnvironment.BROKER_ADDRESS + "/ngsi-ld/v1/entities/"+lastTrip).newBuilder();

        String url = urlBuilder.build().toString();

        // Anfrage erstellen
        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Link", getLinkHeader())
                .addHeader("Accept", "application/ld+json")
                .build();

        Response response = HTTP_CLIENT.newCall(request).execute();
        assertEquals(HttpStatus.SC_OK, response.code(), "The entity should be present.");

        JsonNode jsonNode = OBJECT_MAPPER.readTree(response.body().string());
        assertNotNull(jsonNode, "One Trip should be present.");
        boolean tripPlanned = Optional
                .of(jsonNode)
                .map(a -> a.get("status"))
                .map(a -> a.get("value"))
                .map(JsonNode::textValue)
                .filter("Planned"::equals)
                .isPresent();
        assertTrue(tripPlanned, "Trip of the user should be marked as planned");
        return true;
    }
}
