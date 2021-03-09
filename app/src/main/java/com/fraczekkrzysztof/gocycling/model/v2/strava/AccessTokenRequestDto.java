package com.fraczekkrzysztof.gocycling.model.v2.strava;

public class AccessTokenRequestDto {
    private String userUid;
    private String accessToken;

    public AccessTokenRequestDto() {

    }

    public AccessTokenRequestDto(String userUid, String accessToken) {
        this.userUid = userUid;
        this.accessToken = accessToken;
    }

    public String getUserUid() {
        return userUid;
    }

    public void setUserUid(String userUid) {
        this.userUid = userUid;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
