package com.fraczekkrzysztof.gocycling;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;

import com.fraczekkrzysztof.gocycling.event.EventModel;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.client.HttpClient;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private List<EventModel> mListEvents = new ArrayList<>();
    RecyclerView recyclerView;
    RecyclerViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getEvents();
        initRecyclerView(mListEvents);
        Log.d(TAG, "onCreate:  started.");
    }

    private void initRecyclerView(List<EventModel> eventModelList){
        Log.d(TAG, "initRecyclerView: init recycler view");
        Log.d(TAG, "initRecyclerView: " + eventModelList.size());
        recyclerView = findViewById(R.id.recycler_view);
        adapter = new RecyclerViewAdapter(getApplicationContext(),eventModelList);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void getEvents(){
        Log.d(TAG, "getEvents: called");
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth("user","password");
        client.get("http://10.0.2.2:8080/api/events", new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.d(TAG, "onSuccess: ");
                Log.d(TAG, response.toString());
                mListEvents = EventModel.fromJson(response);
                adapter.setEventList(mListEvents);
                adapter.notifyDataSetChanged();
                Log.d(TAG, "onSuccess: size of list " + mListEvents.size());

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Log.d(TAG, "onFailure: ");
                Log.d(TAG, errorResponse.toString());
            }
        });
    }
}
