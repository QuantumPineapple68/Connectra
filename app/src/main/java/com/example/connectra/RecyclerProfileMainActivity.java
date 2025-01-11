package com.example.connectra;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class RecyclerProfileMainActivity extends AppCompatActivity {


    Button connect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_profile_main);

        // Retrieve data from the intent
        String name = getIntent().getStringExtra("name");
        String age = getIntent().getStringExtra("userAge");
        String mySkill = getIntent().getStringExtra("userMySkill");
        String goalSkill = getIntent().getStringExtra("userGoalSkill");
        String gender = getIntent().getStringExtra("userGender");
        String bio = getIntent().getStringExtra("bio");
        String userName = getIntent().getStringExtra("userName");

        // Set data to UI elements
        TextView nameTextView = findViewById(R.id.text_name);
        TextView ageTextView = findViewById(R.id.toolbar_age);
        TextView mySkillTextView = findViewById(R.id.text_myskill);
        TextView goalSkillTextView = findViewById(R.id.text_goalskill);
        ImageView genderIcon = findViewById(R.id.image_gender);
        TextView bioTextView = findViewById(R.id.text_bio);
        TextView usernameTextView = findViewById(R.id.toolbar_username);
        connect = findViewById(R.id.connect_button);


        nameTextView.setText(name);
        ageTextView.setText("Age: " + age);
        mySkillTextView.setText(mySkill);
        goalSkillTextView.setText(goalSkill);
        bioTextView.setText(bio);
        usernameTextView.setText(userName);

        if ("Male".equalsIgnoreCase(gender)) {
            genderIcon.setImageResource(R.drawable.icon_male);
        } else if ("Female".equalsIgnoreCase(gender)) {
            genderIcon.setImageResource(R.drawable.icon_female);
        } else {
            genderIcon.setImageResource(R.drawable.icon_default); // Default icon
        }

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String chatPartnerId = getIntent().getStringExtra("userId");
                String chatPartnerName = getIntent().getStringExtra("name");

                Intent intent = new Intent(RecyclerProfileMainActivity.this, ChatActivity.class);
                intent.putExtra("chatPartnerId", chatPartnerId);
                intent.putExtra("chatPartnerName", chatPartnerName);
                startActivity(intent);
            }
        });
    }
}
