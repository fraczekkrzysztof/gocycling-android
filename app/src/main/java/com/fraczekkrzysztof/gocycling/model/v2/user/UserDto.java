package com.fraczekkrzysztof.gocycling.model.v2.user;

import com.fraczekkrzysztof.gocycling.model.v2.route.ExternalApps;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class UserDto {

    private String id;
    private String name;
    private List<ExternalApps> externalApps;
}
