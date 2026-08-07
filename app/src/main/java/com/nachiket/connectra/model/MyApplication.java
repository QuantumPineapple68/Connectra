package com.nachiket.connectra.model;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";
    private static final String SECONDARY_APP_NAME = "secondary";

    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();

        // Make sure the default Firebase instance is ready as soon as the app starts
        FirebaseApp.initializeApp(this);
    }

    public static Context getAppContext() {
        return appContext;
    }

    // Deletes any existing secondary Firebase instance so a fresh one can be created
    public static void cleanupFirebaseInstances() {
        try {
            FirebaseApp secondaryApp = FirebaseApp.getInstance(SECONDARY_APP_NAME);
            secondaryApp.delete();
        } catch (IllegalStateException e) {
            // Secondary app doesn't exist yet, nothing to clean up
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up Firebase instances: " + e.getMessage());
        }
    }

    // Initializes (or returns) the secondary Firebase instance used for storage uploads
    public static FirebaseApp initializeSecondaryApp() {
        try {
            return FirebaseApp.getInstance(SECONDARY_APP_NAME);
        } catch (IllegalStateException e) {
            // Secondary app doesn't exist yet, create it below
        }

        // Get the options from the default app
        FirebaseOptions options = FirebaseApp.getInstance().getOptions();

        // Initialize the secondary app
        return FirebaseApp.initializeApp(appContext, options, SECONDARY_APP_NAME);
    }
}
