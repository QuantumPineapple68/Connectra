package com.nachiket.connectra;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.nachiket.connectra.Authentication.LoginActivity;
import com.nachiket.connectra.Fragments.HomeFragment;
import com.nachiket.connectra.Fragments.ProfileFragment;
import com.nachiket.connectra.Fragments.ScheduleFragment;
import com.nachiket.connectra.Fragments.SearchFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nachiket.connectra.utility.SystemBarsUtil;
import com.nachiket.connectra.utility.UpdateChecker;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Fragment selectorFragment;
    private static final String TAG = "MainActivity";
    private DatabaseReference detonatorRef;
    private ValueEventListener stateListener;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        SystemBarsUtil.apply(this, getResources().getColor(R.color.white, getTheme()));

        // Initialize Firebase Apps
        initializeFirebaseApps();

        // Initialize UI
        initializeUI();

        // Force-update gate (Remote Config, min_version_code vs installed versionCode)
        UpdateChecker.check(this);

        // Needed on Android 13+ so FCM pushes from the "all" topic are visible
        requestNotificationPermission();

        // Set default fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkLoginStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void checkLoginStatus() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // User is not logged in, redirect to LoginActivity
            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(loginIntent);
            finish();
        } else {
            // Check if user has completed profile
            FirebaseDatabase.getInstance().getReference("Users")
                    .child(currentUser.getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            boolean isBanned = snapshot.child("banned").getValue(Boolean.class) != null
                                    ? snapshot.child("banned").getValue(Boolean.class)
                                    : false;
                            if (!snapshot.exists() && isBanned) {
                                // User hasn't completed profile, redirect to ExtraDetailsActivity
                                Intent intent = new Intent(MainActivity.this, ExtraDetailsActivity.class);
                                intent.putExtra("email", currentUser.getEmail());
                                startActivity(intent);
                                finish();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(MainActivity.this, "Database error: " + error.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void initializeFirebaseApps() {
        try {
            // Initialize default Firebase instance
            FirebaseApp.initializeApp(this);

            // Initialize secondary Firebase instance if it doesn't exist
            FirebaseApp secondaryApp = null;
            try {
                secondaryApp = FirebaseApp.getInstance("secondary");
            } catch (IllegalStateException e) {
                // Secondary app doesn't exist yet
            }

            if (secondaryApp == null) {
                // Get the options from the default app
                FirebaseOptions options = FirebaseApp.getInstance().getOptions();

                // Initialize the secondary app
                FirebaseApp.initializeApp(this, options, "secondary");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase: " + e.getMessage());
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST);
        }
    }

    private void initializeUI() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        int itemId = item.getItemId();

                        if (itemId == R.id.nav_home) {
                            selectorFragment = new HomeFragment();
                        } else if (itemId == R.id.nav_search) {
                            selectorFragment = new SearchFragment();
                        } else if (itemId == R.id.nav_person) {
                            selectorFragment = new ProfileFragment();
                        } else if (itemId == R.id.nav_calender) {
                            selectorFragment = new ScheduleFragment();
                        }

                        if (selectorFragment != null) {
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, selectorFragment)
                                    .commit();
                        }
                        return true;
                    }
                });
    }

    @Override
    protected void onDestroy() {
        if (stateListener != null) {
            detonatorRef.removeEventListener(stateListener);
        }
        super.onDestroy();
        // Cleanup if needed
        try {
            FirebaseApp secondaryApp = FirebaseApp.getInstance("secondary");
            if (secondaryApp != null) {
                secondaryApp.delete();
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error cleaning up Firebase: " + e.getMessage());
        }
    }
}