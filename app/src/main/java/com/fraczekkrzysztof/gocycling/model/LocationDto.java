package com.fraczekkrzysztof.gocycling.model;

import java.io.Serializable;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LocationDto implements Serializable {

    double latitude;
    double longitude;
    String description;
}
