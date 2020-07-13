package com.fraczekkrzysztof.gocycling.logging;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.event.EventListActivity;
import com.fraczekkrzysztof.gocycling.model.UserModel;
import com.fraczekkrzysztof.gocycling.worker.NotificationChecker;
import com.fraczekkrzysztof.gocycling.worker.NotificationChecker2;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpStatus;
import cz.msebera.android.httpclient.entity.StringEntity;

public class LoggingActivity extends AppCompatActivity {


    private static final String TAG = "LoggingActivity";
    private static final String SHARED_PREFERENCES_STRING = "LoggingPref";
    private static final String SHARED_PREFERENCES_LOGGED_USER_STRING = "LoggedUserId";
    private static final int RC_SIGN_IN = 1232;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null){
            Log.d(TAG, "onCreate: user already logged in. Redirect to EventListActivity");
            checkThatUserExistsOrCreate(user);
            startApp();
        } else {
            Log.d(TAG, "onCreate: user not logged. Start logging.");
            startLoggingIn();
        }

    }

    private void createPeriodicWorkedForNotificationCheck(){
        PeriodicWorkRequest pwr = new PeriodicWorkRequest.Builder(
                NotificationChecker.class,15, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(LoggingActivity.this).enqueue(pwr);

        Intent serviceIntent = new Intent(this, NotificationChecker2.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void startLoggingIn(){
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setTheme(R.style.FirebaseUI)
                        .setLogo(R.drawable.chain)
                        .setIsSmartLockEnabled(false)
                        .build(),
                RC_SIGN_IN);
    }

    private void startApp(){
        createPeriodicWorkedForNotificationCheck();
        Intent startIntent =  new Intent(getApplicationContext(), EventListActivity.class);
        startActivity(startIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN){
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (response == null ){ //operation canceled by the user
                return;
            }
            if (resultCode ==RESULT_OK){

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                checkThatUserExistsOrCreate(user);
                Log.d(TAG, "onActivityResult: user successfully logged in " + user.getUid());
                Toast.makeText(getApplicationContext(),"Successfully logged in!",Toast.LENGTH_SHORT).show();
                startApp();
            } else {
                Log.d(TAG, "onActivityResult: " + response.getError().getMessage());
                Toast.makeText(getApplicationContext(),"Error during logging in!",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkThatUserExistsOrCreate(final FirebaseUser user){
        try{
            AsyncHttpClient client = new AsyncHttpClient();
            client.setBasicAuth(getResources().getString(R.string.api_user), getResources().getString(R.string.api_password));
            String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_users);
            requestAddress = requestAddress + "/" + user.getUid();
            Log.d(TAG, "checkThatUserExistsOrCreate: request addres");
            client.get(requestAddress, new JsonHttpResponseHandler(){
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    Log.d(TAG, "onSuccess: Successfully return user list");
                    super.onSuccess(statusCode, headers, response);
                    UserModel userModel = UserModel.fromJsonUser(response,true);
                    if (userModel.getId().equals(user.getUid())){
                        Log.d(TAG, "onSuccess: user already exists in database");
                        return;
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    if (statusCode == HttpStatus.SC_NOT_FOUND){
                        Log.d(TAG, "onFailure: user don't exist. Start saving into db");
                        createUser(user);
                    } else {
                        Log.e(TAG, "onFailure: error during checking, that user exists in db",throwable );
                        super.onFailure(statusCode, headers, responseString, throwable);    
                    }
                    
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "checkThatUserExistsOrCreate: error during checking that user exists in database", e);
        }
    }
    private void createUser(FirebaseUser user){
        try{
            AsyncHttpClient client = new AsyncHttpClient();
            client.setBasicAuth(getResources().getString(R.string.api_user), getResources().getString(R.string.api_password));
            String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_users);
            JSONObject params = new JSONObject();
            params.put("id", user.getUid());
            params.put("name", user.getDisplayName());
            Log.d(TAG, "onClick: " + params.toString());
            StringEntity stringParams = new StringEntity(params.toString(),"UTF-8");
            client.post(getBaseContext(), requestAddress, stringParams, "application/json;charset=UTF-8", new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    Log.d(TAG, "onSuccess: successfuly saved user in db");
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    Log.e(TAG, "onFailure: error during saving user to db", error);
                }
            });
        } catch (Exception e){
            Log.e(TAG, "createUser: error during creating user", e);
        }
    }
}
