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

import com.example.connectra.Fragments.NewUser;
import com.example.connectra.R;
import com.example.connectra.RecyclerProfileMainActivity;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class TileAdapter extends RecyclerView.Adapter<TileAdapter.TileViewHolder> {

    private Context context;
    private List<NewUser> userList;

    public TileAdapter(Context context, List<NewUser> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public TileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new TileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TileViewHolder holder, int position) {
        NewUser user = userList.get(position);

        // Set user data
        holder.profileName.setText(user.getName());
        holder.profileAge.setText(user.getAge() + " y/o");
        holder.offeredSkill.setText("Offered: " + user.getMyskill());
        holder.wishSkill.setText("Wants to learn: " + user.getGoalskill());

        // Set gender icon
        if ("Male".equalsIgnoreCase(user.getGender())) {
            holder.genderIcon.setImageResource(R.drawable.icon_male);
        } else if ("Female".equalsIgnoreCase(user.getGender())) {
            holder.genderIcon.setImageResource(R.drawable.icon_female);
        } else {
            holder.genderIcon.setImageResource(R.drawable.icon_default); // Default icon
        }

        // Set profile image (optional logic can be added here if you fetch images)
//        holder.profileImage.setImageResource(R.drawable.no_profile_pic); // Replace with user's image if available

        // Set item click listener
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, RecyclerProfileMainActivity.class);

            // Pass user data to the new activity
            intent.putExtra("name", user.getName());
            intent.putExtra("userAge", user.getAge());
            intent.putExtra("userMySkill", user.getMyskill());
            intent.putExtra("userGoalSkill", user.getGoalskill());
            intent.putExtra("userGender", user.getGender());
            intent.putExtra("userName", user.getUsername());
            intent.putExtra("bio", user.getBio());

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class TileViewHolder extends RecyclerView.ViewHolder {
        TextView profileName, profileAge, offeredSkill, wishSkill, username, bio;
        ImageView genderIcon;
        CircleImageView profileImage;

        public TileViewHolder(@NonNull View itemView) {
            super(itemView);

            profileName = itemView.findViewById(R.id.profile_name);
            profileAge = itemView.findViewById(R.id.profile_age);
            offeredSkill = itemView.findViewById(R.id.offered_skill);
            wishSkill = itemView.findViewById(R.id.wish_skill);
            genderIcon = itemView.findViewById(R.id.gender_icon);
            profileImage = itemView.findViewById(R.id.profile_image);
            username = itemView.findViewById(R.id.toolbar_username);
            bio = itemView.findViewById(R.id.text_bio);
        }
    }
}
