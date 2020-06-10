package com.fraczekkrzysztof.gocycling.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MemberModel {
    private static final String TAG = "MemberModel";
    private long id;
    private String userUid;
    private boolean confirmed;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUserUid() {
        return userUid;
    }

    public void setUserUid(String userUid) {
        this.userUid = userUid;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public static List<MemberModel> fromJson(JSONObject jsonObject){
        Log.d(TAG, "fromJson: parsing response to receiving list of Members");
        List<MemberModel> listOfMembers= new ArrayList<>();
        try{
            JSONArray memberArray = jsonObject.getJSONObject("_embedded").getJSONArray("members");
            for (int i = 0; i < memberArray.length(); i++) {
                JSONObject memberObject = memberArray.getJSONObject(i);
                MemberModel memberModel = new MemberModel();
                memberModel.setId(memberObject.getLong("id"));
                memberModel.setUserUid(memberObject.getString("userUid"));
                memberModel.setConfirmed(memberObject.getBoolean("confirmed"));
                listOfMembers.add(memberModel);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return listOfMembers;
    }
}
