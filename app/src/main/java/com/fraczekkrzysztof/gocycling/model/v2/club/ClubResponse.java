package com.fraczekkrzysztof.gocycling.model.v2.club;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ClubResponse {

    private ClubDto club;
}
