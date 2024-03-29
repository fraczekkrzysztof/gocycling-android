package com.fraczekkrzysztof.gocycling.model;

import android.util.Log;

import com.fraczekkrzysztof.gocycling.utils.DateUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.content.ContentValues.TAG;

public class EventModel implements Serializable {
    private static final String TAG = "EventModel";
    private String name;
    private String place;
    private double latitude;
    private double longitude;
    private Date dateAndTime;
    private String details;
    private String createdBy;
    private boolean canceled;
    private long id;
    private String routeLink;

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

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getRouteLink() {
        return routeLink;
    }

    public void setRouteLink(String routeLink) {
        this.routeLink = routeLink;
    }

    public static List<EventModel> fromJson(JSONObject jsonObject) {
        Log.d(TAG, "fromJson: parsing response for receiving list of events");
        List<EventModel> eventList = new ArrayList<>();
        try{
            JSONArray eventArray = jsonObject.getJSONObject("_embedded").getJSONArray("events");
            for (int i = 0; i < eventArray.length(); i++) {
                JSONObject eventObject = eventArray.getJSONObject(i);
                EventModel eventModel = new EventModel();
                eventModel.setId(eventObject.getLong("id"));
                eventModel.setName(eventObject.getString("name"));
                eventModel.setPlace(eventObject.getString("place"));
                eventModel.setLatitude(eventObject.getDouble("latitude"));
                eventModel.setLongitude(eventObject.getDouble("longitude"));
                eventModel.setDetails(eventObject.getString("details"));
                eventModel.setCreatedBy(eventObject.getString("createdBy"));
                eventModel.setCanceled(eventObject.getBoolean("canceled"));
                eventModel.setDateAndTime(DateUtils.sdfWithFullTime.parse(eventObject.getString("dateAndTime")));
                eventModel.setRouteLink(eventObject.getString("routeLink"));
                eventList.add(eventModel);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return eventList;

    }

    public static int getTotalPageFromJson(JSONObject jsonObject){
        int totalPage = 0;
        try{
            totalPage = jsonObject.getJSONObject("page").getInt("totalPages");

        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "fromJson: total number of pages " + totalPage );
        return totalPage;
    }
}
