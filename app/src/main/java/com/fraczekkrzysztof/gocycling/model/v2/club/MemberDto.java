package com.fraczekkrzysztof.gocycling.model.v2.club;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor()
public class MemberDto implements Serializable {
    private long id;
    private String userUid;
    private String userName;
    private boolean confirmed;
}
