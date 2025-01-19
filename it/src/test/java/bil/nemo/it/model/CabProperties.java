package bil.nemo.it.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CabProperties {

    private GeoLocation location;
    private Double bearing;
    private Double batteryLevel;
    private Double consumption;
    private Double speed;
    private Integer chainedPosition;
    private String received_at;

}
