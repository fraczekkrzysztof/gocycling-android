package com.fraczekkrzysztof.gocycling.model.v2.notificatication;


import com.fraczekkrzysztof.gocycling.model.v2.PageDto;

import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationListResponseDto {

    private List<NotificationDto> notifications;
    private PageDto page;
}
