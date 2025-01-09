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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private List<NewUser> usersList, filteredList;
    private FirebaseFirestore firestore;
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

        firestore = FirebaseFirestore.getInstance();

        // Fetching data from Firestore
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
        CollectionReference usersRef = firestore.collection("Users");
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        usersRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore Error", error.getMessage());
                return;
            }

            usersList.clear();

            if (value != null) {
                for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                    NewUser user = doc.toObject(NewUser.class);
                    if (!user.getId().equals(currentUserId)) { // Exclude current user
                        usersList.add(user);
                    }
                }

                // Initially show all users
                filteredList.clear();
                filteredList.addAll(usersList);
                userAdapter.notifyDataSetChanged();
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
