package com.nachiket.connectra.utility;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Client for the Connectra edge API (Cloudflare Worker):
 * AI skill matching (Workers AI + Vectorize), NSFW moderation,
 * certificate verification and Skill DNA card generation.
 * Every request is authenticated with the caller's Firebase ID token.
 */
public class ConnectraApi {

    private static final String TAG = "ConnectraApi";
    private static final String BASE_URL = "https://connectra-api.nachiketj14.workers.dev";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ---------- result callbacks (always invoked on the main thread) ----------

    public interface ModerationCallback {
        void onResult(boolean nsfw, String reason);
        void onError(String message);
    }

    public interface CertificateCallback {
        void onResult(boolean isCertificate, boolean matchesSkill, String detectedSkill,
                      String title, String issuer, double confidence);
        void onError(String message);
    }

    public interface MatchCallback {
        void onResult(List<SkillMatch> matches);
        void onError(String message);
    }

    public interface BytesCallback {
        void onResult(byte[] bytes);
        void onError(String message);
    }

    public static class SkillMatch {
        public final String uid, name, myskill, goalskill;
        public final int score;
        public final boolean mutual;

        public SkillMatch(String uid, String name, String myskill, String goalskill,
                          int score, boolean mutual) {
            this.uid = uid;
            this.name = name;
            this.myskill = myskill;
            this.goalskill = goalskill;
            this.score = score;
            this.mutual = mutual;
        }
    }

    // ---------- public API ----------

    /** NSFW check for any image (profile picture or certificate). */
    public static void moderateImage(String imageDataUri, ModerationCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("image", imageDataUri);
        } catch (Exception e) {
            postError(() -> callback.onError("Bad request"));
            return;
        }
        post("/moderate-image", body, new JsonHandler() {
            @Override
            public void onJson(JSONObject json) {
                boolean nsfw = json.optBoolean("nsfw", false);
                String reason = json.optString("reason", "");
                mainHandler.post(() -> callback.onResult(nsfw, reason));
            }

            @Override
            public void onFailure(String message) {
                mainHandler.post(() -> callback.onError(message));
            }
        });
    }

    /** OCR + semantic verification of a certificate against the user's claimed skill. */
    public static void verifyCertificate(String imageDataUri, String claimedSkill,
                                         CertificateCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("image", imageDataUri);
            body.put("skill", claimedSkill == null ? "" : claimedSkill);
        } catch (Exception e) {
            postError(() -> callback.onError("Bad request"));
            return;
        }
        post("/verify-certificate", body, new JsonHandler() {
            @Override
            public void onJson(JSONObject json) {
                boolean isCert = json.optBoolean("isCertificate", false);
                boolean matches = json.optBoolean("matchesSkill", false);
                String skill = json.optString("detectedSkill", "");
                String title = json.optString("title", "");
                String issuer = json.optString("issuer", "");
                double confidence = json.optDouble("confidence", 0);
                mainHandler.post(() -> callback.onResult(isCert, matches, skill, title, issuer, confidence));
            }

            @Override
            public void onFailure(String message) {
                mainHandler.post(() -> callback.onError(message));
            }
        });
    }

    /** Semantic skill matches for the current user (Workers AI embeddings + Vectorize). */
    public static void fetchMatches(String uid, String myskill, String goalskill, String name,
                                    MatchCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("uid", uid);
            body.put("name", name == null ? "" : name);
            body.put("myskill", myskill);
            body.put("goalskill", goalskill);
        } catch (Exception e) {
            postError(() -> callback.onError("Bad request"));
            return;
        }
        post("/match", body, new JsonHandler() {
            @Override
            public void onJson(JSONObject json) {
                List<SkillMatch> matches = new ArrayList<>();
                JSONArray arr = json.optJSONArray("matches");
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject m = arr.optJSONObject(i);
                        if (m == null) continue;
                        matches.add(new SkillMatch(
                                m.optString("uid"),
                                m.optString("name"),
                                m.optString("myskill"),
                                m.optString("goalskill"),
                                m.optInt("score", 0),
                                m.optBoolean("mutual", false)));
                    }
                }
                mainHandler.post(() -> callback.onResult(matches));
            }

            @Override
            public void onFailure(String message) {
                mainHandler.post(() -> callback.onError(message));
            }
        });
    }

    /** Renders the shareable Skill DNA card as PNG bytes at the edge. */
    public static void fetchSkillDna(String name, String username, String myskill, String goalskill,
                                     float rating, long reviews, boolean aiVerified,
                                     BytesCallback callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("name", name);
            body.put("username", username);
            body.put("myskill", myskill);
            body.put("goalskill", goalskill);
            body.put("rating", rating);
            body.put("reviews", reviews);
            body.put("aiVerified", aiVerified);
        } catch (Exception e) {
            postError(() -> callback.onError("Bad request"));
            return;
        }
        withToken(token -> {
            Request request = new Request.Builder()
                    .url(BASE_URL + "/skill-dna")
                    .header("Authorization", "Bearer " + token)
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful() || response.body() == null) {
                        response.close();
                        mainHandler.post(() -> callback.onError("Card generation failed"));
                        return;
                    }
                    byte[] bytes = response.body().bytes();
                    mainHandler.post(() -> callback.onResult(bytes));
                }
            });
        }, message -> mainHandler.post(() -> callback.onError(message)));
    }

    // ---------- internals ----------

    private interface JsonHandler {
        void onJson(JSONObject json);
        void onFailure(String message);
    }

    private interface TokenConsumer {
        void onToken(String token);
    }

    private interface ErrorConsumer {
        void onError(String message);
    }

    private static void withToken(TokenConsumer consumer, ErrorConsumer onError) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            onError.onError("Not signed in");
            return;
        }
        user.getIdToken(false)
                .addOnSuccessListener(result -> consumer.onToken(result.getToken()))
                .addOnFailureListener(e -> onError.onError("Auth error: " + e.getMessage()));
    }

    private static void post(String path, JSONObject body, JsonHandler handler) {
        withToken(token -> {
            Request request = new Request.Builder()
                    .url(BASE_URL + path)
                    .header("Authorization", "Bearer " + token)
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, path + " failed: " + e.getMessage());
                    handler.onFailure(e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    String raw = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        Log.e(TAG, path + " HTTP " + response.code() + ": " + raw);
                        handler.onFailure("Service error (" + response.code() + ")");
                        return;
                    }
                    try {
                        handler.onJson(new JSONObject(raw));
                    } catch (Exception e) {
                        handler.onFailure("Bad response");
                    }
                }
            });
        }, handler::onFailure);
    }

    private static void postError(Runnable r) {
        mainHandler.post(r);
    }
}
