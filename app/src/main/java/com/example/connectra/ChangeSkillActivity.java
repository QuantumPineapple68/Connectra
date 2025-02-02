package com.example.connectra;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Objects;

public class ChangeSkillActivity extends AppCompatActivity {

    private EditText mySkillEditText, goalSkillEditText, bioEditText;
    private Button saveButton, uploadBtn;
    private ImageView certificate;

    private FirebaseAuth auth;
    private DatabaseReference userRef;
    private FirebaseStorage storage;
    private Uri certificateUri;

    private ActivityResultLauncher<Intent> certificatePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_skill);

        // Initialize Views
        mySkillEditText = findViewById(R.id.new_myskill);
        goalSkillEditText = findViewById(R.id.new_goalskill);
        saveButton = findViewById(R.id.save);
        bioEditText = findViewById(R.id.new_bio);
        uploadBtn = findViewById(R.id.cerf_btn);
        certificate = findViewById(R.id.reg_cerf);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance(FirebaseApp.getInstance("secondary"));

        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish(); // End the activity if user is not authenticated
        }

        // Save button functionality
        saveButton.setOnClickListener(v -> saveSkills());

        // Certificate upload button functionality
        uploadBtn.setOnClickListener(v -> openCertificatePicker());

        // Initialize the ActivityResultLauncher for picking a certificate
        certificatePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        certificateUri = result.getData().getData();
                        uploadCertificate();
                    }
                }
        );
        userRef.child("certificateUrl").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String url = task.getResult().getValue(String.class);
                Glide.with(this)
                        .load(url)
                        .placeholder(R.drawable.default_certificate)
                        .error(R.drawable.default_certificate)
                        .into(certificate);
            } else {
                Toast.makeText(this, "Failed to load certificate.", Toast.LENGTH_SHORT).show();
            }
        });
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
                            snackbar("Your Skill has been Updated");
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
                            snackbar("Your Interest has been Updated");
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
                            snackbar("Your Bio has been Updated");
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

    private void openCertificatePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        certificatePickerLauncher.launch(intent);
    }

    private void uploadCertificate() {
        if (certificateUri == null) {
            Toast.makeText(this, "No certificate selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userRef == null) {
            Toast.makeText(this, "User reference is null.", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Uploading...");
        pd.show();

        // Extract a valid file name using timestamp and file extension
        String fileName = System.currentTimeMillis() + "." + getFileExtension(certificateUri);

        // Create a reference to the storage path
        StorageReference certRef = storage.getReference().child("connectra_certificates").child(fileName);

        certRef.putFile(certificateUri).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Retrieve the download URL of the uploaded certificate
                certRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String url = uri.toString();

                    // Update or create the 'certificateUrl' field in the database
                    userRef.child("certificateUrl").setValue(url).addOnCompleteListener(task1 -> {
                        pd.dismiss();
                        if (task1.isSuccessful()) {
                            // Display the uploaded certificate using Glide
                            Glide.with(this)
                                    .load(url)
                                    .placeholder(R.drawable.default_certificate)
                                    .error(R.drawable.default_certificate)
                                    .into(certificate);

                            snackbar("Certificate uploaded and updated successfully!");
                        } else {
                            // Handle failure to update database
                            showError(task1);
                        }
                    });
                }).addOnFailureListener(e -> {
                    pd.dismiss();
                    // Handle failure to get the download URL
                    Toast.makeText(this, "Failed to retrieve certificate URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } else {
                pd.dismiss();
                // Handle failure to upload file
                if (task.getException() != null) {
                    Toast.makeText(this, "Certificate upload failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Certificate upload failed.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Helper method to get file extension
    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void showError(@NonNull Task<Void> task) {
        String message = "Error updating skills.";
        if (task.getException() != null) {
            message = task.getException().getMessage();
        }
        Toast.makeText(ChangeSkillActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private void snackbar(String msg) {
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT).show();
    }
}
