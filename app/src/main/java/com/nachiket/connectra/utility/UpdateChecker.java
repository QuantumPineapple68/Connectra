package com.nachiket.connectra.utility;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.HashMap;
import java.util.Map;

/**
 * Force-update gate backed by Firebase Remote Config.
 * If the installed versionCode is lower than "min_version_code",
 * a non-dismissable dialog sends the user to the Play Store.
 */
public class UpdateChecker {

    private static final String TAG = "UpdateChecker";
    private static final String KEY_MIN_VERSION = "min_version_code";
    private static final String KEY_UPDATE_URL = "update_url";
    private static final String DEFAULT_UPDATE_URL =
            "https://play.google.com/store/apps/details?id=com.nachiket.connectra";

    public static void check(Activity activity) {
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();

        FirebaseRemoteConfigSettings settings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        remoteConfig.setConfigSettingsAsync(settings);

        Map<String, Object> defaults = new HashMap<>();
        defaults.put(KEY_MIN_VERSION, 0);
        defaults.put(KEY_UPDATE_URL, DEFAULT_UPDATE_URL);
        remoteConfig.setDefaultsAsync(defaults);

        remoteConfig.fetchAndActivate().addOnCompleteListener(activity, task -> {
            long minVersion = remoteConfig.getLong(KEY_MIN_VERSION);
            String updateUrl = remoteConfig.getString(KEY_UPDATE_URL);
            long installedVersion = getVersionCode(activity);

            if (installedVersion > 0 && minVersion > installedVersion) {
                showForceUpdateDialog(activity,
                        updateUrl.isEmpty() ? DEFAULT_UPDATE_URL : updateUrl);
            }
        });
    }

    private static long getVersionCode(Activity activity) {
        try {
            PackageInfo info = activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0);
            return info.versionCode;
        } catch (Exception e) {
            Log.e(TAG, "Could not read versionCode: " + e.getMessage());
            return -1;
        }
    }

    private static void showForceUpdateDialog(Activity activity, String updateUrl) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        new AlertDialog.Builder(activity)
                .setTitle("Update Required")
                .setMessage("A new version of Connectra is available. Please update to continue using the app.")
                .setCancelable(false)
                .setPositiveButton("Update", (dialog, which) -> {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl)));
                    activity.finishAffinity();
                })
                .show();
    }
}
