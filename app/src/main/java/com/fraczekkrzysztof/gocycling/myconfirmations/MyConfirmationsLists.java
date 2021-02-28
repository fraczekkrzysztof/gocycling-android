package com.fraczekkrzysztof.gocycling.myconfirmations;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.adapters.ClubAdapter;
import com.fraczekkrzysztof.gocycling.httpclient.GoCyclingHttpClientHelper;
import com.fraczekkrzysztof.gocycling.model.v2.PageDto;
import com.fraczekkrzysztof.gocycling.model.v2.club.ClubDto;
import com.fraczekkrzysztof.gocycling.model.v2.club.ClubListResponse;
import com.fraczekkrzysztof.gocycling.model.v2.event.EventListResponseDto;
import com.fraczekkrzysztof.gocycling.utils.ToastUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MyConfirmationsLists extends AppCompatActivity {

    private static final String TAG = "MyConfirmationsLists";
    private static final String SHARED_PREF_TAG = "EVENTS_LIST";
    private static final String SHARED_PREF_LAST_SELECTED_CLUB = "LAST_SELECTED_CLUB";
    private final Gson gson = new Gson();

    private RecyclerView mRecyclerView;
    private MyConfirmationListRecyclerViewAdapter mAdapter;
    private SwipeRefreshLayout mConfirmationListSwipe;
    private Spinner mClubListSpinner;
    private List<ClubDto> mListOfClubs;
    private PageDto mPageDto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_confirmations);
        mClubListSpinner = findViewById(R.id.my_confirmation_list_club_spinner);
        mConfirmationListSwipe = findViewById(R.id.myconfirmations_list_swipe);
        mConfirmationListSwipe.setOnRefreshListener(onRefreshListener);
        initRecyclerView();
        getSupportActionBar().setSubtitle("Confirmed");
    }

    private void initRecyclerView() {
        Log.d(TAG, "initRecyclerView: init recycler view");
        mRecyclerView = findViewById(R.id.my_confirmation_recycler_view);
        mAdapter = new MyConfirmationListRecyclerViewAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnScrollListener(prOnScrollListener);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        divider.setDrawable(getResources().getDrawable(R.drawable.event_list_divider));
        mRecyclerView.addItemDecoration(divider);
    }

    private void getEvents(int page, long clubId) {
        mConfirmationListSwipe.setRefreshing(true);
        Log.d(TAG, "getEvents: called");
        Request request = prepareEventRequest(page, clubId);
        OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(getResources());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                ToastUtils.backgroundThreadShortToast(MyConfirmationsLists.this, "There is an error. Please try again!");
                Log.e(TAG, "Error during retrieving event list", e);
                mConfirmationListSwipe.setRefreshing(false);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "onResponse: Successfully received response with Events");
                    EventListResponseDto apiResponse = gson.fromJson(response.body().charStream(), EventListResponseDto.class);
                    runOnUiThread(() -> {
                        mPageDto = apiResponse.getPage();
                        mAdapter.addEvents(apiResponse.getEvents());
                        mConfirmationListSwipe.setRefreshing(false);
                    });
                    return;
                }
                mConfirmationListSwipe.setRefreshing(false);
                Log.w(TAG, String.format("onResponse: Response reseived but %d status", response.code()));
                ToastUtils.backgroundThreadShortToast(MyConfirmationsLists.this, "There is an error. Try again");
            }
        });
    }

    private Request prepareEventRequest(int page, long clubId) {
        String request = getResources().getString(R.string.api_base_address) +
                String.format(getResources().getString(R.string.api_event_confirmed_by_address), clubId);
        HttpUrl.Builder urlBuilder = HttpUrl.parse(request).newBuilder()
                .addQueryParameter("userUid", FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addQueryParameter("page", String.valueOf(page))
                .addQueryParameter("sort", "created,desc");
        return new Request.Builder()
                .url(urlBuilder.build().toString())
                .build();
    }

    private SwipeRefreshLayout.OnRefreshListener onRefreshListener = () -> {
        Log.d(TAG, "onRefresh: refreshing");
        refreshData();
    };

    private void getClubsForSpinner() {
        Log.d(TAG, "getClubsForSpinner: called");
        mConfirmationListSwipe.setRefreshing(true);
        Request request = prepareClubsRequest();
        OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(getResources());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                ToastUtils.backgroundThreadShortToast(MyConfirmationsLists.this, "There is an error. Please try again!");
                Log.e(TAG, "Error during retrieving club list for spinner", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "onResponse: Successfully received response with club list for spinner");
                    ClubListResponse apiResponse = gson.fromJson(response.body().charStream(), ClubListResponse.class);
                    if (apiResponse.getClubs().isEmpty()) {
                        ToastUtils.backgroundThreadShortToast(MyConfirmationsLists.this, "Join to at least one club to show an events!");
                        mConfirmationListSwipe.setRefreshing(false);
                        return;
                    }
                    runOnUiThread(() -> {
                        addClubsToSpinner(apiResponse.getClubs());
                        mConfirmationListSwipe.setRefreshing(false);
                    });
                    return;
                }
                mConfirmationListSwipe.setRefreshing(false);
                Log.e(TAG, String.format("onResponse: Response with club for spinner received, but %s status.", response.code()));
            }
        });
    }

    @NotNull
    private Request prepareClubsRequest() {
        String url = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_clubs);
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder()
                .addQueryParameter("limit", "50")
                .addQueryParameter("sort", "created,desc")
                .addQueryParameter("userUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
        return new Request.Builder()
                .url(urlBuilder.build().toString())
                .build();
    }

    private void addClubsToSpinner(List<ClubDto> listClubs) {
        mListOfClubs = listClubs;
        ClubAdapter clubAdapter = new ClubAdapter(MyConfirmationsLists.this, R.layout.club_list_item, listClubs);
        mClubListSpinner.setAdapter(clubAdapter);
        mClubListSpinner.setOnItemSelectedListener(spinnerItemSelectedListener);
        if (getLastPickedClubId() != -1) {
            for (ClubDto club : listClubs) {
                if (club.getId() == getLastPickedClubId()) {
                    int spinnerPosition = clubAdapter.getPosition(club);
                    mClubListSpinner.setSelection(spinnerPosition);
                    break;
                }
            }
        }
    }

    private void refreshData() {
        mAdapter.clearEvents();
        getClubsForSpinner();
    }

    private long getLastPickedClubId() {
        return getApplicationContext().getSharedPreferences(SHARED_PREF_TAG, Context.MODE_PRIVATE)
                .getLong(SHARED_PREF_LAST_SELECTED_CLUB, -1);
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
                getEvents(mPageDto.getNextPage(), getLastPickedClubId());
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

    private AdapterView.OnItemSelectedListener spinnerItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            saveLastPickedClubId(mListOfClubs.get(i).getId());
            mAdapter.clearEvents();
            getEvents(0, mListOfClubs.get(i).getId());
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
            saveLastPickedClubId(-1);
        }

        private void saveLastPickedClubId(long clubId) {
            SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences(SHARED_PREF_TAG, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putLong(SHARED_PREF_LAST_SELECTED_CLUB, clubId);
            editor.commit();
        }
    };

    @Override
    protected void onPostResume() {
        super.onPostResume();
        refreshData();
    }

    public void setRefreshing(boolean refreshing) {
        mConfirmationListSwipe.setRefreshing(refreshing);
    }

    public void notifyAboutDataSetChange() {
        refreshData();
    }
}
