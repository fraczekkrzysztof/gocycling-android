package com.fraczekkrzysztof.gocycling.worker;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.util.Log;

import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.androidnotification.NotificationHelper;
import com.fraczekkrzysztof.gocycling.httpclient.GoCyclingHttpClientHelper;
import com.fraczekkrzysztof.gocycling.usernotifications.NotificationLists;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NotificationTools extends ContextWrapper{

    private static final String TAG = "NotificationTools";
    private static final String SHARED_PREF_TAG = "NOTIFICATION_CHECKER";
    private static final String SHARED_PREF_LAST_NOT_ID_TAG = "LAST_NOTIFICATION_ID";
    private NotificationHelper mNotificationHelper;

    public NotificationTools(Context base) {
        super(base);
        mNotificationHelper = new NotificationHelper(base);
    }


    private long getLastNotificationForUser(){
        return getApplicationContext().getSharedPreferences(SHARED_PREF_TAG,Context.MODE_PRIVATE)
                .getLong(SHARED_PREF_LAST_NOT_ID_TAG,0);
    }
    private void saveLastNotificationIdForUser(long notificationId){
        SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences(SHARED_PREF_TAG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putLong(SHARED_PREF_LAST_NOT_ID_TAG,notificationId);
        editor.commit();

    }

    private void doActionForSuccess(long maxNotificationId){
        long lastNotificationId = getLastNotificationForUser();
        Log.d(TAG, "doActionForSuccess: last notification id " + lastNotificationId);
        if (maxNotificationId > lastNotificationId){
            Log.d(TAG, "doActionForSuccess: maxNotificationId " + maxNotificationId + "is greater that lats one - generating notification ");
            saveLastNotificationIdForUser(maxNotificationId);
            mNotificationHelper.sendHighPriorityNotifiction("New Notifications", "Check detail to not missed your rides information", "", NotificationLists.class);
        }
    }

    public void getMaxNotificationIdForUser(){
        Request request = prepareRequestForMaxNotification();
        OkHttpClient httpClient = GoCyclingHttpClientHelper.getInstance(getResources());
        try {
            Response response = httpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                doActionForSuccess(Long.valueOf(response.body().string()));
            }
        } catch (IOException e) {
            Log.e(TAG, "getMaxNotificationIdForUser: error during retrieving max notification id for user", e);
        }
    }

    private Request prepareRequestForMaxNotification() {
        String requestAddress = getResources().getString(R.string.api_base_address) +
                String.format(getResources().getString(R.string.api_notification_max_id_for_user), FirebaseAuth.getInstance().getCurrentUser().getUid());
        return new Request.Builder()
                .url(requestAddress)
                .build();
    }
}
