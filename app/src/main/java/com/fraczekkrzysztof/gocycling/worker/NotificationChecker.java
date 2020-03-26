package com.fraczekkrzysztof.gocycling.worker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.androidnotification.NotificationHelper;
import com.fraczekkrzysztof.gocycling.apiutils.ApiUtils;
import com.fraczekkrzysztof.gocycling.usernotifications.NotificationLists;
import com.google.firebase.auth.FirebaseAuth;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.SyncHttpClient;
import cz.msebera.android.httpclient.Header;

public class NotificationChecker extends Worker {

    private static final String TAG = "NotificationChecker";
    private static final String SHARED_PREF_TAG = "NOTIFICATION_CHECKER";
    private static final String SHARED_PREF_LAST_NOT_ID_TAG = "LAST_NOTIFICATION_ID";
    private NotificationHelper mNotificationHelper;


    public NotificationChecker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mNotificationHelper = new NotificationHelper(getApplicationContext());
    }

    @NonNull
    @Override
    public Result doWork() {
        try{
            getMaxNotificationIdForUser();
            Log.d(TAG, "doWork: SUCCESS, after sync request");
        } catch (Exception e){
            Log.e(TAG, "doWork: Error",e );
            return Result.failure();
        }


        return Result.success();
    }

    private long getLastNotificationForUser(){
        return getApplicationContext().getSharedPreferences(SHARED_PREF_TAG,Context.MODE_PRIVATE)
                .getLong(SHARED_PREF_LAST_NOT_ID_TAG,0);
    }
    private void saveLastNotificationIdForUser(long notificationId){
        SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences(SHARED_PREF_TAG,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putLong(SHARED_PREF_LAST_NOT_ID_TAG,notificationId);
        editor.commit();

    }

    private void doActionForSuccess(long maxNotificationId){
        long lastNotificationId = getLastNotificationForUser();
        Log.d(TAG, "doActionForSuccess: last notification id " + lastNotificationId);
        if (maxNotificationId > lastNotificationId){
            saveLastNotificationIdForUser(maxNotificationId);
            mNotificationHelper.sendHighPriorityNotifiction("New Notifications","There is some changes in events you confirmed. Check details","", NotificationLists.class);
        }
    }

    private void getMaxNotificationIdForUser(){
        AsyncHttpResponseHandler responseHandler = new AsyncHttpResponseHandler(Looper.getMainLooper()) {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d(TAG, "onSuccess: Successfully get max notification id for user " + new String(responseBody));
                doActionForSuccess(Long.valueOf(new String(responseBody)));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.e(TAG, "onFailure: error during getting max notification id",error);
                if (responseBody != null) {
                    Log.e(TAG, "onFailure: " + responseBody.toString()); }
            }
        };
        responseHandler.setUseSynchronousMode(true);
        AsyncHttpClient client = new SyncHttpClient();
        client.setBasicAuth(getApplicationContext().getResources().getString(R.string.api_user), getApplicationContext().getResources().getString(R.string.api_password));
        String requestAddress = getApplicationContext().getResources().getString(R.string.api_base_address) + getApplicationContext().getResources().getString(R.string.api_notification_max_id_for_user);
        requestAddress = requestAddress + ApiUtils.PARAMS_START + "userUid=" + FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d(TAG, "getMaxNotificationIdForUser: " + requestAddress);
        client.get(requestAddress, responseHandler);
    }
}
