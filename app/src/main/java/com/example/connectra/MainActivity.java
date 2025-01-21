package com.example.connectra;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.connectra.Fragments.HomeFragment;
import com.example.connectra.Fragments.ProfileFragment;
import com.example.connectra.Fragments.ScheduleFragment;
import com.example.connectra.Fragments.SearchFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Fragment selectorFragment;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if the user is logged in
        checkLoginStatus();

        // Initialize Firebase Apps
        initializeFirebaseApps();

        // Initialize UI
        initializeUI();

        // Set default fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();
    }

    private void checkLoginStatus() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // User is not logged in, redirect to LoginActivity
            Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(loginIntent);
            finish(); // Prevent returning to MainActivity
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
