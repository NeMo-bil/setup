package bil.nemo.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.wistefan.mapping.CacheSerdeableObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.awaitility.Awaitility;
import org.fiware.ngsi.model.SubscriptionListVO;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static bil.nemo.it.LocalSetupEnvironment.CAB_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


@Slf4j
public class StepDefinitions {

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();
    private static final ObjectMapper OBJECT_MAPPER = new CacheSerdeableObjectMapper();

    private UserApplication userApplication = new UserApplication();

    private List<String> createdEntities = new ArrayList<>();

    @After
    public void cleanUp() {
        cleanUpEntities();
    }

    private void cleanUpEntities() {
        createdEntities.forEach(entityId -> {
            Request deletionRequest = new Request.Builder()
                    .url(LocalSetupEnvironment.BROKER_ADDRESS + "/ngsi-ld/v1/entities/" + entityId)
                    .delete()
                    .build();
            try {
                HTTP_CLIENT.newCall(deletionRequest).execute();
            } catch (IOException e) {
                // just log
                log.warn("Was not able to clean up entitiy {}.", entityId, e);
            }
        });
    }

    @Given("alle Services laufen")
    public void checkServices() throws Exception {
        // Check broker is initialized with data
        checkForEntity(CAB_ID);
        //Check grafana is initialized
        Request grafanaDashboardRequest = new Request.Builder().get()
                .url(LocalSetupEnvironment.VISUALISATION_ADDRESS + "/d/cdstxz6x03r40a/fahrzeuge?orgId=1")
                .build();

        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .until(() -> HttpStatus.SC_OK == HTTP_CLIENT.newCall(grafanaDashboardRequest).execute().code());
    }

    private void checkForEntity(String entityId) {
        Request retrieveEntityRequest = new Request.Builder().get()
                .url(LocalSetupEnvironment.BROKER_ADDRESS + "/ngsi-ld/v1/entities/" + entityId)
                .addHeader("Accept", "application/json")
                .build();

        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .until(() -> HttpStatus.SC_OK == HTTP_CLIENT.newCall(retrieveEntityRequest).execute().code());
    }

    @Given("Alle Subscriptions sind angelegt")
    public void checkSubscriptions() throws Exception {
        Request retrieveEntityRequest = new Request.Builder().get()
                .url(LocalSetupEnvironment.BROKER_ADDRESS + "/ngsi-ld/v1/subscriptions/")
                .addHeader("Accept", "application/ld+json")
                .build();
        Response response = HTTP_CLIENT.newCall(retrieveEntityRequest).execute();
        assertEquals(HttpStatus.SC_OK, response.code(), "The subscription endpoint should have responded successfully");
        String responseString = response.body().string();
        //SubscriptionListVO subscriptions = OBJECT_MAPPER.readValue(responseString, SubscriptionListVO.class);
        JsonNode jsonNode = OBJECT_MAPPER.readTree(responseString);

        assertFalse(jsonNode.isEmpty(), "The subscription should have been registered.");
    }

    @Given("Cabs existieren im System")
    public void checkForVehicle() throws Exception {
        // Check broker is initialized with a vehicle
        checkForEntity(CAB_ID);
    }

    @Given("Kunde hat valide Zahlungsinformationen")
    public void checkForPaymentData() throws Exception {
        // FIXME after payment service was added
    }

    @When("der Nutzer eine Fahrt für 1 Person und 1 Cab bucht")
    public void requestTrip() throws Exception {
        createdEntities.add(userApplication.sendUserRequest());
    }

    @Then("soll die operative Planung 3 Vorschlaege schicken")
    public void checkForTripProposals() throws Exception {

        List<JsonNode> tripProposals = Awaitility
                .await()
                .atMost(30, TimeUnit.SECONDS)
                .ignoreExceptions()
                .until(()->userApplication.getTripProposals(), list -> !list.isEmpty());
        tripProposals
                .stream()
                .map(e->e.get("id"))
                .map(JsonNode::asText)
                .forEach(createdEntities::add);
    }

    @Then("der Nutzer einen auswaehlen")
    public void sendTripProposalAccept() throws Exception {
        userApplication.acceptTripProposal();
    }

    @Then("der Bezahlservice die Buchung erlaubt")
    public void allowPayment() throws Exception {
    }

    @Then("die Operative Planung die Buchung bestaetigt")
    public void checkBookingConfirmation() throws Exception {
        Awaitility
                .await()
                .atMost(30, TimeUnit.SECONDS)
                .ignoreExceptions()
                .until(()->userApplication.checkTripAccepted());
    }

}
