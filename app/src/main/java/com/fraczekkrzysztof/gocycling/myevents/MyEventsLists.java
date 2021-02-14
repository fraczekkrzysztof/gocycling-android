package com.fraczekkrzysztof.gocycling.myevents;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.adapters.ClubAdapter;
import com.fraczekkrzysztof.gocycling.apiutils.ApiUtils;
import com.fraczekkrzysztof.gocycling.event.EventListActivity;
import com.fraczekkrzysztof.gocycling.model.ClubModel;
import com.fraczekkrzysztof.gocycling.model.EventModel;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONObject;

import java.util.List;

import cz.msebera.android.httpclient.Header;

public class MyEventsLists extends AppCompatActivity {

    private static final String TAG = "MyEventsLists";
    private static final String SHARED_PREF_TAG = "EVENTS_LIST";
    private static final String SHARED_PREF_LAST_SELECTED_CLUB = "LAST_SELECTED_CLUB";


    private RecyclerView mRecyclerView;
    private MyEventListRecyclerViewAdapter mAdapter;
    private SwipeRefreshLayout myEventListSwipe;
    private Spinner mClubSpinner;
    private List<ClubModel> mListOfClubs;
    private long mSelectedClubId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_events);
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        myEventListSwipe = findViewById(R.id.my_events_list_swipe);
        mClubSpinner = findViewById(R.id.my_event_list_club_spinner);
        myEventListSwipe.setOnRefreshListener(onRefresListener);
        getSupportActionBar().setSubtitle("My Events");
        initRecyclerView();
        Log.d(TAG, "onCreate:  started.");
    }


    private void initRecyclerView(){
        Log.d(TAG, "initRecyclerView: init recycler view");
        mRecyclerView = findViewById(R.id.myevents_recycler_view);
        mAdapter = new MyEventListRecyclerViewAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration divider = new DividerItemDecoration(this,DividerItemDecoration.VERTICAL);
        divider.setDrawable(getResources().getDrawable(R.drawable.event_list_divider));
        mRecyclerView.addItemDecoration(divider);
    }

    private void getEvents(long clubId) {
        myEventListSwipe.setRefreshing(true);
        Log.d(TAG, "getEvents: called");
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(getResources().getString(R.string.api_user),getResources().getString(R.string.api_password));
        String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_event_by_useruid_and_clubid);
        requestAddress = requestAddress + ApiUtils.PARAMS_START + "userUid="+FirebaseAuth.getInstance().getCurrentUser().getUid();
        requestAddress = requestAddress + ApiUtils.PARAMS_AND + "clubId=" + clubId;
        requestAddress = requestAddress + ApiUtils.PARAMS_AND + ApiUtils.getSizeToRequest(1000);
        Log.d(TAG, "Events: created request " + requestAddress);
        client.get(requestAddress, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.d(TAG, "onSuccess: response successfully received");
                List<EventModel> listEvents = EventModel.fromJson(response);
                mAdapter.addEvents(listEvents);
                myEventListSwipe.setRefreshing(false);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Toast.makeText(MyEventsLists.this,"There is an error. Please try again!",Toast.LENGTH_SHORT).show();
                Log.e(TAG,"Error during retrieving event list", throwable);
                if (errorResponse != null){
                    Log.d(TAG, errorResponse.toString());
                }
                myEventListSwipe.setRefreshing(false);
            }
        });
    }

    void refreshData(){
        mAdapter.clearEvents();
        getClubsForSpinner();
    }


    private SwipeRefreshLayout.OnRefreshListener onRefresListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            Log.d(TAG, "onRefresh: refreshing");
            refreshData();
        }
    };


    @Override
    protected void onPostResume() {
        super.onPostResume();
        refreshData();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent (MyEventsLists.this, EventListActivity.class);
        startActivity(intent);
    }

    void setRefreshing(boolean refreshing){
        myEventListSwipe.setRefreshing(refreshing);
    }

    private long getLastPickedClubId() {
        return getApplicationContext().getSharedPreferences(SHARED_PREF_TAG, Context.MODE_PRIVATE)
                .getLong(SHARED_PREF_LAST_SELECTED_CLUB, -1);
    }

    private void saveLastPickedClubId(long clubId) {
        SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences(SHARED_PREF_TAG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putLong(SHARED_PREF_LAST_SELECTED_CLUB, clubId);
        editor.commit();

    }


    private void getClubsForSpinner() {
        Log.d(TAG, "getClubsForSpinner: called");
        myEventListSwipe.setRefreshing(true);
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(getResources().getString(R.string.api_user), getResources().getString(R.string.api_password));
        String requestAddress = getResources().getString(R.string.api_base_address);
//        requestAddress = requestAddress + getResources().getString(R.string.api_clubs_which_user_is_member) + ApiUtils.PARAMS_START + "userUid=" + FirebaseAuth.getInstance().getCurrentUser().getUid();
        requestAddress = requestAddress + ApiUtils.PARAMS_AND + ApiUtils.getSizeToRequest(1000);

        Log.d(TAG, "getClubsForSpinner: created request " + requestAddress);
        client.get(requestAddress, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.d(TAG, "onSuccess: response successfully received");
                List<ClubModel> listClubs = ClubModel.fromJson(response);
                addClubsToSpinner(listClubs);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Toast.makeText(MyEventsLists.this, "There is an error. Please try again!", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error during retrieving club list", throwable);
                if (errorResponse != null) {
                    Log.d(TAG, errorResponse.toString());
                }
                myEventListSwipe.setRefreshing(false);
            }
        });
    }

    private void addClubsToSpinner(List<ClubModel> listClubs) {
        mListOfClubs = listClubs;
        ClubAdapter clubAdapter = new ClubAdapter(MyEventsLists.this, R.layout.club_list_item, listClubs);
        mClubSpinner.setAdapter(clubAdapter);
        mClubSpinner.setOnItemSelectedListener(spinnerItemSelectedListener);
        if (getLastPickedClubId() != -1) {
            for (ClubModel club : listClubs) {
                if (club.getId() == getLastPickedClubId()) {
                    int spinnerPosition = clubAdapter.getPosition(club);
                    mClubSpinner.setSelection(spinnerPosition);
                    break;
                }
            }
        }
    }

    private AdapterView.OnItemSelectedListener spinnerItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            mSelectedClubId = mListOfClubs.get(i).getId();
            saveLastPickedClubId(mSelectedClubId);
            mAdapter.clearEvents();
            getEvents(mSelectedClubId);
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
            mSelectedClubId = -1;
            saveLastPickedClubId(mSelectedClubId);
        }
    };
}
