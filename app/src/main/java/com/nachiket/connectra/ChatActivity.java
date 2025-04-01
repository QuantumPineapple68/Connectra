package com.nachiket.connectra;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.MenuItem;
import android.view.Menu;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.color.MaterialColors;
import com.nachiket.connectra.model.BlockedUser;
import com.nachiket.connectra.model.ChatTexts;
import com.nachiket.connectra.adapter.ChatTextsAdapter;
import com.nachiket.connectra.utility.MessageFilter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nachiket.connectra.utility.MessageViolationTracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView messageRecyclerView;
    private ChatTextsAdapter messageAdapter;
    private List<ChatTexts> messageList;
    private ImageView backButton;
    MediaPlayer sentSound;

    private String currentUserId;
    private String chatPartnerId;
    private String chatPartnerName;
    private View activeIndicator;
    private DatabaseReference onlineRef;
    private ValueEventListener onlineListener;
    private DatabaseReference blockedUsersRef;
    private DatabaseReference reportedUsersRef;
    private boolean isUserBlocked = false;
    private boolean isUserReported = false;
    private MenuItem blockMenuItem, reportMenuItem;
    private TextView fullName;

    private DatabaseReference messagesRef;

    private MessageViolationTracker violationTracker;
    private Handler restrictionHandler;
    private EditText messageInput;
    private ImageView sendButton;
    private ImageView profileImageView; // Profile image view to show chat partner's profile image
    private final Runnable updateCountdownRunnable = new Runnable() {
        @Override
        public void run() {
            if (violationTracker.isRestricted()) {
                long remainingMillis = violationTracker.getRemainingTime();
                String timeDisplay = formatTime(remainingMillis);
                messageInput.setText(timeDisplay);
                messageInput.setEnabled(false);
                sendButton.setEnabled(false);
                restrictionHandler.postDelayed(this, 1000);
            } else {
                messageInput.setText("");
                messageInput.setHint("Type a message");
                messageInput.setEnabled(true);
                sendButton.setEnabled(true);
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Window window = getWindow();
        int statusBarColor = getResources().getColor(R.color.inverted_top, getTheme());
        window.setStatusBarColor(statusBarColor);
        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(window, window.getDecorView());
        boolean isLightBackground = MaterialColors.isColorLight(statusBarColor);
        windowInsetsController.setAppearanceLightStatusBars(isLightBackground);

        backButton=findViewById(R.id.backBtn);
        chatPartnerId = getIntent().getStringExtra("chatPartnerId");
        chatPartnerName = getIntent().getStringExtra("chatPartnerName");
        currentUserId = getIntent().getStringExtra("currentUserId");
        String profileImage = getIntent().getStringExtra("profileImage");
        boolean profileApproved = getIntent().getBooleanExtra("profileApproved", false);

        String conversationId = getConversationId(currentUserId, chatPartnerId);
        sentSound = MediaPlayer.create(this, R.raw.send);

        violationTracker = new MessageViolationTracker(this);
        restrictionHandler = new Handler(Looper.getMainLooper());
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
        fullName = findViewById(R.id.full_name);
        activeIndicator = findViewById(R.id.active_indicator);

        onlineRef = FirebaseDatabase.getInstance().getReference("OnlineUsers");
        messagesRef = FirebaseDatabase.getInstance().getReference("Messages").child(conversationId);
        blockedUsersRef = FirebaseDatabase.getInstance().getReference("BlockedUsers");
        reportedUsersRef = FirebaseDatabase.getInstance().getReference("ReportedUsers");

        if (violationTracker.isRestricted()) {
            messageInput.setEnabled(false);
            sendButton.setEnabled(false);
            restrictionHandler.post(updateCountdownRunnable);
        }

        markMessagesAsRead();
        setupOnlineStatusListener();
        initializeFields();
        setupToolbar();

        setupRecyclerView();
        loadMessages();

        // Set profile image using Glide
        profileImageView = findViewById(R.id.profile_image);
        if (profileImage != null && !profileImage.isEmpty() && profileApproved) {
            Glide.with(this)
                    .load(profileImage)
                    .placeholder(R.drawable.no_profile_pic)
                    .error(R.drawable.no_profile_pic)
                    .into(profileImageView);
        } else {
            profileImageView.setImageResource(R.drawable.no_profile_pic); // Default image if no profile image URL
        }

        profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatActivity.this, ShowImage.class);
                intent.putExtra("image", profileImage);
                intent.putExtra("imageApproved", profileApproved);
                intent.putExtra("isProfilePic", true);
                startActivity(intent);
            }
        });

        ImageView sendButton = findViewById(R.id.send_button);
        EditText messageInput = findViewById(R.id.message_input);

        sendButton.setOnClickListener(v -> {
            String messageText = messageInput.getText().toString().trim();

            if (!TextUtils.isEmpty(messageText)) {
                if (violationTracker.isRestricted()) {
                    long remainingMinutes = violationTracker.getRemainingTime() / (60 * 1000);
                    Toast.makeText(ChatActivity.this,
                            "You are restricted from sending messages for " + remainingMinutes + " minutes",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean containsViolation = MessageFilter.containsInappropriateContent(messageText);
                violationTracker.recordMessage(containsViolation);

                if (violationTracker.isRestricted()) {
                    messageInput.setEnabled(false);
                    sendButton.setEnabled(false);
                    restrictionHandler.post(updateCountdownRunnable);
                    Toast.makeText(ChatActivity.this,
                            "You have been restricted from sending messages for 10 minutes due to multiple violations",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                sentSound.start();
                sendMessage(messageText);
                messageInput.setText("");
            }
        });

        backButton.setOnClickListener(v -> finish());
        fullName.setOnClickListener(v -> finish());

    }

    private void initializeFields() {
        messageRecyclerView = findViewById(R.id.message_list_users);
        messageList = new ArrayList<>();
        TextView nameTextView = findViewById(R.id.full_name);
        nameTextView.setText(chatPartnerName);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.chat_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    private void setupRecyclerView() {
        messageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        String profileImageUrl = getIntent().getStringExtra("profileImage"); // Get the profile image URL
        boolean profileApproved = getIntent().getBooleanExtra("profileApproved", false);
        messageAdapter = new ChatTextsAdapter(messageList, currentUserId, profileImageUrl, profileApproved);
        messageAdapter.setOnMessageLongClickListener((message, view) -> {
            showReactionDialog(message, view);
        });

        messageRecyclerView.setAdapter(messageAdapter);
    }

    private void updateMessageReaction(ChatTexts message, String reaction) {
        if (message == null || message.getMessageId() == null) {
            Toast.makeText(this, "Cannot update reaction: Invalid message", Toast.LENGTH_SHORT).show();
            return;
        }

        String conversationId = getConversationId(currentUserId, chatPartnerId);
        DatabaseReference messageRef = FirebaseDatabase.getInstance()
                .getReference("Messages")
                .child(conversationId)
                .child(message.getMessageId());

        HashMap<String, Object> updates = new HashMap<>();
        updates.put("reaction", reaction);
        updates.put("reactionBy", currentUserId);

        messageRef.updateChildren(updates)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Reaction added", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to add reaction", Toast.LENGTH_SHORT).show());
    }

    private void setupOnlineStatusListener() {
        onlineListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isOnline = snapshot.child("online").getValue(Boolean.class);
                activeIndicator.setVisibility(isOnline != null && isOnline ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatActivity", "Failed to get online status: " + error.getMessage());
            }
        };
        onlineRef.child(chatPartnerId).addValueEventListener(onlineListener);
    }

    private void showReactionDialog(ChatTexts message, View anchorView) {
        View reactionView = getLayoutInflater().inflate(R.layout.message_reaction_layout, null);
        PopupWindow popupWindow = new PopupWindow(
                reactionView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        // Set up reaction click listeners
        TextView heartReaction = reactionView.findViewById(R.id.reaction_heart);
        TextView laughReaction = reactionView.findViewById(R.id.reaction_laugh);
        TextView likeReaction = reactionView.findViewById(R.id.reaction_like);
        TextView wowReaction = reactionView.findViewById(R.id.reaction_wow);

        View.OnClickListener reactionClickListener = v -> {
            String reaction = ((TextView) v).getText().toString();
            updateMessageReaction(message, reaction);
            popupWindow.dismiss();
        };

        heartReaction.setOnClickListener(reactionClickListener);
        laughReaction.setOnClickListener(reactionClickListener);
        likeReaction.setOnClickListener(reactionClickListener);
        wowReaction.setOnClickListener(reactionClickListener);

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(10);

        // Show popup above the message for sender's messages, below for receiver's
        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        boolean isSender = message.getSenderId().equals(currentUserId);
        int y = isSender ? location[1] - anchorView.getHeight() : location[1] + anchorView.getHeight();
        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, location[0], y);
    }

    private String formatTime(long millis) {
        int seconds = (int) (millis / 1000) % 60;
        int minutes = (int) ((millis / (1000 * 60)) % 60);
        return String.format(Locale.US, "%d:%02d remaining", minutes, seconds);
    }

    private void loadMessages() {
        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    ChatTexts message = dataSnapshot.getValue(ChatTexts.class);
                    if (message != null) {
                        messageList.add(message);
                    }
                }
                messageAdapter.notifyDataSetChanged();
                messageRecyclerView.scrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void sendMessage(String messageText) {

        if (MessageFilter.containsInappropriateContent(messageText)) {
            Toast.makeText(ChatActivity.this,
                    "Please keep the conversation appropriate.",
                    Toast.LENGTH_SHORT).show();
            messageText = MessageFilter.filterMessage(messageText);
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId);
        Log.e("namaiwa", currentUserId);

        String finalMessageText = messageText;
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String myName = snapshot.child("name").getValue(String.class); // Fetch "name" from the database
                    Log.e("namaiwa", myName);
                    Log.e("namaiwa", chatPartnerId);
                    if (myName != null) {
                        String messageId = messagesRef.push().getKey();
                        long timestamp = System.currentTimeMillis(); // added for future use in updates

                        HashMap<String, Object> messageMap = new HashMap<>();
                        messageMap.put("messageId", messageId);
                        messageMap.put("message", finalMessageText);
                        messageMap.put("senderId", currentUserId);
                        messageMap.put("receiverId", chatPartnerId);
                        messageMap.put("timestamp", timestamp);
                        messageMap.put("read", false);

                        if (messageId != null) {
                            messagesRef.child(messageId).setValue(messageMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Log.e("namaiwa", "Message sent successfully");
                                    } else {
                                        Log.e("namaiwa", "Failed to send message");
                                    }
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private String getConversationId(String user1, String user2) {
        List<String> ids = new ArrayList<>();
        ids.add(user1);
        ids.add(user2);
        Collections.sort(ids);
        return ids.get(0) + "_" + ids.get(1);
    }

    private void markMessagesAsRead() {
        String conversationId = getConversationId(currentUserId, chatPartnerId);
        DatabaseReference messagesRef = FirebaseDatabase.getInstance()
                .getReference("Messages")
                .child(conversationId);

        messagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    ChatTexts message = messageSnapshot.getValue(ChatTexts.class);
                    if (message != null && message.getReceiverId().equals(currentUserId)
                            && !message.isRead()) {
                        messageSnapshot.getRef().child("read").setValue(true);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        blockMenuItem = menu.findItem(R.id.action_block);
        reportMenuItem = menu.findItem(R.id.action_report);
        checkIfUserBlocked();
        checkIfUserReported();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_block) {
            if (isUserBlocked) {
                unblockUser();
            } else {
                blockUser();
            }
            return true;
        } else if (itemId == R.id.action_report) {
            if (!isUserReported) {
                reportUser();
            } else {
                Toast.makeText(ChatActivity.this, "You have already reported this user", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateBlockMenuItem() {
        if (blockMenuItem != null) {
            blockMenuItem.setTitle(isUserBlocked ? "\uD83D\uDD13  Unblock User" : "\uD83D\uDEAB  Block User");
        }
    }

    private void updateReportMenuItem() {
        if (reportMenuItem != null) {
            reportMenuItem.setTitle(isUserReported ? "✓  Reported" : "\uD83D\uDEA8  Report User");
            reportMenuItem.setEnabled(!isUserReported);
        }
    }

    private void checkIfUserBlocked() {
        DatabaseReference blockedUsersRef = FirebaseDatabase.getInstance().getReference("BlockedUsers");

        // Check if current user is blocked by chat partner
        blockedUsersRef.child(chatPartnerId).child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean blockedByPartner = snapshot.exists();
                updateMessageInput(blockedByPartner);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        blockedUsersRef.child(currentUserId).child(chatPartnerId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isUserBlocked = snapshot.exists();
                updateBlockMenuItem();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void checkIfUserReported() {
        reportedUsersRef.child(currentUserId).child(chatPartnerId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isUserReported = snapshot.exists();
                updateReportMenuItem();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void updateMessageInput(boolean blockedByPartner) {
        if (blockedByPartner) {
            messageInput.setHint("You have been blocked");
            messageInput.setEnabled(false);
            sendButton.setEnabled(false);
        } else if (violationTracker.isRestricted()) {
            messageInput.setEnabled(false);
            sendButton.setEnabled(false);
            restrictionHandler.post(updateCountdownRunnable);
        } else {
            messageInput.setHint("Type a message");
            messageInput.setEnabled(true);
            sendButton.setEnabled(true);
        }
    }

    private void blockUser() {
        BlockedUser blockedUser = new BlockedUser(chatPartnerId);
        blockedUsersRef.child(currentUserId).child(chatPartnerId).setValue(blockedUser)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ChatActivity.this, "User blocked", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void unblockUser() {
        blockedUsersRef.child(currentUserId).child(chatPartnerId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ChatActivity.this, "User unblocked", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void reportUser() {
        // First check if user has already reported this user
        reportedUsersRef.child(currentUserId).child(chatPartnerId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // User has already reported this person
                    Toast.makeText(ChatActivity.this, "You have already reported this user", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Add to reported users to prevent multiple reports from same user
                HashMap<String, Object> reportMap = new HashMap<>();
                reportMap.put("reportedAt", System.currentTimeMillis());
                reportedUsersRef.child(currentUserId).child(chatPartnerId).setValue(reportMap);

                // Increment the reportedCount under the reported user's profile
                DatabaseReference userReportRef = FirebaseDatabase.getInstance().getReference("Users").child(chatPartnerId).child("reportedCount");

                userReportRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int currentReportCount = 0;
                        if (snapshot.exists()) {
                            Long value = snapshot.getValue(Long.class);
                            if (value != null) {
                                currentReportCount = value.intValue();
                            }
                        }

                        userReportRef.setValue(currentReportCount + 1).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(ChatActivity.this, "User reported successfully", Toast.LENGTH_SHORT).show();
                                isUserReported = true;
                                updateReportMenuItem();
                            } else {
                                Toast.makeText(ChatActivity.this, "Failed to report user", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ChatActivity.this, "Failed to report user: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Failed to check report status: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (violationTracker.isRestricted()) {
            messageInput.setEnabled(false);
            sendButton.setEnabled(false);
            restrictionHandler.post(updateCountdownRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        restrictionHandler.removeCallbacks(updateCountdownRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        restrictionHandler.removeCallbacks(updateCountdownRunnable);
        if (onlineListener != null) {
            onlineRef.child(chatPartnerId).removeEventListener(onlineListener);
        }
    }

}