package com.fraczekkrzysztof.gocycling.event;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.firebase.ui.auth.AuthUI;
import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.adapters.ClubAdapter;
import com.fraczekkrzysztof.gocycling.apiutils.ApiUtils;
import com.fraczekkrzysztof.gocycling.clubs.ClubListActivity;
import com.fraczekkrzysztof.gocycling.logging.LoggingActivity;
import com.fraczekkrzysztof.gocycling.model.ClubModel;
import com.fraczekkrzysztof.gocycling.model.EventModel;
import com.fraczekkrzysztof.gocycling.myaccount.MyAccount;
import com.fraczekkrzysztof.gocycling.myconfirmations.MyConfirmationsLists;
import com.fraczekkrzysztof.gocycling.myevents.MyEventsLists;
import com.fraczekkrzysztof.gocycling.newevent.NewEventActivity;
import com.fraczekkrzysztof.gocycling.usernotifications.NotificationLists;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONObject;

import java.util.List;

import cz.msebera.android.httpclient.Header;

public class EventListActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "EventListActivity";
    private static final String SHARED_PREF_TAG = "EVENTS_LIST";
    private static final String SHARED_PREF_LAST_SELECTED_CLUB = "LAST_SELECTED_CLUB";

    private RecyclerView mRecyclerView;
    private EventListRecyclerViewAdapter mAdapter;
    private SwipeRefreshLayout eventListSwipe;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private int page = 0;
    private int totalPages = 0;
    private AlertDialog mDialog;
    private ImageView dwaKolaImage;
    private Spinner mClubSpinner;
    //    private AdView mAdView;
    private List<ClubModel> mListOfClubs;
    private long mSelectedClubId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_list);
//        MobileAds.initialize(this, new OnInitializationCompleteListener() {
//            @Override
//            public void onInitializationComplete(InitializationStatus initializationStatus) {
//            }
//        });
//        mAdView = findViewById(R.id.adView);
//        AdRequest adRequest = new AdRequest.Builder().build();
//        mAdView.loadAd(adRequest);
        dwaKolaImage = findViewById(R.id.addImage);
        dwaKolaImage.setOnClickListener(dwaKolaButtonClickedListener);
        eventListSwipe = findViewById(R.id.event_list_swipe);
        eventListSwipe.setOnRefreshListener(onRefresListener);
        Toolbar mToolbar = findViewById(R.id.event_list_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setSubtitle("All Events");
        mDrawerLayout = findViewById(R.id.event_list_layout);
        mNavigationView = findViewById(R.id.navigation);
        ((TextView) mNavigationView.getHeaderView(0).findViewById(R.id.header_text)).
                setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());
        mNavigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toogle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toogle);
        toogle.syncState();
        createDialogForQuit();
        initRecyclerView();
        mClubSpinner = findViewById(R.id.event_list_club_spinner);
        showDialogForAppPermission();
        Log.d(TAG, "onCreate:  started.");
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            mDialog.show();
        }

    }

    private View.OnClickListener dwaKolaButtonClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.2-kola.pl/"));
            startActivity(browserIntent);
        }
    };


    private void createDialogForQuit() {
        DialogInterface.OnClickListener positiveAnswerListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                moveTaskToBack(true);
//                this code is used to shut down app completely
//                finishAffinity();
//                System.exit(0);
            }
        };

        DialogInterface.OnClickListener negativeAnswerListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this).setMessage(R.string.quit_app_question);
        builder.setPositiveButton(R.string.ok, positiveAnswerListener);
        builder.setNegativeButton(R.string.cancel, negativeAnswerListener);

        mDialog = builder.create();
    }

    private void initRecyclerView() {
        Log.d(TAG, "initRecyclerView: init recycler view");
        mRecyclerView = findViewById(R.id.recycler_view);
        mAdapter = new EventListRecyclerViewAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setOnScrollListener(prOnScrollListener);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        divider.setDrawable(getResources().getDrawable(R.drawable.event_list_divider));
        mRecyclerView.addItemDecoration(divider);
    }

    private void getEvents(int page, long clubId) {
        eventListSwipe.setRefreshing(true);
        Log.d(TAG, "getEvents: called");
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(getResources().getString(R.string.api_user), getResources().getString(R.string.api_password));
        String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_event_by_club_id);
        requestAddress = requestAddress + ApiUtils.PARAMS_START + "clubId=" + clubId + ApiUtils.PARAMS_AND + ApiUtils.getPageToRequest(page);
        Log.d(TAG, "Events: created request " + requestAddress);
        client.get(requestAddress, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.d(TAG, "onSuccess: response successfully received");
                List<EventModel> listEvents = EventModel.fromJson(response);
                if (totalPages == 0) {
                    totalPages = EventModel.getTotalPageFromJson(response);
                }
                mAdapter.addEvents(listEvents);
                eventListSwipe.setRefreshing(false);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Toast.makeText(EventListActivity.this, "There is an error. Please try again!", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error during retrieving event list", throwable);
                if (errorResponse != null) {
                    Log.d(TAG, errorResponse.toString());
                }
                eventListSwipe.setRefreshing(false);
            }
        });
    }

    private void refreshData() {
        mAdapter.clearEvents();
        page = 0;
        totalPages = 0;
        getClubsForSpinner();
    }

    private RecyclerView.OnScrollListener prOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            if (isLastItemDisplaying(recyclerView)) {
                if (page == totalPages - 1) {
                    Log.d(TAG, "There is nothing to load");
                    return;
                }
                page++;
                Log.d(TAG, "Load more data for page " + page);
                getEvents(page, mSelectedClubId);
            }
        }


    };

    private boolean isLastItemDisplaying(RecyclerView recyclerView) {
        //check if the adapter item count is greater than 0
        if (recyclerView.getAdapter().getItemCount() != 0) {
            //get the last visible item on screen using the layoutmanager
            int lastVisibleItemPosition = ((LinearLayoutManager) recyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition();
            //apply some logic here.
            if (lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition == recyclerView.getAdapter().getItemCount() - 1)
                return true;
        }
        return false;
    }

    private SwipeRefreshLayout.OnRefreshListener onRefresListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            Log.d(TAG, "onRefresh: refreshing");
            refreshData();
        }
    };


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.logout_menu:
                AuthUI.getInstance().signOut(this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "onComplete: Successfuly logged out");
                                    Toast.makeText(getApplicationContext(), "User properly logged out", Toast.LENGTH_SHORT).show();
                                    Intent loginIntent = new Intent(getApplicationContext(), LoggingActivity.class);
                                    startActivity(loginIntent);
                                } else {
                                    Log.d(TAG, "onComplete: " + task.getException().getMessage());
                                }
                            }
                        });
                break;
            case R.id.my_confirmation_menu:
                Intent intent = new Intent(getApplicationContext(), MyConfirmationsLists.class);
                startActivity(intent);
                break;
            case R.id.new_event_menu:
                Intent intent2 = new Intent(getApplicationContext(), NewEventActivity.class);
                startActivity(intent2);
                break;
            case R.id.my_account_menu:
                Intent intent3 = new Intent(getApplicationContext(), MyAccount.class);
                startActivity(intent3);
                break;
            case R.id.my_events_menu:
                Intent intent4 = new Intent(getApplicationContext(), MyEventsLists.class);
                startActivity(intent4);
                break;
            case R.id.my_notification_menu:
                Intent intent5 = new Intent(getApplicationContext(), NotificationLists.class);
                startActivity(intent5);
                break;
            case R.id.club_list_menu:
                Intent intent6 = new Intent(getApplicationContext(), ClubListActivity.class);
                startActivity(intent6);
                break;
        }
        menuItem.setChecked(false);
        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        refreshData();
    }

    public void showDialogForAppPermission() {

        final String SHARED_PREF_TAG = "PERMISSION INFO";
        final String SHARED_PREF_DONT_SHOW = "DONT_SHOW_AGAIN";

        if (getSharedPreferences(SHARED_PREF_TAG, Context.MODE_PRIVATE)
                .getBoolean(SHARED_PREF_DONT_SHOW, false)) {
            return;
        }
        DialogInterface.OnClickListener positiveAnswerListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(EventListActivity.this);
        builder.setCustomTitle(getLayoutInflater().inflate(R.layout.custom_dialog_title, null));
        String[] array = {"Don't show again"};
        boolean[] checked = {false};
        builder.setMultiChoiceItems(array, checked, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences(SHARED_PREF_TAG, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putBoolean(SHARED_PREF_DONT_SHOW, b);
                editor.commit();
            }
        });
        builder.setPositiveButton("Open App setting", positiveAnswerListener);
        builder.setNegativeButton(R.string.close, null);

        builder.create().show();
    }

    private long getLastPickedClubId() {
        return getApplicationContext().getSharedPreferences(SHARED_PREF_TAG, Context.MODE_PRIVATE)
                .getLong(SHARED_PREF_LAST_SELECTED_CLUB, -1);
    }

    private void saveLastPickedClubId(long clubId) {
        SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences(SHARED_PREF_TAG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putLong(SHARED_PREF_LAST_SELECTED_CLUB, clubId);
        editor.commit();

    }

    private void getClubsForSpinner() {
        Log.d(TAG, "getClubsForSpinner: called");
        eventListSwipe.setRefreshing(true);
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(getResources().getString(R.string.api_user), getResources().getString(R.string.api_password));
        String requestAddress = getResources().getString(R.string.api_base_address);
//        requestAddress = requestAddress + getResources().getString(R.string.api_clubs_which_user_is_member) + ApiUtils.PARAMS_START + "userUid=" + FirebaseAuth.getInstance().getCurrentUser().getUid();
        requestAddress = requestAddress + ApiUtils.PARAMS_AND + ApiUtils.getSizeToRequest(1000);

        Log.d(TAG, "getClubsForSpinner: created request " + requestAddress);
        client.get(requestAddress, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.d(TAG, "onSuccess: response successfully received");
                List<ClubModel> listClubs = ClubModel.fromJson(response);
                if (listClubs.isEmpty()) {
                    Toast.makeText(EventListActivity.this, "Join to art least one club to show an events!", Toast.LENGTH_SHORT).show();
                    eventListSwipe.setRefreshing(false);
                } else {
                    addClubsToSpinner(listClubs);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Toast.makeText(EventListActivity.this, "There is an error. Please try again!", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error during retrieving club list", throwable);
                if (errorResponse != null) {
                    Log.d(TAG, errorResponse.toString());
                }
                eventListSwipe.setRefreshing(false);
            }
        });
    }

    private void addClubsToSpinner(List<ClubModel> listClubs) {
        mListOfClubs = listClubs;
        ClubAdapter clubAdapter = new ClubAdapter(EventListActivity.this, R.layout.club_list_item, listClubs);
        mClubSpinner.setAdapter(clubAdapter);
        mClubSpinner.setOnItemSelectedListener(spinnerItemSelectedListener);
        if (getLastPickedClubId() != -1) {
            for (ClubModel club : listClubs) {
                if (club.getId() == getLastPickedClubId()) {
                    int spinnerPosition = clubAdapter.getPosition(club);
                    mClubSpinner.setSelection(spinnerPosition);
                    break;
                }
            }
        }
    }

    private AdapterView.OnItemSelectedListener spinnerItemSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            mSelectedClubId = mListOfClubs.get(i).getId();
            saveLastPickedClubId(mSelectedClubId);
            mAdapter.clearEvents();
            getEvents(0, mSelectedClubId);
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
            mSelectedClubId = -1;
            saveLastPickedClubId(mSelectedClubId);
        }
    };
}
