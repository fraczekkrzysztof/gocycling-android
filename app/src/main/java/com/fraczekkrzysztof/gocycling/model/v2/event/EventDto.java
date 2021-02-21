package com.fraczekkrzysztof.gocycling.model.v2.event;

import java.io.Serializable;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class EventDto implements Serializable {

    private long id;
    private String name;
    private String place;
    private double latitude;
    private double longitude;
    private String dateAndTime;
    private String created;
    private String updated;
    private String details;
    private String userId;
    private String userName;
    private boolean canceled;
    private String routeLink;
    private List<ConfirmationDto> confirmationList;
    private long clubId;
    private String clubName;
}
