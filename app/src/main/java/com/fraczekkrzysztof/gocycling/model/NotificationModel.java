package com.fraczekkrzysztof.gocycling.model;

import android.util.Log;

import com.fraczekkrzysztof.gocycling.utils.DateUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NotificationModel {
    private static final String TAG = "NotificationModel";
    private long id;
    private String userUid;
    private String title;
    private String content;
    private Date created;
    private boolean read;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public static List<NotificationModel> fromJson(JSONObject jsonObject) {
        Log.d(TAG, "fromJson: parsing response for receiving list of events");
        List<NotificationModel> notificationList = new ArrayList<>();
        try{
            JSONArray notificationArray = jsonObject.getJSONObject("_embedded").getJSONArray("notifications");
            for (int i = 0; i < notificationArray.length(); i++) {
                JSONObject notificationObject = notificationArray.getJSONObject(i);
                NotificationModel notificationModel = new NotificationModel();
                notificationModel.setId(notificationObject.getLong("id"));
                notificationModel.setUserUid(notificationObject.getString("userUid"));
                notificationModel.setTitle(notificationObject.getString("title"));
                notificationModel.setContent(notificationObject.getString("content"));
                notificationModel.setCreated(DateUtils.sdfWithFullTime.parse(notificationObject.getString("created")));
                notificationModel.setRead(notificationObject.getBoolean("read"));
                notificationList.add(notificationModel);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return notificationList;
    }
}
