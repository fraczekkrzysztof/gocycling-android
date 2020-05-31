package com.fraczekkrzysztof.gocycling.eventdetails;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fraczekkrzysztof.gocycling.MapsActivity;
import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.apiutils.ApiUtils;
import com.fraczekkrzysztof.gocycling.apiutils.SortTypes;
import com.fraczekkrzysztof.gocycling.conversation.ConversationListActivity;
import com.fraczekkrzysztof.gocycling.model.ConfirmationModel;
import com.fraczekkrzysztof.gocycling.model.EventModel;
import com.fraczekkrzysztof.gocycling.model.UserModel;
import com.fraczekkrzysztof.gocycling.myconfirmations.MyConfirmationsLists;
import com.fraczekkrzysztof.gocycling.utils.DateUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class EventDetailActivity extends AppCompatActivity {

    private static final String TAG = "EventDetailActivity";
    private long confirmationId = -1;
    TextView mWho;
    TextView mTitle;
    TextView mWhere;
    TextView mWhen;
    TextView mDetails;
    EventModel mEvent;
    List<String> mUserConfirmed = new ArrayList<>();
    Button mConfirmButton;
    ImageButton mLocationButton;
    ImageButton mConversationButton;
    ImageButton mRouteButton;
    ListView mListView;
    SwipeRefreshLayout mSwipeRefreshLayout;
    AlertDialog mDialog;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: started!");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_details);
        mEvent = (EventModel) getIntent().getSerializableExtra("Event");
        mTitle = findViewById(R.id.event_detail_title);
        mWho = findViewById(R.id.event_detail_who);
        mWhere = findViewById(R.id.event_detail_where);
        mWhen = findViewById(R.id.event_detail_when);
        mDetails = findViewById(R.id.event_detail_details);
        mListView = findViewById(R.id.list_of_users_confirmed);
        mConfirmButton = findViewById(R.id.event_confirm_button);
        mConfirmButton.setOnClickListener(confirmedButtonClickedListener);
        mConversationButton = findViewById(R.id.event_conversation);
        mConversationButton.setOnClickListener(conversationClickedListener);
        mLocationButton = findViewById(R.id.event_detail_show_location);
        mLocationButton.setOnClickListener(showLocationButtonListener);
        mRouteButton = findViewById(R.id.event_detail_show_route);
        setVisibleOfRouteButton();
        mRouteButton.setOnClickListener(showRouteClickedListener);
        setDialogMessage();
        mSwipeRefreshLayout = findViewById(R.id.event_detail_swipe_layout);
        mSwipeRefreshLayout.setOnRefreshListener(onRefresListener);
        getSupportActionBar().setSubtitle("Events details");
    }

    private void setVisibleOfRouteButton(){
        if (mEvent.getRouteLink() == null || mEvent.getRouteLink() == ""){
            mRouteButton.setVisibility(View.INVISIBLE);
        }
    }
    private SwipeRefreshLayout.OnRefreshListener onRefresListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            Log.d(TAG, "onRefresh: refreshing");
            refreshData(FirebaseAuth.getInstance().getCurrentUser().getUid(),mEvent.getId());
        }
    };

    private void setConfirmationButton(boolean isConfirmed) {
        if (isConfirmed){
            setConfirmedButtonToConfirmed();
        } else {
            setConfirmedButtonToNotConfirmed();
        }
    }

    private void setConfirmedButtonToConfirmed(){
        mConfirmButton.setText("CANCEL CONFIRMATION");
        mConfirmButton.setBackgroundColor(getResources().getColor(R.color.secondaryDarkColor));
    }

    private void setConfirmedButtonToNotConfirmed(){
        mConfirmButton.setText("CONFIRM");
        mConfirmButton.setBackgroundColor(getResources().getColor(R.color.primaryDarkColor));

    }

    private void setTexts(){
        mTitle.setText(mEvent.getName());
        mWhen.setText(DateUtils.sdfWithTime.format(mEvent.getDateAndTime()));
        mWhere.setText(mEvent.getPlace());
        mDetails.setText(mEvent.getDetails());
    }

    private void refreshData(String userUid, long eventId){
        mSwipeRefreshLayout.setRefreshing(true);
        getCreatorUserName();
        getInformationAboutUserConfirmation(userUid,eventId);
        getConfirmedUser();

    }

    private void getInformationAboutUserConfirmation(String userUid, long eventId){
        Log.d(TAG, "getInformationAboutUserConfirmation: called");
        final boolean[] toReturn = {false};
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(getResources().getString(R.string.api_user),getResources().getString(R.string.api_password));
        String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_find_confirmation_by_user_and_event);
        requestAddress = requestAddress + ApiUtils.PARAMS_START + "userUid=" + userUid;
        requestAddress = requestAddress + ApiUtils.PARAMS_AND + "id=" + eventId;
        Log.d(TAG, "getEvents: created request" + requestAddress);
        client.get(requestAddress,new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.d(TAG, "onSuccess: This event is already confirmed by user");
                List<ConfirmationModel> listOfConfirmation = ConfirmationModel.fromJson(response);
                setConfirmationButton((listOfConfirmation.size()>0));
                if (listOfConfirmation.size()>0){
                    confirmationId = listOfConfirmation.get(0).getId();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                Log.e(TAG, "onFailure: error on checking is this event confirmed", throwable);
            }
        });
    }



    private void getConfirmedUser(){
        Log.d(TAG, "getConfirmedUser: called");
        mUserConfirmed.clear();
        final boolean[] toReturn = {false};
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(getResources().getString(R.string.api_user),getResources().getString(R.string.api_password));
        String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_event_user_confirmed);
        requestAddress = requestAddress + ApiUtils.PARAMS_START + "eventId=" + mEvent.getId();
        Log.d(TAG, "getConfirmedUser: created request" + requestAddress);
        client.get(requestAddress,new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                List<UserModel> userList = UserModel.fromJsonUserList(response,false);
                for (UserModel user : userList){
                    mUserConfirmed.add(user.getName());
                }
                setArrayAdapterToListView();
                mSwipeRefreshLayout.setRefreshing(false);
                Log.d(TAG, "onSuccess: Successfully retrieved list of user whose already confirmed event");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                mSwipeRefreshLayout.setRefreshing(false);
                Log.e(TAG, "onFailure: There is an error while retrieving list of users whose already confirmed event",throwable);
            }
        });
    }

    private void getCreatorUserName(){
        Log.d(TAG, "getCreatorUserName: called");
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(getResources().getString(R.string.api_user),getResources().getString(R.string.api_password));
        String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_users);
        requestAddress = requestAddress + "/" + mEvent.getCreatedBy();
        Log.d(TAG, "getCreatorUserName: created request" + requestAddress);
        client.get(requestAddress,new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                UserModel creator = UserModel.fromJsonUser(response,false);
                mWho.setText(creator.getName());
                Log.d(TAG, "onSuccess: Successfully retrieved user who create event");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                Log.e(TAG, "onFailure: There is an error while retrieving user who create event",throwable);
            }
        });
    }

    private View.OnClickListener showLocationButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent showLocation = new Intent(EventDetailActivity.this, MapsActivity.class);
            showLocation.putExtra("Event",mEvent);
            startActivity(showLocation);
        }
    };
    private View.OnClickListener conversationClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Log.d(TAG, "onClick: clicked on conversation button. For event " + mEvent.getId());
            Intent newCityIntent = new Intent(EventDetailActivity.this, ConversationListActivity.class);
            newCityIntent.putExtra("Event",mEvent);
            startActivity(newCityIntent);
        }
    };
    private View.OnClickListener detailsButtonClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mDialog.show();
        }
    };

    private View.OnClickListener confirmedButtonClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            try {
                mSwipeRefreshLayout.setRefreshing(true);
                AsyncHttpClient client = new AsyncHttpClient();
                client.setBasicAuth(getResources().getString(R.string.api_user), getResources().getString(R.string.api_password));
                String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_confirmation_address);
                if (confirmationId > 0){
                    requestAddress = requestAddress + "/" + confirmationId;
                    client.delete(requestAddress, new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            Log.d(TAG, "onSuccess: successfully delete confirmation");
                            confirmationId=0;
                            mSwipeRefreshLayout.setRefreshing(false);
                            Toast.makeText(getBaseContext(),"Successfully cancel confirmation",Toast.LENGTH_SHORT).show();
                            refreshData(FirebaseAuth.getInstance().getCurrentUser().getUid(),mEvent.getId());
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                            Log.e(TAG, "onFailure: error during deleting confirmation " +responseBody.toString(), error);
                            mSwipeRefreshLayout.setRefreshing(false);
                            Toast.makeText(getApplicationContext(),"Error while canceling confirmation!",Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    JSONObject params = new JSONObject();
                    params.put("id", 0);
                    params.put("userUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                    params.put("event", getResources().getString(R.string.api_event_address) + "/" + mEvent.getId());
                    Log.d(TAG, "onClick: " + params.toString());
                    StringEntity stringParams = new StringEntity(params.toString(),"UTF-8");
                    client.post(getApplicationContext(), requestAddress, stringParams, "application/json;charset=UTF-8", new AsyncHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                            Log.d(TAG, "onSuccess: Successfully add confirmtion");
                            confirmationId = getConfirmationIdFromHeaderResponse(headers);
                            Toast.makeText(EventDetailActivity.this,"Successfully confirmed!",Toast.LENGTH_SHORT).show();
                            mSwipeRefreshLayout.setRefreshing(false);
                            refreshData(FirebaseAuth.getInstance().getCurrentUser().getUid(),mEvent.getId());
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                            Log.e(TAG, "onFailure: " + responseBody.toString(),error);
                            mSwipeRefreshLayout.setRefreshing(false);
                            Toast.makeText(getApplicationContext(),"Error while confirmed!",Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            } catch (Exception e){
                Log.e(TAG, "onClick: Error during confirmation events",e);
            }
        }
    };

    private void setArrayAdapterToListView(){
        ArrayAdapter arrayAdapter = new ArrayAdapter(getApplicationContext(),android.R.layout.simple_list_item_1,mUserConfirmed){
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(Color.BLACK);
                text.setTextSize(Float.parseFloat("15"));
                return view;
            }
        };
        mListView.setAdapter(arrayAdapter);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        refreshData(FirebaseAuth.getInstance().getCurrentUser().getUid(),mEvent.getId());
        setTexts();
    }

    private void setDialogMessage(){
        String eventDetails = mEvent.getDetails();
        if (eventDetails == null){
            eventDetails = "";
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setMessage(eventDetails);
        builder.setNegativeButton(R.string.close,null);

        mDialog = builder.create();
    }

    public int getConfirmationIdFromHeaderResponse(Header[] headers){
        int id = 0;
        for (int i=0 ; i<headers.length; i++ ){
            if(headers[i].getName().equals("Location")){
                String address = headers[i].getValue();
                id = Integer.valueOf(address.substring(address.lastIndexOf("/")+1));
                return id;
            }
            Header header = headers[i];
            System.out.println(header.getName());
        }
        return id;
    }

    private View.OnClickListener showRouteClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (URLUtil.isValidUrl(mEvent.getRouteLink())){
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mEvent.getRouteLink()));
                startActivity(intent);
            } else {
                Toast.makeText(EventDetailActivity.this,"Provided link is not viliad!",Toast.LENGTH_SHORT).show();
            }
        }
    };
}
