package com.nachiket.connectra.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.nachiket.connectra.MainActivity;
import com.nachiket.connectra.R;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Posts the one-shot onboarding nudge a few minutes after the very first launch.
 * The copy comes from Firebase Remote Config ("welcome_push_title" /
 * "welcome_push_body") so it can be reworded without shipping a new build.
 * This is a local notification — no FCM token and no server are involved.
 */
public class WelcomeNotificationWorker extends Worker {

    private static final String TAG = "WelcomeNotification";

    public static final String KEY_TITLE = "welcome_push_title";
    public static final String KEY_BODY = "welcome_push_body";

    private static final String DEFAULT_TITLE = "Welcome to Connectra 👋";
    private static final String DEFAULT_BODY =
            "Find someone who teaches what you want to learn — your matches are ready.";

    /** Own id and tag so this never collapses the scheduled engagement broadcast. */
    private static final int NOTIFICATION_ID = 1002;
    private static final String NOTIFICATION_TAG = "connectra_welcome";

    private static final long CONFIG_TIMEOUT_SECONDS = 15;

    public WelcomeNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.d(TAG, "Notifications disabled, skipping the welcome nudge");
            return Result.success();
        }

        String title = DEFAULT_TITLE;
        String body = DEFAULT_BODY;

        try {
            FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();

            FirebaseRemoteConfigSettings settings = new FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(3600)
                    .build();
            remoteConfig.setConfigSettingsAsync(settings);

            Map<String, Object> defaults = new HashMap<>();
            defaults.put(KEY_TITLE, DEFAULT_TITLE);
            defaults.put(KEY_BODY, DEFAULT_BODY);
            remoteConfig.setDefaultsAsync(defaults);

            Tasks.await(remoteConfig.fetchAndActivate(), CONFIG_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            String remoteTitle = remoteConfig.getString(KEY_TITLE);
            String remoteBody = remoteConfig.getString(KEY_BODY);
            if (!remoteTitle.isEmpty()) title = remoteTitle;
            if (!remoteBody.isEmpty()) body = remoteBody;
        } catch (Exception e) {
            // Offline or the fetch timed out — the hardcoded copy still goes out
            Log.w(TAG, "Remote Config unavailable, using defaults: " + e.getMessage());
        }

        showNotification(context, title, body);
        return Result.success();
    }

    private void showNotification(Context context, String title, String body) {
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    ConnectraMessagingService.CHANNEL_ID,
                    "Connectra Updates",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("News, matches and updates from Connectra");
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification =
                new NotificationCompat.Builder(context, ConnectraMessagingService.CHANNEL_ID)
                        .setSmallIcon(R.drawable.send_logo)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .build();

        manager.notify(NOTIFICATION_TAG, NOTIFICATION_ID, notification);
    }
}
