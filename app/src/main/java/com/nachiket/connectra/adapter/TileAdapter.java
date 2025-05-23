package com.nachiket.connectra.adapter;

import static androidx.core.content.ContextCompat.getSystemService;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nachiket.connectra.model.NewUser;
import com.nachiket.connectra.R;
import com.nachiket.connectra.RecyclerProfileMainActivity;

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
        View view = LayoutInflater.from(context).inflate(R.layout.item_new_user, parent, false);
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

        View notificationDot = holder.itemView.findViewById(R.id.notification_dot);
        if (user.hasUnreadMessages()) {
            notificationDot.setVisibility(View.VISIBLE);
        } else {
            notificationDot.setVisibility(View.GONE);
        }

        // Set gender icon
        if ("Male".equalsIgnoreCase(user.getGender())) {
            holder.genderIcon.setImageResource(R.drawable.icon_male);
        } else if ("Female".equalsIgnoreCase(user.getGender())) {
            holder.genderIcon.setImageResource(R.drawable.icon_female);
        } else {
            holder.genderIcon.setImageResource(R.drawable.icon_default); // Default icon
        }

        // Set profile image using Glide
        String profileImage = user.getProfileImage();
        if (profileImage != null && !profileImage.isEmpty() && user.isProfileAprooved()) {
            Glide.with(context)
                    .load(profileImage)
                    .placeholder(R.drawable.no_profile_pic)
                    .error(R.drawable.no_profile_pic)
                    .into(holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.no_profile_pic);
        }

        DatabaseReference onlineRef = FirebaseDatabase.getInstance()
                .getReference("OnlineUsers")
                .child(user.getId());

        onlineRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isOnline = snapshot.child("online").getValue(Boolean.class);
                holder.activeIndicator.setVisibility(
                        isOnline != null && isOnline ? View.VISIBLE : View.GONE
                );
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TileAdapter", "Failed to get online status: " + error.getMessage());
            }
        });

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
            intent.putExtra("userId", user.getId());
            intent.putExtra("profileImage", user.getProfileImage());
            intent.putExtra("certificate", user.getCerf());
            intent.putExtra("profileApproved", user.isProfileAprooved());
            intent.putExtra("cerfApproved", user.isCerfApproved());

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class TileViewHolder extends RecyclerView.ViewHolder {
        TextView profileName, profileAge, offeredSkill, wishSkill;
        ImageView genderIcon;
        CircleImageView profileImage;
        View activeIndicator;

        public TileViewHolder(@NonNull View itemView) {
            super(itemView);

            profileName = itemView.findViewById(R.id.profile_name);
            profileAge = itemView.findViewById(R.id.profile_age);
            offeredSkill = itemView.findViewById(R.id.offered_skill);
            wishSkill = itemView.findViewById(R.id.wish_skill);
            genderIcon = itemView.findViewById(R.id.gender_icon);
            profileImage = itemView.findViewById(R.id.profile_image);
            activeIndicator = itemView.findViewById(R.id.active_indicator);

        }
    }
}
