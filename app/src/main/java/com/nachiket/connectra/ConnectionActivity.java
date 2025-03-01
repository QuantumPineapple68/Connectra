package com.nachiket.connectra;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nachiket.connectra.adapter.UserAdapter;
import com.nachiket.connectra.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConnectionActivity extends AppCompatActivity {

    private EditText etSearch;
    private RecyclerView rvUsers;
    private TextView tvNoUsers;
    private ProgressBar progressBar;

    private DatabaseReference usersRef;
    private DatabaseReference connectionsRef;
    private String currentUserId;

    private UserAdapter userAdapter;
    private List<User> userList;
    private List<User> filteredList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        etSearch = findViewById(R.id.et_search);
        rvUsers = findViewById(R.id.rv_users);
        tvNoUsers = findViewById(R.id.tv_no_users);
        progressBar = findViewById(R.id.progress_bar);

        currentUserId = FirebaseAuth.getInstance().getUid();
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        connectionsRef = FirebaseDatabase.getInstance().getReference("Connections");

        userList = new ArrayList<>();
        filteredList = new ArrayList<>();
        userAdapter = new UserAdapter (filteredList, currentUserId); // Pass currentUserId
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(userAdapter);

        fetchAllUsers();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        userAdapter.setOnConnectClickListener(user -> sendConnectionRequest(user));
    }

    private void fetchAllUsers() {
        progressBar.setVisibility(View.VISIBLE);
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    if (!userSnapshot.getKey().equals(currentUserId)) {
                        User user = userSnapshot.getValue(User.class);
                        if (user != null && isValidUser(user)) {
                            user.setId(userSnapshot.getKey());
                            userList.add(user);
                        }
                    }
                }

                // Fetch current user's connections to determine statuses
                connectionsRef.child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot connSnapshot) {
                        Map<String, String> statusMap = new HashMap<>();
                        for (DataSnapshot connUserSnapshot : connSnapshot.getChildren()) {
                            String userId = connUserSnapshot.getKey();
                            String status = connUserSnapshot.child("status").getValue(String.class);
                            statusMap.put(userId, status);
                        }

                        // Update each user's connection status
                        for (User user : userList) {
                            String status = statusMap.get(user.getId());
                            user.setConnectionStatus(status != null ? status : "none");
                        }

                        filteredList.clear();
                        filteredList.addAll(userList);
                        userAdapter.notifyDataSetChanged();
                        updateListVisibility(); // Update visibility here
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ConnectionActivity.this, "Error fetching connections: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ConnectionActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isValidUser(User user) {
        return !TextUtils.isEmpty(user.getName()) && !TextUtils.isEmpty(user.getUsername());
    }

    private void filterUsers(String query) {
        List<User> tempList = new ArrayList<>();

        if (query.isEmpty()) {
            tempList.addAll(userList);
        } else {
            for (User user : userList) {
                if (user.getName() != null && user.getName().toLowerCase().contains(query.toLowerCase())) {
                    tempList.add(user);
                }
            }
        }

        filteredList.clear();
        filteredList.addAll(tempList);
        userAdapter.notifyDataSetChanged();
        updateListVisibility(); // Update visibility here
    }

    private void sendConnectionRequest(User user) {
        progressBar.setVisibility(View.VISIBLE);

        connectionsRef.child(currentUserId).orderByChild("status").equalTo("connected")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Toast.makeText(ConnectionActivity.this,
                                    "You're already connected to someone", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            return;
                        }

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("/Connections/" + currentUserId + "/" + user.getId() + "/status", "sent");
                        updates.put("/Connections/" + currentUserId + "/" + user.getId() + "/timestamp", System.currentTimeMillis());
                        updates.put("/Connections/" + user.getId() + "/" + currentUserId + "/status", "pending");
                        updates.put("/Connections/" + user.getId() + "/" + currentUserId + "/timestamp", System.currentTimeMillis());

                        FirebaseDatabase.getInstance().getReference().updateChildren(updates)
                                .addOnSuccessListener(aVoid -> {
                                    progressBar.setVisibility(View.GONE);
                                    Snackbar.make(findViewById(android.R.id.content), "Connection request sent", Snackbar.LENGTH_SHORT).show();
                                    user.setConnectionStatus("sent");
                                    userAdapter.notifyDataSetChanged();
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(ConnectionActivity.this, "Failed to send request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ConnectionActivity.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void updateListVisibility() {
        if (filteredList.isEmpty()) {
            tvNoUsers.setVisibility(View.VISIBLE);
            rvUsers.setVisibility(View.GONE);
        } else {
            tvNoUsers.setVisibility(View.GONE);
            rvUsers.setVisibility(View.VISIBLE);
        }
    }
}
