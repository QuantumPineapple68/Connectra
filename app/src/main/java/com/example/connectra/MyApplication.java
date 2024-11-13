package com.example.connectra;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.storage.FirebaseStorage;

public class MyApplication extends Application {
    private static final String TAG = "FirebaseInit";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the primary Firebase app (default project)
        FirebaseApp.initializeApp(this);

        // Log currently available Firebase apps
        for (FirebaseApp app : FirebaseApp.getApps(this)) {
            Log.d(TAG, "Available FirebaseApp: " + app.getName());
        }

        // Delay initialization of the secondary Firebase app slightly
        new Handler(Looper.getMainLooper()).postDelayed(this::initializeSecondaryApp, 200);
    }

    private void initializeSecondaryApp() {
        try {
            // Check if the secondary app is already initialized
            FirebaseApp secondaryApp = FirebaseApp.getInstance("secondary");
            Log.d(TAG, "Secondary FirebaseApp already exists.");
        } catch (IllegalStateException e) {
            Log.d(TAG, "Initializing Secondary FirebaseApp...");

            // Set up FirebaseOptions for the secondary project
            FirebaseOptions secondaryOptions = new FirebaseOptions.Builder()
                    .setProjectId("genzcrop-c72a2")
                    .setApplicationId("1:371842266437:android:cbdf5cd0f890f851647a2c")
                    .setApiKey("AIzaSyAWrork2SzWDZSovFD1W5CSS5dvoOvOZaQ")
                    .setStorageBucket("genzcrop-c72a2.appspot.com")
                    .build();

            // Initialize secondary Firebase app with name "secondary"
            FirebaseApp secondaryApp;
            try {
                secondaryApp = FirebaseApp.initializeApp(this, secondaryOptions, "secondary");
                Log.d(TAG, "Secondary FirebaseApp initialized successfully.");

                // Test Firebase Storage with secondary app to confirm initialization
                FirebaseStorage secondaryStorage = FirebaseStorage.getInstance(secondaryApp);
                Log.d(TAG, "Successfully accessed Firebase Storage from secondary FirebaseApp.");
            } catch (Exception ex) {
                Log.e(TAG, "Failed to initialize secondary FirebaseApp: " + ex.getMessage());
            }
        }
    }
}


