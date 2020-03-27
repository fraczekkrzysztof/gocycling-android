package com.fraczekkrzysztof.gocycling.worker;


import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.fraczekkrzysztof.gocycling.R;


public class NotificationChecker2 extends Service {

    private static final String TAG = "NotificationChecker2";
    private static final String NOT_CHANNEL_ID = "APP_IS_RUNNING";
    NotificationTools tools;
    Thread mThread;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        tools = new NotificationTools(getApplicationContext());
        Log.d(TAG, "NotificationChecker2: created successfully");

        Notification notification = new NotificationCompat.Builder(this,NOT_CHANNEL_ID)
                .setContentTitle("App is running")
                .setContentText("Don't kill me for receiving all notification as soon as possible")
                .setSmallIcon(R.drawable.chain)
                .build();

        startForeground(1,notification);

        if(mThread != null){
            mThread.interrupt();
            while (mThread.isInterrupted()){
                try {
                    Log.d(TAG, "onStartCommand: Thread is still running. Waiting for finish.");
                    Thread.currentThread().sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        mThread = new Thread(){
            @Override
            public void run() {
                try{
                    while(true){
                        tools.getMaxNotificationIdForUser();
                        Log.d(TAG, "Successfully finished sync job");
                        Thread.currentThread().sleep(60000);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        };

        mThread.start();
        return START_NOT_STICKY;
    }



    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
