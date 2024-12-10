package com.example.connectra.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageSwitcher;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectra.Fragments.NewUser;
import com.example.connectra.R;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private List<NewUser> users;

    public UserAdapter(List<NewUser> users) {
        this.users = users;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NewUser user = users.get(position);
        holder.name.setText(user.getName());
        holder.offeredSkill.setText("Offered: " + user.getMyskill());
        holder.wishSkill.setText("Wants to learn: " + user.getGoalskill());
//        if (holder.genderImageView != null) {
//            if (user.getGender() != null) {
//                String gender = user.getGender().toLowerCase();
//                if (gender.equals("male")) {
//                    holder.genderImageView.setImageResource(R.drawable.icon_male);
//                } else if (gender.equals("female")) {
//                    holder.genderImageView.setImageResource(R.drawable.icon_female);
//                } else {
//                    holder.genderImageView.setImageResource(R.drawable.icon_default); // No image for other genders
//                }
//            } else {
//                holder.genderImageView.setImageResource(R.drawable.icon_default); // Remove image if gender is null
//            }
//        }

    }

    @Override
    public int getItemCount() {

        return users.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageSwitcher genderImageView;
        TextView name, offeredSkill, wishSkill;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.profile_name);
            offeredSkill = itemView.findViewById(R.id.offered_skill);
            wishSkill = itemView.findViewById(R.id.wish_skill);
            genderImageView = itemView.findViewById(R.id.gender_icon);
        }
    }
}
