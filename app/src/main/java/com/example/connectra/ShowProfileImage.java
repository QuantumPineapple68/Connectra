package com.example.connectra;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;

public class ShowProfileImage extends AppCompatActivity {

    private ImageView imageView, back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_show_profile_image);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Fixed the findViewById line
        imageView = findViewById(R.id.profile_image);
        back = findViewById(R.id.backBtnpfp);

        back.setOnClickListener(v -> {
            finish();
        });

        String profileImage = getIntent().getStringExtra("profileImage");
        boolean profileApproved = getIntent().getBooleanExtra("profileApproved", false);

        // Load image using Glide
        if (profileImage != null && !profileImage.isEmpty() && profileApproved) {
            Glide.with(this)
                    .load(profileImage)
                    .placeholder(R.drawable.no_profile_pic)
                    .error(R.drawable.no_profile_pic)
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.no_profile_pic);
        }
    }
}