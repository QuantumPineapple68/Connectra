package com.example.connectra.Fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectra.R;
import com.example.connectra.adapter.UserAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private RecyclerView recyclerView;
    private UserAdapter userAdapter; // Your adapter
    private List<NewUser> usersList; // List of users
    private FirebaseFirestore firestore;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        // Initialize RecyclerView and other components
        recyclerView = view.findViewById(R.id.recycler_view); // Ensure RecyclerView exists in your layout
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        usersList = new ArrayList<>();
        userAdapter = new UserAdapter(usersList); // Initialize the adapter with usersList
        recyclerView.setAdapter(userAdapter);

        firestore = FirebaseFirestore.getInstance();

        // Fetching data from Firestore
        fetchUsers();

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

            // Clear the existing list to avoid duplicates
            usersList.clear();

            if (value != null) {
                for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                    NewUser user = doc.toObject(NewUser.class);
                    if (!user.getId().equals(currentUserId)) { // Exclude current user
                        usersList.add(user);
                    }
                }

                // Notify the adapter about the data change
                userAdapter.notifyDataSetChanged();
            }
        });
    }
}
