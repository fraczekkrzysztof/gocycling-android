package com.fraczekkrzysztof.gocycling.myevents;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.apiutils.ApiUtils;
import com.fraczekkrzysztof.gocycling.apiutils.SortTypes;
import com.fraczekkrzysztof.gocycling.event.EventListActivity;
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

    private RecyclerView mRecyclerView;
    private MyEventListRecyclerViewAdapter mAdapter;
    private SwipeRefreshLayout myEventListSwipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_events);
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        myEventListSwipe = findViewById(R.id.myevents_list_swipe);
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

    private void getEvents(){
        myEventListSwipe.setRefreshing(true);
        Log.d(TAG, "getEvents: called");
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(getResources().getString(R.string.api_user),getResources().getString(R.string.api_password));
        String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_event_by_useruid);
        requestAddress = requestAddress + ApiUtils.PARAMS_START + "userUid="+FirebaseAuth.getInstance().getCurrentUser().getUid();
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
        getEvents();
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
}