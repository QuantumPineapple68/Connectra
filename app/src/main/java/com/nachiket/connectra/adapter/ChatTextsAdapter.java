package com.nachiket.connectra.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nachiket.connectra.model.ChatTexts;
import com.nachiket.connectra.R;

import java.util.List;

public class ChatTextsAdapter extends RecyclerView.Adapter<ChatTextsAdapter.ChatTextsViewHolder> {

    private List<ChatTexts> chatTextsList;
    private String currentUserId;
    private String profileImageUrl; // URL for the profile image of the opposite user
    private boolean profileApproved;
    private OnMessageLongClickListener longClickListener;


    public ChatTextsAdapter(List<ChatTexts> chatTextsList, String currentUserId, String profileImageUrl, boolean profileApproved) {
        this.chatTextsList = chatTextsList;
        this.currentUserId = currentUserId;
        this.profileImageUrl = profileImageUrl;
        this.profileApproved = profileApproved;
    }

    public interface OnMessageLongClickListener {
        void onMessageLongClick(ChatTexts message, View view);
    }

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.longClickListener = listener;
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
        boolean isOnlyEmoji = isOnlyEmojis(chatText.getMessage());

        holder.receiverMessageText.setVisibility(View.GONE);
        holder.receiverEmojiText.setVisibility(View.GONE);
        holder.senderMessageText.setVisibility(View.GONE);
        holder.senderEmojiText.setVisibility(View.GONE);
        holder.senderReaction.setVisibility(View.GONE);
        holder.receiverReaction.setVisibility(View.GONE);

        if (chatText.getSenderId().equals(currentUserId)) {
            // Sender message
            holder.profileImage.setVisibility(View.GONE);
            if (isOnlyEmoji) {
                holder.senderEmojiText.setVisibility(View.VISIBLE);
                holder.senderEmojiText.setText(chatText.getMessage());
            } else {
                holder.senderMessageText.setVisibility(View.VISIBLE);
                holder.senderMessageText.setText(chatText.getMessage());
            }

            // Show reaction if exists
            if (chatText.getReaction() != null) {
                holder.senderReaction.setVisibility(View.VISIBLE);
                holder.senderReaction.setText(chatText.getReaction());
            }
        } else {
            // Receiver message
            if (isOnlyEmoji) {
                holder.receiverEmojiText.setVisibility(View.VISIBLE);
                holder.receiverEmojiText.setText(chatText.getMessage());
            } else {
                holder.receiverMessageText.setVisibility(View.VISIBLE);
                holder.receiverMessageText.setText(chatText.getMessage());
            }

            // Show reaction if exists
            if (chatText.getReaction() != null) {
                holder.receiverReaction.setVisibility(View.VISIBLE);
                holder.receiverReaction.setText(chatText.getReaction());
            }

            // Handle profile image visibility
            if (position == 0 || !chatTextsList.get(position - 1).getSenderId().equals(chatText.getSenderId())) {
                holder.profileImage.setVisibility(View.VISIBLE);
                if (profileApproved) {
                    Glide.with(holder.itemView.getContext())
                            .load(profileImageUrl)
                            .placeholder(R.drawable.no_profile_pic)
                            .error(R.drawable.no_profile_pic)
                            .into(holder.profileImage);
                } else {
                    holder.profileImage.setImageResource(R.drawable.no_profile_pic);
                }
            } else {
                holder.profileImage.setVisibility(View.INVISIBLE);
            }
        }

        // Set long click listener on the container
        View messageContainer = chatText.getSenderId().equals(currentUserId)
                ? holder.itemView.findViewById(R.id.sender_container)
                : holder.itemView.findViewById(R.id.receiver_container);

        messageContainer.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onMessageLongClick(chatText, messageContainer);
                return true;
            }
            return false;
        });
    }

    private boolean isOnlyEmojis(String message) {
        String emojiPattern = "[\\p{Emoji}\\p{Emoji_Presentation}\\p{Emoji_Modifier}\\p{Emoji_Component}&&[^\\p{Alnum}]]+";
        return message.matches(emojiPattern) && !message.contains("*");
    }

    @Override
    public int getItemCount() {
        return chatTextsList.size();
    }

    public static class ChatTextsViewHolder extends RecyclerView.ViewHolder {
        public ImageView profileImage;
        TextView senderMessageText, receiverMessageText;
        TextView senderEmojiText, receiverEmojiText, senderReaction, receiverReaction;

        public ChatTextsViewHolder(@NonNull View itemView) {
            super(itemView);
            senderMessageText = itemView.findViewById(R.id.sender_message_text);
            receiverMessageText = itemView.findViewById(R.id.receiver_message_text);
            senderEmojiText = itemView.findViewById(R.id.sender_emoji_text);
            receiverEmojiText = itemView.findViewById(R.id.receiver_emoji_text);
            profileImage = itemView.findViewById(R.id.message_profile_image);
            senderReaction = itemView.findViewById(R.id.sender_reaction);
            receiverReaction = itemView.findViewById(R.id.receiver_reaction);
        }
    }
}