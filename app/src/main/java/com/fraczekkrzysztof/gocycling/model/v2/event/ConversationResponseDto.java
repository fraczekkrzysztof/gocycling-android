package com.fraczekkrzysztof.gocycling.model.v2.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class ConversationResponseDto {

    ConversationDto conversation;
}
