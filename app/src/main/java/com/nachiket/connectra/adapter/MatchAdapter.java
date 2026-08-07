package com.nachiket.connectra.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nachiket.connectra.R;
import com.nachiket.connectra.RecyclerProfileMainActivity;
import com.nachiket.connectra.utility.ConnectraApi;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Horizontal "✨ Matches for you" carousel on the home screen.
 * Backed by the AI Skill-Match Engine (Workers AI + Vectorize).
 */
public class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.MatchViewHolder> {

    private final Context context;
    private final List<ConnectraApi.SkillMatch> matches;

    public MatchAdapter(Context context, List<ConnectraApi.SkillMatch> matches) {
        this.context = context;
        this.matches = matches;
    }

    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_match, parent, false);
        return new MatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        ConnectraApi.SkillMatch match = matches.get(position);

        String firstName = match.name != null && !match.name.isEmpty()
                ? match.name.split(" ")[0] : "User";
        holder.name.setText(firstName);
        holder.skill.setText(match.myskill);
        holder.score.setText(match.score + "%");
        holder.avatar.setImageResource(R.drawable.no_profile_pic);

        // Load the avatar (base64 data URI or legacy URL) from RTDB
        FirebaseDatabase.getInstance().getReference("Users").child(match.uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String profileImage = snapshot.child("profileImage").getValue(String.class);
                        boolean profileApproved = snapshot.child("profileApproved").getValue(Boolean.class) != null
                                ? snapshot.child("profileApproved").getValue(Boolean.class)
                                : false;
                        if (profileImage != null && !profileImage.isEmpty() && profileApproved) {
                            Glide.with(context)
                                    .load(profileImage)
                                    .placeholder(R.drawable.no_profile_pic)
                                    .error(R.drawable.no_profile_pic)
                                    .into(holder.avatar);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });

        holder.itemView.setOnClickListener(v -> openProfile(match));
    }

    private void openProfile(ConnectraApi.SkillMatch match) {
        // Fetch the full user record so the profile screen gets the same extras as TileAdapter
        FirebaseDatabase.getInstance().getReference("Users").child(match.uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;

                        boolean profileApproved = snapshot.child("profileApproved").getValue(Boolean.class) != null
                                ? snapshot.child("profileApproved").getValue(Boolean.class)
                                : false;
                        boolean cerfApproved = snapshot.child("cerfApproved").getValue(Boolean.class) != null
                                ? snapshot.child("cerfApproved").getValue(Boolean.class)
                                : false;

                        Intent intent = new Intent(context, RecyclerProfileMainActivity.class);
                        intent.putExtra("name", snapshot.child("name").getValue(String.class));
                        intent.putExtra("userAge", snapshot.child("age").getValue(String.class));
                        intent.putExtra("userMySkill", snapshot.child("myskill").getValue(String.class));
                        intent.putExtra("userGoalSkill", snapshot.child("goalskill").getValue(String.class));
                        intent.putExtra("userGender", snapshot.child("gender").getValue(String.class));
                        intent.putExtra("userName", snapshot.child("username").getValue(String.class));
                        intent.putExtra("bio", snapshot.child("bio").getValue(String.class));
                        intent.putExtra("userId", match.uid);
                        intent.putExtra("profileImage", snapshot.child("profileImage").getValue(String.class));
                        intent.putExtra("certificate", snapshot.child("certificateUrl").getValue(String.class));
                        intent.putExtra("profileApproved", profileApproved);
                        intent.putExtra("cerfApproved", cerfApproved);

                        context.startActivity(intent);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    @Override
    public int getItemCount() {
        return matches.size();
    }

    public static class MatchViewHolder extends RecyclerView.ViewHolder {
        CircleImageView avatar;
        TextView score, name, skill;

        public MatchViewHolder(@NonNull View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.match_avatar);
            score = itemView.findViewById(R.id.match_score);
            name = itemView.findViewById(R.id.match_name);
            skill = itemView.findViewById(R.id.match_skill);
        }
    }
}
