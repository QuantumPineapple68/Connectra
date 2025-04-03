package com.nachiket.connectra.adapter;

import android.graphics.Color;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nachiket.connectra.model.ChatTexts;
import com.nachiket.connectra.R;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ChatTextsAdapter extends RecyclerView.Adapter<ChatTextsAdapter.ChatTextsViewHolder> {

    private List<ChatTexts> chatTextsList;
    private String currentUserId;
    private String profileImageUrl;
    private boolean profileApproved;
    private OnMessageLongClickListener longClickListener;
    private OnMessageActionListener actionListener;
    private String chatPartnerName;
    private RecyclerView recyclerView;

    public ChatTextsAdapter(List<ChatTexts> chatTextsList, String currentUserId, String profileImageUrl, boolean profileApproved, String chatPartnerName, RecyclerView recyclerView) {
        this.chatTextsList = chatTextsList;
        this.currentUserId = currentUserId;
        this.profileImageUrl = profileImageUrl;
        this.profileApproved = profileApproved;
        this.chatPartnerName = chatPartnerName;
        this.recyclerView = recyclerView;
    }

    public interface OnMessageLongClickListener {
        void onMessageLongClick(ChatTexts message, View view);
    }

    public interface OnMessageActionListener {
        void onReplyMessage(ChatTexts message);
        void onDeleteMessage(ChatTexts message);
        void onReactToMessage(ChatTexts message, String reaction);
    }

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnMessageActionListener(OnMessageActionListener listener) {
        this.actionListener = listener;
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

        // Reset visibilities
        holder.receiverMessageText.setVisibility(View.GONE);
        holder.receiverEmojiText.setVisibility(View.GONE);
        holder.senderMessageText.setVisibility(View.GONE);
        holder.senderEmojiText.setVisibility(View.GONE);
        holder.senderReaction.setVisibility(View.GONE);
        holder.receiverReaction.setVisibility(View.GONE);
        holder.senderTimestamp.setVisibility(View.GONE);
        holder.receiverTimestamp.setVisibility(View.GONE);
        holder.replyPreview.setVisibility(View.GONE);
        holder.senderReplyPreview.setVisibility(View.GONE);

        // Format timestamp with improved formatting
        String timeText = formatMessageTime(chatText.getTimestamp());

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

            // Always show timestamp for sender messages
            holder.senderTimestamp.setVisibility(View.VISIBLE);
            holder.senderTimestamp.setText(timeText);

            // Show reaction if exists
            if (chatText.getReaction() != null && !chatText.getReaction().isEmpty()) {
                holder.senderReaction.setVisibility(View.VISIBLE);
                holder.senderReaction.setText(chatText.getReaction());
            }

            // Handle reply preview for sender messages
            if (chatText.getReplyToId() != null && !chatText.getReplyToId().isEmpty()) {
                holder.senderReplyPreview.setVisibility(View.VISIBLE);
                String senderName = chatText.getReplyToSenderId().equals(currentUserId)
                        ? "You"
                        : chatPartnerName;

                holder.senderReplyPreviewSender.setText(senderName);
                holder.senderReplyPreviewText.setText(chatText.getReplyToText());
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

            // Always show timestamp for receiver messages
            holder.receiverTimestamp.setVisibility(View.VISIBLE);
            holder.receiverTimestamp.setText(timeText);

            // Show reaction if exists
            if (chatText.getReaction() != null && !chatText.getReaction().isEmpty()) {
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

            // Handle reply preview for receiver messages
            if (chatText.getReplyToId() != null && !chatText.getReplyToId().isEmpty()) {
                holder.replyPreview.setVisibility(View.VISIBLE);
                String senderName = chatText.getReplyToSenderId().equals(currentUserId)
                        ? "You"
                        : chatPartnerName;

                holder.replyPreviewSender.setText(senderName);
                holder.replyPreviewText.setText(chatText.getReplyToText());
            }
        }

        // Set click listener for reply preview to navigate to original message
        View.OnClickListener replyClickListener = v -> {
            if (chatText.getReplyToId() != null) {
                int targetPosition = findMessagePositionById(chatText.getReplyToId());
                if (targetPosition != -1) {
                    recyclerView.smoothScrollToPosition(targetPosition);

                    // Optional: Highlight the target message briefly
                    View targetView = recyclerView.getLayoutManager().findViewByPosition(targetPosition);
                    if (targetView != null) {
                        targetView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.login_bar));
                        new android.os.Handler().postDelayed(() ->
                                targetView.setBackgroundColor(Color.TRANSPARENT), 1000);
                    }
                }
            }
        };

        holder.replyPreview.setOnClickListener(replyClickListener);
        holder.senderReplyPreview.setOnClickListener(replyClickListener);

        // Set long click listener for message actions
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

    private int findMessagePositionById(String messageId) {
        for (int i = 0; i < chatTextsList.size(); i++) {
            if (chatTextsList.get(i).getMessageId().equals(messageId)) {
                return i;
            }
        }
        return -1;
    }

    private String formatMessageTime(long timestamp) {
        Calendar messageTime = Calendar.getInstance();
        messageTime.setTimeInMillis(timestamp);

        Calendar now = Calendar.getInstance();

        // Format for just the time (today's messages)
        String timeFormat = DateFormat.format("h:mm a", messageTime).toString();

        // If message is from today, just show the time
        if (isSameDay(messageTime, now)) {
            return timeFormat;
        }

        // If message is from yesterday, show "Yesterday, time"
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        if (isSameDay(messageTime, yesterday)) {
            return "Yesterday, " + timeFormat;
        }

        // If message is from this year, show "Mon, Jan 5, time"
        if (messageTime.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
            return DateFormat.format("MMM d, h:mm a", messageTime).toString();
        }

        // Otherwise show full date "Jan 5, 2024, time"
        return DateFormat.format("MMM d, yyyy, h:mm a", messageTime).toString();
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
    }

    private boolean isOnlyEmojis(String message) {
        String emojiPattern = "[\\p{Emoji}\\p{Emoji_Presentation}\\p{Emoji_Modifier}\\p{Emoji_Component}&&[^\\p{Alnum}]]+";
        return message != null && message.matches(emojiPattern) && !message.contains("*");
    }

    @Override
    public int getItemCount() {
        return chatTextsList.size();
    }

    public static class ChatTextsViewHolder extends RecyclerView.ViewHolder {
        public ImageView profileImage;
        TextView senderMessageText, receiverMessageText;
        TextView senderEmojiText, receiverEmojiText, senderReaction, receiverReaction;
        TextView senderTimestamp, receiverTimestamp;

        // Receiver reply preview elements
        LinearLayout replyPreview;
        TextView replyPreviewSender;
        TextView replyPreviewText;
        View replyPreviewDivider;

        // Sender reply preview elements
        LinearLayout senderReplyPreview;
        TextView senderReplyPreviewSender;
        TextView senderReplyPreviewText;
        View senderReplyPreviewDivider;

        public ChatTextsViewHolder(@NonNull View itemView) {
            super(itemView);
            senderMessageText = itemView.findViewById(R.id.sender_message_text);
            receiverMessageText = itemView.findViewById(R.id.receiver_message_text);
            senderEmojiText = itemView.findViewById(R.id.sender_emoji_text);
            receiverEmojiText = itemView.findViewById(R.id.receiver_emoji_text);
            profileImage = itemView.findViewById(R.id.message_profile_image);
            senderReaction = itemView.findViewById(R.id.sender_reaction);
            receiverReaction = itemView.findViewById(R.id.receiver_reaction);
            senderTimestamp = itemView.findViewById(R.id.sender_timestamp);
            receiverTimestamp = itemView.findViewById(R.id.receiver_timestamp);

            // Initialize receiver reply preview elements
            replyPreview = itemView.findViewById(R.id.reply_preview);
            replyPreviewSender = itemView.findViewById(R.id.reply_preview_sender);
            replyPreviewText = itemView.findViewById(R.id.reply_preview_text);
            replyPreviewDivider = itemView.findViewById(R.id.reply_preview_divider);

            // Initialize sender reply preview elements
            senderReplyPreview = itemView.findViewById(R.id.sender_reply_preview);
            senderReplyPreviewSender = itemView.findViewById(R.id.sender_reply_preview_sender);
            senderReplyPreviewText = itemView.findViewById(R.id.sender_reply_preview_text);
            senderReplyPreviewDivider = itemView.findViewById(R.id.sender_reply_preview_divider);
        }
    }
}