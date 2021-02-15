package com.fraczekkrzysztof.gocycling.clubdetails;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fraczekkrzysztof.gocycling.MapsActivity;
import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.clubs.ClubListActivity;
import com.fraczekkrzysztof.gocycling.httpclient.GoCyclingHttpClientHelper;
import com.fraczekkrzysztof.gocycling.model.LocationDto;
import com.fraczekkrzysztof.gocycling.model.v2.club.ClubDto;
import com.fraczekkrzysztof.gocycling.model.v2.club.ClubResponse;
import com.fraczekkrzysztof.gocycling.model.v2.club.MemberDto;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.stream.Collectors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ClubDetailActivity extends AppCompatActivity {


    private final Gson gson = new Gson();
    private static final String TAG = "ClubDetailActivity";
    boolean isMember;
    TextView mOwner;
    TextView mName;
    TextView mLocation;
    TextView mDetails;
    long clubId;
    ClubDto mClubDto;
    Button mJoinButton;
    ImageButton mLocationButton;
    ListView mListView;
    SwipeRefreshLayout mSwipeRefreshLayout;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: started!");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_club_details);
        mName = findViewById(R.id.club_detail_name);
        mOwner = findViewById(R.id.club_detail_owner);
        mLocation = findViewById(R.id.club_detail_location);
        mDetails = findViewById(R.id.club_detail_details);
        mListView = findViewById(R.id.club_details_members);
        mJoinButton = findViewById(R.id.club_join_button);
        mJoinButton.setOnClickListener(joinedButtonClickedListener);
        mLocationButton = findViewById(R.id.club_detail_show_location);
        mLocationButton.setOnClickListener(showLocationButtonListener);
        mSwipeRefreshLayout = findViewById(R.id.club_detail_swipe_layout);
        mSwipeRefreshLayout.setOnRefreshListener(onRefresListener);
        getSupportActionBar().setSubtitle("Club details");
        clubId = getIntent().getLongExtra("clubId", -1);
    }

    private void getClubDetails() {
        mSwipeRefreshLayout.setRefreshing(true);
        String url = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_clubs) + "/" + clubId;
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        Request request = new Request.Builder()
                .url(urlBuilder.build().toString())
                .build();
        OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(getResources());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Toast.makeText(ClubDetailActivity.this, "There is an error. Please try again!", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error during retrieving club list", e);
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "onResponse: Successfully received response with ClubList");
                    Reader receivedResponse = response.body().charStream();
                    ClubResponse apiResponse = gson.fromJson(receivedResponse, ClubResponse.class);
                    runOnUiThread(() -> {
                        mClubDto = apiResponse.getClub();
                        setFields(apiResponse.getClub());
                        mSwipeRefreshLayout.setRefreshing(false);
                        return;
                    });
                }
                mSwipeRefreshLayout.setRefreshing(false);
                Log.e(TAG, String.format("onResponse: Response received, but not %s status.", response.code()));
            }
        });
    }

    private void setFields(ClubDto club) {
        mName.setText(club.getName());
        mLocation.setText(club.getLocation());
        mDetails.setText(club.getDetails());
        mOwner.setText(club.getOwnerName());
        setUserMembership(club.getMemberList());
        setMembers(club.getMemberList());
    }

    private void setUserMembership(List<MemberDto> memberList) {
        setJoinButton(false);
        if (isCurrentUserMember(memberList)) {
            setJoinButton(true);
        }
    }

    private boolean isCurrentUserMember(List<MemberDto> memberList) {
        return memberList.stream()
                .anyMatch(m -> m.getUserUid().equals(FirebaseAuth.getInstance().getCurrentUser().getUid()));
    }

    private void setMembers(List<MemberDto> memberList) {
        setArrayAdapterToMemberListView(memberList.stream().map(MemberDto::getUserName).collect(Collectors.toList()));
    }

    private void refreshData() {
        getClubDetails();

    }

    private SwipeRefreshLayout.OnRefreshListener onRefresListener = () -> {
        Log.d(TAG, "onRefresh: refreshing");
        refreshData();
    };

    private void setJoinButton(boolean isConfirmed) {
        if (isConfirmed) {
            isMember = true;
            setJoinButtonToJoined();
        } else {
            isMember = false;
            setJoinButtonToNotJoined();
        }
    }

    private void setJoinButtonToJoined() {
        mJoinButton.setText("LEAVE");
        mJoinButton.setBackgroundColor(getResources().getColor(R.color.secondaryDarkColor));
    }

    private void setJoinButtonToNotJoined() {
        mJoinButton.setText("JOIN");
        mJoinButton.setBackgroundColor(getResources().getColor(R.color.primaryDarkColor));

    }

    private View.OnClickListener showLocationButtonListener = view -> {
        Intent showLocation = new Intent(ClubDetailActivity.this, MapsActivity.class);
        showLocation.putExtra("location", LocationDto.builder()
                .latitude(mClubDto.getLatitude())
                .longitude(mClubDto.getLongitude())
                .description(mClubDto.getLocation())
                .build());
        startActivity(showLocation);
    };

    private View.OnClickListener joinedButtonClickedListener = view -> manageUserMembership();

    private void manageUserMembership() {
        mSwipeRefreshLayout.setRefreshing(true);
        Request request = prepareRequest();
        OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(getResources());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "onFailure: error during joining", e);
                mSwipeRefreshLayout.setRefreshing(false);
                backgroundThreadShortToast(getApplicationContext(), "Operation finished with error!");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    refreshData();
                    mSwipeRefreshLayout.setRefreshing(false);
                    backgroundThreadShortToast(getApplicationContext(), "Operation finished successfully!");
                    return;
                }
                Log.e(TAG, String.format("onResponse: response received but status %d. %s", response.code(), response.body().string()));
                backgroundThreadShortToast(getApplicationContext(), "Operation finished with error!");
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });

    }

    @NotNull
    private Request prepareRequest() {
        String baseRequestAddress = String.format(getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_members), clubId);
        HttpUrl url = HttpUrl.parse(baseRequestAddress).newBuilder()
                .addQueryParameter("userUid", FirebaseAuth.getInstance().getCurrentUser().getUid()).build();
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .method("POST", RequestBody.create("", null));
        if (isMember) {
            requestBuilder.method("DELETE", null);
        }
        return requestBuilder.build();
    }

    private void setArrayAdapterToMemberListView(List<String> memberList) {
        ArrayAdapter arrayAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, memberList) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(Color.BLACK);
                text.setTextSize(Float.parseFloat("15"));
                return view;
            }
        };
        mListView.setAdapter(arrayAdapter);
    }


    @Override
    protected void onPostResume() {
        super.onPostResume();
        refreshData();
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(ClubDetailActivity.this, ClubListActivity.class));
    }

    public static void backgroundThreadShortToast(final Context context,
                                                  final String msg) {
        if (context != null && msg != null) {
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show());
        }
    }
}
