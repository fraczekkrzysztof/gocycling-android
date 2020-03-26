package com.fraczekkrzysztof.gocycling.androidnotification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.fraczekkrzysztof.gocycling.R;

import java.util.Random;

public class NotificationHelper extends ContextWrapper {

    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_NAME = "HIGH_GOCYCLING_NOTIFICATION_CHANNEL";
    private static final String CHANNEL_ID = "com.fraczekkrzysztof.gocycling.androidnotification"+CHANNEL_NAME;
    private static final int NEW_INTENT_CODE = 123456;

    public NotificationHelper(Context base) {
        super(base);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createChannels();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createChannels(){
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID,CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.enableLights(true);
        notificationChannel.enableVibration(true);
        notificationChannel.setDescription("Go Cycling Notification Channel");
        notificationChannel.setLightColor(R.color.secondaryDarkColor);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(notificationChannel);
    }

    public void sendHighPriorityNotifiction(String title, String body, String summary, Class activityName){
        Intent intent =  new Intent(this,activityName);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,NEW_INTENT_CODE,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notifiacation = new NotificationCompat.Builder(this,CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.chain)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle(new NotificationCompat.BigTextStyle().setSummaryText(summary).setBigContentTitle(title).bigText(body))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        NotificationManagerCompat.from(this).notify(new Random().nextInt(),notifiacation);
    }
}
