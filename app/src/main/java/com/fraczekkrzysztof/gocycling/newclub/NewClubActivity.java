package com.fraczekkrzysztof.gocycling.newclub;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.clubs.ClubListActivity;
import com.fraczekkrzysztof.gocycling.event.EventListActivity;
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
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class NewClubActivity extends AppCompatActivity {

    private static final String TAG = "NewClubActivity";
    private EditText mEditTextName;
    private EditText mEditTextLocation;
    private EditText mEditTextDetails;
    private Button mAddButton;
    private double latitude = -999;
    private double longtitude = -999;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_club);
        mAddButton = findViewById(R.id.new_club_create);
        mAddButton.setOnClickListener(addButtonListener);
        mEditTextName = findViewById(R.id.new_club_name);
        mEditTextLocation = findViewById(R.id.new_club_location);
        mEditTextDetails = findViewById(R.id.new_club_detail);

        mEditTextLocation.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    hideSoftInput(view);
                    startActivityForMapResult();
                }
            }
        });

        mEditTextLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideSoftInput(view);
                startActivityForMapResult();
            }
        });

        getSupportActionBar().setSubtitle("New club");
    }


    private void hideSoftInput(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }


    private View.OnClickListener addButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (validateFields()) {
                createEvent();
            }
        }
    };

    private boolean validateFields() {
        boolean check1 = validateEditTextIsEmpty(mEditTextName);
        boolean check2 = validateEditTextIsEmpty(mEditTextLocation);
        boolean check4 = (latitude != -999);
        boolean check5 = (longtitude != -999);
        return check1 && check2 && check4 & check5;
    }

    private boolean validateEditTextIsEmpty(EditText field) {
        if (field.getText().toString().isEmpty()) {
            field.setError("Required");
            return false;
        } else {
            field.setError(null);
            return true;
        }
    }

    private void createEvent() {
        try {
            AsyncHttpClient client = new AsyncHttpClient();
            client.setBasicAuth(getResources().getString(R.string.api_user), getResources().getString(R.string.api_password));
            String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_clubs);
            JSONObject params = new JSONObject();
            params.put("name", mEditTextName.getText().toString());
            params.put("location",mEditTextLocation.getText().toString());
            params.put("latitude",latitude);
            params.put("longitude",longtitude);
            params.put("owner", FirebaseAuth.getInstance().getCurrentUser().getUid());
            params.put("details",mEditTextDetails.getText().toString());
            params.put("privateMode",false);
            Log.d(TAG, "onClick: " + params.toString());
            StringEntity stringParams = new StringEntity(params.toString(), "UTF-8");
            client.post(getApplicationContext(), requestAddress, stringParams, "application/json;charset=UTF-8", new TextHttpResponseHandler() {
                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    Log.e(TAG, "onFailure: error during creating club " + responseString, throwable);
                    Toast.makeText(getBaseContext(), "Error during creating club", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, String responseString) {
                    Toast.makeText(getBaseContext(), "Successfully create club", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getBaseContext(), ClubListActivity.class);
                    startActivity(intent);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "createEvent: error during creating club", e);
        }
    }


    private void startActivityForMapResult() {
        Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        PlacesClient placesClient = Places.createClient(getApplicationContext());
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(NewClubActivity.this);
        startActivityForResult(intent, 123);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 123:
                try {
                    mEditTextLocation.setText("");
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
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 16.0f));
                            googleMap.getUiSettings().setZoomControlsEnabled(true);
//                            googleMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
                        }
                    });

                    dialog.show();

                    Button cancelButton = (Button) dialog.findViewById(R.id.map_dialog_button_cancel);
                    cancelButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog.dismiss();
                        }
                    });
                    Button okButton = (Button) dialog.findViewById(R.id.map_dialog_button_ok);
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

    private void setFieldForPlace(Place place) {
        latitude = place.getLatLng().latitude;
        longtitude = place.getLatLng().longitude;
        mEditTextLocation.setText(place.getAddress());
    }
}


