package com.fraczekkrzysztof.gocycling.model.v2.notificatication;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationDto {

    private long id;
    private String userUid;
    private String title;
    private String content;
    private String created;
    private boolean read;
    private NotificationType type;
    private long clubId;
    private long eventId;
}
