package com.example.connectra;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class RecyclerProfileMainActivity extends AppCompatActivity {

    private DatabaseReference databaseRef;
    private Button connect, submitButton;
    private RatingBar ratingBar;
    private ImageView displayRating, profilImg, userCerf, backBtnpfp;
    private String profileUserId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_profile_main);

        databaseRef = FirebaseDatabase.getInstance().getReference("Users");
        FirebaseAuth auth = FirebaseAuth.getInstance();

        ratingBar = findViewById(R.id.ratingbar);
        submitButton = findViewById(R.id.submitrev);
        displayRating = findViewById(R.id.display_rating);
        profilImg = findViewById(R.id.profile_image);
        userCerf = findViewById(R.id.user_cerf);
        backBtnpfp = findViewById(R.id.backBtnpfp);

        // Retrieve data from the intent
        String name = getIntent().getStringExtra("name");
        String age = getIntent().getStringExtra("userAge");
        String mySkill = getIntent().getStringExtra("userMySkill");
        String goalSkill = getIntent().getStringExtra("userGoalSkill");
        String gender = getIntent().getStringExtra("userGender");
        String bio = getIntent().getStringExtra("bio");
        String userName = getIntent().getStringExtra("userName");
        String profileImage = getIntent().getStringExtra("profileImage");
        profileUserId = getIntent().getStringExtra("userId");
        String certificate = getIntent().getStringExtra("certificate");

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

        backBtnpfp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Set profile image using Glide
        if (profileImage != null && !profileImage.isEmpty()) {
            Glide.with(this)
                    .load(profileImage)
                    .placeholder(R.drawable.no_profile_pic)
                    .error(R.drawable.no_profile_pic)
                    .into((ImageView) findViewById(R.id.profile_image));
        } else {
            ((ImageView) findViewById(R.id.profile_image)).setImageResource(R.drawable.no_profile_pic);
        }

        if (certificate != null && !certificate.isEmpty()) {
            Glide.with(this)
                    .load(certificate)
                    .placeholder(R.drawable.default_certificate)
                    .error(R.drawable.default_certificate)
                    .into((ImageView) findViewById(R.id.user_cerf));
        } else {
            ((ImageView) findViewById(R.id.user_cerf)).setImageResource(R.drawable.default_certificate);
        }

        // Set gender icon
        if ("Male".equalsIgnoreCase(gender)) {
            genderIcon.setImageResource(R.drawable.icon_male);
        } else if ("Female".equalsIgnoreCase(gender)) {
            genderIcon.setImageResource(R.drawable.icon_female);
        } else {
            genderIcon.setImageResource(R.drawable.icon_default);
        }

        submitButton.setOnClickListener(view -> submitRating());
        connect.setOnClickListener(view -> {
            String chatPartnerId = profileUserId;
            String chatPartnerName = name;

            Intent intent = new Intent(RecyclerProfileMainActivity.this, ChatActivity.class);
            intent.putExtra("currentUserId", Objects.requireNonNull(auth.getCurrentUser()).getUid());
            intent.putExtra("chatPartnerId", chatPartnerId);
            intent.putExtra("chatPartnerName", chatPartnerName);
            intent.putExtra("profileImage", profileImage);
            startActivity(intent);
        });

        fetchAndDisplayRating();

        profilImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RecyclerProfileMainActivity.this, ShowProfileImage.class);
                intent.putExtra("profileImage", profileImage);
                startActivity(intent);
            }
        });
    }

    private void fetchAndDisplayRating() {
        DatabaseReference revRef = databaseRef.child(profileUserId).child("rev");
        DatabaseReference ratingsRef = databaseRef.child(profileUserId).child("ratings");

        ratingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long totalReviews = snapshot.getChildrenCount();

                TextView numbrev = findViewById(R.id.numb_revs);
                numbrev.setText("(" + totalReviews + ")");

                if (totalReviews == 0) {
                    // No reviews available
                    displayRating.setImageResource(R.drawable.r0);
                    return;
                }

                revRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        float totalRev = snapshot.exists() ? snapshot.getValue(Float.class) : 0f;
                        float average = totalRev / totalReviews;

                        // Display appropriate image based on average rating
                        int imageResId = getRatingImageResource(average);
                        displayRating.setImageResource(imageResId);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(RecyclerProfileMainActivity.this, "Error fetching rating: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RecyclerProfileMainActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int getRatingImageResource(float average) {
        if (average <= 0) return R.drawable.r0;
        else if (average <= 0.5) return R.drawable.r0_5;
        else if (average <= 1) return R.drawable.r1;
        else if (average <= 1.5) return R.drawable.r1_5;
        else if (average <= 2) return R.drawable.r2;
        else if (average <= 2.5) return R.drawable.r2_5;
        else if (average <= 3) return R.drawable.r3;
        else if (average <= 3.5) return R.drawable.r3_5;
        else if (average <= 4) return R.drawable.r4;
        else if (average <= 4.5) return R.drawable.r4_5;
        else return R.drawable.r5;
    }

    private void submitRating() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        float ratingValue = ratingBar.getRating();
        String currentUserId = auth.getCurrentUser().getUid();

        if (ratingValue == 0) {
            Toast.makeText(this, "Please select a rating!", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference profileRatingRef = databaseRef.child(profileUserId).child("ratings").child(currentUserId);
        profileRatingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(RecyclerProfileMainActivity.this, "You have already rated this profile!", Toast.LENGTH_SHORT).show();
                } else {
                    DatabaseReference revRef = databaseRef.child(profileUserId).child("rev");
                    revRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            float currentRating = snapshot.exists() ? snapshot.getValue(Float.class) : 0f;
                            float newRating = currentRating + ratingValue;

                            revRef.setValue(newRating).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    profileRatingRef.setValue(ratingValue);
                                    Toast.makeText(RecyclerProfileMainActivity.this, "Rating submitted successfully!", Toast.LENGTH_SHORT).show();
                                    fetchAndDisplayRating(); // Refresh rating display
                                } else {
                                    Toast.makeText(RecyclerProfileMainActivity.this, "Failed to submit rating. Try again.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(RecyclerProfileMainActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(RecyclerProfileMainActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
