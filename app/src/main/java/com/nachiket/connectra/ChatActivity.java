package com.nachiket.connectra;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView messageRecyclerView;
    private ChatTextsAdapter messageAdapter;
    private List<ChatTexts> messageList;
    private ImageView backButton;

    private String currentUserId;
    private String chatPartnerId;
    private String chatPartnerName;

    private DatabaseReference messagesRef;

    private ImageView profileImageView; // Profile image view to show chat partner's profile image

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        backButton=findViewById(R.id.backBtn);

        chatPartnerId = getIntent().getStringExtra("chatPartnerId");
        chatPartnerName = getIntent().getStringExtra("chatPartnerName");
        currentUserId = getIntent().getStringExtra("currentUserId");
        String profileImage = getIntent().getStringExtra("profileImage");
        boolean profileApproved = getIntent().getBooleanExtra("profileApproved", false);

        String conversationId = getConversationId(currentUserId, chatPartnerId);

        messagesRef = FirebaseDatabase.getInstance().getReference("Messages").child(conversationId);

        markMessagesAsRead();

        initializeFields();
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

        ImageView sendButton = findViewById(R.id.send_button);
        EditText messageInput = findViewById(R.id.message_input);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageText = messageInput.getText().toString().trim();

                if (!TextUtils.isEmpty(messageText)) {
                    sendMessage(messageText);
                    messageInput.setText(""); // Clear the input field
                }
            }
        });

        backButton.setOnClickListener(v -> finish());
    }

    private void initializeFields() {
        messageRecyclerView = findViewById(R.id.message_list_users);
        messageList = new ArrayList<>();
        TextView nameTextView = findViewById(R.id.full_name);
        nameTextView.setText(chatPartnerName);
    }



    private void setupRecyclerView() {
        messageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        String profileImageUrl = getIntent().getStringExtra("profileImage"); // Get the profile image URL
        boolean profileApproved = getIntent().getBooleanExtra("profileApproved", false);
        messageAdapter = new ChatTextsAdapter(messageList, currentUserId, profileImageUrl, profileApproved);
        messageRecyclerView.setAdapter(messageAdapter);
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
                        messageMap.put("senderName", myName);
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

}
