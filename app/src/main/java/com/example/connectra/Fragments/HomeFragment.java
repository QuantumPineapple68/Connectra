package com.example.connectra.Fragments;

import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private TileAdapter tileAdapter;
    private List<NewUser> userList;
    private FirebaseFirestore firestore;
    TextView hi;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        hi = view.findViewById(R.id.welcome_name);
        // Initialize Firestore and RecyclerView
        firestore = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recycler_view_home);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // Initialize user list and adapter
        userList = new ArrayList<>();
        tileAdapter = new TileAdapter(getContext(), userList);
        recyclerView.setAdapter(tileAdapter);

        fetchUserData();
        // Fetch users from Firestore
        fetchUsersFromFirestore();

        return view;
    }

    private void fetchUserData() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        firestore.collection("Users").document(userId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String fullName = snapshot.getString("name");
                        if (getActivity() != null && fullName != null) {
                            hi.setText("Hi, " + fullName);
                        }
                    } else {
                        Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to fetch data: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }


    private void fetchUsersFromFirestore() {
        CollectionReference usersRef = firestore.collection("Users");
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        usersRef.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userList.clear(); // Clear the list before adding new data
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String userId = document.getId(); // Firestore document ID (UID)
                            if (!userId.equals(currentUserId)) { // Exclude current user's profile
                                String name = document.getString("name");
                                String myskill = document.getString("myskill");
                                String goalskill = document.getString("goalskill");
                                String gender = document.getString("gender");
                                String age = document.getString("age");
                                String bio = document.getString("bio");
                                String userName = document.getString("username");

                                // Add user to the list
                                userList.add(new NewUser(name, myskill, goalskill, gender, age, userId, userName, bio));
                            }
                        }
                        // Notify adapter of data changes
                        tileAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(getContext(), "Failed to fetch data: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
