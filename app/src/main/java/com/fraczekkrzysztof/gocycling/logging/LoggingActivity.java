package com.fraczekkrzysztof.gocycling.logging;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.fraczekkrzysztof.gocycling.R;
import com.fraczekkrzysztof.gocycling.event.EventListActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;

public class LoggingActivity extends AppCompatActivity {


    private static final String TAG = "LoggingActivity";
    private static final String sharedPreferencesString = "LoggingPref";
    private static final String sharedPreferencesLoggedUserString = "LoggedUserId";
    private static final int RC_SIGN_IN = 1232;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences pref = getApplicationContext().getSharedPreferences(sharedPreferencesString,MODE_PRIVATE);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null){
            Log.d(TAG, "onCreate: user already logged in. Redirect to EventListActivity");
            startApp();
        } else {
            Log.d(TAG, "onCreate: user not logged. Start logging.");
            startLoggingIn();
        }
    }

    private void startLoggingIn(){
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setTheme(R.style.FirebaseUI)
                        .setLogo(R.drawable.chain)
                        .setIsSmartLockEnabled(false)
                        .build(),
                RC_SIGN_IN);
    }

    private void startApp(){
        Intent startIntent =  new Intent(getApplicationContext(), EventListActivity.class);
        startActivity(startIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN){
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (response == null ){ //operation canceled by the user
                return;
            }
            if (resultCode ==RESULT_OK){

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                SharedPreferences pref = getApplicationContext().getSharedPreferences(sharedPreferencesString,MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString(sharedPreferencesLoggedUserString,user.getUid());
                editor.commit();
                Log.d(TAG, "onActivityResult: user successfully logged in " + user.getUid());
                Toast.makeText(getApplicationContext(),"Successfully logged in!",Toast.LENGTH_SHORT).show();
                startApp();
            } else {
                Log.d(TAG, "onActivityResult: " + response.getError().getMessage());
                Toast.makeText(getApplicationContext(),"Error during logging in!",Toast.LENGTH_SHORT).show();
            }
        }
    }
}
