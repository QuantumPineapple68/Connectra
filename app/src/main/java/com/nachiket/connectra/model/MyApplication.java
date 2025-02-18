package com.nachiket.connectra.model;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.util.List;

public class MyApplication extends Application {
    private static final String TAG = "FirebaseInit";
    private static boolean isSecondaryInitialized = false;
    private static final Object initLock = new Object();
    private static MyApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        registerActivityLifecycleCallbacks(new AppDetonator());

        // Initialize the primary Firebase app (default project)
        FirebaseApp.initializeApp(this);
    }

    public static synchronized FirebaseApp initializeSecondaryApp() {
        synchronized (initLock) {
            try {
                // First try to get existing instance
                try {
                    FirebaseApp existingApp = FirebaseApp.getInstance("secondary");
                    isSecondaryInitialized = true;
                    return existingApp;
                } catch (IllegalStateException e) {
                    // App doesn't exist, continue with initialization
                }

                // Set up FirebaseOptions for the secondary project
                FirebaseOptions secondaryOptions = new FirebaseOptions.Builder()
                        .setProjectId("genzcrop-c72a2")
                        .setApplicationId("1:371842266437:android:cbdf5cd0f890f851647a2c")
                        .setApiKey("AIzaSyAWrork2SzWDZSovFD1W5CSS5dvoOvOZaQ")
                        .setStorageBucket("genzcrop-c72a2.appspot.com")
                        .build();

                // Initialize secondary Firebase app
                FirebaseApp secondaryApp = FirebaseApp.initializeApp(instance, secondaryOptions, "secondary");
                isSecondaryInitialized = true;
                Log.d(TAG, "Secondary FirebaseApp initialized successfully.");
                return secondaryApp;
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize secondary FirebaseApp: " + e.getMessage());
                throw new RuntimeException("Failed to initialize Firebase", e);
            }
        }
    }

    public static MyApplication getInstance() {
        return instance;
    }

    public static void cleanupFirebaseInstances() {
        synchronized (initLock) {
            try {
                // Get all Firebase app instances
                List<FirebaseApp> apps = FirebaseApp.getApps(instance);
                for (FirebaseApp app : apps) {
                    if ("secondary".equals(app.getName())) {
                        app.delete();
                    }
                }
                isSecondaryInitialized = false;
                Log.d(TAG, "Secondary FirebaseApp cleaned up successfully.");
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up Firebase instances: " + e.getMessage());
            }
        }
    }

    public static void reinitializeSecondaryIfNeeded() {
        synchronized (initLock) {
            if (!isSecondaryInitialized) {
                initializeSecondaryApp();
            }
        }
    }
}