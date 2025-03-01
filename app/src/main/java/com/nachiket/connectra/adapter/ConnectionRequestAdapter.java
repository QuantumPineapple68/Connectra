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
import com.nachiket.connectra.R;
import com.nachiket.connectra.model.User;

import java.util.List;

public class ConnectionRequestAdapter extends RecyclerView.Adapter<ConnectionRequestAdapter.RequestViewHolder> {

    private final List<User> requestList;
    private OnAcceptClickListener acceptClickListener;
    private OnRejectClickListener rejectClickListener;

    public interface OnAcceptClickListener {
        void onAcceptClick(User user);
    }

    public interface OnRejectClickListener {
        void onRejectClick(User user);
    }

    public ConnectionRequestAdapter(List<User> requestList) {
        this.requestList = requestList;
    }

    public void setOnAcceptClickListener(OnAcceptClickListener listener) {
        this.acceptClickListener = listener;
    }

    public void setOnRejectClickListener(OnRejectClickListener listener) {
        this.rejectClickListener = listener;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        User user = requestList.get(position);

        holder.tvName.setText(user.getName());
        holder.tvUsername.setText("@" + user.getUsername());

        // Load profile image
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

        holder.btnAccept.setOnClickListener(v -> {
            if (acceptClickListener != null) {
                acceptClickListener.onAcceptClick(user);
            }
        });

        holder.btnReject.setOnClickListener(v -> {
            if (rejectClickListener != null) {
                rejectClickListener.onRejectClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProfile;
        TextView tvName, tvUsername;
        Button btnAccept, btnReject;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfile = itemView.findViewById(R.id.iv_profile);
            tvName = itemView.findViewById(R.id.tv_name);
            tvUsername = itemView.findViewById(R.id.tv_username);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnReject = itemView.findViewById(R.id.btn_reject);
        }
    }
}