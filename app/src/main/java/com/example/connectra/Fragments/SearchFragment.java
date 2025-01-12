// Updated SearchFragment.java using Firebase Realtime Database
package com.example.connectra.Fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectra.R;
import com.example.connectra.adapter.UserAdapter;
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
    private UserAdapter userAdapter;
    private List<NewUser> usersList, filteredList;
    private DatabaseReference databaseRef;
    private AutoCompleteTextView searchBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        // Initialize components
        recyclerView = view.findViewById(R.id.recycler_view);
        searchBar = view.findViewById(R.id.search_bar);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        usersList = new ArrayList<>();
        filteredList = new ArrayList<>();
        userAdapter = new UserAdapter(filteredList);
        recyclerView.setAdapter(userAdapter);

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
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                usersList.clear();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    // Fetch each field explicitly like in HomeFragment
                    String userId = userSnapshot.getKey();
                    if (!userId.equals(currentUserId)) {
                        String name = userSnapshot.child("name").getValue(String.class);
                        String myskill = userSnapshot.child("myskill").getValue(String.class);
                        String goalskill = userSnapshot.child("goalskill").getValue(String.class);
                        String gender = userSnapshot.child("gender").getValue(String.class);
                        String age = userSnapshot.child("age").getValue(String.class);
                        String bio = userSnapshot.child("bio").getValue(String.class);
                        String userName = userSnapshot.child("username").getValue(String.class);

                        // Create NewUser object with all fields
                        NewUser user = new NewUser(name, myskill, goalskill, gender, age, userId, userName, bio);
                        usersList.add(user);
                    }
                }
                // Update filtered list and adapter
                filteredList.clear();
                filteredList.addAll(usersList);
                userAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Database Error", error.getMessage());
            }
        });
    }

    private void filterUsersBySkill(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(usersList); // Show all users when query is empty
        } else {
            for (NewUser user : usersList) {
                if (user.getMyskill() != null && user.getMyskill().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(user);
                }
            }
        }
        userAdapter.notifyDataSetChanged(); // Notify the adapter
    }
}
