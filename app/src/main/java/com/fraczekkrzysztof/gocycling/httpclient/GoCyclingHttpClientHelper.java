package com.fraczekkrzysztof.gocycling.httpclient;


import android.content.res.Resources;

import com.fraczekkrzysztof.gocycling.R;

import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;


public class GoCyclingHttpClientHelper {

    private static OkHttpClient instance;

    private GoCyclingHttpClientHelper() {
    }

    private static OkHttpClient createInstance(final Resources res) {
        return new OkHttpClient.Builder().authenticator((route, response) -> {
            String credential = Credentials.basic(res.getString(R.string.api_user), res.getString(R.string.api_password));
            return response.request().newBuilder().header("Authorization", credential).build();
        }).addInterceptor(chain -> {
            Request request = chain.request().newBuilder()
                    .addHeader("Content-Type", "application/json;charset=UTF-8")
                    .build();
            return chain.proceed(request);
        }).
                build();
    }

    public static OkHttpClient getInstance(Resources res) {
        if (instance == null) {
            instance = createInstance(res);
        }
        return instance;
    }
}
