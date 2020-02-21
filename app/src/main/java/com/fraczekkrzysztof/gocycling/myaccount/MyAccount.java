package com.fraczekkrzysztof.gocycling.myaccount;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
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

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpStatus;
import cz.msebera.android.httpclient.entity.StringEntity;

public class MyAccount extends AppCompatActivity {

    private static final String TAG = "MyAccount";
    private EditText mEditTextUserName;
    private Button mUpdateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_account);
        mEditTextUserName = findViewById(R.id.myaccount_name);

        mUpdateButton = findViewById(R.id.my_account_update_button);
        mUpdateButton.setOnClickListener(updateButtonListener);
        getUserInfo(FirebaseAuth.getInstance().getCurrentUser());

    }

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

    private void updateUserData(final FirebaseUser user, final String name){
        try{
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
                }
            });

        } catch (Exception e){
            Log.e(TAG, "updateUserData: Error during updating user data!",e);
        }
    }

    private void updateUserInFirebase(FirebaseUser user, String name){
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
                    }
                });
    }
}
