package com.fraczekkrzysztof.gocycling.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConversationModel {

    private static final String TAG = "ConversationModel";
    private long id;
    private String userUid;
    private String username;
    private Date created;
    private String message;

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static List<ConversationModel> fromJson(JSONObject jsonObject) {
        Log.d(TAG, "fromJson: parsing response for receiving list of events");
        List<ConversationModel> conversationList = new ArrayList<>();
        try{
            JSONArray conversationArray = jsonObject.getJSONObject("_embedded").getJSONArray("conversations");
            for (int i = 0; i < conversationArray.length(); i++) {
                JSONObject conversationObject = conversationArray.getJSONObject(i);
                ConversationModel conversationModel = new ConversationModel();
                conversationModel.setId(conversationObject.getLong("id"));
                conversationModel.setUserUid(conversationObject.getString("userUid"));
                conversationModel.setUsername(conversationObject.getString("username"));
//                conversationModel.setCreated(DateUtils.SDF_WITH_FULL_TIME.parse(conversationObject.getString("created")));
                conversationModel.setMessage(conversationObject.getString("message"));
                conversationList.add(conversationModel);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conversationList;

    }
}
