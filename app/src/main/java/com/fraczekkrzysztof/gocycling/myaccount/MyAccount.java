package com.fraczekkrzysztof.gocycling.myaccount;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.model.UserModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpStatus;
import cz.msebera.android.httpclient.entity.StringEntity;

public class MyAccount extends AppCompatActivity {

    private static final String TAG = "MyAccount";
    private EditText mEditTextUserName;
    private Button mUpdateButton;
    private Button mStravaButton;
    private boolean isStravaConnected = false;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_account);
        mSwipeRefreshLayout = findViewById(R.id.my_account_swipe_refresh);
        mEditTextUserName = findViewById(R.id.myaccount_name);
        mUpdateButton = findViewById(R.id.my_account_update_button);
        mUpdateButton.setOnClickListener(updateButtonListener);
        mStravaButton = findViewById(R.id.my_account_strava_button);
        mStravaButton.setOnClickListener(stravaButtonClickedListener);
        getSupportActionBar().setSubtitle("My account");


    }

    SwipeRefreshLayout.OnRefreshListener mOnRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            refreshData();
        }
    };
    private void getUserInfo(final FirebaseUser user){
        try{
            AsyncHttpClient client = new AsyncHttpClient();
            client.setBasicAuth(getResources().getString(R.string.api_user), getResources().getString(R.string.api_password));
            String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_users);
            requestAddress = requestAddress + "/" + user.getUid();
            Log.d(TAG, "getUserInfo: request address " + requestAddress);
            client.get(requestAddress, new JsonHttpResponseHandler(){
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    Log.d(TAG, "onSuccess: Successfully return user list");
                    super.onSuccess(statusCode, headers, response);
                    UserModel userModel = UserModel.fromJsonUser(response,true);
                    if (userModel.getId().equals(user.getUid())){
                        setUserInformation(userModel);
                    }

                }
                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        Log.e(TAG, "onFailure: error during checking, that user exists in db",throwable );
                        super.onFailure(statusCode, headers, responseString, throwable);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "getUserInfo: error during checking that user exists in database", e);
        }
    }
    private void setUserInformation(UserModel user){
        mEditTextUserName.setText(user.getName());
    }

    private View.OnClickListener updateButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            InputMethodManager imm = (InputMethodManager)view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            updateUserData(FirebaseAuth.getInstance().getCurrentUser(),mEditTextUserName.getText().toString());
        }
    };

    private void getUserExternalApps(final FirebaseUser user){
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(getResources().getString(R.string.api_user), getResources().getString(R.string.api_password));
        String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_external_app_list);
        requestAddress = requestAddress + "?userUid=" + user.getUid();
        Log.d(TAG, "getUserExternalApps: created request: " + requestAddress);
        client.get(requestAddress,new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.d(TAG, "onSuccess: Successfully get response about user external apps");
                super.onSuccess(statusCode, headers, response);
                List<String> listOfApps = new ArrayList<>();
                for (int i = 0; i < response.length(); i++){
                    try {
                        listOfApps.add(response.getString(i));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    processUserExternalApps(listOfApps);
                }
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
//                super.onFailure(statusCode, headers, responseString, throwable);
                if(statusCode != 404){
                    Toast.makeText(MyAccount.this,"Error during checking user external apps",Toast.LENGTH_SHORT).show();
                    if (responseString!= null){
                        Log.e(TAG, "onFailure: " + responseString);
                    }
                    Log.e(TAG, "onFailure: Error during checking user external apps", throwable);
                }
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void processUserExternalApps(List<String> userExternalApps){
        if (userExternalApps.contains("STRAVA")){
            setStravaButton(true);
        }
    }

    private void setStravaButton(boolean isStrava){
        if (isStrava){
            isStravaConnected = true;
            mStravaButton.setBackgroundColor(getResources().getColor(R.color.secondaryDarkColor));
            mStravaButton.setText("DEAUTHORIZE STRAVA");
        } else {
            isStravaConnected = false;
            Button defbtn=new Button(this);
            mStravaButton.setBackgroundColor(getResources().getColor(R.color.primaryLightColor));
            mStravaButton.setText("CONNECT WITH STRAVA");
        }
    }
    private void updateUserData(final FirebaseUser user, final String name){
        try{
            mSwipeRefreshLayout.setRefreshing(true);
            AsyncHttpClient client = new AsyncHttpClient();
            client.setBasicAuth(getResources().getString(R.string.api_user), getResources().getString(R.string.api_password));
            String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_users);
            requestAddress = requestAddress + "/" + user.getUid();
            JSONObject params = new JSONObject();
            params.put("name", name);
            StringEntity stringParams = new StringEntity(params.toString());
            Log.d(TAG, "updateUserData: request address " + requestAddress);
            client.put(getBaseContext(), requestAddress, stringParams, "application/json", new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    Log.d(TAG, "onSuccess: Successfully updated user info in local db");
                    updateUserInFirebase(user,name);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Log.e(TAG, "onFailure: Error during updating user info",error);
                    Toast.makeText(getBaseContext(),"Error during updating information!",Toast.LENGTH_SHORT).show();
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            });

        } catch (Exception e){
            Log.e(TAG, "updateUserData: Error during updating user data!",e);
        }
    }

    private void refreshData(){
        mSwipeRefreshLayout.setRefreshing(true);
        getUserInfo(FirebaseAuth.getInstance().getCurrentUser());
        getUserExternalApps(FirebaseAuth.getInstance().getCurrentUser());
    }

    private void updateUserInFirebase(FirebaseUser user, String name){
        mSwipeRefreshLayout.setRefreshing(true);
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: User profile updated.");
                            Toast.makeText(getBaseContext(),"Successfully updated information!",Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "onComplete: Error during updating user profile. " + task.getException());
                        }
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    View.OnClickListener stravaButtonClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (!isStravaConnected){
                getStravaAuthorizationLink();
            } else {
                deauthorizeStrava();
            }
        }
    };

    private void getStravaAuthorizationLink(){
        mSwipeRefreshLayout.setRefreshing(true);
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(getResources().getString(R.string.api_user), getResources().getString(R.string.api_password));
        String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_strava_authorization_link);
        Log.d(TAG, "getStravaAuthorizationLink: created request: " + requestAddress);
        client.get(requestAddress, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Log.e(TAG, "onFailure: Error during get authorization link",throwable);
                if (responseString != null){
                    Log.e(TAG, "onFailure: " + responseString);
                }
                Toast.makeText(getBaseContext(),"Error during get authorization link!",Toast.LENGTH_SHORT).show();
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                Intent stravaIntent = new Intent(Intent.ACTION_VIEW,Uri.parse(responseString));
                startActivity(stravaIntent);
            }
        });
    }

    private void processAccessCode(Uri uri){
        try {
            String code = uri.getQueryParameter("code");
            if (code != null){
                Toast.makeText(MyAccount.this,"Access code "+ code,Toast.LENGTH_LONG).show();
                AsyncHttpClient client = new AsyncHttpClient();
                client.setBasicAuth(getResources().getString(R.string.api_user), getResources().getString(R.string.api_password));
                String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_strava_process_access_code);
                Log.d(TAG, "processAccessCode: created request: " + requestAddress);
                JSONObject params = new JSONObject();
                params.put("userUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                params.put("accessToken",code);
                StringEntity stringParams = new StringEntity(params.toString());
                client.post(getBaseContext(), requestAddress, null, stringParams, "application/json", new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        Log.d(TAG, "onSuccess: Successfully connect strava");
                        Toast.makeText(getApplicationContext(),"Successfully connected with Strava!",Toast.LENGTH_SHORT).show();
                        setStravaButton(true);
                        mSwipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        Log.e(TAG, "onFailure: Error during authorizing strava!", error);
                        String errorRespone = new String(responseBody);
                        Toast.makeText(MyAccount.this,"Error during connecting with Strava!",Toast.LENGTH_SHORT).show();
                        if (errorRespone != null && errorRespone != ""){
                            Log.e(TAG, "onFailure: " + errorRespone);
//                            Toast.makeText(MyAccount.this,errorRespone,Toast.LENGTH_SHORT).show();
                        }
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "processAccessCode: error during proccess access code!",e );
        }
    }

    private void deauthorizeStrava(){
        try {
            mSwipeRefreshLayout.setRefreshing(true);
            AsyncHttpClient client = new AsyncHttpClient();
            client.setBasicAuth(getResources().getString(R.string.api_user), getResources().getString(R.string.api_password));
            String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_strava_deauthorize);
            requestAddress = requestAddress + "?userUid=" + FirebaseAuth.getInstance().getCurrentUser().getUid();
            Log.d(TAG, "deauthorizeStrava: created request: " + requestAddress);
            JSONObject params = new JSONObject();
            params.put("userUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
            StringEntity stringParams = new StringEntity(params.toString());
            client.post(getBaseContext(), requestAddress, null, stringParams, "application/json", new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    Log.d(TAG, "onSuccess: Successfully deauthorize strava");
                    Toast.makeText(getApplicationContext(),"Successfully deauthorize strava",Toast.LENGTH_SHORT).show();
                    setStravaButton(false);
                    mSwipeRefreshLayout.setRefreshing(false);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Log.e(TAG, "onFailure: Error during deauthorizing strava!", error);
                    String errorRespone = new String(responseBody);
                    if (errorRespone != null && errorRespone != ""){
                        Log.e(TAG, "onFailure: " + errorRespone);
                    }
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "deauthorizeStrava: Error during deauthorizing strava",e );
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Uri uri = getIntent().getData();
        if (uri != null){
            if (uri.toString().startsWith(getResources().getString(R.string.redirect_uri_strava))){
                afterReturnFromSatrava(uri);
                return;
            }
        }
        refreshData();
    }

    private void afterReturnFromSatrava(Uri uri){
        mSwipeRefreshLayout.setRefreshing(true);
        getUserInfo(FirebaseAuth.getInstance().getCurrentUser());
        processAccessCode(uri);
    }
}
