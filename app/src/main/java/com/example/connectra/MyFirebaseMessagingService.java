package com.example.connectra;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Check if the message contains data payload
        if (remoteMessage.getData().size() > 0) {
            String senderName = remoteMessage.getData().get("senderName");
            String message = remoteMessage.getData().get("message");

            // Show notification
            showNotification(senderName, message);
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // User is signed in, save the token
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(currentUser.getUid());
            userRef.child("fcmToken").setValue(token);
        } else {
            // Save token somewhere temporarily or handle the case when user isn't signed in
            // You might want to save it in SharedPreferences and update Firebase when user signs in
            SharedPreferences prefs = getSharedPreferences("FCM", Context.MODE_PRIVATE);
            prefs.edit().putString("token", token).apply();
        }
    }


    private void showNotification(String senderName, String message) {
        String channelId = "chat_notifications";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Chat Notifications", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // Create intent to open ChatActivity when notification is clicked
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("chatPartnerName", senderName); // Pass additional data
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.connectralogo) // Replace with your app's notification icon
                .setContentTitle(senderName)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Show notification
        notificationManager.notify(1, builder.build());
    }

    private void updateFCMToken() {
        SharedPreferences prefs = getSharedPreferences("FCM", Context.MODE_PRIVATE);
        String token = prefs.getString("token", null);
        if (token != null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(userId);
            userRef.child("fcmToken").setValue(token);
            // Clear the saved token
            prefs.edit().remove("token").apply();
        }
    }
}
