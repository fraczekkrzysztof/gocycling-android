package com.fraczekkrzysztof.gocycling.clubs;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.apiutils.ApiUtils;
import com.fraczekkrzysztof.gocycling.model.ClubModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONObject;

import java.util.List;

import cz.msebera.android.httpclient.Header;

public class ClubListActivity extends AppCompatActivity {

    private static final String TAG = "ClubListAct";
    private RecyclerView mRecyclerView;
    private ClubListRecyclerViewAdapter mAdapter;
    private SwipeRefreshLayout mClubListSwype;
    private FloatingActionButton mAddButton;
    private int page = 0;
    private int totalPages = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clubs);
        mClubListSwype = findViewById(R.id.clubs_list_swipe);
        mClubListSwype.setOnRefreshListener(OnRefreshListener);
        mAddButton = findViewById(R.id.add_club);
        mAddButton.setOnClickListener(addButtonClickedListener);
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

    private void getClubs(int page){
        mClubListSwype.setRefreshing(true);
        Log.d(TAG, "getClubs: called");
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(getResources().getString(R.string.api_user),getResources().getString(R.string.api_password));
        String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_clubs_list);
        requestAddress = requestAddress + ApiUtils.PARAMS_START + ApiUtils.getPageToRequest(page);
        Log.d(TAG, "getClubs: created request " + requestAddress);
        client.get(requestAddress, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.d(TAG, "onSuccess: response successfully received");
                List<ClubModel> listClubs = ClubModel.fromJson(response);
                if (totalPages == 0 ){
                    totalPages = ClubModel.getTotalPageFromJson(response);
                }
                mAdapter.addClubs(listClubs);
                mClubListSwype.setRefreshing(false);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Toast.makeText(ClubListActivity.this,"There is an error. Please try again!",Toast.LENGTH_SHORT).show();
                Log.e(TAG,"Error during retrieving club list", throwable);
                if (errorResponse != null){
                    Log.d(TAG, errorResponse.toString());
                }
                mClubListSwype.setRefreshing(false);
            }
        });
    }

    private SwipeRefreshLayout.OnRefreshListener OnRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            Log.d(TAG, "onRefresh: refreshing");
            refreshData();
        }
    };

    private void refreshData(){
        mAdapter.clearClubs();
        page = 0;
        totalPages = 0;
        getClubs(0);
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
                getClubs(page);
            }
        }

    };

    private View.OnClickListener addButtonClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Toast.makeText(ClubListActivity.this,"clicked",Toast.LENGTH_SHORT).show();
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
