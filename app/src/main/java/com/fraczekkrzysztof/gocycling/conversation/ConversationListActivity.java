package com.fraczekkrzysztof.gocycling.conversation;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.httpclient.GoCyclingHttpClientHelper;
import com.fraczekkrzysztof.gocycling.model.v2.PageDto;
import com.fraczekkrzysztof.gocycling.model.v2.event.ConversationDto;
import com.fraczekkrzysztof.gocycling.model.v2.event.ConversationListResponseDto;
import com.fraczekkrzysztof.gocycling.model.v2.event.ConversationResponseDto;
import com.fraczekkrzysztof.gocycling.utils.ToastUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ConversationListActivity extends AppCompatActivity {

    private static final String TAG = "ConversationList";

    private final Gson gson = new Gson();

    private RecyclerView mRecyclerView;
    private ConversationListRecyclerViewAdapter mAdapter;
    private SwipeRefreshLayout conversationListSwipe;
    private PageDto mPageDto;
    private long eventId;
    private long clubId;
    private ImageButton mSendButton;
    private EditText mMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);
        conversationListSwipe = findViewById(R.id.conversation_list_swipe);
        conversationListSwipe.setOnRefreshListener(onRefreshListener);
        mSendButton = findViewById(R.id.conversation_send_button);
        mSendButton.setOnClickListener(sendButtonClickedListener);
        mMessage = findViewById(R.id.conversation_message_input);
        getSupportActionBar().setSubtitle("Conversation");
        eventId = getIntent().getLongExtra("eventId", -1);
        clubId = getIntent().getLongExtra("clubId", -1);
        initRecyclerView();
        Log.d(TAG, "onCreate:  started.");
    }


    private void initRecyclerView() {
        Log.d(TAG, "initRecyclerView: init recycler view");
        mRecyclerView = findViewById(R.id.conversation_recycler_view);
        mAdapter = new ConversationListRecyclerViewAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addOnScrollListener(prOnScrollListener);
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        divider.setDrawable(getResources().getDrawable(R.drawable.event_list_divider));
        mRecyclerView.addItemDecoration(divider);
    }

    private void getConversation(int page, boolean getAllData) {
        conversationListSwipe.setRefreshing(true);
        Log.d(TAG, "getConversation: called");
        Request request = prepareGetConversationRequest(clubId, eventId, page);
        OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(getResources());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "getConversation onFailure: error during retrieving conversation list.", e);
                ToastUtils.backgroundThreadShortToast(ConversationListActivity.this, "Error occurred! Try again.");
                conversationListSwipe.setRefreshing(false);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "getConversation onResponse: successfully retrieved conversations");
                    ConversationListResponseDto apiResponse = gson.fromJson(response.body().charStream(), ConversationListResponseDto.class);
                    runOnUiThread(() -> {
                        mPageDto = apiResponse.getPageDto();
                        mAdapter.addConversation(apiResponse.getConversations());
                        if (getAllData && mPageDto.getNextPage() != 0) {
                            getConversation(mPageDto.getNextPage(), true);
                        }
                        if (getAllData && mPageDto.getNextPage() == 0)
                            mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
                    });
                    conversationListSwipe.setRefreshing(false);
                    return;
                }
                Log.w(TAG, String.format("getConversation onResponse: response received but %d status", response.code()));
                ToastUtils.backgroundThreadShortToast(ConversationListActivity.this, "");
                conversationListSwipe.setRefreshing(false);
            }
        });
    }

    private Request prepareGetConversationRequest(long clubId, long eventId, int page) {
        String requestAddress = getResources().getString(R.string.api_base_address) +
                String.format(getResources().getString(R.string.api_conversation), clubId, eventId);
        HttpUrl url = HttpUrl.parse(requestAddress).newBuilder()
                .addQueryParameter("page", String.valueOf(page))
                .build();
        return new Request.Builder()
                .url(url)
                .build();
    }

    private void saveConversation(final View view, String message) {
        Log.d(TAG, "saveConversation: called");
        conversationListSwipe.setRefreshing(true);
        Request request = prepareRequestForAddConversation(clubId, eventId, message);
        OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(getResources());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "Create conversation onFailure: error during creating conversation", e);
                ToastUtils.backgroundThreadShortToast(ConversationListActivity.this, "Error occurred. Try again!");
                conversationListSwipe.setRefreshing(false);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "createConversation onResponse: successfully created conversation");
                    ConversationResponseDto apiResponse = gson.fromJson(response.body().charStream(), ConversationResponseDto.class);
                    runOnUiThread(() -> {
                        mAdapter.addSingleConversation(apiResponse.getConversation());
                        mMessage.setText(null);
                        hideSoftInput(view);
                        conversationListSwipe.setRefreshing(false);
                        mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
                    });
                    ToastUtils.backgroundThreadShortToast(ConversationListActivity.this, "Successfully created message!");
                    return;
                }
                Log.w(TAG, String.format("create conversation onResponse: received response but %d status", response.code()));
                conversationListSwipe.setRefreshing(false);
            }
        });
    }

    private Request prepareRequestForAddConversation(long clubId, long eventId, String message) {
        String requestAddress = getResources().getString(R.string.api_base_address) +
                String.format(getResources().getString(R.string.api_conversation), clubId, eventId);
        ConversationDto requestBody = ConversationDto.builder()
                .message(message)
                .userId(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .build();
        return new Request.Builder()
                .url(requestAddress)
                .post(RequestBody.create(gson.toJson(requestBody), MediaType.parse("application/json;charset=UTF-8")))
                .build();
    }

    private void refreshData() {
        mAdapter.clearEvents();
        getConversation(0, false);
    }

    private SwipeRefreshLayout.OnRefreshListener onRefreshListener = () -> refreshData();

    private View.OnClickListener sendButtonClickedListener = view -> {
        Log.d(TAG, "onClick: sending message");
        if (checkMessageField(mMessage)) {
            saveConversation(view, mMessage.getText().toString());
        }
    };

    private boolean checkMessageField(EditText fieldToCheck) {
        String text = fieldToCheck.getText().toString();
        if (text != null && text.length() != 0) {
            text = text.trim();
            if (text != null && text.length() != 0) {
                return true;
            }
        }
        Toast.makeText(ConversationListActivity.this, "Enter the message", Toast.LENGTH_SHORT).show();
        return false;
    }

    private void hideSoftInput(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getConversation(0, true);
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
                getConversation(mPageDto.getNextPage(), false);
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
}
