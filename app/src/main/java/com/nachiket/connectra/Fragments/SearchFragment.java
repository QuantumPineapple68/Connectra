// Updated SearchFragment.java using Firebase Realtime Database
package com.nachiket.connectra.Fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.color.MaterialColors;
import com.nachiket.connectra.R;
import com.nachiket.connectra.adapter.NewUserAdapter;
import com.nachiket.connectra.model.NewUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private RecyclerView recyclerView;
    private NewUserAdapter newUserAdapter;
    private List<NewUser> usersList, filteredList;
    private DatabaseReference databaseRef;
    private AutoCompleteTextView searchBar;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        Window window = requireActivity().getWindow();
        int statusBarColor = getResources().getColor(R.color.inverted_top, requireContext().getTheme());
        window.setStatusBarColor(statusBarColor);
        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(window, window.getDecorView());
        boolean isLightBackground = MaterialColors.isColorLight(statusBarColor);
        windowInsetsController.setAppearanceLightStatusBars(isLightBackground);

        // Initialize components
        recyclerView = view.findViewById(R.id.recycler_view);
        searchBar = view.findViewById(R.id.search_bar);
        progressBar=view.findViewById(R.id.loading_progress_bar_search);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayoutSearch);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.theme));
        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchUsers();
        });


        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        usersList = new ArrayList<>();
        filteredList = new ArrayList<>();
        newUserAdapter = new NewUserAdapter(filteredList);
        recyclerView.setAdapter(newUserAdapter);

        databaseRef = FirebaseDatabase.getInstance().getReference("Users");


        // Fetching data from Firebase Realtime Database
        fetchUsers();


        // Add search functionality
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsersBySkill(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }


    private void fetchUsers() {

        if (!swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                usersList.clear();
                filteredList.clear(); // Clear both lists before adding users

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    String userId = userSnapshot.getKey();
                    if (!userId.equals(currentUserId)) {
                        boolean isBanned = userSnapshot.child("banned").getValue(Boolean.class) != null
                                ? userSnapshot.child("banned").getValue(Boolean.class)
                                : false;

                        // Skip banned users
                        if (isBanned) continue;

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

                        float rating = 0f;
                        DataSnapshot ratingsSnapshot = userSnapshot.child("ratings");
                        DataSnapshot revSnapshot = userSnapshot.child("rev");

                        if (ratingsSnapshot.exists() && revSnapshot.exists()) {
                            float totalRev = revSnapshot.getValue(Float.class);
                            long totalReviews = ratingsSnapshot.getChildrenCount();
                            rating = totalReviews > 0 ? totalRev / totalReviews : 0f;
                        }

                        NewUser newUser = new NewUser(name, myskill, goalskill, gender, age, userId,
                                userName, bio, profileImage, rating, certificate, profileApproved, cerfApproved, false);

                        usersList.add(newUser);
                    }
                }
                // Update filtered list and adapter
                filteredList.addAll(usersList);
                newUserAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Database Error", error.getMessage());
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }



    private void filterUsersBySkill(String query) {
        List<NewUser> tempList = new ArrayList<>();
        if (query.isEmpty()) {
            tempList.addAll(usersList);
        } else {
            for (NewUser user : usersList) {
                if (user.getMyskill() != null && user.getMyskill().toLowerCase().contains(query.toLowerCase())) {
                    tempList.add(user);
                }
            }
        }
        filteredList.clear();
        filteredList.addAll(tempList);
        newUserAdapter.notifyDataSetChanged(); // Notify the adapter
    }

}
