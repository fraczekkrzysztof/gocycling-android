package com.fraczekkrzysztof.gocycling.myaccount;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.httpclient.GoCyclingHttpClientHelper;
import com.fraczekkrzysztof.gocycling.model.v2.route.ExternalApps;
import com.fraczekkrzysztof.gocycling.model.v2.strava.AccessTokenRequestDto;
import com.fraczekkrzysztof.gocycling.model.v2.user.UserDto;
import com.fraczekkrzysztof.gocycling.model.v2.user.UserResponseDto;
import com.fraczekkrzysztof.gocycling.utils.ToastUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MyAccount extends AppCompatActivity {

    private static final String TAG = "MyAccount";
    private final Gson gson = new Gson();
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
        mSwipeRefreshLayout.setOnRefreshListener(mOnRefreshListener);
        mEditTextUserName = findViewById(R.id.myaccount_name);
        mUpdateButton = findViewById(R.id.my_account_update_button);
        mUpdateButton.setOnClickListener(updateButtonListener);
        mStravaButton = findViewById(R.id.my_account_strava_button);
        mStravaButton.setOnClickListener(stravaButtonClickedListener);
        getSupportActionBar().setSubtitle("My account");


    }

    SwipeRefreshLayout.OnRefreshListener mOnRefreshListener = this::refreshData;

    private void refreshData() {
        getUserInfo();
    }

    private void getUserInfo() {
        mSwipeRefreshLayout.setRefreshing(true);
        Request request = prepareGetUserRequest();
        OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(getResources());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "getUserInfo onFailure: error during getting user data", e);
                ToastUtils.backgroundThreadShortToast(MyAccount.this, "Error occurred, please try again later!");
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "getUserInfo onResponse: successfully received response");
                    UserResponseDto apiResponse = gson.fromJson(response.body().charStream(), UserResponseDto.class);
                    runOnUiThread(() -> {
                        setUserInformation(apiResponse.getUser());
                        mSwipeRefreshLayout.setRefreshing(false);
                    });
                    return;
                }
                Log.w(TAG, String.format("getUserInfo onResponse: successfully received response but %d status", response.code()));
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private Request prepareGetUserRequest() {
        String requestAddress = getResources().getString(R.string.api_base_address) +
                String.format(getResources().getString(R.string.api_user_details), FirebaseAuth.getInstance().getCurrentUser().getUid());
        return new Request.Builder()
                .url(requestAddress)
                .build();
    }

    private void setUserInformation(UserDto user) {
        mEditTextUserName.setText(user.getName());
        processUserExternalApps(user.getExternalApps());
    }

    private void processUserExternalApps(List<ExternalApps> userExternalApps) {
        if (userExternalApps == null) {
            setStravaButton(false);
            return;
        }
        if (userExternalApps.stream().anyMatch(e -> e.equals(ExternalApps.STRAVA))) {
            setStravaButton(true);
        }
    }

    private void setStravaButton(boolean isStrava) {
        if (isStrava) {
            isStravaConnected = true;
            mStravaButton.setBackgroundColor(getResources().getColor(R.color.secondaryDarkColor));
            mStravaButton.setText("DEAUTHORIZE STRAVA");
        } else {
            isStravaConnected = false;
            mStravaButton.setBackgroundColor(getResources().getColor(R.color.primaryLightColor));
            mStravaButton.setText("CONNECT WITH STRAVA");
        }
    }

    private View.OnClickListener updateButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            updateUserData(mEditTextUserName.getText().toString());
        }
    };

    private void updateUserData(final String name) {
        mSwipeRefreshLayout.setRefreshing(true);
        Request request = prepareUpdateUserRequest(name);
        OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(getResources());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "updateUserDate onFailure: error during updating user data", e);
                ToastUtils.backgroundThreadShortToast(MyAccount.this, "Error occured, try again later!");
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "updateUserData onResponse: successfully received response");
                    UserResponseDto apiResponse = gson.fromJson(response.body().charStream(), UserResponseDto.class);
                    runOnUiThread(() -> {
                        setUserInformation(apiResponse.getUser());
                        updateUserInFirebase(name);
                    });
                    return;
                }
                Log.w(TAG, String.format("updateUserData onResponse: successfully received response but %d status", response.code()));
                mSwipeRefreshLayout.setRefreshing(false);

            }
        });
    }

    private Request prepareUpdateUserRequest(String name) {
        String requestAddress = getResources().getString(R.string.api_base_address) +
                String.format(getResources().getString(R.string.api_user_details), FirebaseAuth.getInstance().getCurrentUser().getUid());
        return new Request.Builder()
                .url(requestAddress)
                .patch(RequestBody.create(gson.toJson(UserDto.builder().name(name).build()), MediaType.parse("application/json;charset=UTF-8")))
                .build();
    }

    private void updateUserInFirebase(String name) {
        mSwipeRefreshLayout.setRefreshing(true);
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();
        FirebaseAuth.getInstance().getCurrentUser().updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "onComplete: User profile updated.");
                        runOnUiThread(() -> {
                            ToastUtils.backgroundThreadShortToast(MyAccount.this, "Successfully updated information!");
                            mSwipeRefreshLayout.setRefreshing(false);
                        });
                        return;
                    }
                    Log.d(TAG, "onComplete: Error during updating user profile. " + task.getException());
                    mSwipeRefreshLayout.setRefreshing(false);
                });
    }

    View.OnClickListener stravaButtonClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (!isStravaConnected) {
                getStravaAuthorizationLink();
            } else {
                deauthorizeStrava();
            }
        }
    };

    private void getStravaAuthorizationLink() {
        mSwipeRefreshLayout.setRefreshing(true);
        Request request = prepareStarvaAuthorizathionLinkRequest();
        OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(getResources());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "getStravaAuthorizationLink onFailure: error during retreiving Strava authorization link", e);
                ToastUtils.backgroundThreadShortToast(MyAccount.this, "Error occurred, try again later!");
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "getStravaAuthorizationLInk onResponse: successfully received response with strava authorization link");
                    String authorizationLink = response.body().string();
                    runOnUiThread(() -> {
                        Intent stravaIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(authorizationLink));
                        startActivity(stravaIntent);
                    });
                    return;
                }
                Log.d(TAG, String.format("getStravaAuthorizationLink onResponse: Received response but %d status.", response.code()));
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private Request prepareStarvaAuthorizathionLinkRequest() {
        String requestAddress = getResources().getString(R.string.api_base_address) +
                getResources().getString(R.string.api_strava_authorization_link);
        return new Request.Builder()
                .url(requestAddress)
                .build();
    }

    private void processAccessCode(Uri uri) {
        String code = uri.getQueryParameter("code");
        if (code != null) {
            mSwipeRefreshLayout.setRefreshing(true);
            Request request = prepareStravaProcessAccessCodeRequest(code);
            OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(getResources());
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Log.e(TAG, "processAccessCode onFailure: error during processing access code", e);
                    ToastUtils.backgroundThreadShortToast(MyAccount.this, "Error occurred. Try again later!");
                    mSwipeRefreshLayout.setRefreshing(false);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "processAccessCode onResponse: successfully received response with processed access code");
                        runOnUiThread(() -> {
                            ToastUtils.backgroundThreadShortToast(MyAccount.this, "Successfully connected Strava!");
                            setStravaButton(true);
                            mSwipeRefreshLayout.setRefreshing(false);
                        });
                        return;
                    }
                    Log.w(TAG, String.format("processAccessCOde onResponse: received response but %d status", response.code()));
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            });
        }
    }

    private Request prepareStravaProcessAccessCodeRequest(String code) {
        String requestAddress = getResources().getString(R.string.api_base_address) +
                getResources().getString(R.string.api_strava_process_access_code);
        AccessTokenRequestDto requestBody = new AccessTokenRequestDto(FirebaseAuth.getInstance().getCurrentUser().getUid(), code);
        return new Request.Builder()
                .url(requestAddress)
                .post(RequestBody.create(gson.toJson(requestBody), MediaType.parse("application/json;charset=UTF-8")))
                .build();

    }

    private void deauthorizeStrava() {
        mSwipeRefreshLayout.setRefreshing(true);
        Request request = prepareDeauthorizeStravaRequest();
        OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(getResources());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "deauthorizeStrava onFailure: error occurred during deauthorizing strava", e);
                ToastUtils.backgroundThreadShortToast(MyAccount.this, "Error occurred during deathorizing Strava. Try again later!");
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "deauthorizeStrava onResponse: Successfully received response with Strava deauthorizing");
                    runOnUiThread(() -> {
                        ToastUtils.backgroundThreadShortToast(MyAccount.this, "Successfully deauthorize Strava");
                        setStravaButton(false);
                        mSwipeRefreshLayout.setRefreshing(false);
                    });
                    return;
                }
                Log.w(TAG, String.format("deauthorizeStrava onResponse: received reponse with deauthorizing Strava but %d status.", response.code()));
                ToastUtils.backgroundThreadShortToast(MyAccount.this, "Error occurred during deauthorizing Strava. Try again later!");
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    Request prepareDeauthorizeStravaRequest() {
        HttpUrl url = HttpUrl.parse(getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_strava_deauthorize)).newBuilder()
                .addQueryParameter("userUid", FirebaseAuth.getInstance().getCurrentUser().getUid())
                .build();
        return new Request.Builder()
                .url(url)
                .post(RequestBody.create("", MediaType.parse("application/json;charset=UTF-8")))
                .build();

    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        refreshData();
        Uri uri = getIntent().getData();
        if (uri != null && uri.toString().startsWith(getResources().getString(R.string.redirect_uri_strava))) {
            processAccessCode(uri);
        }
    }
}
