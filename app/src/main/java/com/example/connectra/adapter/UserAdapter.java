package com.example.connectra.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connectra.R;
import com.example.connectra.Fragments.NewUser;
import com.example.connectra.RecyclerProfileMainActivity;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private List<NewUser> users;
    private Context context;  // Added context

    public UserAdapter(List<NewUser> users) {
        this.users = users;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();  // Initialize context
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NewUser user = users.get(position);
        holder.name.setText(user.getName());
        holder.offeredSkill.setText("Offered: " + user.getMyskill());
        holder.wishSkill.setText("Wants to learn: " + user.getGoalskill());

        // Set profile image using Glide
        String profileImage = user.getProfileImage();
        if (profileImage != null && !profileImage.isEmpty()) {
            Glide.with(context)
                    .load(profileImage)
                    .placeholder(R.drawable.no_profile_pic)
                    .error(R.drawable.no_profile_pic)
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.no_profile_pic);
        }

        // Gender logic
        if (holder.genderImageView != null) {
            String gender = user.getGender() == null ? "" : user.getGender().toLowerCase();
            switch (gender) {
                case "male":
                    holder.genderImageView.setImageResource(R.drawable.icon_male);
                    break;
                case "female":
                    holder.genderImageView.setImageResource(R.drawable.icon_female);
                    break;
                default:
                    holder.genderImageView.setImageResource(R.drawable.icon_default);
                    break;
            }
        }

        // Add click listener matching TileAdapter
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, RecyclerProfileMainActivity.class);

            // Pass the same user data as TileAdapter
            intent.putExtra("name", user.getName());
            intent.putExtra("userAge", user.getAge());
            intent.putExtra("userMySkill", user.getMyskill());
            intent.putExtra("userGoalSkill", user.getGoalskill());
            intent.putExtra("userGender", user.getGender());
            intent.putExtra("userName", user.getUsername());
            intent.putExtra("bio", user.getBio());
            intent.putExtra("userId", user.getId());
            intent.putExtra("profileImage", user.getProfileImage());

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView genderImageView;
        TextView name, offeredSkill, wishSkill;
        CircleImageView profileImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.profile_name);
            offeredSkill = itemView.findViewById(R.id.offered_skill);
            wishSkill = itemView.findViewById(R.id.wish_skill);
            genderImageView = itemView.findViewById(R.id.gender_icon);
            profileImage = itemView.findViewById(R.id.profile_image);

        }
    }
}