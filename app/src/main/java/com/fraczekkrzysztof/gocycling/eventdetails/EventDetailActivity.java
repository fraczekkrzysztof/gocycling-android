package com.fraczekkrzysztof.gocycling.eventdetails;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.apiutils.ApiUtils;
import com.fraczekkrzysztof.gocycling.apiutils.SortTypes;
import com.fraczekkrzysztof.gocycling.model.ConfirmationModel;
import com.fraczekkrzysztof.gocycling.model.EventModel;
import com.fraczekkrzysztof.gocycling.myconfirmations.MyConfirmationsLists;
import com.fraczekkrzysztof.gocycling.utils.DateUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONObject;

import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class EventDetailActivity extends AppCompatActivity {

    private static final String TAG = "EventDetailActivity";
    TextView mTitle;
    TextView mWhere;
    TextView mWhen;
    EventModel mEvent;
    Button mConfirmButton;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_details);
        mTitle = findViewById(R.id.event_detail_title);
        mWhere = findViewById(R.id.event_detail_where);
        mWhen = findViewById(R.id.event_detail_when);
        mConfirmButton = findViewById(R.id.event_confirm_button);
        mConfirmButton.setOnClickListener(confirmedButtonClickedListener);
        mEvent = (EventModel) getIntent().getSerializableExtra("Event");
        Log.d(TAG, "onCreate: started!");
        getInformationAboutUserConfirmation(FirebaseAuth.getInstance().getCurrentUser().getUid(),mEvent.getId());
        setTexts();
    }

    private void setTexts(){
        mTitle.setText(mEvent.getName());
        mWhen.setText(DateUtils.sdfWithTime.format(mEvent.getDateAndTime()));
        mWhere.setText(mEvent.getPlace());

    }

    private boolean getInformationAboutUserConfirmation(String userUid, long eventId){
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
                toReturn[0] = (listOfConfirmation.size()>0);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
                Log.e(TAG, "onFailure: error on checking is this event confirmed", throwable);
            }
        });
        return toReturn[0];
    }

    private View.OnClickListener confirmedButtonClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            try {
                AsyncHttpClient client = new AsyncHttpClient();
                client.setBasicAuth(getResources().getString(R.string.api_user), getResources().getString(R.string.api_password));
                String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_confirmation_address);
                JSONObject params = new JSONObject();
                params.put("id", 0);
                params.put("userUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
                params.put("event", getResources().getString(R.string.api_event_address) + "/" + mEvent.getId());
                Log.d(TAG, "onClick: " + params.toString());
                StringEntity stringParams = new StringEntity(params.toString());
                client.post(getApplicationContext(), requestAddress, stringParams, "application/json", new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        Log.d(TAG, "onSuccess: Successfully add confirmtion");
                        Intent intent = new Intent(getApplicationContext(), MyConfirmationsLists.class);
                        startActivity(intent);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        Log.e(TAG, "onFailure: " + responseBody.toString(),error  );
                        Toast.makeText(getApplicationContext(),"Error while confirmed!",Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e){

            }
        }
    };
}
