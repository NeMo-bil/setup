package bil.nemo.it.vehicle;

import bil.nemo.it.model.CabProperties;
import bil.nemo.it.model.GeoLocation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static bil.nemo.it.LocalSetupEnvironment.CAB_ID;

public class CarPool {
    private List<Cab> cabs = new ArrayList<>();

    public Cab addCab() throws Exception {
        Cab cab = new Cab(CAB_ID);
        cab.connect();
        cab.updateProperties(new CabProperties(new GeoLocation(51.71951625039531, 8.754242510633853), 30.0, 81.0, 3200.0, 10.0, 0, Instant.now().toString()));
        return cab;
    }
}

