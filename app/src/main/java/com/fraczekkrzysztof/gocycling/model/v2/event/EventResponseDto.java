package com.fraczekkrzysztof.gocycling.model.v2.event;


import java.io.Serializable;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class EventResponseDto implements Serializable {

    EventDto event;
}
