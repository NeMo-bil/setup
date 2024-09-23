package bil.nemo.it;

import static bil.nemo.it.LocalSetupEnvironment.CONTEXT_FILE;

public abstract class TestUtils {

    public static String getLinkHeader() {
        return String.format("<%s>; rel=\"http://www.w3.org/ns/json-ld#context\"; type=\"application/ld+json", CONTEXT_FILE);
    }
}
