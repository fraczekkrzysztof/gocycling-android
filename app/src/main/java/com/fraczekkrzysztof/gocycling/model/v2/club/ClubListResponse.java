package com.fraczekkrzysztof.gocycling.model.v2.club;

import com.fraczekkrzysztof.gocycling.model.v2.PageDto;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor()
public class ClubListResponse implements Serializable {
    List<ClubDto> clubs;
    PageDto page;
}
