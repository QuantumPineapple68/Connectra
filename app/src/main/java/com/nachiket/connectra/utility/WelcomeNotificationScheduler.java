package com.nachiket.connectra.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.nachiket.connectra.service.WelcomeNotificationWorker;

import java.util.concurrent.TimeUnit;

/**
 * Schedules the onboarding nudge exactly once per install: 3 minutes after the
 * very first launch. WorkManager keeps the job across process death and reboot,
 * and a SharedPreferences flag stops it from being re-armed on later launches.
 */
public class WelcomeNotificationScheduler {

    private static final String TAG = "WelcomeScheduler";

    private static final String PREFS_NAME = "connectra_onboarding";
    private static final String KEY_SCHEDULED = "welcome_push_scheduled";

    private static final String WORK_NAME = "connectra_welcome_push";
    private static final long DELAY_MINUTES = 3;

    private WelcomeNotificationScheduler() {
    }

    public static void scheduleOnFirstLaunch(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(KEY_SCHEDULED, false)) return;

        try {
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(WelcomeNotificationWorker.class)
                    .setInitialDelay(DELAY_MINUTES, TimeUnit.MINUTES)
                    .build();

            WorkManager.getInstance(context)
                    .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request);

            prefs.edit().putBoolean(KEY_SCHEDULED, true).apply();
        } catch (Exception e) {
            // Never let a scheduling failure take the app down on startup
            Log.e(TAG, "Could not schedule the welcome notification: " + e.getMessage());
        }
    }
}
