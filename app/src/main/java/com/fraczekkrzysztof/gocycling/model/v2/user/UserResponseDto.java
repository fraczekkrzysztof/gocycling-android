package com.fraczekkrzysztof.gocycling.model.v2.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class UserResponseDto {

    private UserDto user;
}
