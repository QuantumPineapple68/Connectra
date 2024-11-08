package com.example.connectra;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPassword extends AppCompatActivity {

    private EditText emailAddress;
    private Button resetPasswordButton;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private ValueAnimator animator;
    private TextView headlineForgotPass;
    private TextView headlineForgotPass1;

    private boolean isDarkMode(ForgotPassword context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // enableEdgeToEdge() is a Kotlin extension function; you may need to implement similar functionality if required in Java
        setContentView(R.layout.activity_forgot_password);

        auth = FirebaseAuth.getInstance();
        emailAddress = findViewById(R.id.resetEmailAddress);
        resetPasswordButton = findViewById(R.id.resetBtn);
        headlineForgotPass = findViewById(R.id.headlineForgotPass);
        headlineForgotPass1 = findViewById(R.id.headline1ForgotPass);

        if (isDarkMode(this)) {
            headlineForgotPass.setTextColor(Color.WHITE);
            headlineForgotPass1.setTextColor(Color.WHITE);
        } else {
            headlineForgotPass.setTextColor(Color.BLACK);
            headlineForgotPass1.setTextColor(Color.BLACK);
        }

        resetPasswordButton.setOnClickListener(v -> {
            String email = emailAddress.getText().toString().trim();

            if (email.isEmpty()) {
                emailAddress.setError("Please enter your email");
                return;
            }

            startAnimatingButton();

            auth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                stopAnimatingButton();

                if (task.isSuccessful()) {
                    Toast.makeText(ForgotPassword.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(ForgotPassword.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void startAnimatingButton() {
        animator = ValueAnimator.ofInt(0, 3);
        animator.setDuration(2500);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            switch (value) {
                case 0:
                    resetPasswordButton.setText("Resetting the password.");
                    break;
                case 1:
                    resetPasswordButton.setText("Resetting the password..");
                    break;
                case 2:
                    resetPasswordButton.setText("Resetting the password...");
                    break;
                case 3:
                    resetPasswordButton.setText("Resetting the password....");
                    break;
            }
        });
        animator.start();
        resetPasswordButton.setEnabled(false);
    }

    private void stopAnimatingButton() {
        animator.cancel();
        resetPasswordButton.setText("Reset Password");
        resetPasswordButton.setEnabled(true);
    }
}
