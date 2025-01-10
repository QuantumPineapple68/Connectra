package com.example.connectra;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ChangeSkillActivity extends AppCompatActivity {

    private EditText mySkillEditText, goalSkillEditText, bioEditText;
    private Button saveButton;

    private FirebaseAuth auth;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_skill);

        // Initialize Views
        mySkillEditText = findViewById(R.id.new_myskill);
        goalSkillEditText = findViewById(R.id.new_goalskill);
        saveButton = findViewById(R.id.save);
        bioEditText = findViewById(R.id.new_bio);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish(); // End the activity if user is not authenticated
        }

        // Save button functionality
        saveButton.setOnClickListener(v -> saveSkills());
    }

    private void saveSkills() {
        String mySkill = mySkillEditText.getText().toString().trim();
        String goalSkill = goalSkillEditText.getText().toString().trim();
        String bio = bioEditText.getText().toString().trim();
        boolean hasUpdates = false;

        if (userRef == null) {
            Toast.makeText(this, "Unable to save skills. User reference is null.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update myskill if not empty
        if (!mySkill.isEmpty()) {
            userRef.child("myskill").setValue(mySkill)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ChangeSkillActivity.this, "Your Skill has been Updated", Toast.LENGTH_SHORT).show();
                        } else {
                            showError(task);
                        }
                    });
            hasUpdates = true;
        }

        // Update goalskill if not empty
        if (!goalSkill.isEmpty()) {
            userRef.child("goalskill").setValue(goalSkill)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ChangeSkillActivity.this, "Your Interest has been Updated", Toast.LENGTH_SHORT).show();
                        } else {
                            showError(task);
                        }
                    });
            hasUpdates = true;
        }

        // Update bio if not empty
        if (!bio.isEmpty()) {
            userRef.child("bio").setValue(bio)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ChangeSkillActivity.this, "Bio Updated", Toast.LENGTH_SHORT).show();
                        } else {
                            showError(task);
                        }
                    });
            hasUpdates = true;
        }

        if (!hasUpdates) {
            Toast.makeText(this, "No changes to update", Toast.LENGTH_SHORT).show();
        }

        Intent intent = new Intent(ChangeSkillActivity.this, MainActivity.class);
        intent.putExtra("refreshProfile", true);
        startActivity(intent);
        finish();
    }

    private void showError(@NonNull Task<Void> task) {
        String message = "Error updating skills.";
        if (task.getException() != null) {
            message = task.getException().getMessage();
        }
        Toast.makeText(ChangeSkillActivity.this, message, Toast.LENGTH_SHORT).show();
    }
}
