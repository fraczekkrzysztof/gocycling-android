package com.fraczekkrzysztof.gocycling.clubdetails;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.apiutils.ApiUtils;
import com.fraczekkrzysztof.gocycling.model.ClubModel;
import com.fraczekkrzysztof.gocycling.model.MemberModel;
import com.fraczekkrzysztof.gocycling.model.UserModel;
import com.google.firebase.auth.FirebaseAuth;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class ClubDetailActivity extends AppCompatActivity {

    private static final String TAG = "ClubDetailActivity";
    private long membershipId = -1;
    TextView mOwner;
    TextView mName;
    TextView mLocation;
    TextView mDetails;
    ClubModel mClub;
    List<String> mMembersList = new ArrayList<>();
    Button mJoinButton;
    ImageButton mLocationButton;
    ListView mListView;
    SwipeRefreshLayout mSwipeRefreshLayout;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: started!");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_club_details);
        mClub = (ClubModel) getIntent().getSerializableExtra("Club");
        mName = findViewById(R.id.club_detail_name);
        mOwner = findViewById(R.id.club_detail_owner);
        mLocation = findViewById(R.id.club_detail_location);
        mDetails = findViewById(R.id.club_detail_details);
        mListView = findViewById(R.id.club_details_members);
        mJoinButton = findViewById(R.id.club_join_button);
//        mConfirmButton.setOnClickListener(confirmedButtonClickedListener);
        mLocationButton = findViewById(R.id.event_detail_show_location);
//        mLocationButton.setOnClickListener(showLocationButtonListener);
        mSwipeRefreshLayout = findViewById(R.id.club_detail_swipe_layout);
        mSwipeRefreshLayout.setOnRefreshListener(onRefresListener);
        getSupportActionBar().setSubtitle("Club details");
    }

    private void refreshData(String userUid, long clubId){
        mSwipeRefreshLayout.setRefreshing(true);
        getOwnerUserName();
        getInformationAboutUserMembership(userUid,clubId);
        getMembers();

    }

    private SwipeRefreshLayout.OnRefreshListener onRefresListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            Log.d(TAG, "onRefresh: refreshing");
            refreshData(FirebaseAuth.getInstance().getCurrentUser().getUid(),mClub.getId());
        }
    };

    private void setJoinButton(boolean isConfirmed) {
        if (isConfirmed){
            setJoinButtonToJoined();
        } else {
            setJoinButtonToNotJoined();
        }
    }

    private void setJoinButtonToJoined(){
        mJoinButton.setText("LEAVE");
        mJoinButton.setBackgroundColor(getResources().getColor(R.color.secondaryDarkColor));
    }

    private void setJoinButtonToNotJoined(){
        mJoinButton.setText("JOIN");
        mJoinButton.setBackgroundColor(getResources().getColor(R.color.primaryDarkColor));

    }

    private void setTexts(){
        mName.setText(mClub.getName());
        mLocation.setText(mClub.getLocation());
        mDetails.setText(mClub.getDetails());
    }



    private void getInformationAboutUserMembership(String userUid, long clubId){
        Log.d(TAG, "getInformationAboutUserMembership: called");
        final boolean[] toReturn = {false};
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(getResources().getString(R.string.api_user),getResources().getString(R.string.api_password));
        String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_club_user_membership);
        requestAddress = requestAddress + ApiUtils.PARAMS_START + "userUid=" + userUid;
        requestAddress = requestAddress + ApiUtils.PARAMS_AND + "clubId=" + clubId;
        Log.d(TAG, "getInformationAboutUserMembership: created request" + requestAddress);
        client.get(requestAddress,new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.d(TAG, "onSuccess: successfully received information about user membership");
                List<MemberModel> listOfMembers = MemberModel.fromJson(response);
                setJoinButton((listOfMembers.size()>0));
                if (listOfMembers.size()>0){
                    membershipId = listOfMembers.get(0).getId();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                Log.e(TAG, "onFailure: error on checking that user is member of club", throwable);
            }
        });
    }



    private void getMembers(){
        Log.d(TAG, "getMembers: called");
        mMembersList.clear();
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(getResources().getString(R.string.api_user),getResources().getString(R.string.api_password));
        String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_clubs_members);
        requestAddress = requestAddress + ApiUtils.PARAMS_START + "clubId=" + mClub.getId();
        Log.d(TAG, "getConfirmedUser: created request" + requestAddress);
        client.get(requestAddress,new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                List<UserModel> userList = UserModel.fromJsonUserList(response,false);
                for (UserModel user : userList){
                    mMembersList.add(user.getName());
                }
                setArrayAdapterToListView();
                mSwipeRefreshLayout.setRefreshing(false);
                Log.d(TAG, "onSuccess: Successfully retrieved list of members");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                mSwipeRefreshLayout.setRefreshing(false);
                Log.e(TAG, "onFailure: There is an error while retrieving list of members",throwable);
            }
        });
    }

    private void getOwnerUserName(){
        Log.d(TAG, "getOwnerUserName: called");
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(getResources().getString(R.string.api_user),getResources().getString(R.string.api_password));
        String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_users);
        requestAddress = requestAddress + "/" + mClub.getOwner();
        Log.d(TAG, "getOwnerUserName: created request" + requestAddress);
        client.get(requestAddress,new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                UserModel creator = UserModel.fromJsonUser(response,false);
                mOwner.setText(creator.getName());
                Log.d(TAG, "onSuccess: Successfully retrieved club owner");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                Log.e(TAG, "onFailure: There is an error while retrieving club owner",throwable);
            }
        });
    }
//
//    private View.OnClickListener showLocationButtonListener = new View.OnClickListener() {
//        @Override
//        public void onClick(View view) {
//            Intent showLocation = new Intent(ClubDetailActivity.this, MapsActivity.class);
//            showLocation.putExtra("Event",mEvent);
//            startActivity(showLocation);
//        }
//    };
//    private View.OnClickListener conversationClickedListener = new View.OnClickListener() {
//        @Override
//        public void onClick(View view) {
//            Log.d(TAG, "onClick: clicked on conversation button. For event " + mEvent.getId());
//            Intent newCityIntent = new Intent(ClubDetailActivity.this, ConversationListActivity.class);
//            newCityIntent.putExtra("Event",mEvent);
//            startActivity(newCityIntent);
//        }
//    };
//    private View.OnClickListener detailsButtonClickedListener = new View.OnClickListener() {
//        @Override
//        public void onClick(View view) {
//            mDialog.show();
//        }
//    };
//
//    private View.OnClickListener confirmedButtonClickedListener = new View.OnClickListener() {
//        @Override
//        public void onClick(View view) {
//            try {
//                mSwipeRefreshLayout.setRefreshing(true);
//                AsyncHttpClient client = new AsyncHttpClient();
//                client.setBasicAuth(getResources().getString(R.string.api_user), getResources().getString(R.string.api_password));
//                String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_confirmation_address);
//                if (membershipId > 0){
//                    requestAddress = requestAddress + "/" + membershipId;
//                    client.delete(requestAddress, new AsyncHttpResponseHandler() {
//                        @Override
//                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
//                            Log.d(TAG, "onSuccess: successfully delete confirmation");
//                            membershipId=0;
//                            mSwipeRefreshLayout.setRefreshing(false);
//                            Toast.makeText(getBaseContext(),"Successfully cancel confirmation",Toast.LENGTH_SHORT).show();
//                            refreshData(FirebaseAuth.getInstance().getCurrentUser().getUid(),mEvent.getId());
//                        }
//
//                        @Override
//                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
//                            Log.e(TAG, "onFailure: error during deleting confirmation " +responseBody.toString(), error);
//                            mSwipeRefreshLayout.setRefreshing(false);
//                            Toast.makeText(getApplicationContext(),"Error while canceling confirmation!",Toast.LENGTH_SHORT).show();
//                        }
//                    });
//                } else {
//                    JSONObject params = new JSONObject();
//                    params.put("id", 0);
//                    params.put("userUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
//                    params.put("event", getResources().getString(R.string.api_event_address) + "/" + mEvent.getId());
//                    Log.d(TAG, "onClick: " + params.toString());
//                    StringEntity stringParams = new StringEntity(params.toString(),"UTF-8");
//                    client.post(getApplicationContext(), requestAddress, stringParams, "application/json;charset=UTF-8", new AsyncHttpResponseHandler() {
//                        @Override
//                        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
//                            Log.d(TAG, "onSuccess: Successfully add confirmtion");
//                            membershipId = getConfirmationIdFromHeaderResponse(headers);
//                            Toast.makeText(ClubDetailActivity.this,"Successfully confirmed!",Toast.LENGTH_SHORT).show();
//                            mSwipeRefreshLayout.setRefreshing(false);
//                            refreshData(FirebaseAuth.getInstance().getCurrentUser().getUid(),mEvent.getId());
//                        }
//
//                        @Override
//                        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
//                            Log.e(TAG, "onFailure: " + responseBody.toString(),error);
//                            mSwipeRefreshLayout.setRefreshing(false);
//                            Toast.makeText(getApplicationContext(),"Error while confirmed!",Toast.LENGTH_SHORT).show();
//                        }
//                    });
//                }
//
//            } catch (Exception e){
//                Log.e(TAG, "onClick: Error during confirmation events",e);
//            }
//        }
//    };
//
    private void setArrayAdapterToListView(){
        ArrayAdapter arrayAdapter = new ArrayAdapter(getApplicationContext(),android.R.layout.simple_list_item_1,mMembersList){
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
//
    @Override
    protected void onPostResume() {
        super.onPostResume();
        refreshData(FirebaseAuth.getInstance().getCurrentUser().getUid(),mClub.getId());
        setTexts();
    }
//
//    private void setDialogMessage(){
//        String eventDetails = mEvent.getDetails();
//        if (eventDetails == null){
//            eventDetails = "";
//        }
//        AlertDialog.Builder builder = new AlertDialog.Builder(this).setMessage(eventDetails);
//        builder.setNegativeButton(R.string.close,null);
//
//        mDialog = builder.create();
//    }
//
//    public int getConfirmationIdFromHeaderResponse(Header[] headers){
//        int id = 0;
//        for (int i=0 ; i<headers.length; i++ ){
//            if(headers[i].getName().equals("Location")){
//                String address = headers[i].getValue();
//                id = Integer.valueOf(address.substring(address.lastIndexOf("/")+1));
//                return id;
//            }
//            Header header = headers[i];
//            System.out.println(header.getName());
//        }
//        return id;
//    }
//
//    private View.OnClickListener showRouteClickedListener = new View.OnClickListener() {
//        @Override
//        public void onClick(View view) {
//            if (URLUtil.isValidUrl(mEvent.getRouteLink())){
//                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mEvent.getRouteLink()));
//                startActivity(intent);
//            } else {
//                Toast.makeText(ClubDetailActivity.this,"Provided link is not viliad!",Toast.LENGTH_SHORT).show();
//            }
//        }
//    };
}
