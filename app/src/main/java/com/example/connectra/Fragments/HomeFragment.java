package com.example.connectra.Fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectra.R;
import com.example.connectra.adapter.TileAdapter;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private TileAdapter tileAdapter;
    private List<NewUser> userList;
    private DatabaseReference databaseRef;
    private TextView hi;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        hi = view.findViewById(R.id.welcome_name);

        // Initialize Realtime Database and RecyclerView
        databaseRef = FirebaseDatabase.getInstance().getReference("Users");
        recyclerView = view.findViewById(R.id.recycler_view_home);

        // Dynamically set the span count
        int spanCount = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 4 : 2;
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), spanCount);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(8)); // 16dp spacing

        // Initialize user list and adapter
        userList = new ArrayList<>();
        tileAdapter = new TileAdapter(getContext(), userList);
        recyclerView.setAdapter(tileAdapter);

        fetchUserData();
        fetchUsersFromDatabase();
        saveFCMToken(); // Save FCM token logic here

        if (!isInternetAvailable()) {
            showNoInternetDialog();
        }

        return view;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Update span count on orientation change
        int spanCount = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ? 4 : 2;
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), spanCount);
        recyclerView.setLayoutManager(gridLayoutManager);
        tileAdapter.notifyDataSetChanged();
    }

    private void saveFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String token = task.getResult();
                        Log.d("FCM Token", "Token: " + token);


                        if (!isAdded()) {
                            // Fragment is no longer attached, abort further operations
                            return;
                        }

                        // Save the token in SharedPreferences
                        Context context = getContext();
                        if (context != null) {
                            SharedPreferences prefs = context.getSharedPreferences("FCM", Context.MODE_PRIVATE);
                            prefs.edit().putString("token", token).apply();
                        }

                        // If the user is logged in, update the token in the database
                        FirebaseAuth auth = FirebaseAuth.getInstance();
                        if (auth.getCurrentUser() != null) {
                            String userId = auth.getCurrentUser().getUid();
                            databaseRef.child(userId).child("fcmToken").setValue(token)
                                    .addOnCompleteListener(dbTask -> {
                                        if (!dbTask.isSuccessful()) {
                                            if (isAdded()) {
                                                Toast.makeText(getContext(), "Failed to update FCM token", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                        }
                    } else {
                        Log.e("FCM Token", "Fetching token failed", task.getException());
                    }
                });
    }


    private void fetchUserData() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        databaseRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String fullName = snapshot.child("name").getValue(String.class);
                    if (getActivity() != null && fullName != null) {
                        hi.setText("Hi, " + fullName);
                    }
                } else {
                    Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to fetch data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUsersFromDatabase() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear(); // Clear the list before adding new data
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    if (!userId.equals(currentUserId)) { // Exclude current user's profile
                        String name = userSnapshot.child("name").getValue(String.class);
                        String myskill = userSnapshot.child("myskill").getValue(String.class);
                        String goalskill = userSnapshot.child("goalskill").getValue(String.class);
                        String gender = userSnapshot.child("gender").getValue(String.class);
                        String age = userSnapshot.child("age").getValue(String.class);
                        String bio = userSnapshot.child("bio").getValue(String.class);
                        String userName = userSnapshot.child("username").getValue(String.class);
                        String profileImage = userSnapshot.child("profileImage").getValue(String.class);
                        String certificate = userSnapshot.child("certificateUrl").getValue(String.class);

                        Log.e("test1", profileImage + "");

                        float rating = 0f;
                        DataSnapshot ratingsSnapshot = userSnapshot.child("ratings");
                        DataSnapshot revSnapshot = userSnapshot.child("rev");

                        if (ratingsSnapshot.exists() && revSnapshot.exists()) {
                            float totalRev = revSnapshot.getValue(Float.class);
                            long totalReviews = ratingsSnapshot.getChildrenCount();
                            rating = totalReviews > 0 ? totalRev / totalReviews : 0f;

                            Log.e("rat",rating+"");
                        }

                        userList.add(new NewUser(name, myskill, goalskill, gender, age, userId,
                                userName, bio, profileImage, rating, certificate));
                    }
                }
                // Notify adapter of data changes
                tileAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to fetch data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spacing;

        public GridSpacingItemDecoration(int spacing) {
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.left = spacing;
            outRect.right = spacing;
            outRect.bottom = spacing;

            // Add top margin only for the first row to avoid double space between items
            if (parent.getChildAdapterPosition(view) < 2) {
                outRect.top = spacing;
            }
        }
    }

    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkCapabilities capabilities =
                    connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            if (capabilities != null) {
                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            }
        }
        return false;
    }

    // Show a popup dialog when there is no internet
    private void showNoInternetDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("No Internet Connection")
                .setMessage("Please check your internet connection and try again.")
                .setCancelable(false) // User can't dismiss the dialog by tapping outside
                .setPositiveButton("Retry", (dialog, which) -> {
                    // Retry logic: Check for internet again
                    if (!isInternetAvailable()) {
                        showNoInternetDialog(); // Show the dialog again if still no internet
                    } else {
                        dialog.dismiss(); // Dismiss if internet is available
                    }
                })
                .setNegativeButton("Exit", (dialog, which) -> {
                    requireActivity().finish(); // Exit the app
                })
                .show();
    }
}
