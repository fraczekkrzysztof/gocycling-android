package com.fraczekkrzysztof.gocycling.model;

import android.util.Log;

import com.fraczekkrzysztof.gocycling.utils.DateUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ConfirmationModel {
    private static final String TAG = "ConfirmationModel";
    private long id;
    private String userUid;

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


    public static List<ConfirmationModel> fromJson(JSONObject jsonObject){
        Log.d(TAG, "fromJson: parsing response to receiving list of Confirmaion");
        List<ConfirmationModel> listOfConfirmations= new ArrayList<>();
        try{
            JSONArray eventArray = jsonObject.getJSONObject("_embedded").getJSONArray("confirmations");
            for (int i = 0; i < eventArray.length(); i++) {
                JSONObject confirmationObject = eventArray.getJSONObject(i);
                ConfirmationModel confirmationModel = new ConfirmationModel();
                confirmationModel.setId(confirmationObject.getLong("id"));
                confirmationModel.setUserUid(confirmationObject.getString("userUid"));
                listOfConfirmations.add(confirmationModel);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return listOfConfirmations;
    }
}
