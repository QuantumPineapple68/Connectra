package com.nachiket.connectra.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.nachiket.connectra.R;
import com.nachiket.connectra.model.MessagePreview;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessagePreviewAdapter extends RecyclerView.Adapter<MessagePreviewAdapter.MessageViewHolder> {

    private List<MessagePreview> messageList;
    private Context context;
    private OnMessageClickListener listener;

    public MessagePreviewAdapter(Context context, OnMessageClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.messageList = new ArrayList<>();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_preview, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        MessagePreview message = messageList.get(position);

        holder.username.setText(message.getPartnerUsername());
        holder.lastMessage.setText(message.getLastMessage());
        holder.messageTime.setText(getFormattedTime(message.getTimestamp()));

        // Set unread indicator visibility
        holder.unreadIndicator.setVisibility(message.isRead() ? View.GONE : View.VISIBLE);

        // Load profile image with Glide
        if (message.getPartnerProfileImage() != null && !message.getPartnerProfileImage().isEmpty()) {
            Glide.with(context)
                    .load(message.getPartnerProfileImage())
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.no_profile_pic)
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.no_profile_pic);
        }

        // Set item click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMessageClick(message);
            }
        });
        holder.userActiveIndicator.setVisibility(message.isOnline() ? View.VISIBLE : View.GONE);

    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public void setMessages(List<MessagePreview> messages) {
        this.messageList = messages;
        notifyDataSetChanged();
    }

    public void addMessage(MessagePreview message) {
        this.messageList.add(0, message);
        notifyItemInserted(0);
    }

    public void updateMessage(MessagePreview updatedMessage) {
        for (int i = 0; i < messageList.size(); i++) {
            if (messageList.get(i).getChatId().equals(updatedMessage.getChatId())) {
                messageList.set(i, updatedMessage);
                notifyItemChanged(i);
                return;
            }
        }
        // If not found, add it as a new message
        addMessage(updatedMessage);
    }

    private String getFormattedTime(long timestamp) {
        long currentTime = System.currentTimeMillis();
        long diffInMillis = currentTime - timestamp;
        long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis);

        if (diffInMinutes < 1) {
            return "Now";
        } else if (diffInMinutes < 60) {
            return diffInMinutes + "m";
        } else if (diffInMinutes < 24 * 60) {
            return TimeUnit.MILLISECONDS.toHours(diffInMillis) + "h";
        } else if (diffInMinutes < 7 * 24 * 60) {
            return TimeUnit.MILLISECONDS.toDays(diffInMillis) + "d";
        } else {
            Calendar messageCalendar = Calendar.getInstance();
            messageCalendar.setTimeInMillis(timestamp);

            Calendar nowCalendar = Calendar.getInstance();
            nowCalendar.setTimeInMillis(currentTime);

            if (messageCalendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR)) {
                // Same year, show day and month
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
                return sdf.format(new Date(timestamp));
            } else {
                // Different year, show date with year
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
                return sdf.format(new Date(timestamp));
            }
        }
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView username;
        TextView lastMessage;
        TextView messageTime;
        View unreadIndicator;
        View userActiveIndicator;


        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            username = itemView.findViewById(R.id.username);
            lastMessage = itemView.findViewById(R.id.lastMessage);
            messageTime = itemView.findViewById(R.id.messageTime);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
            userActiveIndicator = itemView.findViewById(R.id.userActiveIndicator);
        }
    }

    public interface OnMessageClickListener {
        void onMessageClick(MessagePreview message);
    }
}
