package com.example.connectra;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.app.AlertDialog;
import android.widget.Toast;

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

        // Initialize Firebase Apps
        initializeFirebaseApps();

        // Initialize UI
        initializeUI();

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
        checkAppState();
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
        // Check if activity is finishing or destroyed
        if (isFinishing() || isDestroyed()) {
            return;
        }

        try {
            runOnUiThread(() -> {
                try {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("App Unavailable")
                            .setMessage("This application is currently under maintenance. Please try again later.")
                            .setCancelable(false)
                            .setPositiveButton("Exit", (dialog, which) -> {
                                finishAffinity();
                            })
                            .create()
                            .show();
                } catch (Exception e) {
                    Log.e(TAG, "Error showing dialog: " + e.getMessage());
                    finishAffinity(); // Safely exit if we can't show the dialog
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in showExitDialog: " + e.getMessage());
            finishAffinity(); // Safely exit if we can't show the dialog
        }
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
                            if (!snapshot.exists()) {
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