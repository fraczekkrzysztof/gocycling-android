package com.fraczekkrzysztof.gocycling.event;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.content.ContentValues.TAG;

public class EventModel {
    private String name;
    private String place;
    private Date dateAndTime;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public Date getDateAndTime() {
        return dateAndTime;
    }

    public void setDateAndTime(Date dateAndTime) {
        this.dateAndTime = dateAndTime;
    }

    public static List<EventModel> fromJson(JSONObject jsonObject) {
        List<EventModel> eventList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        try{
            JSONArray eventArray = jsonObject.getJSONObject("_embedded").getJSONArray("events");
            for (int i = 0; i < eventArray.length(); i++) {
                JSONObject eventObject = eventArray.getJSONObject(i);
                EventModel eventModel = new EventModel();
                eventModel.setName(eventObject.getString("name"));
                eventModel.setPlace(eventObject.getString("place"));
                eventModel.setDateAndTime(sdf.parse(eventObject.getString("dateAndTime")));
                eventList.add(eventModel);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "fromJson: size of list " + eventList.size() );
        return eventList;

    }
}
