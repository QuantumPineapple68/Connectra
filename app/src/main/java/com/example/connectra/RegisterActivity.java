package com.example.connectra;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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

public class RegisterActivity extends AppCompatActivity {

    EditText email, password, name, username, myskill, goalskill, age, gender, confirmPass;
    Button register, cerfbtn;
    TextView login;

    FirebaseAuth auth;
    DatabaseReference databaseRef;
    ProgressDialog pd;
    ImageView togglePassword, cerf;

    FirebaseStorage storage;
    Uri certificateUri;
    File localCertificateFile;
    ActivityResultLauncher<Intent> certificatePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register2);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        email = findViewById(R.id.txtEmailAddress);
        password = findViewById(R.id.txtPassword);
        register = findViewById(R.id.btnRegNow);
        name = findViewById(R.id.fulltxtname);
        username = findViewById(R.id.fulltxtusername);
        myskill = findViewById(R.id.mytxtskill);
        goalskill = findViewById(R.id.goaltxtskill);
        age = findViewById(R.id.mytxtage);
        gender = findViewById(R.id.mytxtgender);
        confirmPass = findViewById(R.id.comfirmtxtPassword);
        togglePassword = findViewById(R.id.togglePassword);
        login = findViewById(R.id.txtLoginNow);
        cerfbtn = findViewById(R.id.cerf_btn);
        cerf = findViewById(R.id.reg_cerf);

        auth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("Users");
        storage = FirebaseStorage.getInstance(FirebaseApp.getInstance("secondary"));

        pd = new ProgressDialog(this);

        initializeFirebaseInstances();

        register.setOnClickListener(v -> {
            String txt_email = email.getText().toString().trim();
            String txt_password = password.getText().toString().trim();
            String txt_name = name.getText().toString().trim();
            String txt_username = username.getText().toString().trim();
            String txt_myskill = myskill.getText().toString().trim();
            String txt_goalskill = goalskill.getText().toString().trim();
            String txt_age = age.getText().toString().trim();
            String txt_gender = gender.getText().toString().trim().toLowerCase();;
            String txt_confirmPass = confirmPass.getText().toString().trim();

            if (TextUtils.isEmpty(txt_email) || TextUtils.isEmpty(txt_password) || TextUtils.isEmpty(txt_name) || TextUtils.isEmpty(txt_username)) {
                Toast.makeText(RegisterActivity.this, "Fields can't be Empty", Toast.LENGTH_SHORT).show();
            } else if (txt_password.length() < 6) {
                Toast.makeText(RegisterActivity.this, "Password must be at least 6 Digits", Toast.LENGTH_SHORT).show();
            } else if (!txt_password.equals(txt_confirmPass)) {
                Toast.makeText(RegisterActivity.this, "Password doesn't match", Toast.LENGTH_SHORT).show();
            } else if (txt_myskill.length() > 30 || txt_goalskill.length() > 30) {
                Toast.makeText(RegisterActivity.this, "Describe skill sets in brief (30 letters)", Toast.LENGTH_SHORT).show();
            } else if (!txt_email.endsWith(".com")) {
                Toast.makeText(RegisterActivity.this, "Enter Valid e-mail", Toast.LENGTH_SHORT).show();
            } else if (!txt_name.contains(" ")) {
                Toast.makeText(RegisterActivity.this, "Please enter your full name with space in between", Toast.LENGTH_SHORT).show();
            } else if (!txt_gender.equals("male") && !txt_gender.equals("female")) {
                Toast.makeText(RegisterActivity.this, "Gender must be either male or female", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    int ageValue = Integer.parseInt(txt_age);
                    if (ageValue < 5 || ageValue > 150) {
                        Toast.makeText(RegisterActivity.this, "Age must be between 5 and 150", Toast.LENGTH_SHORT).show();
                    } else {
                        registerUser(txt_email,txt_password,txt_name, txt_username, txt_myskill, txt_goalskill, txt_age, txt_gender);
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(RegisterActivity.this, "Please enter a valid age", Toast.LENGTH_SHORT).show();
                }
            }
        });

        togglePassword.setOnClickListener(v -> togglePasswordVisibility(password, togglePassword));

        login.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
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

    private void registerUser(String Email, String Password, String name, String username, String myskill, String goalskill, String age, String gender) {
        pd.setMessage("Please Wait ...");
        pd.show();

        auth.createUserWithEmailAndPassword(Email, Password).addOnSuccessListener(authResult -> {
            String userId = auth.getCurrentUser().getUid();

            HashMap<String, Object> map = new HashMap<>();
            map.put("name", name);
            map.put("username", username);
            map.put("email", Email);
            map.put("id", userId);
            map.put("myskill", myskill);
            map.put("goalskill", goalskill);
            map.put("age", age);
            map.put("gender", gender);

            databaseRef.child(userId).setValue(map).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    uploadCertificate(userId);
                } else {
                    pd.dismiss();
                    Toast.makeText(RegisterActivity.this, "Registration Failed", Toast.LENGTH_SHORT).show();
                }
            });
        }).addOnFailureListener(e -> {
            pd.dismiss();
            Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void uploadCertificate(String userId) {
        // If no certificate is selected, proceed directly to completion
        if (localCertificateFile == null) {
            completeRegistration(userId, null);
            return;
        }

        // If certificate is selected, upload it
        StorageReference certRef = storage.getReference().child("connectra_certificates")
                .child(System.currentTimeMillis() + "_" + localCertificateFile.getName());

        certRef.putFile(Uri.fromFile(localCertificateFile)).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                certRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String url = uri.toString();
                    completeRegistration(userId, url);
                }).addOnFailureListener(e -> {
                    pd.dismiss();
                    Toast.makeText(RegisterActivity.this, "Failed to get certificate URL", Toast.LENGTH_SHORT).show();
                });
            } else {
                pd.dismiss();
                Toast.makeText(RegisterActivity.this, "Certificate upload failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void completeRegistration(String userId, String certificateUrl) {
        DatabaseReference userRef = databaseRef.child(userId);

        if (certificateUrl != null) {
            // If certificate was uploaded, save its URL
            userRef.child("certificateUrl").setValue(certificateUrl)
                    .addOnCompleteListener(task -> {
                        pd.dismiss();
                        handleRegistrationCompletion(task.isSuccessful());
                    });
        } else {
            // If no certificate, just complete registration
            pd.dismiss();
            handleRegistrationCompletion(true);
        }
    }

    private void handleRegistrationCompletion(boolean isSuccessful) {
        if (isSuccessful) {
            Toast.makeText(RegisterActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(RegisterActivity.this, "Error completing registration.", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("PrivateResource")
    private void togglePasswordVisibility(EditText passwordEditText, ImageView togglePassword) {
        if (passwordEditText.getTransformationMethod() instanceof PasswordTransformationMethod) {
            passwordEditText.setTransformationMethod(null);
            togglePassword.setImageResource(com.google.android.material.R.drawable.design_ic_visibility);
        } else {
            passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            togglePassword.setImageResource(com.google.android.material.R.drawable.design_ic_visibility_off);
        }
        passwordEditText.setSelection(passwordEditText.getText().length());
    }

    private void initializeFirebaseInstances() {
        auth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("Users");

        // Try to initialize storage with retry mechanism
        initializeStorage();
    }

    private void initializeStorage() {
        if (!MyApplication.isSecondaryInitialized()) {
            // Show a loading dialog
            ProgressDialog initDialog = new ProgressDialog(this);
            initDialog.setMessage("Initializing...");
            initDialog.setCancelable(false);
            initDialog.show();

            // Retry mechanism with a max of 3 attempts
            new Handler().postDelayed(new Runnable() {
                private int attempts = 0;

                @Override
                public void run() {
                    try {
                        storage = FirebaseStorage.getInstance(FirebaseApp.getInstance("secondary"));
                        initDialog.dismiss();
                    } catch (IllegalStateException e) {
                        attempts++;
                        if (attempts < 3) {
                            // Retry after 1 second
                            new Handler().postDelayed(this, 1000);
                        } else {
                            initDialog.dismiss();
                            // Show error dialog
                            new AlertDialog.Builder(RegisterActivity.this)
                                    .setTitle("Initialization Error")
                                    .setMessage("Failed to initialize storage. Please restart the app.")
                                    .setPositiveButton("OK", (dialog, which) -> finish())
                                    .setCancelable(false)
                                    .show();
                        }
                    }
                }
            }, 500);
        } else {
            storage = FirebaseStorage.getInstance(FirebaseApp.getInstance("secondary"));
        }
    }

}
