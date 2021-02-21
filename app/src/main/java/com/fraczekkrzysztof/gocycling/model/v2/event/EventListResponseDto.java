package com.fraczekkrzysztof.gocycling.model.v2.event;

import com.fraczekkrzysztof.gocycling.model.v2.PageDto;

import java.io.Serializable;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class EventListResponseDto implements Serializable {

    private List<EventDto> events;
    private PageDto page;
}
