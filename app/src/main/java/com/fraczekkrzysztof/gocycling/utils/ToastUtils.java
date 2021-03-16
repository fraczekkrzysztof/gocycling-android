package com.fraczekkrzysztof.gocycling.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ToastUtils {

    public static void backgroundThreadShortToast(final Context context,
                                                  final String msg) {
        if (context != null && msg != null) {
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show());
        }
    }
}
