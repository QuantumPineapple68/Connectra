package com.nachiket.connectra;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.nachiket.connectra.utility.MessageFilter;
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

public class ChangeSkillActivity extends AppCompatActivity {

    private EditText mySkillEditText, goalSkillEditText, bioEditText, nameEditText, ageEditText;
    private Button saveButton, uploadBtn;
    private ImageView certificate;

    private FirebaseAuth auth;
    private DatabaseReference userRef;
    private FirebaseStorage storage;
    private Uri certificateUri;
    private AlertDialog verificationDialog;

    private ActivityResultLauncher<Intent> certificatePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_skill);

        nameEditText = findViewById(R.id.new_name);
        ageEditText = findViewById(R.id.new_age);
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
        loadUserData();
    }

    private void loadUserData() {
        if (auth.getCurrentUser() != null) {
            userRef.get().addOnSuccessListener(dataSnapshot -> {
                if (dataSnapshot.exists()) {
                    nameEditText.setText(dataSnapshot.child("name").getValue(String.class));
                    String age = dataSnapshot.child("age").getValue(String.class);
                    ageEditText.setText(age != null ? age : "");
                    String bio = dataSnapshot.child("bio").getValue(String.class);
                    bioEditText.setText(bio != null ? bio : "");
                    String mySkill = dataSnapshot.child("myskill").getValue(String.class);
                    mySkillEditText.setText(mySkill != null ? mySkill : "");
                    String goalSkill = dataSnapshot.child("goalskill").getValue(String.class);
                    goalSkillEditText.setText(goalSkill != null ? goalSkill : "");
                }
            }).addOnFailureListener(e -> toast("Failed to load user data"));
        }
    }

    private void saveSkills() {
        String name = nameEditText.getText().toString().trim();
        String age = ageEditText.getText().toString().trim();
        String mySkill = mySkillEditText.getText().toString().trim();
        String goalSkill = goalSkillEditText.getText().toString().trim();
        String bio = bioEditText.getText().toString().trim();

        if (MessageFilter.containsInappropriateContent(mySkill + goalSkill + bio + name)) {
            toast("Can't use inappropriate words");
            return;
        }

        Map<String, Object> updates = new HashMap<>();

        // Add name update
        if (!name.isEmpty()) {
            updates.put("name", name);
        }

        // Add age update with validation
        if (!age.isEmpty()) {
            try {
                int ageValue = Integer.parseInt(age);
                if (ageValue < 13 || ageValue > 100) {
                    toast("Age must be between 13 and 100");
                    return;
                }
                updates.put("age", age);
            } catch (NumberFormatException e) {
                toast("Please enter a valid age");
                return;
            }
        }

        // Your existing skill updates
        if (!mySkill.isEmpty()) {
            updates.put("myskill", mySkill);
        }
        if (!goalSkill.isEmpty()) {
            updates.put("goalskill", goalSkill);
        }
        if (!bio.isEmpty()) {
            if (bio.length() > 150) {
                Toast.makeText(ChangeSkillActivity.this,
                        "Bio can't be more than 150 characters",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            updates.put("bio", bio);
        }

        if (updates.isEmpty()) {
            snackbar("No changes to update");
            return;
        }

        userRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                toast("Information has been updated!");
                Intent intent = new Intent(ChangeSkillActivity.this, MainActivity.class);
                intent.putExtra("refreshProfile", true);
                startActivity(intent);
                finish();
            } else {
                String message = task.getException() != null ?
                        task.getException().getMessage() : "Error updating information.";
                toast(message);
            }
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
                            userRef.child("cerfApproved").setValue(false);
                            // Display the uploaded certificate using Glide
                            Glide.with(this)
                                    .load(url)
                                    .placeholder(R.drawable.default_certificate)
                                    .error(R.drawable.default_certificate)
                                    .into(certificate);

                            snackbar("Certificate uploaded and updated successfully!");
                            verificationDialog = new AlertDialog.Builder(this)
                                    .setTitle("Image sent for Verification")
                                    .setMessage("Your certificate has been sent to the admin for NSFW verification to check for mature content. It will appear to everyone once verified.")
                                    .setCancelable(true)
                                    .setPositiveButton("OK", (dialog, which) -> {
                                        dialog.dismiss();
                                    })
                                    .create();

                            verificationDialog.show();
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
