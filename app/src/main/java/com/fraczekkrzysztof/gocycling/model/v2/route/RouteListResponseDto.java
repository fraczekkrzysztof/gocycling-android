package com.fraczekkrzysztof.gocycling.model.v2.route;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class RouteListResponseDto {

    private List<RouteDto> routes;
}
