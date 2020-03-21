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
import com.fraczekkrzysztof.gocycling.apiutils.ApiUtils;
import com.fraczekkrzysztof.gocycling.model.ConversationModel;
import com.fraczekkrzysztof.gocycling.model.EventModel;
import com.google.firebase.auth.FirebaseAuth;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class ConversationListActivity extends AppCompatActivity{

    private static final String TAG = "ConversationList";

    private RecyclerView mRecyclerView;
    private ConversationListRecyclerViewAdapter mAdapter;
    private SwipeRefreshLayout conversationListSwipe;
    private EventModel mEvent;
    private ImageButton mSendButton;
    private EditText mMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);
        conversationListSwipe = findViewById(R.id.conversation_list_swipe);
        conversationListSwipe.setOnRefreshListener(onRefresListener);
        mSendButton = findViewById(R.id.conversation_send_button);
        mSendButton.setOnClickListener(sendButtonClickedListener);
        mMessage = findViewById(R.id.conversation_message_input);
        getSupportActionBar().setSubtitle("Conversation");
        mEvent = (EventModel) getIntent().getSerializableExtra("Event");
        initRecyclerView();
        Log.d(TAG, "onCreate:  started.");
    }


    private void initRecyclerView(){
        Log.d(TAG, "initRecyclerView: init recycler view");
        mRecyclerView = findViewById(R.id.conversation_recycler_view);
        mAdapter = new ConversationListRecyclerViewAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration divider = new DividerItemDecoration(this,DividerItemDecoration.VERTICAL);
        divider.setDrawable(getResources().getDrawable(R.drawable.event_list_divider));
        mRecyclerView.addItemDecoration(divider);
    }

    private void getConversation(){
        conversationListSwipe.setRefreshing(true);
        Log.d(TAG, "getConversation: called");
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(getResources().getString(R.string.api_user),getResources().getString(R.string.api_password));
        String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_conversation_by_event_id);
        requestAddress = requestAddress + ApiUtils.PARAMS_START + "eventId="+mEvent.getId();
        requestAddress = requestAddress + ApiUtils.PARAMS_AND + ApiUtils.getSizeToRequest(1000);
        Log.d(TAG, "Conversation: created request " + requestAddress);
        client.get(requestAddress, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.d(TAG, "onSuccess: response successfully received");
                List<ConversationModel> listConversation = ConversationModel.fromJson(response);
                mAdapter.addConversation(listConversation);
                conversationListSwipe.setRefreshing(false);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Toast.makeText(ConversationListActivity.this,"There is an error. Please try again!",Toast.LENGTH_SHORT).show();
                Log.e(TAG,"Error during retrieving conversation list", throwable);
                if (errorResponse != null){
                    Log.d(TAG, errorResponse.toString());
                }
                conversationListSwipe.setRefreshing(false);
            }
        });
    }

    private void saveConversation(final View view,String message){
        try {
            conversationListSwipe.setRefreshing(true);
            Log.d(TAG, "saveConversation: called");
            AsyncHttpClient client = new AsyncHttpClient();
            client.setBasicAuth(getResources().getString(R.string.api_user),getResources().getString(R.string.api_password));
            String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_conversation);
            JSONObject params = new JSONObject();
            params.put("userUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
            params.put("username", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
            params.put("message", message);
            params.put("event", "api/events/"+mEvent.getId());
            Log.d(TAG, "saveConversation: " + params.toString());
            StringEntity stringParams = new StringEntity(params.toString(),"UTF-8");
            client.post(getApplicationContext(), requestAddress, stringParams, "application/json;charset=UTF-8", new TextHttpResponseHandler() {
                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    conversationListSwipe.setRefreshing(false);
                    Log.e(TAG, "onFailure: error during creating conversation" + responseString,throwable );
                    Toast.makeText(getBaseContext(),"Error during creating conversation",Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, String responseString) {
                    conversationListSwipe.setRefreshing(false);
                    mMessage.setText(null);
                    hideSoftInput(view);
                    Toast.makeText(getBaseContext(),"Successfully create conversation",Toast.LENGTH_SHORT).show();
                    refreshData();
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void refreshData(){
        mAdapter.clearEvents();
        getConversation();
    }


    private SwipeRefreshLayout.OnRefreshListener onRefresListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            Log.d(TAG, "onRefresh: refreshing");
            refreshData();
        }
    };

    private View.OnClickListener sendButtonClickedListener = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            Log.d(TAG, "onClick: sending message");

            saveConversation(view,mMessage.getText().toString());
        }
    };

    private void hideSoftInput(View view){
        InputMethodManager imm = (InputMethodManager)view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        refreshData();
    }
}
