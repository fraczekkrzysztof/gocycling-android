package com.fraczekkrzysztof.gocycling.newclub;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.fraczekkrzysztof.gocycling.clubdetails.ClubDetailActivity;
import com.fraczekkrzysztof.gocycling.httpclient.GoCyclingHttpClientHelper;
import com.fraczekkrzysztof.gocycling.model.v2.club.ClubDto;
import com.fraczekkrzysztof.gocycling.model.v2.club.ClubResponse;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NewClubActivity extends AppCompatActivity {

    private final Gson gson = new Gson();
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


    private View.OnClickListener addButtonListener = view -> {
        if (validateFields()) {
            createClub();
        }
    };

    private boolean validateFields() {
        boolean check1 = validateEditTextIsEmpty(mEditTextName);
        boolean check2 = validateEditTextIsEmpty(mEditTextLocation);
        boolean check4 = (latitude != -999);
        boolean check5 = (longtitude != -999);
        return check1 && check2 && check4 && check5;
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

    private void createClub() {
        ClubDto clubToCreate = ClubDto.builder()
                .name(mEditTextName.getText().toString())
                .location(mEditTextLocation.getText().toString())
                .latitude(latitude)
                .longitude(longtitude)
                .ownerId(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .details(mEditTextDetails.getText().toString())
                .privateMode(false)
                .build();
        String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_clubs);
        HttpUrl url = HttpUrl.parse(requestAddress)
                .newBuilder().build();
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(gson.toJson(clubToCreate), MediaType.parse("application/json; charset=utf-8")))
                .build();
        OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(getResources());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "onFailure: error during creating club", e);
                backgroundThreadShortToast(NewClubActivity.this, "Error during creating club");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                backgroundThreadShortToast(NewClubActivity.this, "Successfully created club");
                ClubResponse apiResponse = gson.fromJson(response.body().string(), ClubResponse.class);
                Intent intent = new Intent(getBaseContext(), ClubDetailActivity.class);
                intent.putExtra("clubId", apiResponse.getClub().getId());
                startActivity(intent);
            }
        });
    }

    public static void backgroundThreadShortToast(final Context context,
                                                  final String msg) {
        if (context != null && msg != null) {
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show());
        }
    }


    private void startActivityForMapResult() {
        Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        Places.createClient(getApplicationContext());
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(NewClubActivity.this);
        startActivityForResult(intent, 123);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (123 == requestCode && data != null) {
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

                mMapView.getMapAsync(googleMap -> {
                    LatLng posisiabsen = place.getLatLng(); ////your lat lng
                    googleMap.addMarker(new MarkerOptions().position(posisiabsen).title(place.getAddress()));
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 16.0f));
                    googleMap.getUiSettings().setZoomControlsEnabled(true);
                });

                dialog.show();

                Button cancelButton = (Button) dialog.findViewById(R.id.map_dialog_button_cancel);
                cancelButton.setOnClickListener(view -> dialog.dismiss());
                Button okButton = (Button) dialog.findViewById(R.id.map_dialog_button_ok);
                okButton.setOnClickListener(view -> {
                    setFieldForPlace(place);
                    dialog.dismiss();
                });
            } catch (Exception e) {
                Log.e(TAG, "onActivityResult: Error during getting location", e);
            }
        }
    }

    private void setFieldForPlace(Place place) {
        latitude = place.getLatLng().latitude;
        longtitude = place.getLatLng().longitude;
        mEditTextLocation.setText(place.getAddress());
    }


}


