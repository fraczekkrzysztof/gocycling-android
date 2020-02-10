package com.fraczekkrzysztof.gocycling.logging;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.event.EventListActivity;
import com.fraczekkrzysztof.gocycling.model.UserModel;
import com.fraczekkrzysztof.gocycling.utils.DateUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

import org.json.JSONObject;

public class LoggingActivity extends AppCompatActivity {


    private static final String TAG = "LoggingActivity";
    private static final String sharedPreferencesString = "LoggingPref";
    private static final String sharedPreferencesLoggedUserString = "LoggedUserId";
    private static final int RC_SIGN_IN = 1232;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null){
            Log.d(TAG, "onCreate: user already logged in. Redirect to EventListActivity");
            startApp();
        } else {
            Log.d(TAG, "onCreate: user not logged. Start logging.");
            startLoggingIn();
        }
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
            client.get(requestAddress, new JsonHttpResponseHandler(){
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    Log.d(TAG, "onSuccess: Successfully return user list");
                    super.onSuccess(statusCode, headers, response);
                    List<UserModel> userLists = UserModel.fromJson(response);
                    for (UserModel userModel : userLists){
                        if (userModel.getId().equals(user.getUid())) {
                            Log.d(TAG, "onSuccess: user already exists in database");
                           return;
                        }
                    }
                    createUser(user);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    Log.e(TAG, "onFailure: error during checking, that user exists in db",throwable );
                    super.onFailure(statusCode, headers, responseString, throwable);
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
            StringEntity stringParams = new StringEntity(params.toString());
            client.post(getBaseContext(), requestAddress, stringParams, "application/json", new AsyncHttpResponseHandler() {
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
