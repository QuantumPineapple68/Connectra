package com.example.connectra;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.app.AlertDialog;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private Fragment selectorFragment;
    private static final String TAG = "MainActivity";
    private DatabaseReference detonatorRef;
    private ValueEventListener stateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize detonator reference and check app state
        detonatorRef = FirebaseDatabase.getInstance().getReference("Detonator");
        checkAppState();

        setContentView(R.layout.activity_main);

        // Check if the user is logged in
        checkLoginStatus();

        // Initialize Firebase Apps
        initializeFirebaseApps();

        // Initialize UI
        initializeUI();

        // Set up state listener
        setupStateListener();

        // Set default fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAppState();
    }

    private void setupStateListener() {
        stateListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isActive = snapshot.child("isActive_1,0").getValue(Boolean.class);
                if (isActive != null && !isActive) {
                    showExitDialog();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        };
        detonatorRef.addValueEventListener(stateListener);
    }

    private void checkAppState() {
        detonatorRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Boolean isActive = task.getResult().child("isActive_1,0").getValue(Boolean.class);
                if (isActive != null && !isActive) {
                    showExitDialog();
                }
            }
        });
    }

    private void showExitDialog() {
        if (isFinishing()) return;

        new AlertDialog.Builder(this)
                .setTitle("App Unavailable")
                .setMessage("This application is currently under maintenance. Please try again later.")
                .setCancelable(false)
                .setPositiveButton("Exit", (dialog, which) -> {
                    finishAffinity();
                })
                .show();
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