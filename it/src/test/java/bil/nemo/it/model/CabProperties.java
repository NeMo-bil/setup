package bil.nemo.it.model;

public record CabProperties (GeoLocation location,double bearing, Double batteryLevel,Double consumption, Double speed, Integer chainedPosition, String received_at){

}
