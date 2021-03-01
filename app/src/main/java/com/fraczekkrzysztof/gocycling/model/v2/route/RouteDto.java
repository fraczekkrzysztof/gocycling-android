package com.fraczekkrzysztof.gocycling.model.v2.route;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class RouteDto {

    private ExternalApps appType;
    private String link;
    private String name;
    private String length;
    private String elevation;
    private long created;


}
