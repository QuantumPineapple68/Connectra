package com.example.connectra;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class RecyclerProfileMainActivity extends AppCompatActivity {

    Button connect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_profile_main);

        FirebaseAuth auth = FirebaseAuth.getInstance();

        // Retrieve data from the intent
        String name = getIntent().getStringExtra("name");
        String age = getIntent().getStringExtra("userAge");
        String mySkill = getIntent().getStringExtra("userMySkill");
        String goalSkill = getIntent().getStringExtra("userGoalSkill");
        String gender = getIntent().getStringExtra("userGender");
        String bio = getIntent().getStringExtra("bio");
        String userName = getIntent().getStringExtra("userName");
        String profileImage = getIntent().getStringExtra("profileImage");

        // Set data to UI elements
        TextView nameTextView = findViewById(R.id.text_name);
        TextView ageTextView = findViewById(R.id.toolbar_age);
        TextView mySkillTextView = findViewById(R.id.text_myskill);
        TextView goalSkillTextView = findViewById(R.id.text_goalskill);
        ImageView genderIcon = findViewById(R.id.image_gender);
        TextView bioTextView = findViewById(R.id.text_bio);
        TextView usernameTextView = findViewById(R.id.toolbar_username);
        ImageView profileImageView = findViewById(R.id.profile_image);
        connect = findViewById(R.id.connect_button);

        // Set the received data into UI views
        nameTextView.setText(name);
        ageTextView.setText("Age: " + age);
        mySkillTextView.setText(mySkill);
        goalSkillTextView.setText(goalSkill);
        bioTextView.setText(bio);
        usernameTextView.setText(userName);

        // Set profile image using Glide
        if (profileImage != null && !profileImage.isEmpty()) {
            Glide.with(this)
                    .load(profileImage)
                    .placeholder(R.drawable.no_profile_pic)
                    .error(R.drawable.no_profile_pic)
                    .into(profileImageView); // Use profileImageView instead of holder.profileImage
        } else {
            profileImageView.setImageResource(R.drawable.no_profile_pic);
        }

        // Set gender icon
        if ("Male".equalsIgnoreCase(gender)) {
            genderIcon.setImageResource(R.drawable.icon_male);
        } else if ("Female".equalsIgnoreCase(gender)) {
            genderIcon.setImageResource(R.drawable.icon_female);
        } else {
            genderIcon.setImageResource(R.drawable.icon_default); // Default icon
        }

        // Set the onClick listener for the connect button
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String chatPartnerId = getIntent().getStringExtra("userId");
                String chatPartnerName = getIntent().getStringExtra("name");

                Intent intent = new Intent(RecyclerProfileMainActivity.this, ChatActivity.class);
                intent.putExtra("currentUserId", Objects.requireNonNull(auth.getCurrentUser()).getUid());
                intent.putExtra("chatPartnerId", chatPartnerId);
                intent.putExtra("chatPartnerName", chatPartnerName);
                intent.putExtra("profileImage", profileImage);
                startActivity(intent);
            }
        });
    }
}
