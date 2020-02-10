package com.fraczekkrzysztof.gocycling.myaccount;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.model.UserModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpStatus;

public class MyAccount extends AppCompatActivity {

    private static final String TAG = "MyAccount";
    private EditText mEditTextUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_account);
        mEditTextUserName = findViewById(R.id.myaccount_name);
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
                    UserModel userModel = UserModel.fromJsonUser(response);
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
}
