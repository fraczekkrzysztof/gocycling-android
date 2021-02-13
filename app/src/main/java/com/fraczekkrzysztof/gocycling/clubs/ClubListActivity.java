package com.fraczekkrzysztof.gocycling.clubs;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.event.EventListActivity;
import com.fraczekkrzysztof.gocycling.httpclient.GoCyclingHttpClientHelper;
import com.fraczekkrzysztof.gocycling.model.v2.PageDto;
import com.fraczekkrzysztof.gocycling.model.v2.club.ClubListResponse;
import com.fraczekkrzysztof.gocycling.newclub.NewClubActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ClubListActivity extends AppCompatActivity {

    private static final String TAG = "ClubListAct";
    private RecyclerView mRecyclerView;
    private ClubListRecyclerViewAdapter mAdapter;
    private SwipeRefreshLayout mClubListSwype;
    private FloatingActionButton mAddButton;
    private PageDto mPageDto;
    private CheckBox mOnlyUserClubs;
    final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clubs);
        mClubListSwype = findViewById(R.id.clubs_list_swipe);
        mClubListSwype.setOnRefreshListener(onRefreshListener);
        mAddButton = findViewById(R.id.add_club);
        mAddButton.setOnClickListener(addButtonClickedListener);
        mOnlyUserClubs = findViewById(R.id.clubs_list_checkbox);
        mOnlyUserClubs.setOnClickListener(onCheckboxClickedListener);
        initRecyclerView();
        getSupportActionBar().setSubtitle("Clubs");
    }

    private void initRecyclerView() {
        Log.d(TAG, "initRecyclerView: init recycler view");
        mRecyclerView = findViewById(R.id.clubs_recycler_view);
        mAdapter = new ClubListRecyclerViewAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnScrollListener(prOnScrollListener);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        divider.setDrawable(getResources().getDrawable(R.drawable.event_list_divider));
        mRecyclerView.addItemDecoration(divider);
    }

    private void getClubs(int page) {
        mClubListSwype.setRefreshing(true);
        Log.d(TAG, "getClubs: called");
        Request request = prepareRequest(page);
        OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(getResources());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Toast.makeText(ClubListActivity.this, "There is an error. Please try again!", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error during retrieving club list", e);
                mClubListSwype.setRefreshing(false);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "onResponse: Successfully received response with ClubList");
                    Reader receivedResponse = response.body().charStream();
                    ClubListResponse apiResponse = gson.fromJson(receivedResponse, ClubListResponse.class);
                    runOnUiThread(() -> {
                        mPageDto = apiResponse.getPage();
                        mAdapter.addClubs(apiResponse.getClubs());
                        mClubListSwype.setRefreshing(false);
                        return;
                    });
                }
                mClubListSwype.setRefreshing(false);
                Log.e(TAG, String.format("onResponse: Response received, but not %s status.", response.code()));
            }
        });
    }

    @NotNull
    private Request prepareRequest(int page) {
        String url = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_clubs);
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        if (mOnlyUserClubs.isChecked()) {
            urlBuilder.addQueryParameter("userUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
        }
        urlBuilder.addQueryParameter("page", String.valueOf(page));
        return new Request.Builder()
                .url(urlBuilder.build().toString())
                .build();
    }

    private SwipeRefreshLayout.OnRefreshListener onRefreshListener = () -> {
            Log.d(TAG, "onRefresh: refreshing");
            refreshData();
    };

    private void refreshData() {
        mAdapter.clearClubs();
        getClubs(0);
    }

    private RecyclerView.OnScrollListener prOnScrollListener = new RecyclerView.OnScrollListener() {

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (isLastItemDisplaying(recyclerView)) {
                if (mPageDto.getNextPage() == 0) {
                    Log.d(TAG, "There is nothing to load");
                    return;
                }
                Log.d(TAG, "Load more data");
                getClubs(mPageDto.getNextPage());
            }
        }

        private boolean isLastItemDisplaying(RecyclerView recyclerView) {
            //check if the adapter item count is greater than 0
            if (recyclerView.getAdapter().getItemCount() != 0) {
                //get the last visible item on screen using the layoutmanager
                int lastVisibleItemPosition = ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition();
                //apply some logic here.
                if (lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition == recyclerView.getAdapter().getItemCount() - 1)
                    return true;
            }
            return false;
        }
    };

    private View.OnClickListener addButtonClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            startActivity(new Intent(ClubListActivity.this, NewClubActivity.class));
        }
    };

    @Override
    protected void onPostResume() {
        super.onPostResume();
        refreshData();
    }

    private View.OnClickListener onCheckboxClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            refreshData();
        }
    };

    @Override
    public void onBackPressed() {
        startActivity(new Intent(ClubListActivity.this, EventListActivity.class));
    }
}
