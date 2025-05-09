package com.nachiket.connectra;

import android.os.Bundle;
import android.view.Window;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.color.MaterialColors;

public class ShowImage extends AppCompatActivity {

    private PhotoView photoView;
    private ImageView back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_image);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Window window = getWindow();
        int statusBarColor = getResources().getColor(R.color.black, getTheme());
        window.setStatusBarColor(statusBarColor);
        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(window, window.getDecorView());
        boolean isLightBackground = MaterialColors.isColorLight(statusBarColor);
        windowInsetsController.setAppearanceLightStatusBars(isLightBackground);

        photoView = findViewById(R.id.image);
        back = findViewById(R.id.backBtnpfp);

        // Configure PhotoView
        photoView.setMinimumScale(1.0f);
        photoView.setMaximumScale(5.0f);
        photoView.setMediumScale(2.5f);

        back.setOnClickListener(v -> finish());

        String image = getIntent().getStringExtra("image");
        boolean imageApproved = getIntent().getBooleanExtra("imageApproved", false);
        boolean isProfilePic = getIntent().getBooleanExtra("isProfilePic", true);

        if (image != null && !image.isEmpty() && imageApproved) {
            Glide.with(this)
                    .load(image)
                    .placeholder(isProfilePic ? R.drawable.no_profile_pic : R.drawable.default_certificate)
                    .error(isProfilePic ? R.drawable.no_profile_pic : R.drawable.default_certificate)
                    .into(photoView);
        } else {
            photoView.setImageResource(isProfilePic ? R.drawable.no_profile_pic : R.drawable.default_certificate);
        }
    }
}

//test push