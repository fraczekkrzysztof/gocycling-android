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
import com.fraczekkrzysztof.gocycling.httpclient.GoCyclingHttpClientHelper;
import com.fraczekkrzysztof.gocycling.model.v2.user.UserDto;
import com.fraczekkrzysztof.gocycling.utils.ToastUtils;
import com.fraczekkrzysztof.gocycling.worker.NotificationChecker;
import com.fraczekkrzysztof.gocycling.worker.NotificationChecker2;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoggingActivity extends AppCompatActivity {

    private final Gson gson = new Gson();
    private static final String TAG = "LoggingActivity";
    private static final int RC_SIGN_IN = 1232;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null){
            Log.d(TAG, "onCreate: user already logged in. Redirect to EventListActivity");
            createUser(user);
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
                createUser(user);
                Log.d(TAG, "onActivityResult: user successfully logged in " + user.getUid());
                Toast.makeText(getApplicationContext(),"Successfully logged in!",Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "onActivityResult: " + response.getError().getMessage());
                Toast.makeText(getApplicationContext(),"Error during logging in!",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createUser(FirebaseUser user){
        Request request = prepareRequest(user);
        OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(getResources());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "createUser onFailure: error during creating user", e);
                ToastUtils.backgroundThreadShortToast(LoggingActivity.this, "Error during storing user in application");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "createUser onResponse: user successfully created");
                } else if (409 == response.code()) {
                    Log.d(TAG, "createUser onResponse: user already exists in application database");
                }
                startApp();
            }
        });
    }

    private Request prepareRequest(FirebaseUser user) {
        String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_users);
        UserDto userDto = UserDto.builder().id(user.getUid()).name(user.getDisplayName()).build();
        return new Request.Builder()
                .url(requestAddress)
                .post(RequestBody.create(gson.toJson(userDto), MediaType.parse("application/json;charset=UTF-8")))
                .build();
    }
}
