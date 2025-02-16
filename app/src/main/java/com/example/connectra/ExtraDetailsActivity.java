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
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.bumptech.glide.Glide;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
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
    Button register, cerfbtn;
    ImageView cerf;

    FirebaseAuth auth;
    DatabaseReference databaseRef;
    ProgressDialog pd;

    FirebaseStorage storage;
    Uri certificateUri;
    File localCertificateFile;
    ActivityResultLauncher<Intent> certificatePickerLauncher;
    String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extra_details);

        // Initialize views
        name = findViewById(R.id.fulltxtname);
        username = findViewById(R.id.fulltxtusername);
        myskill = findViewById(R.id.mytxtskill);
        goalskill = findViewById(R.id.goaltxtskill);
        age = findViewById(R.id.mytxtage);
        gender = findViewById(R.id.mytxtgender);
        register = findViewById(R.id.btnRegNow);
        cerfbtn = findViewById(R.id.cerf_btn);
        cerf = findViewById(R.id.reg_cerf);

        // Get email from intent
        userEmail = getIntent().getStringExtra("email");

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("Users");
        storage = FirebaseStorage.getInstance(FirebaseApp.getInstance("secondary"));
        pd = new ProgressDialog(this);

        register.setOnClickListener(v -> {
            String txt_name = name.getText().toString().trim();
            String txt_username = username.getText().toString().trim();
            String txt_myskill = myskill.getText().toString().trim();
            String txt_goalskill = goalskill.getText().toString().trim();
            String txt_age = age.getText().toString().trim();
            String txt_gender = gender.getText().toString().trim().toLowerCase();;

            if (TextUtils.isEmpty(txt_name) || TextUtils.isEmpty(txt_username)) {
                Toast.makeText(ExtraDetailsActivity.this, "Fields can't be Empty", Toast.LENGTH_SHORT).show();
            }
            else if (!txt_name.contains(" ")) {
                Toast.makeText(ExtraDetailsActivity.this, "Please enter your full name with space in between", Toast.LENGTH_SHORT).show();
            }
            else if (txt_myskill.length() > 30 || txt_goalskill.length() > 30) {
                Toast.makeText(ExtraDetailsActivity.this, "Describe skill sets in brief (30 letters)", Toast.LENGTH_SHORT).show();
            }
            else if (!txt_gender.equals("male") && !txt_gender.equals("female")) {
                Toast.makeText(ExtraDetailsActivity.this, "Gender must be either male or female", Toast.LENGTH_SHORT).show();
            }
            else {
                try {
                    int ageValue = Integer.parseInt(txt_age);
                    if (ageValue < 5 || ageValue > 150) {
                        Toast.makeText(ExtraDetailsActivity.this, "Age must be between 5 and 150", Toast.LENGTH_SHORT).show();
                    } else {
                        registerUser(txt_name, txt_username, txt_myskill, txt_goalskill, txt_age, txt_gender);
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(ExtraDetailsActivity.this, "Please enter a valid age", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Certificate Upload functionality
        cerfbtn.setOnClickListener(v -> openCertificatePicker());

        certificatePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        certificateUri = result.getData().getData();
                        saveCertificateLocally();
                    }
                }
        );
    }

    private void registerUser(String name, String username, String myskill, String goalskill, String age, String gender) {
        pd.setMessage("Please Wait ...");
        pd.show();

        String userId = auth.getCurrentUser().getUid();

        HashMap<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("username", username);
        map.put("email", userEmail);
        map.put("id", userId);
        map.put("myskill", myskill);
        map.put("goalskill", goalskill);
        map.put("age", age);
        map.put("gender", gender);
        map.put("profileApproved", false);
        map.put("cerfApproved", false);
        map.put("banned", false);

        databaseRef.child(userId).setValue(map).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                uploadCertificate(userId);
            } else {
                pd.dismiss();
                Toast.makeText(ExtraDetailsActivity.this, "Registration Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ... Include the same certificate handling methods from RegisterActivity ...
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

    private void uploadCertificate(String userId) {
        if (localCertificateFile == null) {
            pd.dismiss();
            // Registration successful even without certificate
            proceedToMainActivity();
            return;
        }

        StorageReference certRef = storage.getReference().child("connectra_certificates")
                .child(System.currentTimeMillis() + "_" + localCertificateFile.getName());

        certRef.putFile(Uri.fromFile(localCertificateFile)).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                certRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String url = uri.toString();
                    databaseRef.child(userId).child("certificateUrl").setValue(url)
                            .addOnCompleteListener(task1 -> {
                                pd.dismiss();
                                if (task1.isSuccessful()) {
                                    proceedToMainActivity();
                                } else {
                                    Toast.makeText(ExtraDetailsActivity.this,
                                            "Error saving certificate URL.", Toast.LENGTH_SHORT).show();
                                }
                            });
                });
            } else {
                pd.dismiss();
                Toast.makeText(ExtraDetailsActivity.this, "Certificate upload failed",
                        Toast.LENGTH_SHORT).show();
                proceedToMainActivity();
            }
        });
    }

    private void proceedToMainActivity() {
        Toast.makeText(ExtraDetailsActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(ExtraDetailsActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}