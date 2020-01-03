package com.fraczekkrzysztof.gocycling.eventdetails;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.event.EventModel;
import com.fraczekkrzysztof.gocycling.utils.DateUtils;

public class EventDetailActivity extends AppCompatActivity {

    private static final String TAG = "EventDetailActivity";
    TextView mTitle;
    TextView mWhere;
    TextView mWhen;
    EventModel mEvent;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_details);
        mTitle = findViewById(R.id.event_detail_title);
        mWhere = findViewById(R.id.event_detail_where);
        mWhen = findViewById(R.id.event_detail_when);
        mEvent = (EventModel) getIntent().getSerializableExtra("Event");
        Log.d(TAG, "onCreate: started!");
        setTexts();
    }

    private void setTexts(){
        mTitle.setText(mEvent.getName());
        mWhen.setText(DateUtils.sdfWithTime.format(mEvent.getDateAndTime()));
        mWhere.setText(mEvent.getPlace());

    }
}
