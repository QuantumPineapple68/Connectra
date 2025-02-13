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
import com.example.connectra.utility.MessageFilter;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
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

        mySkillEditText = findViewById(R.id.new_myskill);
        goalSkillEditText = findViewById(R.id.new_goalskill);
        saveButton = findViewById(R.id.save);
        bioEditText = findViewById(R.id.new_bio);
        uploadBtn = findViewById(R.id.cerf_btn);
        certificate = findViewById(R.id.reg_cerf);

        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance(FirebaseApp.getInstance("secondary"));

        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
        } else {
            toast("User not logged in!");
            finish();
        }

        saveButton.setOnClickListener(v -> saveSkills());

        uploadBtn.setOnClickListener(v -> openCertificatePicker());

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
                toast("Failed to load certificate.");
            }
        });
    }

    private void saveSkills() {
        String mySkill = mySkillEditText.getText().toString().trim();
        String goalSkill = goalSkillEditText.getText().toString().trim();
        String bio = bioEditText.getText().toString().trim();

        if (MessageFilter.containsInappropriateContent(mySkill + goalSkill + bio)) {
            toast("Can't use inappropriate words");
            return;
        }

        Map<String, Object> updates = new HashMap<>();

        if (!mySkill.isEmpty()) {
            updates.put("myskill", mySkill);
        }
        if (!goalSkill.isEmpty()) {
            updates.put("goalskill", goalSkill);
        }
        if (!bio.isEmpty()) {
            updates.put("bio", bio);
        }


        if (updates.isEmpty()) {
            snackbar("No changes to update");
            return;
        }

        userRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                snackbar("Information has been updated!");
            } else {
                String message = "Error updating skills.";
                if (task.getException() != null) {
                    message = task.getException().getMessage();
                }
                toast(message);
            }
            // Navigate back to MainActivity
            Intent intent = new Intent(ChangeSkillActivity.this, MainActivity.class);
            intent.putExtra("refreshProfile", true);
            startActivity(intent);
            finish();
        });
    }

    private void openCertificatePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        certificatePickerLauncher.launch(intent);
    }

    private void uploadCertificate() {
        if (certificateUri == null) {
            toast("No certificate selected.");
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
                    toast("Failed to retrieve certificate URL: " + e.getMessage());
                });
            } else {
                pd.dismiss();
                // Handle failure to upload file
                if (task.getException() != null) {
                    toast("Certificate upload failed: " + task.getException().getMessage());
                } else {
                    toast("Certificate upload failed.");
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
        String message = "Error occurred";
        if (task.getException() != null) {
            message = task.getException().getMessage();
        }
        toast(message);
    }

    private void snackbar(String msg) {
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT).show();
    }

    private void toast(String msg){
        Toast.makeText(ChangeSkillActivity.this, msg, Toast.LENGTH_SHORT).show();
    }
}
