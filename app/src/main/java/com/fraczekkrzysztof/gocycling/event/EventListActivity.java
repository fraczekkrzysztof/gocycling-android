package com.fraczekkrzysztof.gocycling.event;

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

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.apiutils.ApiUtils;
import com.fraczekkrzysztof.gocycling.apiutils.SortTypes;
import com.fraczekkrzysztof.gocycling.logging.LoggingActivity;
import com.fraczekkrzysztof.gocycling.model.EventModel;
import com.fraczekkrzysztof.gocycling.myaccount.MyAccount;
import com.fraczekkrzysztof.gocycling.myconfirmations.MyConfirmationsLists;
import com.fraczekkrzysztof.gocycling.newevent.NewEventActivity;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;


import cz.msebera.android.httpclient.Header;

public class EventListActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    private static final String TAG = "EventListActivity";

    private RecyclerView mRecyclerView;
    private EventListRecyclerViewAdapter mAdapter;
    private SwipeRefreshLayout eventListSwipe;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private int page = 0;
    private int totalPages = 0;
    private AlertDialog mDialog;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_list);
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        eventListSwipe = findViewById(R.id.event_list_swipe);
        eventListSwipe.setOnRefreshListener(onRefresListener);
        Toolbar mToolbar = findViewById(R.id.event_list_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setSubtitle("All Events");
        mDrawerLayout = findViewById(R.id.event_list_layout);
        mNavigationView = findViewById(R.id.navigation);
        ((TextView)mNavigationView.getHeaderView(0).findViewById(R.id.header_text)).
                setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());
        mNavigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toogle = new ActionBarDrawerToggle(this,mDrawerLayout,mToolbar,R.string.navigation_drawer_open,R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toogle);
        toogle.syncState();
        createDialogForQuit();
        initRecyclerView();
        Log.d(TAG, "onCreate:  started.");
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)){
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            mDialog.show();
        }

    }


    private void createDialogForQuit(){
        DialogInterface.OnClickListener positiveAnswerListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finishAffinity();
                System.exit(0);
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

    private void initRecyclerView(){
        Log.d(TAG, "initRecyclerView: init recycler view");
        mRecyclerView = findViewById(R.id.recycler_view);
        mAdapter = new EventListRecyclerViewAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setOnScrollListener(prOnScrollListener);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration divider = new DividerItemDecoration(this,DividerItemDecoration.VERTICAL);
        divider.setDrawable(getResources().getDrawable(R.drawable.event_list_divider));
        mRecyclerView.addItemDecoration(divider);
    }

    private void getEvents(int page){
        eventListSwipe.setRefreshing(true);
        Log.d(TAG, "getEvents: called");
        AsyncHttpClient client = new AsyncHttpClient();
        client.setBasicAuth(getResources().getString(R.string.api_user),getResources().getString(R.string.api_password));
        String requestAddress = getResources().getString(R.string.api_base_address) + getResources().getString(R.string.api_event_current);
        requestAddress = requestAddress + ApiUtils.PARAMS_START + ApiUtils.getPageToRequest(page);
        requestAddress = requestAddress + ApiUtils.PARAMS_AND + ApiUtils.getSortToRequest("ev_date_and_time", SortTypes.ASC);
        Log.d(TAG, "Events: created request " + requestAddress);
        client.get(requestAddress, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.d(TAG, "onSuccess: response successfully received");
                List<EventModel> listEvents = EventModel.fromJson(response);
                if (totalPages == 0 ){
                    totalPages = EventModel.getTotalPageFromJson(response);
                }
                mAdapter.addEvents(listEvents);
                eventListSwipe.setRefreshing(false);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Toast.makeText(EventListActivity.this,"There is an error. Please try again!",Toast.LENGTH_SHORT).show();
                Log.d(TAG, errorResponse.toString());
                eventListSwipe.setRefreshing(false);
            }
        });
    }

    private void refreshData(){
        mAdapter.clearEvents();
        page = 0;
        totalPages = 0;
        getEvents(0);
    }

    private RecyclerView.OnScrollListener prOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            if(isLastItemDisplaying(recyclerView)){
                if (page == totalPages - 1){
                    Log.d(TAG, "There is nothing to load");
                    return;
                }
                page++;
                Log.d(TAG, "Load more data for page " + page);
                getEvents(page);
            }
        }


    };
    private boolean isLastItemDisplaying(RecyclerView recyclerView){
        //check if the adapter item count is greater than 0
        if(recyclerView.getAdapter().getItemCount() != 0){
            //get the last visible item on screen using the layoutmanager
            int lastVisibleItemPosition = ((LinearLayoutManager)recyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition();
            //apply some logic here.
            if(lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition == recyclerView.getAdapter().getItemCount() - 1)
                return true;
        }
        return  false;
    }

    private SwipeRefreshLayout.OnRefreshListener onRefresListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            Log.d(TAG, "onRefresh: refreshing");
            refreshData();
            eventListSwipe.setRefreshing(false);
        }
    };


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch(menuItem.getItemId()){
            case R.id.logout_menu:
                AuthUI.getInstance().signOut(this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()){
                                    Log.d(TAG, "onComplete: Successfuly logged out");
                                    Toast.makeText(getApplicationContext(),"User properly logged out",Toast.LENGTH_SHORT).show();
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
}
