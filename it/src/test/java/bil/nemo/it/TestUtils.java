package bil.nemo.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.*;
import io.github.wistefan.mapping.CacheSerdeableObjectMapper;
import org.apache.http.HttpStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static bil.nemo.it.LocalSetupEnvironment.CONTEXT_FILE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class TestUtils {
    private static final ObjectMapper OBJECT_MAPPER = new CacheSerdeableObjectMapper();
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

    public static String getLinkHeader() {
        return String.format("<%s>; rel=\"http://www.w3.org/ns/json-ld#context\"; type=\"application/ld+json\"", CONTEXT_FILE);
    }

    public static void createEntity(Map entity) throws Exception{
        Map entityToBeCreated = new HashMap();
        entityToBeCreated.putAll(entity);
        if(!entityToBeCreated.containsKey("@context")){
            entityToBeCreated.put("@context",List.of(CONTEXT_FILE,"http://uri.etsi.org/ngsi-ld/v1/ngsi-ld-core-context.jsonld"));
        }
        String payload = OBJECT_MAPPER.writeValueAsString(entityToBeCreated);
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/ld+json"), payload);


        Request creationRequest = new Request.Builder()
                .url(LocalSetupEnvironment.BROKER_ADDRESS + "/ngsi-ld/v1/entities")
                //.addHeader("Link", getLinkHeader())
                .post(requestBody)
                .build();
        Response response = HTTP_CLIENT.newCall(creationRequest).execute();
        assertEquals(HttpStatus.SC_CREATED, response.code(), "The entity should have been created.");
        System.out.println("Body:" + response.body().string());
    }
}
