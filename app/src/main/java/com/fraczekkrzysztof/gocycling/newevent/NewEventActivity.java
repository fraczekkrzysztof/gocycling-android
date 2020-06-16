package com.fraczekkrzysztof.gocycling.newevent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.apiutils.ApiUtils;
import com.fraczekkrzysztof.gocycling.event.EventListActivity;
import com.fraczekkrzysztof.gocycling.model.ClubModel;
import com.fraczekkrzysztof.gocycling.model.EventModel;
import com.fraczekkrzysztof.gocycling.model.RouteModel;
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
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.TextHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
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
    private EditText mEditTextRoute;
    private EditText mEditTextDetails;
    private ImageButton mRouteButton;
    private Spinner mClubSpinner;
    private Button mAddButton;
    private SwipeRefreshLayout mNewEventSwipe;
    private EventModel mEventToEdit;
    private String mode;
    private List<ClubModel> mListOfClubs = new ArrayList<>();
    private long mSelectedClubId = -1;
    AlertDialog mDialog;
    private double latitude = -999;
    private double longtitude = -999;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_event);
        mAddButton = findViewById(R.id.new_event_create);
        mAddButton.setOnClickListener(addButtonListener);
        mRouteButton = findViewById(R.id.new_event_route_button);
        mRouteButton.setOnClickListener(routeClickedListener);
        mEditTextName = findViewById(R.id.new_event_name);
        mEditTextPlace = findViewById(R.id.new_event_place);
        mEditTextDate = findViewById(R.id.new_event_date);
        mEditTextDetails = findViewById(R.id.new_events_detail);
        mEditTextRoute = findViewById(R.id.new_event_route);
        mClubSpinner = findViewById(R.id.new_event_club_spinner);
        mNewEventSwipe = findViewById(R.id.new_event_swipe_layout);
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
        if (mEventToEdit != null){
            setFieldForEdit(mEventToEdit);
            setTextForButton("UPDATE");
            getSingleClubForSpinnerAndBlockIt(mEventToEdit);
        } else {
            getClubsForSpinner();
        }

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
        mEditTextRoute.setText(event.getRouteLink());
        latitude = event.getLatitude();
        longtitude = event.getLongitude();
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
        boolean check6 = validateClubSelected(mSelectedClubId);
        return check1 && check2 && check3 && check4 & check5 && check6;
    }

    private boolean validateClubSelected(long idOfSelectedClub){
        if (idOfSelectedClub == -1){
            Toast.makeText(NewEventActivity.this,"You have to select club",Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
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
            params.put("latitude",latitude);
            params.put("longitude",longtitude);
            params.put("dateAndTime", DateUtils.sdfWithFullTime.format(DateUtils.sdfWithTime.parse(mEditTextDate.getText().toString())));
            params.put("routeLink",mEditTextRoute.getText());
            params.put("createdBy", FirebaseAuth.getInstance().getCurrentUser().getUid());
            params.put("details", mEditTextDetails.getText().toString());
            params.put("club","api/clubs/"+mSelectedClubId);
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
            params.put("latitude",latitude);
            params.put("longitude",longtitude);
            params.put("dateAndTime", DateUtils.sdfWithFullTime.format(DateUtils.sdfWithTime.parse(mEditTextDate.getText().toString())));
            params.put("routeLink",mEditTextRoute.getText());
            params.put("createdBy", FirebaseAuth.getInstance().getCurrentUser().getUid());
            params.put("details", mEditTextDetails.getText().toString());
            params.put("club","api/clubs/"+mSelectedClubId);
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
                            googleMap.addMarker(new MarkerOptions().position(posisiabsen).title(place.getAddress()));
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(),16.0f));
                            googleMap.getUiSettings().setZoomControlsEnabled(true);
//                            googleMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);

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
        mEditTextPlace.setText(place.getAddress());
    }

    private View.OnClickListener routeClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            getListOfRoutes();
        }
    };

    private void getListOfRoutes(){
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(getResources().getString(R.string.api_user), getResources().getString(R.string.api_password));
        String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_external_routes_list);
        requestAddress = requestAddress + ApiUtils.PARAMS_START + "userUid=" + FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d(TAG, "getListOfRoutes: request " + requestAddress);
        client.get(requestAddress,new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.d(TAG, "onSuccess: successfuly received list of routes" );
                showDialogForResponse(RouteModel.fromJsonArray(response));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Log.e(TAG, "onFailure: error diring receiving list of routes", throwable);
                if (errorResponse != null){
                    Log.e(TAG, "onFailure: " + errorResponse);
                }
                Toast.makeText(NewEventActivity.this,"Error during receiving list of routes",Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDialogForResponse(final List<RouteModel> listOfRoutes){
        AlertDialog.Builder builder = new AlertDialog.Builder(NewEventActivity.this);
        builder.setNegativeButton(R.string.cancel, null);
        RouteAdapter adapter = new RouteAdapter(getApplicationContext(),listOfRoutes);
        builder.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mEditTextRoute.setText(listOfRoutes.get(i).getLink());
                mDialog.dismiss();
            }
        });
        mDialog = builder.create();
        mDialog.show();
    }

    private void getSingleClubForSpinnerAndBlockIt(EventModel event){
        mNewEventSwipe.setRefreshing(true);
        Log.d(TAG, "getSingleClubForSpinnerAndBlockIt: called");
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(getResources().getString(R.string.api_user),getResources().getString(R.string.api_password));
        String requestAddress = getResources().getString(R.string.api_base_address);
        requestAddress = requestAddress + getResources().getString(R.string.api_club_for_event)+ ApiUtils.PARAMS_START + "eventId="+event.getId();

        Log.d(TAG, "getSingleClubForSpinnerAndBlockIt: created request " + requestAddress);
        client.get(requestAddress, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.d(TAG, "onSuccess: response successfully received");
                List<ClubModel> listClubs = ClubModel.fromJson(response);
                addSingleClubForSpinnerAndBlockIt(listClubs.get(0));
                mNewEventSwipe.setRefreshing(false);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Toast.makeText(NewEventActivity.this,"There is an error. Please try again!",Toast.LENGTH_SHORT).show();
                Log.e(TAG,"Error during retrieving signle club for event to edit", throwable);
                if (errorResponse != null){
                    Log.d(TAG, errorResponse.toString());
                }
                mNewEventSwipe.setRefreshing(false);
            }
        });
    }

    private void addSingleClubForSpinnerAndBlockIt(ClubModel clubModel) {
        mSelectedClubId = clubModel.getId();
        ClubAdapter clubAdapter = new ClubAdapter(NewEventActivity.this,R.layout.club_list_item,Arrays.asList(clubModel));
        mClubSpinner.setAdapter(clubAdapter);
        mClubSpinner.setEnabled(false);
    }

    private void getClubsForSpinner(){
        mNewEventSwipe.setRefreshing(true);
        Log.d(TAG, "getClubs: called");
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(getResources().getString(R.string.api_user),getResources().getString(R.string.api_password));
        String requestAddress = getResources().getString(R.string.api_base_address);
        requestAddress = requestAddress + getResources().getString(R.string.api_clubs_which_user_is_member)+ ApiUtils.PARAMS_START + "userUid="+FirebaseAuth.getInstance().getCurrentUser().getUid();
        requestAddress = requestAddress + ApiUtils.PARAMS_AND + ApiUtils.getSizeToRequest(1000);

        Log.d(TAG, "getClubs: created request " + requestAddress);
        client.get(requestAddress, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.d(TAG, "onSuccess: response successfully received");
                List<ClubModel> listClubs = ClubModel.fromJson(response);
                addClubsToSpinner(listClubs);
                mNewEventSwipe.setRefreshing(false);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Toast.makeText(NewEventActivity.this,"There is an error. Please try again!",Toast.LENGTH_SHORT).show();
                Log.e(TAG,"Error during retrieving club list", throwable);
                if (errorResponse != null){
                    Log.d(TAG, errorResponse.toString());
                }
                mNewEventSwipe.setRefreshing(false);
            }
        });
    }

    private void addClubsToSpinner(List<ClubModel> listClubs) {
        mListOfClubs = listClubs;
        List<ClubModel> listOfClubsWithSelectOne = new ArrayList<>();
        ClubModel fakeClubModel = new ClubModel();
        fakeClubModel.setId(-1);
        fakeClubModel.setName("--Pick a club--");
        listOfClubsWithSelectOne.add(fakeClubModel);
        listOfClubsWithSelectOne.addAll(listClubs);
        ClubAdapter clubAdapter = new ClubAdapter(NewEventActivity.this,R.layout.club_list_item,listOfClubsWithSelectOne);
        mClubSpinner.setAdapter(clubAdapter);
        mClubSpinner.setOnItemSelectedListener(spinnerItemSelectedListener);
    }

    private AdapterView.OnItemSelectedListener spinnerItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            //i-1 because there is a fake one on the top of list
            if(i>0) {
                mSelectedClubId = mListOfClubs.get(i - 1).getId();
            } else {
                mSelectedClubId = -1;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
            mSelectedClubId = -1;
        }
    };



}


