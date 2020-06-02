package com.fraczekkrzysztof.gocycling.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ClubModel {

    private static final String TAG = "ClubModel";
    private long id;
    private String name;
    private String location;
    private double latitude;
    private double longitude;
    private String owner;
    private String details;
    private boolean privateMode;

    public ClubModel() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public boolean isPrivateMode() {
        return privateMode;
    }

    public void setPrivateMode(boolean privateMode) {
        this.privateMode = privateMode;
    }

    public static List<ClubModel> fromJson(JSONObject jsonObject){
        Log.d(TAG, "fromJson: parsing response to receiving list of ClubModel");
        List<ClubModel> listOfClubs= new ArrayList<>();
        try{
            JSONArray clubArray = jsonObject.getJSONObject("_embedded").getJSONArray("clubs");
            for (int i = 0; i < clubArray.length(); i++) {
                JSONObject confirmationObject = clubArray.getJSONObject(i);
                ClubModel clubModel = new ClubModel();
                clubModel.setId(confirmationObject.getLong("id"));
                clubModel.setName(confirmationObject.getString("name"));
                clubModel.setLocation(confirmationObject.getString("location"));
                clubModel.setLatitude(confirmationObject.getDouble("latitude"));
                clubModel.setLongitude(confirmationObject.getDouble("longitude"));
                clubModel.setOwner(confirmationObject.getString("owner"));
                clubModel.setDetails(confirmationObject.getString("details"));
                clubModel.setPrivateMode(confirmationObject.getBoolean("privateMode"));
                listOfClubs.add(clubModel);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return listOfClubs;
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
