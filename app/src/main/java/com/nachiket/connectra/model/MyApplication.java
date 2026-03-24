package com.nachiket.connectra.model;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.storage.FirebaseStorage;

public class MyApplication extends Application {
    private static final String TAG = "FirebaseInit";
    private static final String SECONDARY_APP_NAME = "secondary";

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        new Handler(Looper.getMainLooper()).postDelayed(this::initSecondary, 200);
    }

    private void initSecondary() {
        try {
            FirebaseApp.getInstance(SECONDARY_APP_NAME);
            Log.d(TAG, "Secondary already initialized.");
        } catch (IllegalStateException e) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setProjectId("lean-58efd")
                    .setApplicationId("1:372911637561:android:7129c56090bd6f0b0cf9e8")
                    .setApiKey("AIzaSyB__SU0eSAYtHObFVw7jIpMNpjyNtRzkHA")
                    .setStorageBucket("lean-58efd.appspot.com")
                    .setDatabaseUrl("https://lean-58efd-default-rtdb.firebaseio.com")
                    .build();
            FirebaseApp.initializeApp(this, options, SECONDARY_APP_NAME);
            Log.d(TAG, "Secondary FirebaseApp initialized.");
        }
    }

    public static void cleanupFirebaseInstances() {
        try {
            FirebaseApp secondary = FirebaseApp.getInstance(SECONDARY_APP_NAME);
            secondary.delete();
            Log.d(TAG, "Secondary FirebaseApp cleaned up.");
        } catch (IllegalStateException e) {
            // Already gone, nothing to do
        }
    }

    public static FirebaseApp initializeSecondaryApp() {
        try {
            return FirebaseApp.getInstance(SECONDARY_APP_NAME);
        } catch (IllegalStateException e) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setProjectId("lean-58efd")
                    .setApplicationId("1:372911637561:android:7129c56090bd6f0b0cf9e8")
                    .setApiKey("AIzaSyB__SU0eSAYtHObFVw7jIpMNpjyNtRzkHA")
                    .setStorageBucket("lean-58efd.appspot.com")
                    .setDatabaseUrl("https://lean-58efd-default-rtdb.firebaseio.com")
                    .build();
            return FirebaseApp.initializeApp(
                    FirebaseApp.getInstance().getApplicationContext(), options, SECONDARY_APP_NAME
            );
        }
    }
}