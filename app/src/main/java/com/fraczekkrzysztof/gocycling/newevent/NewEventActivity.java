package com.fraczekkrzysztof.gocycling.newevent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import com.fraczekkrzysztof.gocycling.MapsActivity;
import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.event.EventListActivity;
import com.fraczekkrzysztof.gocycling.model.EventModel;
import com.fraczekkrzysztof.gocycling.myevents.MyEventsLists;
import com.fraczekkrzysztof.gocycling.utils.DateUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.FirebaseAuth;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class NewEventActivity extends AppCompatActivity {

    private static final String TAG = "NewEventActivity";
    private EditText mEditTextName;
    private EditText mEditTextPlace;
    private EditText mEditTextDate;
    private EditText mEditTextDetails;
    private Button mAddButton;
    private EventModel mEventToEdit;
    private String mode;
    private double latitude = -999;
    private double longtitude = -999;

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

        mEditTextPlace.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b){
                    hideSoftInput(view);
                    startActivityForMapResult();
                }
            }
        });

        mEditTextPlace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideSoftInput(view);
                startActivityForMapResult();
            }
        });
        mEditTextDate.setInputType(InputType.TYPE_NULL);

        mEditTextDate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b){
                    hideSoftInput(view);
                    showDateTimeDialog(mEditTextDate);
                }
            }
        });
        mEditTextDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideSoftInput(view);
                showDateTimeDialog(mEditTextDate);
            }
        });
        mode = getIntent().getStringExtra("mode");
        mEventToEdit = (EventModel) getIntent().getSerializableExtra("EventToEdit");
        getSupportActionBar().setSubtitle("New event");
    }

    private void hideSoftInput(View view){
        InputMethodManager imm = (InputMethodManager)view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void setFieldForEdit(EventModel event){
        if (event == null) return;
        mEditTextName.setText(event.getName());
        mEditTextPlace.setText(event.getPlace());
        mEditTextDate.setText(DateUtils.sdfWithTime.format(event.getDateAndTime()));
        mEditTextDetails.setText(event.getDetails());
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
                if (mode != null && mode.equals("EDIT")){
                    updateEvent();
                } else{
                    createEvent();
                }

            }
        }
    };

    private boolean validateFields(){
        boolean check1 = validateEditTextIsEmpty(mEditTextName);
        boolean check2 = validateEditTextIsEmpty(mEditTextPlace);
        boolean check3 = validateEditTextIsEmpty(mEditTextDate);
        boolean check4 = (latitude != -999);
        boolean check5 = (longtitude != -999);
        return check1 && check2 && check3 && check4 & check5;
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
            params.put("createdBy", FirebaseAuth.getInstance().getCurrentUser().getUid());
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

    private void updateEvent(){
        try{
            AsyncHttpClient client = new AsyncHttpClient();
            client.setBasicAuth(getResources().getString(R.string.api_user), getResources().getString(R.string.api_password));
            String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_event_address)+"/"+mEventToEdit.getId();
            JSONObject params = new JSONObject();
            params.put("name", mEditTextName.getText().toString());
            params.put("place", mEditTextPlace.getText().toString());
            params.put("dateAndTime", DateUtils.sdfWithFullTime.format(DateUtils.sdfWithTime.parse(mEditTextDate.getText().toString())));
            params.put("createdBy", FirebaseAuth.getInstance().getCurrentUser().getUid());
            params.put("details", mEditTextDetails.getText().toString());
            Log.d(TAG, "onClick: " + params.toString());
            StringEntity stringParams = new StringEntity(params.toString(),"UTF-8");
            client.put(getApplicationContext(), requestAddress, stringParams, "application/json;charset=UTF-8", new TextHttpResponseHandler() {
                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    Log.e(TAG, "onFailure: error during updating event " + responseString,throwable );
                    Toast.makeText(getBaseContext(),"Error during updating event",Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, String responseString) {
                    Toast.makeText(getBaseContext(),"Successfully updated event",Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getBaseContext(), MyEventsLists.class);
                    startActivity(intent);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "createEvent: error during creating event", e);
        }
    }

    private void setTextForButton(String text){
        mAddButton.setText(text);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if(mode != null && mode.equals("EDIT")){
            setFieldForEdit(mEventToEdit);
            setTextForButton("UPDATE");
        } else {
//            mEditTextName.setText("");
//            mEditTextPlace.setText("");
//            mEditTextDate.setText("");
//            latitude = -999;
//            longtitude = -999;
//            setTextForButton("CREATE");
        }
    }

    private void startActivityForMapResult(){
        Places.initialize(getApplicationContext(),getString(R.string.google_maps_key));
        PlacesClient placesClient = Places.createClient(getApplicationContext());
        List<Place.Field> fields = Arrays.asList(Place.Field.ID,Place.Field.NAME,Place.Field.ADDRESS,Place.Field.LAT_LNG);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN,fields).build(NewEventActivity.this);
        startActivityForResult(intent,123);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case 123:
                try {
                    mEditTextPlace.setText("");
                    latitude = -999;
                    longtitude = -999;
                    final Place place = Autocomplete.getPlaceFromIntent(data);
                    final Dialog dialog = new Dialog(this);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    /////make map clear
                    dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    dialog.setContentView(R.layout.mapdialog);////your custom content
                    MapView mMapView = (MapView) dialog.findViewById(R.id.mapdialog_view);
                    MapsInitializer.initialize(this);
                    mMapView.onCreate(dialog.onSaveInstanceState());
                    mMapView.onResume();

                    mMapView.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(final GoogleMap googleMap) {
                            LatLng posisiabsen = place.getLatLng(); ////your lat lng
                            googleMap.addMarker(new MarkerOptions().position(posisiabsen).title(place.getName() + " " + place.getAddress()));
                            googleMap.moveCamera(CameraUpdateFactory.newLatLng(posisiabsen));
                            googleMap.getUiSettings().setZoomControlsEnabled(true);
                            googleMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
                        }
                    });

                    dialog.show();

                    Button cancelButton = (Button)dialog.findViewById(R.id.map_dialog_button_cancel);
                    cancelButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog.dismiss();
                        }
                    });
                    Button okButton = (Button)dialog.findViewById(R.id.map_dialog_button_ok);
                    okButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            setFieldForPlace(place);
                            dialog.dismiss();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private void setFieldForPlace(Place place){
        latitude = place.getLatLng().latitude;
        longtitude = place.getLatLng().longitude;
        mEditTextPlace.setText(place.getName() + " " + place.getAddress());
    }


}


