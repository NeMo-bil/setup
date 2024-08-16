package bil.nemo.it;

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
import java.util.UUID;

import static bil.nemo.it.TestUtils.getLinkHeader;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserApplication {
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private static final ObjectMapper OBJECT_MAPPER = new CacheSerdeableObjectMapper();
    public static final String USER_ID = "urn:ngsi-ld:User:user1";

    private String lastTripRequest;
    private String lastTrip;
    private List<JsonNode> lastTripProposals;


    public String sendUserRequest() throws Exception {

        lastTripRequest = "urn:ngsi-ld:TripRequest:%s".formatted(UUID.randomUUID());

        Map requestEntity = Map.of("type", "TripRequest",
                "id", lastTripRequest,
                "user", Map.of("type", "Property", "value", "urn:ngsi-ld:User:user1"),
                "startLocation", Map.of("type", "GeoProperty", "value", Map.of("type", "Point",
                        "coordinates", List.of(10.890079039102002, 48.0540141182173))),
                "targetLocation", Map.of("type", "GeoProperty", "value", Map.of("type", "Point",
                        "coordinates", List.of(11.890079039102002, 49.0540141182173))),
                "requestedAdults", Map.of("type", "Property", "value", 1),
                "pickupTime", Map.of("type", "Property", "value", "2024-08-08T14:33:06Z"));


        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), OBJECT_MAPPER.writeValueAsString(requestEntity));


        Request creationRequest = new Request.Builder()
                .url(LocalSetupEnvironment.BROKER_ADDRESS + "/ngsi-ld/v1/entities")
                .addHeader("Link", getLinkHeader())
                .post(requestBody)
                .build();
        Response response = HTTP_CLIENT.newCall(creationRequest).execute();
        assertEquals(HttpStatus.SC_CREATED, response.code(), "The entity should have been created.");
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
                .addHeader("Link", getLinkHeader())
                .addHeader("Accept", "application/ld+json")
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
                "cabPickupLocation", Map.of("type", "GeoProperty", "value", Map.of("type", "Point",
                        "coordinates", List.of(10.890079039102002, 48.0540141182173))),
                "cabDropoffLocation", Map.of("type", "GeoProperty", "value", Map.of("type", "Point",
                        "coordinates", List.of(11.890079039102002, 49.0540141182173))),
                "requestedAdults", Map.of("type", "Property", "value", 1),
                "pickupTime", Map.of("type", "Property", "value", "2024-08-08T14:33:06Z"),
                "status", Map.of("type", "Property", "value", List.of("Unplanned")));


        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), OBJECT_MAPPER.writeValueAsString(requestEntity));


        Request creationRequest = new Request.Builder()
                .url(LocalSetupEnvironment.BROKER_ADDRESS + "/ngsi-ld/v1/entities")
                .addHeader("Link", getLinkHeader())
                .post(requestBody)
                .build();
        Response response = HTTP_CLIENT.newCall(creationRequest).execute();
        assertEquals(HttpStatus.SC_CREATED, response.code(), "The entity should have been created.");
        lastTrip = id;
        return lastTrip;
    }

    public boolean checkTripAccepted() throws IOException {

        HttpUrl.Builder urlBuilder = HttpUrl.parse(LocalSetupEnvironment.BROKER_ADDRESS + "/ngsi-ld/v1/entities").newBuilder();
        urlBuilder.addQueryParameter("type", "Trip");
        // FIXME: Searching for all trips of the user and not directly for the trip with the id since the query didn't work properly. Should be fixed to avoid going thru the list
        urlBuilder.addQueryParameter("q", "user==%s".formatted(USER_ID));

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
        assertTrue(jsonNode.isArray() && !jsonNode.isEmpty(), "At least one Trip should be present.");
        long tripsMarkedPlanned = Streams.stream(jsonNode.iterator()).map(a -> a.get("status")).map(a -> a.get("value")).map(JsonNode::textValue).filter("Planned"::equals).count();
        assertEquals(jsonNode.size(),tripsMarkedPlanned, "All trips of the user should be marked as planned");
        return true;
    }
}
