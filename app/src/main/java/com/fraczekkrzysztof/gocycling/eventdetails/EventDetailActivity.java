package com.fraczekkrzysztof.gocycling.eventdetails;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
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
import com.fraczekkrzysztof.gocycling.conversation.ConversationListActivity;
import com.fraczekkrzysztof.gocycling.httpclient.GoCyclingHttpClientHelper;
import com.fraczekkrzysztof.gocycling.model.v2.event.ConfirmationDto;
import com.fraczekkrzysztof.gocycling.model.v2.event.EventDto;
import com.fraczekkrzysztof.gocycling.model.v2.event.EventResponseDto;
import com.fraczekkrzysztof.gocycling.utils.DateUtils;
import com.fraczekkrzysztof.gocycling.utils.ToastUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EventDetailActivity extends AppCompatActivity {

    private static final String TAG = "EventDetailActivity";

    private final Gson gson = new Gson();
    TextView mClub;
    TextView mWho;
    TextView mTitle;
    TextView mWhere;
    TextView mWhen;
    TextView mDetails;
    long eventId;
    long clubId;
    EventDto mEvent;
    Button mConfirmButton;
    ImageButton mLocationButton;
    ImageButton mConversationButton;
    ImageButton mRouteButton;
    ListView mListView;
    SwipeRefreshLayout mSwipeRefreshLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: started!");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_details);
        mClub = findViewById(R.id.event_detail_club);
        mTitle = findViewById(R.id.event_detail_title);
        mWho = findViewById(R.id.event_detail_who);
        mWhere = findViewById(R.id.event_detail_where);
        mWhen = findViewById(R.id.event_detail_when);
        mDetails = findViewById(R.id.event_detail_details);
        mListView = findViewById(R.id.list_of_users_confirmed);
        mConfirmButton = findViewById(R.id.event_confirm_button);
        mConfirmButton.setOnClickListener(confirmedButtonClickedListener);
        mConversationButton = findViewById(R.id.event_conversation);
        mConversationButton.setOnClickListener(conversationClickedListener);
        mLocationButton = findViewById(R.id.event_detail_show_location);
        mLocationButton.setOnClickListener(showLocationButtonListener);
        mRouteButton = findViewById(R.id.event_detail_show_route);
        mRouteButton.setOnClickListener(showRouteClickedListener);
        mSwipeRefreshLayout = findViewById(R.id.event_detail_swipe_layout);
        mSwipeRefreshLayout.setOnRefreshListener(onRefreshListener);
        getSupportActionBar().setSubtitle("Events details");
        clubId = getIntent().getLongExtra("clubId", -1);
        eventId = getIntent().getLongExtra("eventId", -1);
        getEvent(clubId, eventId);
    }

    private void setVisibleOfRouteButton(EventDto event) {
        if (event.getRouteLink() == null || event.getRouteLink().equals("")) {
            mRouteButton.setVisibility(View.INVISIBLE);
        }
    }

    private void getEvent(long clubId, long eventId) {
        mSwipeRefreshLayout.setRefreshing(true);
        Log.d(TAG, "getEvents: called");
        Request request = prepareEventRequest(clubId, eventId);
        OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(getResources());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                ToastUtils.backgroundThreadShortToast(EventDetailActivity.this, "There is an error. Please try again!");
                Log.e(TAG, "getEvent onFailure: error during retrieving Event", e);
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "onResponse: Successfully received response with Events");
                    EventResponseDto apiResponse = gson.fromJson(response.body().charStream(), EventResponseDto.class);
                    runOnUiThread(() -> {
                        mEvent = apiResponse.getEvent();
                        setFields(mEvent);
                        mSwipeRefreshLayout.setRefreshing(false);
                    });
                    return;
                }
                Log.w(TAG, String.format("onResponse: Response reseived but %d status", response.code()));
                ToastUtils.backgroundThreadShortToast(EventDetailActivity.this, "There is an error. Try again");
            }
        });
    }

    private Request prepareEventRequest(long clubId, long eventId) {
        String request = getResources().getString(R.string.api_base_address) +
                String.format(getResources().getString(R.string.api_event_address_event_id), clubId, eventId);
        HttpUrl.Builder urlBuilder = HttpUrl.parse(request).newBuilder();
        return new Request.Builder()
                .url(urlBuilder.build().toString())
                .build();
    }

    private SwipeRefreshLayout.OnRefreshListener onRefreshListener = () -> {
        Log.d(TAG, "onRefresh: refreshing");
        refreshData(clubId, eventId);
    };

    private void setConfirmationButton(boolean isConfirmed) {
        if (isConfirmed) {
            setConfirmedButtonToConfirmed();
        } else {
            setConfirmedButtonToNotConfirmed();
        }
    }

    private void setConfirmedButtonToConfirmed() {
        mConfirmButton.setText("CANCEL CONFIRMATION");
        mConfirmButton.setBackgroundColor(getResources().getColor(R.color.secondaryDarkColor));
    }

    private void setConfirmedButtonToNotConfirmed() {
        mConfirmButton.setText("CONFIRM");
        mConfirmButton.setBackgroundColor(getResources().getColor(R.color.primaryDarkColor));
    }

    private void setConfirmationButtonToInformAboutCanceledEvent() {
        mConfirmButton.setText("EVENT CANCELED");
        mConfirmButton.setBackgroundColor(getResources().getColor(R.color.secondaryDarkColor));
        mConfirmButton.setEnabled(false);
    }

    private void setFields(EventDto event) {
        try {
            mTitle.setText(event.getName());
            mWhen.setText(DateUtils.formatDefaultDateToDateWithTime(event.getDateAndTime()));
            mWhere.setText(event.getPlace());
            mDetails.setText(event.getDetails());
            mClub.setText(event.getClubName());
            mWho.setText(event.getUserName());
            if (event.isCanceled()) {
                setConfirmationButtonToInformAboutCanceledEvent();
                mConversationButton.setVisibility(View.INVISIBLE);
                return;
            }
            setConfirmationButton(event.getConfirmationList().stream()
                    .anyMatch(c -> c.getUserId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())));
            setArrayAdapterToListOfConfirmedUsers(event.getConfirmationList().stream()
                    .map(ConfirmationDto::getUserName)
                    .collect(Collectors.toList()));
            setVisibleOfRouteButton(event);
        } catch (ParseException e) {
            Log.e(TAG, "setFields: Error during parsing received data", e);
        }
    }


    private void setArrayAdapterToListOfConfirmedUsers(List<String> confirmedUsersNames) {
        ArrayAdapter arrayAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, confirmedUsersNames) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);
                text.setTextColor(Color.BLACK);
                text.setTextSize(Float.parseFloat("15"));
                return view;
            }
        };
        mListView.setAdapter(arrayAdapter);
    }

    private void refreshData(long clubId, long eventId) {
        getEvent(clubId, eventId);
    }

    private View.OnClickListener showLocationButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent showLocation = new Intent(EventDetailActivity.this, MapsActivity.class);
            showLocation.putExtra("Event", mEvent);
            startActivity(showLocation);
        }
    };
    private View.OnClickListener conversationClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "onClick: clicked on conversation button. For event " + mEvent.getId());
            Intent conversationIntent = new Intent(EventDetailActivity.this, ConversationListActivity.class);
            conversationIntent.putExtra("clubId", mEvent.getClubId());
            conversationIntent.putExtra("eventId", mEvent.getId());
            startActivity(conversationIntent);
        }
    };

    private View.OnClickListener confirmedButtonClickedListener = view -> {
        mSwipeRefreshLayout.setRefreshing(true);
        boolean isConfirmed = mEvent.getConfirmationList().stream()
                .anyMatch(c -> c.getUserId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid()));
        Request request = prepareRequest(clubId, eventId, isConfirmed);
        OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(getResources());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "Confirmation clicked onFailure: error during confirmation", e);
                ToastUtils.backgroundThreadShortToast(EventDetailActivity.this, "Error occurred. Try again!");
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Confirmation clicked onResponse: Successfully perform confirmation operation");
                    runOnUiThread(() -> refreshData(clubId, eventId));
                    ToastUtils.backgroundThreadShortToast(EventDetailActivity.this, "Successfully performed operation!");
                    return;
                }
                Log.w(TAG, String.format("Confirmation clicked onResponse: received response but %d status", response.code()));
                ToastUtils.backgroundThreadShortToast(EventDetailActivity.this, "Error occurred. Try again!");
            }
        });
    };

    private Request prepareRequest(long clubId, long eventId, boolean isConfirmed) {
        String requestAddress = getResources().getString(R.string.api_base_address) +
                String.format(getResources().getString(R.string.api_event_address_confirmation), clubId, eventId);
        HttpUrl url = HttpUrl.parse(requestAddress).newBuilder()
                .addQueryParameter("userUid", FirebaseAuth.getInstance().getCurrentUser().getUid())
                .build();
        Request.Builder requestBuilder = new Request.Builder()
                .url(url);
        if (isConfirmed) {
            requestBuilder.delete();
        } else {
            requestBuilder.post(RequestBody.create("", null));
        }
        return requestBuilder.build();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        refreshData(clubId, eventId);
    }

    private View.OnClickListener showRouteClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (URLUtil.isValidUrl(mEvent.getRouteLink())) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mEvent.getRouteLink()));
                startActivity(intent);
            } else {
                Toast.makeText(EventDetailActivity.this, "Provided link is not valid!", Toast.LENGTH_SHORT).show();
            }
        }
    };
}
