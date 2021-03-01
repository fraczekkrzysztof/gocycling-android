package com.fraczekkrzysztof.gocycling.newevent;

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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.adapters.ClubAdapter;
import com.fraczekkrzysztof.gocycling.adapters.RouteAdapter;
import com.fraczekkrzysztof.gocycling.event.EventListActivity;
import com.fraczekkrzysztof.gocycling.httpclient.GoCyclingHttpClientHelper;
import com.fraczekkrzysztof.gocycling.model.v2.club.ClubDto;
import com.fraczekkrzysztof.gocycling.model.v2.club.ClubListResponse;
import com.fraczekkrzysztof.gocycling.model.v2.event.EventDto;
import com.fraczekkrzysztof.gocycling.model.v2.route.RouteDto;
import com.fraczekkrzysztof.gocycling.model.v2.route.RouteListResponseDto;
import com.fraczekkrzysztof.gocycling.utils.DateUtils;
import com.fraczekkrzysztof.gocycling.utils.ToastUtils;
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
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import lombok.SneakyThrows;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NewEventActivity extends AppCompatActivity {

    private static final String TAG = "NewEventActivity";
    private final Gson gson = new Gson();

    private EditText mEditTextName;
    private EditText mEditTextPlace;
    private EditText mEditTextDate;
    private EditText mEditTextRoute;
    private EditText mEditTextDetails;
    private ImageButton mRouteButton;
    private Spinner mClubSpinner;
    private Button mAddButton;
    private SwipeRefreshLayout mNewEventSwipe;
    private EventDto mEventToEdit;
    private AlertDialog mDialog;
    private double latitude = -999;
    private double longitude = -999;

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
        setupEditTextForDate();
        setupEditTextForPlace();
        mEventToEdit = (EventDto) getIntent().getSerializableExtra("EventToEdit");
        if (mEventToEdit != null) {
            getSupportActionBar().setSubtitle("Update event");
            setFieldForEdit(mEventToEdit);
            setTextForButton("UPDATE");
            addSingleClubForSpinnerAndBlockIt(mEventToEdit.getClubId(), mEventToEdit.getClubName());
        } else {
            getSupportActionBar().setSubtitle("New event");
            getClubsForSpinner();
        }

    }

    private void setupEditTextForPlace() {
        mEditTextPlace.setOnFocusChangeListener((view, b) -> {
            if (b) {
                hideSoftInput(view);
                startActivityForMapResult();
            }
        });
        mEditTextPlace.setOnClickListener(view -> {
            hideSoftInput(view);
            startActivityForMapResult();
        });
    }

    private void setupEditTextForDate() {
        mEditTextDate.setInputType(InputType.TYPE_NULL);
        mEditTextDate.setOnFocusChangeListener((view, b) -> {
            if (b) {
                hideSoftInput(view);
                showDateTimeDialog(mEditTextDate);
            }
        });
        mEditTextDate.setOnClickListener(view -> {
            hideSoftInput(view);
            showDateTimeDialog(mEditTextDate);
        });
    }


    private void hideSoftInput(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @SneakyThrows
    private void setFieldForEdit(EventDto event) {
        if (event == null) return;
        mEditTextName.setText(event.getName());
        mEditTextPlace.setText(event.getPlace());
        mEditTextDate.setText(DateUtils.formatDefaultDateToDateWithTime(event.getDateAndTime()));
        mEditTextDetails.setText(event.getDetails());
        mEditTextRoute.setText(event.getRouteLink());
        latitude = event.getLatitude();
        longitude = event.getLongitude();
    }

    private void addSingleClubForSpinnerAndBlockIt(long clubId, String clubName) {
        ClubAdapter clubAdapter = new ClubAdapter(NewEventActivity.this, R.layout.club_list_item, Arrays.asList(ClubDto.builder().id(clubId).name(clubName).build()));
        mClubSpinner.setAdapter(clubAdapter);
        mClubSpinner.setEnabled(false);
    }

    private void showDateTimeDialog(final EditText dateTimeIn) {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            TimePickerDialog.OnTimeSetListener timeSetListener = (timeView, hourOfDay, minute) -> {
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                dateTimeIn.setText(DateUtils.formatDateToDateWithTime(calendar.getTime()));

                };
                new TimePickerDialog(NewEventActivity.this, timeSetListener, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        };
        new DatePickerDialog(NewEventActivity.this, dateSetListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private View.OnClickListener addButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (validateFields()) {
                createEvent(mEventToEdit != null);
            }
        }

        private boolean validateFields() {
            boolean check1 = validateEditTextIsEmpty(mEditTextName);
            boolean check2 = validateEditTextIsEmpty(mEditTextPlace);
            boolean check3 = validateEditTextIsEmpty(mEditTextDate);
            boolean check4 = (latitude != -999);
            boolean check5 = (longitude != -999);
            boolean check6 = validateClubSelected((ClubDto) mClubSpinner.getSelectedItem());
            return check1 && check2 && check3 && check4 && check5 && check6;
        }

        private boolean validateClubSelected(ClubDto selection) {
            if (selection.getId() != -1L) return true;
            Toast.makeText(NewEventActivity.this, "You have to select club", Toast.LENGTH_SHORT).show();
            return false;
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
    };

    private void createEvent(boolean isUpdate) {
        Request request = prepareCreateEventRequest(isUpdate);
        OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(getResources());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "onFailure: error during creating event", e);
                ToastUtils.backgroundThreadShortToast(NewEventActivity.this, "Error during creating event");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    ToastUtils.backgroundThreadShortToast(NewEventActivity.this, "Successfully create event");
                    runOnUiThread(() -> {
                        Intent intent = new Intent(NewEventActivity.this, EventListActivity.class);
                        startActivity(intent);
                    });
                    return;
                }
                Log.w(TAG, String.format("onResponse: Response received, but %s status.", response.code()));
                ToastUtils.backgroundThreadShortToast(NewEventActivity.this, "Error during creating event");
            }
        });

    }

    @SneakyThrows
    private Request prepareCreateEventRequest(boolean isUpdate) {
        ClubDto selectedClub = (ClubDto) mClubSpinner.getSelectedItem();
        String requestAddress = getResources().getString(R.string.api_base_address)
                + String.format(getResources().getString(R.string.api_event_address), selectedClub.getId());

        EventDto event = EventDto.builder()
                .name(mEditTextName.getText().toString())
                .place(mEditTextPlace.getText().toString())
                .latitude(latitude)
                .longitude(longitude)
                .dateAndTime(DateUtils.formatDateWithFullTimeToDefaultTime(mEditTextDate.getText().toString()))
                .routeLink(mEditTextRoute.getText().toString())
                .details(mEditTextDetails.getText().toString())
                .userId(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .build();

        Request.Builder requestBuilder = new Request.Builder();

        if (isUpdate) {
            return requestBuilder
                    .url(requestAddress + "/" + mEventToEdit.getId())
                    .put(RequestBody.create(gson.toJson(event), MediaType.parse("application/json; charset=utf-8")))
                    .build();
        }
        return requestBuilder
                .url(requestAddress)
                .post(RequestBody.create(gson.toJson(event), MediaType.parse("application/json; charset=utf-8")))
                .build();

    }

    private void setTextForButton(String text) {
        mAddButton.setText(text);
    }


    private void startActivityForMapResult() {
        Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        Places.createClient(getApplicationContext());
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(NewEventActivity.this);
        startActivityForResult(intent, 123);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 123 && data != null) {
            mEditTextPlace.setText("");
            latitude = -999;
            longitude = -999;
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
        }
    }

    private void setFieldForPlace(Place place) {
        latitude = place.getLatLng().latitude;
        longitude = place.getLatLng().longitude;
        mEditTextPlace.setText(place.getAddress());
    }

    private View.OnClickListener routeClickedListener = view -> getListOfRoutes();


    private void getListOfRoutes() {
        String requestAddress = getResources().getString(R.string.api_base_address) +
                String.format(getResources().getString(R.string.api_external_routes_list), FirebaseAuth.getInstance().getCurrentUser().getUid());
        Request request = new Request.Builder()
                .url(requestAddress)
                .build();
        OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(getResources());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG, "getListOfRoutes onFailure: error during receiving list of routes", e);
                ToastUtils.backgroundThreadShortToast(NewEventActivity.this, "Error during receiving list of routes");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "onResponse: Successfully received response with ReouteList");
                    Reader receivedResponse = response.body().charStream();
                    RouteListResponseDto apiResponse = gson.fromJson(receivedResponse, RouteListResponseDto.class);
                    runOnUiThread(() -> showDialogForRouteListResponse(apiResponse.getRoutes()));
                }
                Log.w(TAG, String.format("getListOfRoutes onResponse: Response received, but not %s status.", response.code()));
            }
        });
    }

    private void showDialogForRouteListResponse(final List<RouteDto> listOfRoutes) {
        AlertDialog.Builder builder = new AlertDialog.Builder(NewEventActivity.this);
        builder.setNegativeButton(R.string.cancel, null);
        RouteAdapter adapter = new RouteAdapter(getApplicationContext(), listOfRoutes);
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

    private void getClubsForSpinner() {
        mNewEventSwipe.setRefreshing(true);
        Log.d(TAG, "getClubs: called");
        Request request = prepareRequest();
        OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(getResources());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Toast.makeText(NewEventActivity.this, "There is an error. Please try again!", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error during retrieving club list", e);
                mNewEventSwipe.setRefreshing(false);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "onResponse: Successfully received response with ClubList");
                    Reader receivedResponse = response.body().charStream();
                    ClubListResponse apiResponse = gson.fromJson(receivedResponse, ClubListResponse.class);
                    runOnUiThread(() -> {
                        addClubsToSpinner(apiResponse.getClubs());
                        mNewEventSwipe.setRefreshing(false);
                    });
                    return;
                }
                mNewEventSwipe.setRefreshing(false);
                Log.e(TAG, String.format("onResponse: Response received, but not %s status.", response.code()));
            }
        });
    }


    @NotNull
    private Request prepareRequest() {
        String url = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_clubs);
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        urlBuilder.addQueryParameter("userUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
        urlBuilder.addQueryParameter("limit", "50");
        urlBuilder.addQueryParameter("sort", "created,desc");
        return new Request.Builder()
                .url(urlBuilder.build().toString())
                .build();
    }

    private void addClubsToSpinner(List<ClubDto> listClubs) {
        List<ClubDto> listOfClubsWithSelectOne = new ArrayList<>();
        ClubDto fakeClubModel = ClubDto.builder().id(-1L).name("--Pick a club--").build();
        listOfClubsWithSelectOne.add(fakeClubModel);
        listOfClubsWithSelectOne.addAll(listClubs);
        ClubAdapter clubAdapter = new ClubAdapter(NewEventActivity.this, R.layout.club_list_item, listOfClubsWithSelectOne);
        mClubSpinner.setAdapter(clubAdapter);
    }
}


