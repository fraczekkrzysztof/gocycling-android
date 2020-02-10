package com.fraczekkrzysztof.gocycling.model;

import android.util.Log;

import com.firebase.ui.auth.data.model.User;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UserModel {
    private static final String TAG = "UserModel";
    private String id;
    private String name;

    public UserModel(){

    }

    public UserModel(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static List<UserModel> fromJson(JSONObject jsonObject) {
        Log.d(TAG, "fromJson: parsing response for receiving list of events");
        List<UserModel> userList = new ArrayList<>();
        try{
            JSONArray userArray= jsonObject.getJSONObject("_embedded").getJSONArray("users");
            for (int i = 0; i < userArray.length(); i++) {
                JSONObject userObject = userArray.getJSONObject(i);
                UserModel userModel = new UserModel();
                userModel.setId(userObject.getString("id"));
                userModel.setName(userObject.getString("name"));
                userList.add(userModel);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return userList;

    }
}
