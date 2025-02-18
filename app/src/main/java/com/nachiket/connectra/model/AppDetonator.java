package com.nachiket.connectra.model;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
    private int activityReferences = 0;
    private boolean isAppInForeground = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private AlertDialog noInternetDialog;
    private boolean isInternetAvailable = false;

    public AppDetonator() {
        detonatorRef = FirebaseDatabase.getInstance().getReference("Detonator");
        setupDetonatorListener();
    }

    private void setupNetworkCallback() {
        if (currentActivity == null) return;

        if (connectivityManager == null) {
            connectivityManager = (ConnectivityManager) currentActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        if (networkCallback == null) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    handler.post(() -> {
                        isInternetAvailable = true;
                        if (noInternetDialog != null && noInternetDialog.isShowing()) {
                            noInternetDialog.dismiss();
                        }
                    });
                }

                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    handler.post(() -> {
                        isInternetAvailable = false;
                        if (isAppInForeground) {
                            showNoInternetDialog();
                        }
                    });
                }
            };

            // Register callback for all network types
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();

            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        }
    }

    private void unregisterNetworkCallback() {
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (IllegalArgumentException e) {
                Log.e("AppDetonator", "Network callback was not registered or already unregistered");
            }
            networkCallback = null;
        }
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

    private void showNoInternetDialog() {
        if (currentActivity == null) return;

        currentActivity.runOnUiThread(() -> {

            noInternetDialog = new AlertDialog.Builder(currentActivity)
                    .setTitle("No Internet Connection")
                    .setMessage("Please check your internet connection and try again.")
                    .setCancelable(false)
                    .setPositiveButton("Retry", (dialog, which) -> {
                        if (!isInternetAvailable) {
                            showNoInternetDialog();
                        }
                    })
                    .setNegativeButton("Exit", (dialog, which) -> forceAppExit())
                    .create();

            noInternetDialog.show();
        });
    }

    private void forceAppExit() {
        if (currentActivity != null) {
            unregisterNetworkCallback();
            currentActivity.finishAffinity();

            handler.postDelayed(() -> {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }, 100);
        }
    }

    private void showExitDialog() {
        if (currentActivity == null) return;

        currentActivity.runOnUiThread(() -> {
            new AlertDialog.Builder(currentActivity)
                    .setTitle("App Unavailable")
                    .setMessage("Detonator Triggered! This application is under maintenance. Try again later.")
                    .setCancelable(false)
                    .setPositiveButton("Exit", (dialog, which) -> forceAppExit())
                    .show();
        });
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        currentActivity = activity;
        activityReferences++;

        if (!isAppInForeground && activityReferences > 0) {
            isAppInForeground = true;
            setupNetworkCallback();
            performDelayedChecks();
        }
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
        if (!isInternetAvailable) {
            showNoInternetDialog();
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        if (noInternetDialog != null && noInternetDialog.isShowing()) {
            noInternetDialog.dismiss();
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        activityReferences--;
        if (isAppInForeground && activityReferences == 0) {
            isAppInForeground = false;
            unregisterNetworkCallback();
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }

    private void performDelayedChecks() {
        handler.postDelayed(() -> {
            if (isAppInForeground && currentActivity != null) {
                checkInitialState();
                if (!isInternetAvailable) {
                    showNoInternetDialog();
                }
            }
        }, 500);
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
}