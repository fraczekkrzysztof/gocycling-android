package com.fraczekkrzysztof.gocycling.usernotifications;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.httpclient.GoCyclingHttpClientHelper;
import com.fraczekkrzysztof.gocycling.model.v2.PageDto;
import com.fraczekkrzysztof.gocycling.model.v2.notificatication.NotificationListResponseDto;
import com.fraczekkrzysztof.gocycling.utils.ToastUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NotificationLists extends AppCompatActivity {

    private static final String TAG = "NotificationLists";
    private final Gson gson = new Gson();
    private RecyclerView mRecyclerView;
    private NotificationListRecyclerViewAdapter mAdapter;
    private SwipeRefreshLayout mNotificationListSwype;
    private PageDto mPageDto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification_list);
        mNotificationListSwype = findViewById(R.id.notification_list_swipe);
        mNotificationListSwype.setOnRefreshListener(onRefreshListener);
        initRecyclerView();
        getSupportActionBar().setSubtitle("Notifications");
    }

    private void initRecyclerView(){
        Log.d(TAG, "initRecyclerView: init recycler view");
        mRecyclerView = findViewById(R.id.notification_recycler_view);
        mAdapter = new NotificationListRecyclerViewAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnScrollListener(prOnScrollListener);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration divider = new DividerItemDecoration(this,DividerItemDecoration.VERTICAL);
        divider.setDrawable(getResources().getDrawable(R.drawable.event_list_divider));
        mRecyclerView.addItemDecoration(divider);
    }

    private void getNotifications(int page){
        Log.d(TAG, "getNotifications: called");
        mNotificationListSwype.setRefreshing(true);
        Request request = prepareRequestForNotification(page);
        OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(getResources());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "getNotification onFailure: error during retrieving notification list.", e);
                ToastUtils.backgroundThreadShortToast(NotificationLists.this, "Error occurred! Try again.");
                mNotificationListSwype.setRefreshing(false);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "getNotification onResponse: successfully received response with Notification list");
                    NotificationListResponseDto apiResponse = gson.fromJson(response.body().charStream(), NotificationListResponseDto.class);
                    runOnUiThread(() -> {
                        mPageDto = apiResponse.getPage();
                        mAdapter.addNotification(apiResponse.getNotifications());
                        mNotificationListSwype.setRefreshing(false);
                    });
                    return;
                }
                Log.w(TAG, String.format("getNotification onResponse: received response but %d status", response.code()));
                mNotificationListSwype.setRefreshing(false);
            }
        });
    }

    private Request prepareRequestForNotification(int page) {
        String requestAddress = getResources().getString(R.string.api_base_address) +
                String.format(getResources().getString(R.string.api_notification_by_user_uid), FirebaseAuth.getInstance().getCurrentUser().getUid());
        HttpUrl url = HttpUrl.parse(requestAddress).newBuilder()
                .addQueryParameter("sort", "created,desc")
                .addQueryParameter("page", String.valueOf(page))
                .build();
        return new Request.Builder()
                .url(url)
                .build();
    }

    private SwipeRefreshLayout.OnRefreshListener onRefreshListener = () -> {
            Log.d(TAG, "onRefresh: refreshing");
            refreshData();
    };

    private void refreshData(){
        mAdapter.clearNotification();
        getNotifications(0);
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
                getNotifications(mPageDto.getNextPage());
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


    @Override
    protected void onPostResume() {
        super.onPostResume();
        refreshData();
    }

    void setRefreshing(boolean refreshing){
        mNotificationListSwype.setRefreshing(refreshing);
    }
}
