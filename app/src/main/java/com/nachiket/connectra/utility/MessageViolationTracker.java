package com.nachiket.connectra.utility;

import android.content.Context;
import android.content.SharedPreferences;

public class MessageViolationTracker {
    private int consecutiveViolations = 0;
    private long restrictionEndTime = 0;
    private static final int MAX_VIOLATIONS = 3;
    private static final long RESTRICTION_DURATION = 10 * 60 * 1000; // 10 minutes in milliseconds
    private static final String PREFS_NAME = "MessageViolationPrefs";
    private static final String KEY_VIOLATIONS = "consecutiveViolations";
    private static final String KEY_RESTRICTION_END = "restrictionEndTime";
    private final Context context;

    public MessageViolationTracker(Context context) {
        this.context = context;
        loadState();
    }

    private void loadState() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        consecutiveViolations = prefs.getInt(KEY_VIOLATIONS, 0);
        restrictionEndTime = prefs.getLong(KEY_RESTRICTION_END, 0);
    }

    private void saveState() {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_VIOLATIONS, consecutiveViolations);
        editor.putLong(KEY_RESTRICTION_END, restrictionEndTime);
        editor.apply();
    }

    public void recordMessage(boolean containsViolation) {
        if (containsViolation) {
            consecutiveViolations++;
            if (consecutiveViolations >= MAX_VIOLATIONS) {
                restrictionEndTime = System.currentTimeMillis() + RESTRICTION_DURATION;
            }
        } else {
            consecutiveViolations = 0;
        }
        saveState();
    }

    public boolean isRestricted() {
        return System.currentTimeMillis() < restrictionEndTime;
    }

    public long getRemainingTime() {
        return Math.max(0, restrictionEndTime - System.currentTimeMillis());
    }

    public void reset() {
        consecutiveViolations = 0;
        restrictionEndTime = 0;
    }
}
