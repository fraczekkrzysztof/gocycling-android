package com.fraczekkrzysztof.gocycling.httpclient;


import android.content.res.Resources;

import com.fraczekkrzysztof.gocycling.R;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;


public class GoCyclingHttpClientHelper {

    private static OkHttpClient instance;

    private GoCyclingHttpClientHelper() {
    }

    private static OkHttpClient createInstance(final Resources res) {
        return new OkHttpClient.Builder().authenticator(new Authenticator() {
            public Request authenticate(Route route, Response response) throws IOException {
                String credential = Credentials.basic(res.getString(R.string.api_user), res.getString(R.string.api_password));
                return response.request().newBuilder().header("Authorization", credential).build();
            }
        }).build();
    }

    public static OkHttpClient getInstance(Resources res) {
        if (instance == null) {
            instance = createInstance(res);
        }
        return instance;
    }
}
