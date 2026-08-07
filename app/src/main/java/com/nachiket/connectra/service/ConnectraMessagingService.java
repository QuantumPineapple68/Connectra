package com.nachiket.connectra.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.nachiket.connectra.MainActivity;
import com.nachiket.connectra.R;

/**
 * Receives FCM pushes (including broadcasts to the "all" topic sent by the
 * scheduled Worker cron) and shows them as notifications.
 */
public class ConnectraMessagingService extends FirebaseMessagingService {

    public static final String CHANNEL_ID = "connectra_updates";
    private static final String TAG = "ConnectraFCM";
    /** Fixed id so a repeated or re-delivered push replaces the old one instead of stacking. */
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        String title = "Connectra";
        String body = "";

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                body = remoteMessage.getNotification().getBody();
            }
        }
        if (body.isEmpty() && remoteMessage.getData().containsKey("body")) {
            body = remoteMessage.getData().get("body");
            if (remoteMessage.getData().containsKey("title")) {
                title = remoteMessage.getData().get("title");
            }
        }
        if (body.isEmpty()) return;

        showNotification(title, body);
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "New FCM token issued");
    }

    private void showNotification(String title, String body) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Connectra Updates", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("News, matches and updates from Connectra");
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.send_logo)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();

        manager.notify(NOTIFICATION_ID, notification);
    }
}
