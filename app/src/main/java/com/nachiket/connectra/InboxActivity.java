package com.nachiket.connectra;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.color.MaterialColors;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.nachiket.connectra.adapter.MessagePreviewAdapter;
import com.nachiket.connectra.model.MessagePreview;
import com.nachiket.connectra.model.UserModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InboxActivity extends AppCompatActivity implements MessagePreviewAdapter.OnMessageClickListener {

    private RecyclerView messagesRecyclerView;
    private MessagePreviewAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private EditText searchEditText;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ImageButton backBtn;
    private ImageButton newMessageBtn;


    private FirebaseAuth auth;
    private DatabaseReference messagesRef;
    private DatabaseReference usersRef;
    private ValueEventListener messagesListener;
    private DatabaseReference onlineUsersRef;
    private Map<String, Boolean> onlineStatusCache = new HashMap<>();

    private String currentUserId;
    private Map<String, UserModel> usersCache = new HashMap<>();
    private List<MessagePreview> allMessages = new ArrayList<>();
    private List<MessagePreview> filteredMessages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        Window window = getWindow();
        int statusBarColor = getResources().getColor(R.color.white, getTheme());
        window.setStatusBarColor(statusBarColor);
        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(window, window.getDecorView());
        boolean isLightBackground = MaterialColors.isColorLight(statusBarColor);
        windowInsetsController.setAppearanceLightStatusBars(isLightBackground);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            // Handle user not logged in
            Toast.makeText(this, "Please log in to view messages", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        messagesRef = FirebaseDatabase.getInstance().getReference().child("Messages");
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        onlineUsersRef = FirebaseDatabase.getInstance().getReference().child("OnlineUsers");

        initViews();
        adapter = new MessagePreviewAdapter(this, this);
        messagesRecyclerView.setAdapter(adapter);

        // Initialize views
        setupListeners();

        // Load messages
        loadMessages();
    }

    private void initViews() {
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        searchEditText = findViewById(R.id.searchEditText);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        backBtn = findViewById(R.id.backBtn);
        newMessageBtn = findViewById(R.id.newMessageBtn);

        // Setup RecyclerView
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MessagePreviewAdapter(this, this);
        messagesRecyclerView.setAdapter(adapter);
    }

    private void setupListeners() {
        // Back button click
        backBtn.setOnClickListener(v -> onBackPressed());

        // New message button click
        newMessageBtn.setOnClickListener(v -> {
            Toast.makeText(this, "Under Development", Toast.LENGTH_SHORT).show();
        });

        // Search functionality
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMessages(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Pull to refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadMessages);
    }

    private void loadMessages() {
        showLoading(true);

        if (messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }

        messagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allMessages.clear();

                for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                    String chatId = chatSnapshot.getKey();

                    // Check if this chat involves the current user
                    if (chatId != null && chatId.contains(currentUserId)) {
                        // Get most recent message for this chat
                        DataSnapshot lastMessageSnapshot = null;
                        long lastTimestamp = 0;

                        for (DataSnapshot messageSnapshot : chatSnapshot.getChildren()) {
                            long timestamp = messageSnapshot.child("timestamp").exists() ?
                                    messageSnapshot.child("timestamp").getValue(Long.class) : 0;

                            if (timestamp > lastTimestamp) {
                                lastTimestamp = timestamp;
                                lastMessageSnapshot = messageSnapshot;
                            }
                        }

                        if (lastMessageSnapshot != null) {
                            String messageId = lastMessageSnapshot.getKey();
                            String message = lastMessageSnapshot.child("message").getValue(String.class);
                            boolean read = lastMessageSnapshot.child("read").exists() ?
                                    lastMessageSnapshot.child("read").getValue(Boolean.class) : true;
                            String senderId = lastMessageSnapshot.child("senderId").getValue(String.class);
                            String receiverId = lastMessageSnapshot.child("receiverId").getValue(String.class);

                            // Get the chat partner ID (the other user in this chat)
                            String partnerId = currentUserId.equals(senderId) ? receiverId : senderId;

                            // Create a message preview object
                            MessagePreview preview = new MessagePreview();
                            preview.setMessageId(messageId);
                            preview.setChatId(chatId);
                            preview.setPartnerId(partnerId);
                            preview.setLastMessage(message);
                            preview.setTimestamp(lastTimestamp);
                            preview.setRead(currentUserId.equals(senderId) || read);

                            // Load user details for the partner
                            loadUserDetails(preview);
                        }
                    }
                }

                showLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showLoading(false);
                Toast.makeText(InboxActivity.this, "Failed to load messages: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        messagesRef.addValueEventListener(messagesListener);
    }

    private void loadUserDetails(MessagePreview preview) {
        String partnerId = preview.getPartnerId();

        // Check if we already have this user's details cached
        if (usersCache.containsKey(partnerId)) {
            UserModel user = usersCache.get(partnerId);

            // Skip banned users
            if (user.isBanned()) {
                return;
            }

            completeMessagePreview(preview, user);
            return;
        }

        // Otherwise load the user details from Firebase
        usersRef.child(partnerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    boolean isBanned = dataSnapshot.child("banned").exists() &&
                            dataSnapshot.child("banned").getValue(Boolean.class);

                    if (isBanned) {
                        return;
                    }

                    String name = dataSnapshot.child("name").getValue(String.class);
                    String username = dataSnapshot.child("username").getValue(String.class);
                    String profileImage = dataSnapshot.child("profileImage").getValue(String.class);
                    boolean profileApproved = dataSnapshot.child("profileApproved").exists() ?
                            dataSnapshot.child("profileApproved").getValue(Boolean.class) : false;


                    UserModel user = new UserModel();
                    user.setId(partnerId);
                    user.setName(name);
                    user.setUsername(username);
                    user.setProfileImage(profileImage);
                    user.setProfileApproved(profileApproved);
                    user.setBanned(isBanned);


                    // Cache the user details
                    usersCache.put(partnerId, user);

                    // Complete the message preview with user details
                    completeMessagePreview(preview, user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Still add the message to the list, but without user details
                allMessages.add(preview);
                sortAndDisplayMessages();
            }
        });

        setupOnlineStatusListener(preview);

    }

    private void setupOnlineStatusListener(MessagePreview preview) {
        String partnerId = preview.getPartnerId();

        onlineUsersRef.child(partnerId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isOnline = snapshot.child("online").getValue(Boolean.class);
                Long lastSeen = snapshot.child("lastSeen").getValue(Long.class);

                // Cache the online status
                onlineStatusCache.put(partnerId, isOnline != null && isOnline);

                // Update the message preview with online status and last seen
                preview.setOnline(isOnline != null && isOnline);
                preview.setLastSeen(lastSeen != null ? lastSeen : 0);

                // Update the UI
                boolean updated = false;
                for (int i = 0; i < allMessages.size(); i++) {
                    if (allMessages.get(i).getPartnerId().equals(partnerId)) {
                        allMessages.set(i, preview);
                        updated = true;
                        break;
                    }
                }

                if (updated) {
                    sortAndDisplayMessages();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("InboxActivity", "Failed to get online status: " + error.getMessage());
            }
        });
    }

    private void completeMessagePreview(MessagePreview preview, UserModel user) {
        preview.setPartnerName(user.getName());
        preview.setPartnerUsername(user.getUsername());
        preview.setPartnerProfileImage(user.getProfileImage());
        preview.setProfileApproved(user.isProfileApproved());

        // Update or add the message preview to our list
        boolean updated = false;
        for (int i = 0; i < allMessages.size(); i++) {
            if (allMessages.get(i).getChatId().equals(preview.getChatId())) {
                allMessages.set(i, preview);
                updated = true;
                break;
            }
        }

        if (!updated) {
            allMessages.add(preview);
        }

        // Sort and display messages
        sortAndDisplayMessages();
    }

    private void sortAndDisplayMessages() {
        // First sort by unread status (unread first)
        // Then sort by timestamp (newest first)
        Collections.sort(allMessages, (m1, m2) -> {
            if (m1.isRead() != m2.isRead()) {
                return m1.isRead() ? 1 : -1;
            }
            return Long.compare(m2.getTimestamp(), m1.getTimestamp());
        });

        // Apply any active filters
        filterMessages(searchEditText.getText().toString());
    }

    private void filterMessages(String query) {
        filteredMessages.clear();

        if (query.isEmpty()) {
            filteredMessages.addAll(allMessages);
        } else {
            String lowerCaseQuery = query.toLowerCase();

            for (MessagePreview message : allMessages) {
                String username = message.getPartnerUsername() != null ? message.getPartnerUsername().toLowerCase() : "";
                String name = message.getPartnerName() != null ? message.getPartnerName().toLowerCase() : "";
                String lastMessage = message.getLastMessage() != null ? message.getLastMessage().toLowerCase() : "";

                if (username.contains(lowerCaseQuery) ||
                        name.contains(lowerCaseQuery) ||
                        lastMessage.contains(lowerCaseQuery)) {
                    filteredMessages.add(message);
                }
            }
        }

        // Update the adapter with filtered messages
        adapter.setMessages(filteredMessages);

        // Show empty view if no messages
        emptyView.setVisibility(filteredMessages.isEmpty() ? View.VISIBLE : View.GONE);
        messagesRecyclerView.setVisibility(filteredMessages.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        swipeRefreshLayout.setRefreshing(isLoading);
    }

    @Override
    public void onMessageClick(MessagePreview message) {
        // Mark message as read in Firebase if needed
        if (!message.isRead()) {
            String chatId = message.getChatId();
            String messageId = message.getMessageId();
            messagesRef.child(chatId).child(messageId).child("read").setValue(true);
        }

        // Navigate to chat activity
        Intent intent = new Intent(InboxActivity.this, ChatActivity.class);
        intent.putExtra("chatPartnerId", message.getPartnerId());
        intent.putExtra("chatPartnerName", message.getPartnerName());
        intent.putExtra("currentUserId", currentUserId);
        intent.putExtra("profileImage", message.getPartnerProfileImage());
        intent.putExtra("profileApproved", message.isProfileApproved());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove Firebase listeners
        if (messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
    }
}