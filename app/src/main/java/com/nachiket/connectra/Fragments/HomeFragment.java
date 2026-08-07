package com.nachiket.connectra.Fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.color.MaterialColors;
import com.nachiket.connectra.Authentication.LoginActivity;
import com.nachiket.connectra.ChangeSkillActivity;
import com.nachiket.connectra.ExtraDetailsActivity;
import com.nachiket.connectra.InboxActivity;
import com.nachiket.connectra.MainActivity;
import com.nachiket.connectra.model.ChatTexts;
import com.nachiket.connectra.R;
import com.nachiket.connectra.adapter.MatchAdapter;
import com.nachiket.connectra.adapter.TileAdapter;
import com.nachiket.connectra.model.NewUser;
import com.nachiket.connectra.utility.ConnectraApi;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String MATCH_PREFS = "connectra_matches";

    private RecyclerView recyclerView;
    private TileAdapter tileAdapter;
    private List<NewUser> userList;
    private DatabaseReference databaseRef, detonatorRef;
    private TextView hi;
    private ProgressBar loadingProgressBar;
    private FirebaseAuth auth;
    private ImageView inbox;
    private SwipeRefreshLayout swipeRefreshLayout;

    // "✨ Matches for you" carousel (AI Skill-Match Engine)
    private View matchesContainer;
    private RecyclerView matchesRecycler;
    private ShimmerFrameLayout matchesShimmer;
    private MatchAdapter matchAdapter;
    private List<ConnectraApi.SkillMatch> matchList;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        Window window = requireActivity().getWindow();
        int statusBarColor = getResources().getColor(R.color.white, requireContext().getTheme());
        window.setStatusBarColor(statusBarColor);
        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(window, window.getDecorView());
        boolean isLightBackground = MaterialColors.isColorLight(statusBarColor);
        windowInsetsController.setAppearanceLightStatusBars(isLightBackground);

        auth = FirebaseAuth.getInstance();
        hi = view.findViewById(R.id.welcome_name);

        // Initialize Realtime Database and RecyclerView
        databaseRef = FirebaseDatabase.getInstance().getReference("Users");
        detonatorRef = FirebaseDatabase.getInstance().getReference("Detonator");

        recyclerView = view.findViewById(R.id.recycler_view_home);
        inbox = view.findViewById(R.id.inbox);
        loadingProgressBar = view.findViewById(R.id.loading_progress_bar);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayoutHome);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.theme));
        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchUserData();
            fetchUsersFromDatabase();
        });

        // Matches carousel setup
        matchesContainer = view.findViewById(R.id.matches_container);
        matchesRecycler = view.findViewById(R.id.matches_recycler);
        matchesShimmer = view.findViewById(R.id.matches_shimmer);
        matchList = new ArrayList<>();
        matchAdapter = new MatchAdapter(getContext(), matchList);
        matchesRecycler.setLayoutManager(new LinearLayoutManager(getContext(),
                LinearLayoutManager.HORIZONTAL, false));
        matchesRecycler.setAdapter(matchAdapter);
        loadCachedMatches();

        // Initialize user list and adapter
        userList = new ArrayList<>();
        tileAdapter = new TileAdapter(getContext(), userList);
        recyclerView.setAdapter(tileAdapter);

        // Dynamically set the span count
        int spanCount = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? 4 : 2;
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), spanCount);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(8)); // 16dp spacing

        loadingProgressBar.setVisibility(View.VISIBLE);
        if (matchList.isEmpty()) {
            showMatchesLoading();
        }
        fetchUserData();
        fetchUsersFromDatabase();

        // extra check to fix clear data bug
        setupDetonatorListener();

        inbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), InboxActivity.class);
                startActivity(intent);
            }
        });

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

    private void setupDetonatorListener() {
        detonatorRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isActive = snapshot.child("isActive_1,2").getValue(Boolean.class);
                if (isActive != null) {
                    if (!isActive) {
                        showExitDialog();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AppDetonator", "Database error: " + error.getMessage());
            }
        });
    }


    private void fetchUserData() {
        if (auth.getCurrentUser() == null) {
            toast("User not logged in");
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        databaseRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    boolean isBanned = snapshot.child("banned").getValue(Boolean.class) != null
                            ? snapshot.child("banned").getValue(Boolean.class)
                            : false;
                    boolean hasOtherData = snapshot.getChildrenCount() > 1;

                    if (isBanned && hasOtherData){
                        auth.signOut();
                        Intent loginIntent = new Intent(getContext(), LoginActivity.class);
                        loginIntent.putExtra("suspended", true);
                        startActivity(loginIntent);
                        getActivity().finish();
                    }
                    else if(isBanned && !hasOtherData){
                        auth.signOut();
                        Intent loginIntent = new Intent(getContext(), LoginActivity.class);
                        loginIntent.putExtra("banned", true);
                        startActivity(loginIntent);
                        getActivity().finish();
                    }
                    else {
                        String fullName = snapshot.child("name").getValue(String.class);
                        if (getActivity() != null && fullName != null) {
                            String[] nameParts = fullName.split(" ");
                            String firstName = nameParts[0];
                            hi.setText("Hi, " + firstName);
                        }

                        String myskill = snapshot.child("myskill").getValue(String.class);
                        String goalskill = snapshot.child("goalskill").getValue(String.class);
                        fetchMatches(userId, myskill, goalskill, fullName);
                    }

                } else {
                    auth.signOut();
                    toast("Fill the Details to continue");
                    Intent loginIntent = new Intent(getContext(), ExtraDetailsActivity.class);
                    startActivity(loginIntent);
                    getActivity().finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    hideMatchesLoading();
                    renderMatches();
                }
            }
        });
    }

    private void fetchMatches(String uid, String myskill, String goalskill, String name) {
        if (myskill == null || myskill.trim().isEmpty()
                || goalskill == null || goalskill.trim().isEmpty()) {
            hideMatchesLoading();
            matchesContainer.setVisibility(View.GONE);
            return;
        }

        showMatchesLoading();

        ConnectraApi.fetchMatches(uid, myskill, goalskill, name, new ConnectraApi.MatchCallback() {
            @Override
            public void onResult(List<ConnectraApi.SkillMatch> matches) {
                if (!isAdded()) return;
                hideMatchesLoading();
                // An empty answer (e.g. the engine is temporarily degraded) must not wipe
                // out the matches we already have on screen
                if (matches.isEmpty()) {
                    renderMatches();
                    return;
                }
                matchList.clear();
                matchList.addAll(matches);
                renderMatches();
                cacheMatches(uid, matches);
            }

            @Override
            public void onError(String message) {
                // The carousel is a bonus feature — fail silently and keep the last known matches
                Log.e("HomeFragment", "Match fetch failed: " + message);
                if (!isAdded()) return;
                hideMatchesLoading();
                renderMatches();
            }
        });
    }

    /** Shows the shimmer skeleton in place of the matches carousel while it loads. */
    private void showMatchesLoading() {
        // On swipe-refresh, keep the existing carousel visible instead of flashing the skeleton
        if (!matchList.isEmpty()) return;
        matchesContainer.setVisibility(View.VISIBLE);
        matchesRecycler.setVisibility(View.GONE);
        matchesShimmer.setVisibility(View.VISIBLE);
        matchesShimmer.startShimmer();
    }

    private void hideMatchesLoading() {
        matchesShimmer.stopShimmer();
        matchesShimmer.setVisibility(View.GONE);
    }

    /** Reflects {@link #matchList} on screen; the whole section hides only when it is empty. */
    private void renderMatches() {
        matchAdapter.notifyDataSetChanged();
        int visibility = matchList.isEmpty() ? View.GONE : View.VISIBLE;
        matchesRecycler.setVisibility(visibility);
        matchesContainer.setVisibility(visibility);
    }

    /**
     * Restores the matches of the last successful fetch so the carousel is on screen
     * instantly, and stays there when the edge API is unreachable or rate limited.
     */
    private void loadCachedMatches() {
        if (getContext() == null || auth.getCurrentUser() == null) return;
        String cached = getContext()
                .getSharedPreferences(MATCH_PREFS, Context.MODE_PRIVATE)
                .getString(auth.getCurrentUser().getUid(), null);
        if (cached == null) return;
        try {
            JSONArray arr = new JSONArray(cached);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject m = arr.optJSONObject(i);
                if (m == null) continue;
                matchList.add(new ConnectraApi.SkillMatch(
                        m.optString("uid"),
                        m.optString("name"),
                        m.optString("myskill"),
                        m.optString("goalskill"),
                        m.optInt("score"),
                        m.optBoolean("mutual")));
            }
        } catch (Exception e) {
            Log.e("HomeFragment", "Cached matches unreadable: " + e.getMessage());
            matchList.clear();
        }
        renderMatches();
    }

    private void cacheMatches(String uid, List<ConnectraApi.SkillMatch> matches) {
        if (getContext() == null) return;
        try {
            JSONArray arr = new JSONArray();
            for (ConnectraApi.SkillMatch match : matches) {
                JSONObject m = new JSONObject();
                m.put("uid", match.uid);
                m.put("name", match.name);
                m.put("myskill", match.myskill);
                m.put("goalskill", match.goalskill);
                m.put("score", match.score);
                m.put("mutual", match.mutual);
                arr.put(m);
            }
            SharedPreferences prefs = getContext().getSharedPreferences(MATCH_PREFS, Context.MODE_PRIVATE);
            prefs.edit().putString(uid, arr.toString()).apply();
        } catch (Exception e) {
            Log.e("HomeFragment", "Caching matches failed: " + e.getMessage());
        }
    }

    private void fetchUsersFromDatabase() {
        String currentUserId = auth.getCurrentUser().getUid();
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
                                boolean profileApproved = userSnapshot.child("profileApproved").getValue(Boolean.class) != null
                                        ? userSnapshot.child("profileApproved").getValue(Boolean.class)
                                        : false;
                                boolean cerfApproved = userSnapshot.child("cerfApproved").getValue(Boolean.class) != null
                                        ? userSnapshot.child("cerfApproved").getValue(Boolean.class)
                                        : false;
                                boolean isBanned = userSnapshot.child("banned").getValue(Boolean.class) != null
                                        ? userSnapshot.child("banned").getValue(Boolean.class)
                                        : false;

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
                                        userName, bio, profileImage, rating, certificate, profileApproved, cerfApproved, isBanned);

                                // Add to appropriate list based on message status
                                if (unreadMessages.containsKey(userId)) {
                                    user.setHasUnreadMessages(true);
                                    user.setLastMessageTimestamp(lastMessageTimes.get(userId));
                                    if (!isBanned) {
                                        usersWithMessages.add(user);
                                    }
                                } else {
                                    if (!isBanned) {
                                        usersWithoutMessages.add(user);
                                    }
                                }
                            }
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                loadingProgressBar.setVisibility(View.GONE);
                                swipeRefreshLayout.setRefreshing(false);
                                tileAdapter.notifyDataSetChanged();
                            });
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

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Context context = getContext(); // Get context safely
                loadingProgressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
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

    private void showExitDialog() {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("App Unavailable")
                    .setMessage("Detonator Triggered! This application is under maintenance. Try again later.")
                    .setCancelable(false)
                    .setPositiveButton("Exit", (dialog, which) -> forceAppExit())
                    .show();
        });
    }

    private void forceAppExit() {
        if (isAdded() && getActivity() != null) {  // Check if Fragment is attached
            requireActivity().finishAffinity();  // Close all activities

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            }, 100);
        }
    }


    private void toast(String msg){
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
