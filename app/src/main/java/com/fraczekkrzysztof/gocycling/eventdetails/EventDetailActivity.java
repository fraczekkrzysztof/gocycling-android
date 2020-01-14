package com.fraczekkrzysztof.gocycling.eventdetails;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.event.EventModel;
import com.fraczekkrzysztof.gocycling.utils.DateUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONObject;

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
        setTexts();
    }

    private void setTexts(){
        mTitle.setText(mEvent.getName());
        mWhen.setText(DateUtils.sdfWithTime.format(mEvent.getDateAndTime()));
        mWhere.setText(mEvent.getPlace());

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
                        //TODO create new Intent
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        Log.e(TAG, "onFailure: ",error );
                        Toast.makeText(getApplicationContext(),"Error while confirmed!",Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e){

            }
        }
    };
}
