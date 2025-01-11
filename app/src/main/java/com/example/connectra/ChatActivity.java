package com.example.connectra;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectra.adapter.ChatTextsAdapter;
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

    private String currentUserId;
    private String chatPartnerId;
    private String chatPartnerName;

    private DatabaseReference messagesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        chatPartnerId = getIntent().getStringExtra("chatPartnerId");
        chatPartnerName = getIntent().getStringExtra("chatPartnerName");
        currentUserId = getIntent().getStringExtra("currentUserId");

        String conversationId = getConversationId(currentUserId, chatPartnerId);

        messagesRef = FirebaseDatabase.getInstance().getReference("Messages").child(conversationId);

        initializeFields();
        setupRecyclerView();
        loadMessages();

        ImageView sendButton = findViewById(R.id.send_button);
        EditText messageInput = findViewById(R.id.message_input);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String messageText = messageInput.getText().toString();

                if (!TextUtils.isEmpty(messageText)) {
                    sendMessage(messageText);
                    messageInput.setText(""); // Clear the input field
                }
            }
        });
    }

    private void initializeFields() {
        messageRecyclerView = findViewById(R.id.message_list_users);
        messageList = new ArrayList<>();
        TextView nameTextView = findViewById(R.id.full_name);
        nameTextView.setText(chatPartnerName);
        // Space for profile pic
    }

    private void setupRecyclerView() {
        messageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new ChatTextsAdapter(messageList, currentUserId);
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
        String messageId = messagesRef.push().getKey();
        long timestamp = System.currentTimeMillis();

        HashMap<String, Object> messageMap = new HashMap<>();
        messageMap.put("messageId", messageId);
        messageMap.put("message", messageText);
        messageMap.put("senderId", currentUserId);
        messageMap.put("receiverId", chatPartnerId);
        messageMap.put("timestamp", timestamp);

        if (messageId != null) {
            messagesRef.child(messageId).setValue(messageMap);
        }
    }

    private String getConversationId(String user1, String user2) {
        List<String> ids = new ArrayList<>();
        ids.add(user1);
        ids.add(user2);
        Collections.sort(ids);
        return ids.get(0) + "_" + ids.get(1);
    }
}
