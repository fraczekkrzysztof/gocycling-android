package com.fraczekkrzysztof.gocycling.model.v2.club;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;


@Builder
@Data
@AllArgsConstructor()
public class ClubDto implements Serializable {
    long id;
    String name;
    String location;
    private double latitude;
    private double longitude;
    private String ownerId;
    private String ownerName;
    private String created;
    private String details;
    private boolean privateMode;
    private List<MemberDto> memberList;
}
