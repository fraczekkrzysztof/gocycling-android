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
import com.fraczekkrzysztof.gocycling.clubs.ClubListActivity;
import com.fraczekkrzysztof.gocycling.httpclient.GoCyclingHttpClientHelper;
import com.fraczekkrzysztof.gocycling.logging.LoggingActivity;
import com.fraczekkrzysztof.gocycling.model.v2.PageDto;
import com.fraczekkrzysztof.gocycling.model.v2.club.ClubDto;
import com.fraczekkrzysztof.gocycling.model.v2.club.ClubListResponse;
import com.fraczekkrzysztof.gocycling.model.v2.event.EventListResponseDto;
import com.fraczekkrzysztof.gocycling.myaccount.MyAccount;
import com.fraczekkrzysztof.gocycling.myconfirmations.MyConfirmationsLists;
import com.fraczekkrzysztof.gocycling.myevents.MyEventsLists;
import com.fraczekkrzysztof.gocycling.newevent.NewEventActivity;
import com.fraczekkrzysztof.gocycling.usernotifications.NotificationLists;
import com.fraczekkrzysztof.gocycling.utils.ToastUtils;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class EventListActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "EventListActivity";
    private static final String SHARED_PREF_TAG = "EVENTS_LIST";
    private static final String SHARED_PREF_LAST_SELECTED_CLUB = "LAST_SELECTED_CLUB";

    private final Gson gson = new Gson();

    private RecyclerView mRecyclerView;
    private EventListRecyclerViewAdapter mAdapter;
    private SwipeRefreshLayout eventListSwipe;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private PageDto mPageDto;
    private AlertDialog mDialog;
    private ImageView dwaKolaImage;
    private Spinner mClubSpinner;
    private List<ClubDto> mListOfClubs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_list);
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

    private View.OnClickListener dwaKolaButtonClickedListener = view -> {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.2-kola.pl/"));
        startActivity(browserIntent);
    };


    private void createDialogForQuit() {
        DialogInterface.OnClickListener positiveAnswerListener = (dialogInterface, i) -> moveTaskToBack(true);


        DialogInterface.OnClickListener negativeAnswerListener = (dialogInterface, i) -> {
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
        mRecyclerView.addOnScrollListener(prOnScrollListener);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration divider = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        divider.setDrawable(getResources().getDrawable(R.drawable.event_list_divider));
        mRecyclerView.addItemDecoration(divider);
    }

    private void getEvents(int page, long clubId) {
        eventListSwipe.setRefreshing(true);
        Log.d(TAG, "getEvents: called");
        Request request = prepareEventRequest(page, clubId);
        OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(getResources());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                ToastUtils.backgroundThreadShortToast(EventListActivity.this, "There is an error. Please try again!");
                Log.e(TAG, "Error during retrieving event list", e);
                eventListSwipe.setRefreshing(false);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "onResponse: Successfully received response with Events");
                    EventListResponseDto apiResponse = gson.fromJson(response.body().charStream(), EventListResponseDto.class);
                    runOnUiThread(() -> {
                        mPageDto = apiResponse.getPage();
                        mAdapter.addEvents(apiResponse.getEvents());
                        eventListSwipe.setRefreshing(false);
                    });
                    return;
                }
                eventListSwipe.setRefreshing(false);
                Log.w(TAG, String.format("onResponse: Response reseived but %d status", response.code()));
                ToastUtils.backgroundThreadShortToast(EventListActivity.this, "There is an error. Try again");
            }
        });
    }

    private Request prepareEventRequest(int page, long clubId) {
        String request = getResources().getString(R.string.api_base_address) +
                String.format(getResources().getString(R.string.api_event_address), clubId);
        HttpUrl.Builder urlBuilder = HttpUrl.parse(request).newBuilder()
                .addQueryParameter("page", String.valueOf(page))
                .addQueryParameter("sort", "created,desc");
        return new Request.Builder()
                .url(urlBuilder.build().toString())
                .build();
    }

    private void refreshData() {
        mAdapter.clearEvents();
        getClubsForSpinner();
    }

    private RecyclerView.OnScrollListener prOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (isLastItemDisplaying(recyclerView)) {
                if (mPageDto.getNextPage() == 0) {
                    Log.d(TAG, "There is nothing to load");
                    return;
                }
                Log.d(TAG, "Load more data");
                getEvents(mPageDto.getNextPage(), getLastPickedClubId());
            }
        }

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
    };

    private SwipeRefreshLayout.OnRefreshListener onRefresListener = () -> {
        Log.d(TAG, "onRefresh: refreshing");
        refreshData();
    };


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.logout_menu:
                AuthUI.getInstance().signOut(this)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "onComplete: Successfuly logged out");
                                ToastUtils.backgroundThreadShortToast(EventListActivity.this, "User properly logged out");
                                Intent loginIntent = new Intent(getApplicationContext(), LoggingActivity.class);
                                startActivity(loginIntent);
                            } else {
                                Log.d(TAG, "onComplete: " + task.getException().getMessage());
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
            default:
                menuItem.setChecked(false);
                mDrawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        refreshData();
    }

    public void showDialogForAppPermission() {

        final String SHARED_PREF_TAG_FOR_PERMISSIONS = "PERMISSION INFO";
        final String SHARED_PREF_FOR_PERMISSIONS_DONT_SHOW = "DONT_SHOW_AGAIN";

        if (getSharedPreferences(SHARED_PREF_TAG_FOR_PERMISSIONS, Context.MODE_PRIVATE)
                .getBoolean(SHARED_PREF_FOR_PERMISSIONS_DONT_SHOW, false)) {
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
                SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences(SHARED_PREF_TAG_FOR_PERMISSIONS, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putBoolean(SHARED_PREF_FOR_PERMISSIONS_DONT_SHOW, b);
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

    private void getClubsForSpinner() {
        Log.d(TAG, "getClubsForSpinner: called");
        eventListSwipe.setRefreshing(true);
        Request request = prepareClubsRequest();
        OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(getResources());
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                ToastUtils.backgroundThreadShortToast(EventListActivity.this, "There is an error. Please try again!");
                Log.e(TAG, "Error during retrieving club list for spinner", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.d(TAG, "onResponse: Successfully received response with club list for spinner");
                    ClubListResponse apiResponse = gson.fromJson(response.body().charStream(), ClubListResponse.class);
                    if (apiResponse.getClubs().isEmpty()) {
                        ToastUtils.backgroundThreadShortToast(EventListActivity.this, "Join to at least one club to show an events!");
                        eventListSwipe.setRefreshing(false);
                        return;
                    }
                    runOnUiThread(() -> {
                        addClubsToSpinner(apiResponse.getClubs());
                        eventListSwipe.setRefreshing(false);
                        return;
                    });
                }
                eventListSwipe.setRefreshing(false);
                Log.e(TAG, String.format("onResponse: Response with club for spinner received, but %s status.", response.code()));
            }
        });
    }

    @NotNull
    private Request prepareClubsRequest() {
        String url = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_clubs);
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder()
                .addQueryParameter("limit", "50")
                .addQueryParameter("sort", "created,desc")
                .addQueryParameter("userUid", FirebaseAuth.getInstance().getCurrentUser().getUid());
        return new Request.Builder()
                .url(urlBuilder.build().toString())
                .build();
    }

    private void addClubsToSpinner(List<ClubDto> listClubs) {
        mListOfClubs = listClubs;
        ClubAdapter clubAdapter = new ClubAdapter(EventListActivity.this, R.layout.club_list_item, listClubs);
        mClubSpinner.setAdapter(clubAdapter);
        mClubSpinner.setOnItemSelectedListener(spinnerItemSelectedListener);
        if (getLastPickedClubId() != -1) {
            for (ClubDto club : listClubs) {
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
            saveLastPickedClubId(mListOfClubs.get(i).getId());
            mAdapter.clearEvents();
            getEvents(0, mListOfClubs.get(i).getId());
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
            saveLastPickedClubId(-1);
        }

        private void saveLastPickedClubId(long clubId) {
            SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences(SHARED_PREF_TAG, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putLong(SHARED_PREF_LAST_SELECTED_CLUB, clubId);
            editor.commit();
        }
    };
}
