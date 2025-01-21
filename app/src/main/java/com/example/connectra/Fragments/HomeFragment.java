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

import com.example.connectra.ChatTexts;
import com.example.connectra.R;
import com.example.connectra.adapter.TileAdapter;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private TileAdapter tileAdapter;
    private List<NewUser> userList;
    private DatabaseReference databaseRef;
    private TextView hi;
    String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

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
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("Messages");

        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                HashMap<String, Boolean> unreadMessages = new HashMap<>();
                HashMap<String, Long> lastMessageTimes = new HashMap<>();

                // First get unread messages status
                for (DataSnapshot conversationSnapshot : snapshot.getChildren()) {
                    String[] users = conversationSnapshot.getKey().split("_");
                    if (users.length == 2) {
                        String otherUserId = users[0].equals(currentUserId) ? users[1] : users[0];

                        for (DataSnapshot messageSnapshot : conversationSnapshot.getChildren()) {
                            ChatTexts message = messageSnapshot.getValue(ChatTexts.class);
                            if (message != null && message.getReceiverId().equals(currentUserId)
                                    && !message.isRead()) {
                                unreadMessages.put(otherUserId, true);
                                lastMessageTimes.put(otherUserId, message.getTimestamp());
                            }
                        }
                    }
                }

                // Then fetch user data
                databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        userList.clear();
                        List<NewUser> usersWithMessages = new ArrayList<>();
                        List<NewUser> usersWithoutMessages = new ArrayList<>();

                        for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                            String userId = userSnapshot.getKey();
                            if (!userId.equals(currentUserId)) {
                                // Get user data
                                String name = userSnapshot.child("name").getValue(String.class);
                                String myskill = userSnapshot.child("myskill").getValue(String.class);
                                String goalskill = userSnapshot.child("goalskill").getValue(String.class);
                                String gender = userSnapshot.child("gender").getValue(String.class);
                                String age = userSnapshot.child("age").getValue(String.class);
                                String bio = userSnapshot.child("bio").getValue(String.class);
                                String userName = userSnapshot.child("username").getValue(String.class);
                                String profileImage = userSnapshot.child("profileImage").getValue(String.class);
                                String certificate = userSnapshot.child("certificateUrl").getValue(String.class);

                                // Calculate rating
                                float rating = 0f;
                                DataSnapshot ratingsSnapshot = userSnapshot.child("ratings");
                                DataSnapshot revSnapshot = userSnapshot.child("rev");

                                if (ratingsSnapshot.exists() && revSnapshot.exists()) {
                                    float totalRev = revSnapshot.getValue(Float.class);
                                    long totalReviews = ratingsSnapshot.getChildrenCount();
                                    rating = totalReviews > 0 ? totalRev / totalReviews : 0f;
                                }

                                // Create user object
                                NewUser user = new NewUser(name, myskill, goalskill, gender, age, userId,
                                        userName, bio, profileImage, rating, certificate);

                                // Add to appropriate list based on message status
                                if (unreadMessages.containsKey(userId)) {
                                    user.setHasUnreadMessages(true);
                                    user.setLastMessageTimestamp(lastMessageTimes.get(userId));
                                    usersWithMessages.add(user);
                                } else {
                                    usersWithoutMessages.add(user);
                                }
                            }
                        }

                        // Sort users with messages by timestamp
                        Collections.sort(usersWithMessages,
                                (u1, u2) -> Long.compare(u2.getLastMessageTimestamp(),
                                        u1.getLastMessageTimestamp()));

                        // Combine lists: users with messages first, then others
                        userList.addAll(usersWithMessages);
                        userList.addAll(usersWithoutMessages);

                        tileAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(getContext(), "Failed to fetch data: " +
                                error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to fetch messages: " +
                        error.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void fetchUsersWithUnreadStatus(String currentUserId,
                                            HashMap<String, Boolean> unreadMessages,
                                            HashMap<String, Long> lastMessageTimes) {
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                List<NewUser> usersWithMessages = new ArrayList<>();
                List<NewUser> usersWithoutMessages = new ArrayList<>();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    if (!userId.equals(currentUserId)) {
                        NewUser user = createUserFromSnapshot(userSnapshot);

                        if (unreadMessages.containsKey(userId)) {
                            user.setHasUnreadMessages(true);
                            user.setLastMessageTimestamp(lastMessageTimes.get(userId));
                            usersWithMessages.add(user);
                        } else {
                            usersWithoutMessages.add(user);
                        }
                    }
                }

                // Sort users with messages by timestamp
                Collections.sort(usersWithMessages,
                        (u1, u2) -> Long.compare(u2.getLastMessageTimestamp(),
                                u1.getLastMessageTimestamp()));

                // Combine lists
                userList.addAll(usersWithMessages);
                userList.addAll(usersWithoutMessages);

                tileAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to fetch data: " +
                        error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private NewUser createUserFromSnapshot(DataSnapshot userSnapshot) {
        String name = userSnapshot.child("name").getValue(String.class);
        String myskill = userSnapshot.child("myskill").getValue(String.class);
        String goalskill = userSnapshot.child("goalskill").getValue(String.class);
        String gender = userSnapshot.child("gender").getValue(String.class);
        String age = userSnapshot.child("age").getValue(String.class);
        String userId = userSnapshot.getKey();
        String userName = userSnapshot.child("username").getValue(String.class);
        String bio = userSnapshot.child("bio").getValue(String.class);
        String profileImage = userSnapshot.child("profileImage").getValue(String.class);
        String certificate = userSnapshot.child("certificateUrl").getValue(String.class);

        float rating = 0f;
        DataSnapshot ratingsSnapshot = userSnapshot.child("ratings");
        DataSnapshot revSnapshot = userSnapshot.child("rev");

        if (ratingsSnapshot.exists() && revSnapshot.exists()) {
            float totalRev = revSnapshot.getValue(Float.class);
            long totalReviews = ratingsSnapshot.getChildrenCount();
            rating = totalReviews > 0 ? totalRev / totalReviews : 0f;
        }

        return new NewUser(name, myskill, goalskill, gender, age, userId,
                userName, bio, profileImage, rating, certificate);
    }
}
