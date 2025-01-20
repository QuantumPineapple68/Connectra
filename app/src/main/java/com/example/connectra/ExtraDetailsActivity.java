package com.example.connectra;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;

public class ExtraDetailsActivity extends AppCompatActivity {

    EditText name, username, myskill, goalskill, age, gender;
    Button submitDetails;
    ImageView cerf;

    Uri certificateUri;
    DatabaseReference databaseRef;
    ProgressDialog pd;
    FirebaseAuth auth;
    FirebaseStorage storage;
    File localCertificateFile;
    ActivityResultLauncher<Intent> certificatePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register2);

        name = findViewById(R.id.fulltxtname);
        username = findViewById(R.id.fulltxtusername);
        myskill = findViewById(R.id.mytxtskill);
        goalskill = findViewById(R.id.goaltxtskill);
        age = findViewById(R.id.mytxtage);
        gender = findViewById(R.id.mytxtgender);
        cerf = findViewById(R.id.reg_cerf);
        submitDetails = findViewById(R.id.btnRegNow);

        auth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("Users");
        storage = FirebaseStorage.getInstance(FirebaseApp.getInstance("secondary"));

        pd = new ProgressDialog(this);

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Authentication failed. Please sign in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String email = getIntent().getStringExtra("email");
        String displayName = getIntent().getStringExtra("name");

        if (email == null || displayName == null) {
            Toast.makeText(this, "Failed to retrieve user details. Please try again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        String userId = currentUser.getUid();

        cerf.setOnClickListener(v -> openCertificatePicker());

        certificatePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        certificateUri = result.getData().getData();
                        saveCertificateLocally();
                    }
                }
        );

        submitDetails.setOnClickListener(v -> {
            String txt_name = name.getText().toString();
            String txt_username = username.getText().toString();
            String txt_myskill = myskill.getText().toString();
            String txt_goalskill = goalskill.getText().toString();
            String txt_age = age.getText().toString();
            String txt_gender = gender.getText().toString();

            if (TextUtils.isEmpty(txt_name) || TextUtils.isEmpty(txt_username)) {
                Toast.makeText(this, "Fields can't be empty", Toast.LENGTH_SHORT).show();
            } else if (txt_myskill.length() > 30 || txt_goalskill.length() > 30) {
                Toast.makeText(this, "Describe skill sets in brief (30 letters)", Toast.LENGTH_SHORT).show();
            } else if (certificateUri == null) {
                Toast.makeText(this, "Please upload a certificate!", Toast.LENGTH_SHORT).show();
            } else {
                uploadCertificate(userId, () -> saveDetailsToDatabase(txt_name, txt_username, txt_myskill, txt_goalskill, txt_age, txt_gender, userId));
            }
        });
    }

    private void openCertificatePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        certificatePickerLauncher.launch(intent);
    }

    private void saveCertificateLocally() {
        try {
            InputStream inputStream = getContentResolver().openInputStream(certificateUri);
            File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (!dir.exists()) dir.mkdirs();

            localCertificateFile = new File(dir, "certificate_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(localCertificateFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            Glide.with(this)
                    .load(localCertificateFile)
                    .placeholder(R.drawable.default_certificate)
                    .error(R.drawable.default_certificate)
                    .into(cerf);

            Toast.makeText(this, "Certificate selected and saved locally.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save certificate locally.", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadCertificate(String userId, Runnable onSuccess) {
        if (certificateUri == null) {
            Toast.makeText(this, "No certificate selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        pd.setMessage("Uploading certificate...");
        pd.show();

        StorageReference certRef = storage.getReference("connectra_certificates").child(userId + "_certificate.jpg");

        certRef.putFile(certificateUri).addOnSuccessListener(taskSnapshot -> certRef.getDownloadUrl().addOnSuccessListener(uri -> {
            databaseRef.child(userId).child("certificateUrl").setValue(uri.toString()).addOnCompleteListener(task -> {
                pd.dismiss();
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Certificate uploaded successfully.", Toast.LENGTH_SHORT).show();
                    onSuccess.run();
                } else {
                    Toast.makeText(this, "Failed to save certificate URL.", Toast.LENGTH_SHORT).show();
                }
            });
        }).addOnFailureListener(e -> {
            pd.dismiss();
            Toast.makeText(this, "Failed to upload certificate.", Toast.LENGTH_SHORT).show();
        }));
    }

    private void saveDetailsToDatabase(String name, String username, String myskill, String goalskill, String age, String gender, String userId) {
        pd.setMessage("Saving details...");
        pd.show();

        HashMap<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("username", username);
        map.put("myskill", myskill);
        map.put("goalskill", goalskill);
        map.put("age", age);
        map.put("gender", gender);

        databaseRef.child(userId).setValue(map).addOnCompleteListener(task -> {
            pd.dismiss();
            if (task.isSuccessful()) {
                Toast.makeText(this, "Details saved successfully.", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            } else {
                Toast.makeText(this, "Failed to save details.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            pd.dismiss();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
