package com.fraczekkrzysztof.gocycling.model.v2.event;

import com.fraczekkrzysztof.gocycling.model.v2.PageDto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ConversationListResponseDto {

    List<ConversationDto> conversations;
    PageDto pageDto;
}
