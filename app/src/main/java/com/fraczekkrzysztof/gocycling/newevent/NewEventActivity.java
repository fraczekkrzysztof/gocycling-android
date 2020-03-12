package com.fraczekkrzysztof.gocycling.newevent;

import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.event.EventListActivity;
import com.fraczekkrzysztof.gocycling.utils.DateUtils;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONObject;

import java.util.Calendar;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class NewEventActivity extends AppCompatActivity {

    private static final String TAG = "NewEventActivity";
    private EditText mEditTextName;
    private EditText mEditTextPlace;
    private EditText mEditTextDate;
    private EditText mEditTextDetails;
    private Button mAddButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_event);
        mAddButton = findViewById(R.id.new_event_create);
        mAddButton.setOnClickListener(addButtonListener);
        mEditTextName = findViewById(R.id.new_event_name);
        mEditTextPlace = findViewById(R.id.new_event_place);
        mEditTextDate = findViewById(R.id.new_event_date);
        mEditTextDetails = findViewById(R.id.new_events_detail);

        mEditTextDate.setInputType(InputType.TYPE_NULL);

        mEditTextDate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b){
                    InputMethodManager imm = (InputMethodManager)view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                    showDateTimeDialog(mEditTextDate);
                }
            }
        });
        getSupportActionBar().setSubtitle("New event");
    }

    private void showDateTimeDialog(final EditText date_time_in) {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                TimePickerDialog.OnTimeSetListener timeSetListener = new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);

                        date_time_in.setText(DateUtils.sdfWithTime.format(calendar.getTime()));
                    }
                };

                new TimePickerDialog(NewEventActivity.this, timeSetListener, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
            }
        };
        new DatePickerDialog(NewEventActivity.this, dateSetListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private View.OnClickListener addButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (validateFields()){
                createEvent();
            }
        }
    };

    private boolean validateFields(){
        boolean check1 = validateEditTextIsEmpty(mEditTextName);
        boolean check2 = validateEditTextIsEmpty(mEditTextPlace);
        boolean check3 = validateEditTextIsEmpty(mEditTextDate);
        return check1 && check2 && check3;
    }

    private boolean validateEditTextIsEmpty(EditText field){
        if (field.getText().toString().isEmpty()){
            field.setError("Required");
            return false;
        } else {
            field.setError(null);
            return true;
        }
    }
    
    private void createEvent(){
        try{
            AsyncHttpClient client = new AsyncHttpClient();
            client.setBasicAuth(getResources().getString(R.string.api_user), getResources().getString(R.string.api_password));
            String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_event_address);
            JSONObject params = new JSONObject();
            params.put("name", mEditTextName.getText().toString());
            params.put("place", mEditTextPlace.getText().toString());
            params.put("dateAndTime", DateUtils.sdfWithFullTime.format(DateUtils.sdfWithTime.parse(mEditTextDate.getText().toString())));
            params.put("details", mEditTextDetails.getText().toString());
            Log.d(TAG, "onClick: " + params.toString());
            StringEntity stringParams = new StringEntity(params.toString(),"UTF-8");
            client.post(getApplicationContext(), requestAddress, stringParams, "application/json;charset=UTF-8", new TextHttpResponseHandler() {
                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    Log.e(TAG, "onFailure: error during creating event " + responseString,throwable );
                    Toast.makeText(getBaseContext(),"Error during creating event",Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, String responseString) {
                    Toast.makeText(getBaseContext(),"Successfully create event",Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getBaseContext(), EventListActivity.class);
                    startActivity(intent);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "createEvent: error during creating event", e);
        }
        
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        mEditTextName.setText("");
        mEditTextPlace.setText("");
        mEditTextDate.setText("");
    }
}
