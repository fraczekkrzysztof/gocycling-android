package com.fraczekkrzysztof.gocycling.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RouteModel {

    private static final String TAG = "RouteModel";
    private String appType;
    private String link;
    private String name;
    private String length;
    private String elevation;
    private long created;

    public String getAppType() {
        return appType;
    }

    public void setAppType(String appType) {
        this.appType = appType;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }

    public String getElevation() {
        return elevation;
    }

    public void setElevation(String elevation) {
        this.elevation = elevation;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public static List<RouteModel> fromJsonArray(JSONArray response){
        Log.d(TAG, "fromJsonArray: start parsing");
        List<RouteModel> toReturn = new ArrayList<>();
        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject routeObject = response.getJSONObject(i);
                RouteModel routeModel = new RouteModel();
                routeModel.setAppType(routeObject.getString("appType"));
                routeModel.setLink(routeObject.getString("link"));
                routeModel.setName(routeObject.getString("name"));
                routeModel.setLength(routeObject.getString("length"));
                routeModel.setElevation(routeObject.getString("elevation"));
                routeModel.setCreated(routeObject.getLong("created"));
                toReturn.add(routeModel);
            }
        } catch (JSONException e) {
            Log.e(TAG, "fromJsonArray: Error during parsing response", e);
        }
        return toReturn;
    }
}
