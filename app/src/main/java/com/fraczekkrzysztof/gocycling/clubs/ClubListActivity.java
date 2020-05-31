package com.fraczekkrzysztof.gocycling.clubs;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.apiutils.ApiUtils;
import com.fraczekkrzysztof.gocycling.model.EventModel;
import com.fraczekkrzysztof.gocycling.myconfirmations.MyConfirmationListRecyclerViewAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import cz.msebera.android.httpclient.Header;

public class ClubListActivity extends AppCompatActivity {

    private static final String TAG = "ClubListAct";
    private RecyclerView mRecyclerView;
    private ClubListRecyclerViewAdapter mAdapter;
    private SwipeRefreshLayout mClubListSwype;
    private int page = 0;
    private int totalPages = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clubs);
        mClubListSwype = findViewById(R.id.clubs_list_swipe);
        mClubListSwype.setOnRefreshListener(OnRefreshListener);
        initRecyclerView();
        getSupportActionBar().setSubtitle("Clubs");
    }

    private void initRecyclerView(){
        Log.d(TAG, "initRecyclerView: init recycler view");
        mRecyclerView = findViewById(R.id.clubs_recycler_view);
        mAdapter = new ClubListRecyclerViewAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setOnScrollListener(prOnScrollListener);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration divider = new DividerItemDecoration(this,DividerItemDecoration.VERTICAL);
        divider.setDrawable(getResources().getDrawable(R.drawable.event_list_divider));
        mRecyclerView.addItemDecoration(divider);
    }

    private void getConfirmations(int page){
        //TODO add code to get confirmation
    }

    private SwipeRefreshLayout.OnRefreshListener OnRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            Log.d(TAG, "onRefresh: refreshing");
            refreshData();
        }
    };

    private void refreshData(){
        mAdapter.clearEvents();
        page = 0;
        totalPages = 0;
        getConfirmations(0);
    }

    private RecyclerView.OnScrollListener prOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            if(isLastItemDisplaying(recyclerView)){
                if (page == totalPages - 1){
                    Log.d(TAG, "There is nothing to load");
                    return;
                }
                page++;
                Log.d(TAG, "Load more data for page " + page);
                getConfirmations(page);
            }
        }

    };
    private boolean isLastItemDisplaying(RecyclerView recyclerView){
        //check if the adapter item count is greater than 0
        if(recyclerView.getAdapter().getItemCount() != 0){
            //get the last visible item on screen using the layoutmanager
            int lastVisibleItemPosition = ((LinearLayoutManager)recyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition();
            //apply some logic here.
            if(lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition == recyclerView.getAdapter().getItemCount() - 1)
                return true;
        }
        return  false;
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        refreshData();
    }
}
