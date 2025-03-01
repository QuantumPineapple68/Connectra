package com.nachiket.connectra;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nachiket.connectra.R;
import com.nachiket.connectra.adapter.ConnectionRequestAdapter;
import com.nachiket.connectra.model.ConnectionRequest;
import com.nachiket.connectra.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConnectionRequestsActivity extends AppCompatActivity {

    private RecyclerView rvRequests;
    private TextView tvNoRequests;
    private ProgressBar progressBar;
    private Button manage;
    private DatabaseReference usersRef;
    private DatabaseReference connectionsRef;
    private String currentUserId;

    private ConnectionRequestAdapter requestAdapter;
    private List<User> requestList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection_requests);

        // Initialize views
        rvRequests = findViewById(R.id.rv_requests);
        tvNoRequests = findViewById(R.id.tv_no_requests);
        progressBar = findViewById(R.id.progress_bar);
        manage = findViewById(R.id.btn_manage);

        // Initialize Firebase
        currentUserId = FirebaseAuth.getInstance().getUid();
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        connectionsRef = FirebaseDatabase.getInstance().getReference("Connections");

        // Set up RecyclerView
        requestList = new ArrayList<>();
        requestAdapter = new ConnectionRequestAdapter(requestList);
        rvRequests.setLayoutManager(new LinearLayoutManager(this));
        rvRequests.setAdapter(requestAdapter);

        // Load pending requests
        loadPendingRequests();

        // Set up click listeners
        requestAdapter.setOnAcceptClickListener(this::acceptRequest);
        requestAdapter.setOnRejectClickListener(this::rejectRequest);

        manage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ConnectionRequestsActivity.this, ConnectionActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loadPendingRequests() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoRequests.setVisibility(View.GONE);

        connectionsRef.child(currentUserId).orderByChild("status").equalTo("pending")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        requestList.clear();

                        if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                            tvNoRequests.setVisibility(View.VISIBLE);
                            rvRequests.setVisibility(View.GONE);
                            progressBar.setVisibility(View.GONE);
                            return;
                        }

                        final int[] pendingCount = {(int) snapshot.getChildrenCount()};
                        final int[] processedCount = {0};

                        for (DataSnapshot requestSnapshot : snapshot.getChildren()) {
                            String senderId = requestSnapshot.getKey();

                            if (senderId != null) {
                                usersRef.child(senderId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                        processedCount[0]++;

                                        if (userSnapshot.exists()) {
                                            String userId = userSnapshot.getKey();
                                            String name = userSnapshot.child("name").getValue(String.class);
                                            String username = userSnapshot.child("username").getValue(String.class);
                                            String profileImage = userSnapshot.child("profileImage").getValue(String.class);

                                            if (name != null && username != null) {
                                                User user = new User(userId, name, username, profileImage);
                                                user.setConnectionStatus("pending");
                                                requestList.add(user);
                                            }
                                        }

                                        if (processedCount[0] >= pendingCount[0]) {
                                            requestAdapter.notifyDataSetChanged();
                                            tvNoRequests.setVisibility(requestList.isEmpty() ? View.VISIBLE : View.GONE);
                                            rvRequests.setVisibility(requestList.isEmpty() ? View.GONE : View.VISIBLE);
                                            progressBar.setVisibility(View.GONE);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        processedCount[0]++;

                                        if (processedCount[0] >= pendingCount[0]) {
                                            progressBar.setVisibility(View.GONE);
                                        }
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ConnectionRequestsActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void acceptRequest(User user) {
        progressBar.setVisibility(View.VISIBLE);

        DatabaseReference currentUserConn = connectionsRef.child(currentUserId);
        DatabaseReference partnerConn = connectionsRef.child(user.getId());

        currentUserConn.orderByChild("status").equalTo("connected").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(ConnectionRequestsActivity.this,
                            "You're already connected to someone", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                partnerConn.orderByChild("status").equalTo("connected").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Toast.makeText(ConnectionRequestsActivity.this,
                                    "This user is already connected to someone", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            return;
                        }

                        // Proceed with original accept logic
                        Map<String, Object> updates = new HashMap<>();
                        // Update both users' connection status to "connected"
                        updates.put("/Connections/" + currentUserId + "/" + user.getId() + "/status", "connected");
                        updates.put("/Connections/" + user.getId() + "/" + currentUserId + "/status", "connected");

                        FirebaseDatabase.getInstance().getReference().updateChildren(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(ConnectionRequestsActivity.this, "Connection accepted", Toast.LENGTH_SHORT).show();
                                    requestList.remove(user);
                                    requestAdapter.notifyDataSetChanged();

                                    if (requestList.isEmpty()) {
                                        tvNoRequests.setVisibility(View.VISIBLE);
                                        rvRequests.setVisibility(View.GONE);
                                    }

                                    progressBar.setVisibility(View.GONE);
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(ConnectionRequestsActivity.this, "Failed to accept request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(ConnectionRequestsActivity.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ConnectionRequestsActivity.this, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void rejectRequest(User user) {
        progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> updates = new HashMap<>();
        // Remove the connection entries for both users
        updates.put("/Connections/" + currentUserId + "/" + user.getId(), null);
        updates.put("/Connections/" + user.getId() + "/" + currentUserId, null);

        FirebaseDatabase.getInstance().getReference().updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ConnectionRequestsActivity.this, "Connection rejected", Toast.LENGTH_SHORT).show();
                    requestList.remove(user);
                    requestAdapter.notifyDataSetChanged();

                    if (requestList.isEmpty()) {
                        tvNoRequests.setVisibility(View.VISIBLE);
                        rvRequests.setVisibility(View.GONE);
                    }

                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ConnectionRequestsActivity.this, "Failed to reject request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}