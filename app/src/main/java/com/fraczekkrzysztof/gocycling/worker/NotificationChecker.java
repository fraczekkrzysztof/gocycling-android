package com.fraczekkrzysztof.gocycling.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;


public class NotificationChecker extends Worker {

    private static final String TAG = "NotificationChecker";
    NotificationTools tools;


    public NotificationChecker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        tools = new NotificationTools(context);
        Log.d(TAG, "NotificationChecker: created successfully");
    }

    @NonNull
    @Override
    public Result doWork() {
        try{
            tools.getMaxNotificationIdForUser();
            Log.d(TAG, "doWork: SUCCESS, after sync request");
        } catch (Exception e){
            Log.e(TAG, "doWork: Error",e );
            return Result.failure();
        }
        return Result.success();
    }
}
