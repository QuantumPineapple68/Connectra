package com.nachiket.connectra.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.FirebaseDatabase;
import com.nachiket.connectra.R;
import com.nachiket.connectra.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final List<User> userList;
    private final String currentUserId; // Added member variable
    private OnConnectClickListener connectClickListener;

    public interface OnConnectClickListener {
        void onConnectClick(User user);
    }

    public UserAdapter(List<User> userList, String currentUserId) {
        this.userList = userList;
        this.currentUserId = currentUserId;
    }

    public void setOnConnectClickListener(OnConnectClickListener listener) {
        this.connectClickListener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        // Check if user object is null
        if (user == null) {
            return;
        }

        // Handle null name and username safely
        holder.tvName.setText(user.getName() != null ? user.getName() : "Unknown");
        holder.tvUsername.setText(user.getUsername() != null ? "@" + user.getUsername() : "@unknown");

        // Load profile image safely
        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getProfileImage())
                    .placeholder(R.drawable.no_profile_pic)
                    .error(R.drawable.no_profile_pic)
                    .circleCrop()
                    .into(holder.ivProfile);
        } else {
            holder.ivProfile.setImageResource(R.drawable.no_profile_pic);
        }

        // Set button text and state based on connection status
        String status = user.getConnectionStatus() != null ? user.getConnectionStatus() : "none";

        switch (status) {
            case "none":
                if(hasExistingConnection()) {
                    holder.btnConnect.setText("Connect");
                    holder.btnConnect.setEnabled(false);
                } else {
                    holder.btnConnect.setText("Connect");
                    holder.btnConnect.setEnabled(true);
                }
                break;
            case "pending":
                holder.btnConnect.setText("Accept");
                holder.btnConnect.setEnabled(true);
                break;
            case "sent":
                holder.btnConnect.setText("Requested");
                holder.btnConnect.setEnabled(false);
                break;
            case "connected":
                holder.btnConnect.setText("Disconnect");
                holder.btnConnect.setEnabled(true);
                break;
        }

        holder.btnConnect.setOnClickListener(v -> {
            if(status.equals("connected")) {
                disconnectUser(user);
            } else {
                if (connectClickListener != null) {
                    connectClickListener.onConnectClick(user);
                }
            }
        });
    }

    private boolean hasExistingConnection() {
        for(User u : userList) {
            if("connected".equals(u.getConnectionStatus())) return true;
        }
        return false;
    }

    private void disconnectUser(User user) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("/Connections/" + currentUserId + "/" + user.getId(), null);
        updates.put("/Connections/" + user.getId() + "/" + currentUserId, null);

        FirebaseDatabase.getInstance().getReference().updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    user.setConnectionStatus("none");
                    notifyDataSetChanged();
                });
    }


    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile;
        TextView tvName, tvUsername;
        Button btnConnect;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.iv_profile);
            tvName = itemView.findViewById(R.id.tv_name);
            tvUsername = itemView.findViewById(R.id.tv_username);
            btnConnect = itemView.findViewById(R.id.btn_connect);
        }
    }
}