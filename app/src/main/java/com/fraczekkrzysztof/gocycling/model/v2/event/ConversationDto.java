package com.fraczekkrzysztof.gocycling.model.v2.event;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ConversationDto {

    private long id;
    private String userId;
    private String userName;
    private String created;
    private String message;
}
