package com.example.connectra.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connectra.model.ChatTexts;
import com.example.connectra.R;

import java.util.List;

public class ChatTextsAdapter extends RecyclerView.Adapter<ChatTextsAdapter.ChatTextsViewHolder> {

    private List<ChatTexts> chatTextsList;
    private String currentUserId;
    private String profileImageUrl; // URL for the profile image of the opposite user

    public ChatTextsAdapter(List<ChatTexts> chatTextsList, String currentUserId, String profileImageUrl) {
        this.chatTextsList = chatTextsList;
        this.currentUserId = currentUserId;
        this.profileImageUrl = profileImageUrl;
    }

    @NonNull
    @Override
    public ChatTextsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_layout_view, parent, false);
        return new ChatTextsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatTextsViewHolder holder, int position) {
        ChatTexts chatText = chatTextsList.get(position);

        if (chatText.getSenderId().equals(currentUserId)) {
            // Message sent by the current user
            holder.sentMessage.setVisibility(View.VISIBLE);
            holder.receivedMessage.setVisibility(View.GONE);
            holder.profileImage.setVisibility(View.GONE);
            holder.sentMessage.setText(chatText.getMessage());

        } else {
            // Message received from the chat partner
            holder.receivedMessage.setVisibility(View.VISIBLE);
            holder.sentMessage.setVisibility(View.GONE);
            holder.receivedMessage.setText(chatText.getMessage());

            // Show profile image only at the start of a group
            if (position == 0 || !chatTextsList.get(position - 1).getSenderId().equals(chatText.getSenderId())) {
                holder.profileImage.setVisibility(View.VISIBLE);

                // Load the profile image using Glide
                Glide.with(holder.itemView.getContext())
                        .load(profileImageUrl)
                        .placeholder(R.drawable.no_profile_pic)
                        .error(R.drawable.no_profile_pic)
                        .into(holder.profileImage);
            } else {
                // Hide profile image for subsequent messages in the group
                holder.profileImage.setVisibility(View.INVISIBLE);
            }
        }
    }



    @Override
    public int getItemCount() {
        return chatTextsList.size();
    }

    public static class ChatTextsViewHolder extends RecyclerView.ViewHolder {

        public ImageView profileImage;
        TextView sentMessage, receivedMessage;

        public ChatTextsViewHolder(@NonNull View itemView) {
            super(itemView);
            sentMessage = itemView.findViewById(R.id.sender_message_text);
            receivedMessage = itemView.findViewById(R.id.receiver_message_text);
            profileImage = itemView.findViewById(R.id.message_profile_image);
        }
    }
}
