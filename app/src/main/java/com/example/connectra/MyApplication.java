package com.example.connectra;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.storage.FirebaseStorage;

public class MyApplication extends Application {
    private static final String TAG = "FirebaseInit";
    private AppDetonator appDetonator;
    private static boolean isSecondaryInitialized = false;
    private static final Object initLock = new Object();


    @Override
    public void onCreate() {
        super.onCreate();

        appDetonator = new AppDetonator();
        registerActivityLifecycleCallbacks(appDetonator);

        // Initialize the primary Firebase app (default project)
        FirebaseApp.initializeApp(this);

        // Create notification channel
        createNotificationChannel();

        initializeSecondaryApp();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "chat_notifications",  // Must match the ID in MyFirebaseMessagingService
                    "Chat Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for chat messages");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void initializeSecondaryApp() {
        synchronized (initLock) {
            if (isSecondaryInitialized) {
                return;
            }

            try {
                // Set up FirebaseOptions for the secondary project
                FirebaseOptions secondaryOptions = new FirebaseOptions.Builder()
                        .setProjectId("genzcrop-c72a2")
                        .setApplicationId("1:371842266437:android:cbdf5cd0f890f851647a2c")
                        .setApiKey("AIzaSyAWrork2SzWDZSovFD1W5CSS5dvoOvOZaQ")
                        .setStorageBucket("genzcrop-c72a2.appspot.com")
                        .build();

                // Initialize secondary Firebase app
                FirebaseApp secondaryApp = FirebaseApp.initializeApp(this, secondaryOptions, "secondary");

                // Test Firebase Storage to confirm initialization
                FirebaseStorage.getInstance(secondaryApp);

                isSecondaryInitialized = true;
                Log.d(TAG, "Secondary FirebaseApp initialized successfully.");
            } catch (IllegalStateException e) {
                Log.d(TAG, "Secondary FirebaseApp already exists.");
                isSecondaryInitialized = true;
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize secondary FirebaseApp: " + e.getMessage());
            }
        }
    }

    public static boolean isSecondaryInitialized() {
        synchronized (initLock) {
            return isSecondaryInitialized;
        }
    }
}