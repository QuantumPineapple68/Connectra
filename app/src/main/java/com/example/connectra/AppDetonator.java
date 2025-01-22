package com.example.connectra;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AppDetonator implements Application.ActivityLifecycleCallbacks {
    private Activity currentActivity;
    private final DatabaseReference detonatorRef;
    private boolean initialCheckDone = false;

    public AppDetonator() {
        detonatorRef = FirebaseDatabase.getInstance().getReference("Detonator");
        setupDetonatorListener();
    }

    private void setupDetonatorListener() {
        ValueEventListener detonatorListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isActive = snapshot.child("isActive_1,0").getValue(Boolean.class);
                if (isActive != null) {
                    if (!isActive) {
                        showExitDialog();
                    } else if (!initialCheckDone) {
                        // App is active and this is our first check
                        initialCheckDone = true;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AppDetonator", "Database error: " + error.getMessage());
            }
        };
        detonatorRef.addValueEventListener(detonatorListener);
    }

    private void checkInitialState() {
        if (!initialCheckDone && currentActivity != null) {
            detonatorRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    Boolean isActive = task.getResult().child("isActive_1,0").getValue(Boolean.class);
                    if (isActive != null && !isActive) {
                        showExitDialog();
                    }
                }
                initialCheckDone = true;
            });
        }
    }

    private void showExitDialog() {
        if (currentActivity == null) return;

        currentActivity.runOnUiThread(() -> {
            new AlertDialog.Builder(currentActivity)
                    .setTitle("App Unavailable")
                    .setMessage("This application is currently under maintenance. Please try again later.")
                    .setCancelable(false)
                    .setPositiveButton("Exit", (dialog, which) -> {
                        if (currentActivity != null) {
                            currentActivity.finishAffinity();
                        }
                    })
                    .show();
        });
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        currentActivity = activity;
        checkInitialState();
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
        checkInitialState();
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {}

    @Override
    public void onActivityStopped(@NonNull Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }
}