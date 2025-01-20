package com.example.connectra;

import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

public class FCMSender {

    private static final String FCM_API_URL = "https://fcm.googleapis.com/fcm/send";
    private static final String SERVER_KEY = "YOUR_SERVER_KEY"; // Replace with your actual Server Key from Firebase.

    public void sendPushNotification(String deviceToken, String title, String message) {
        OkHttpClient client = new OkHttpClient();

        try {
            // Create the JSON payload
            JSONObject notification = new JSONObject();
            notification.put("title", title);
            notification.put("body", message);

            JSONObject body = new JSONObject();
            body.put("to", deviceToken);
            body.put("notification", notification);

            // Build the HTTP request
            RequestBody requestBody = RequestBody.create(
                    MediaType.parse("application/json"), body.toString()
            );

            Request request = new Request.Builder()
                    .url(FCM_API_URL)
                    .post(requestBody)
                    .addHeader("Authorization", "key=" + SERVER_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            // Execute the request asynchronously
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

