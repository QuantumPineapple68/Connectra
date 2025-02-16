package com.example.connectra.Authentication;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.connectra.ChatActivity;
import com.example.connectra.MainActivity;
import com.example.connectra.model.MyApplication;
import com.example.connectra.R;
import com.example.connectra.utility.MessageFilter;
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
    AlertDialog termsDialog;
    private final Handler handler = new Handler(Looper.getMainLooper());

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

        initializeFirebaseInstances();

        pd = new ProgressDialog(this);

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
                toast("Fields can't be Empty");
            } else if (txt_password.length() < 6) {
                toast("Password must be at least 6 Digits");
            } else if (!txt_password.equals(txt_confirmPass)) {
                toast("Password doesn't match");
            } else if (txt_myskill.length() > 30 || txt_goalskill.length() > 30) {
                toast("Describe skill sets in brief (30 letters)");
            } else if (!txt_email.endsWith(".com")) {
                toast("Enter Valid e-mail");
            } else if (!txt_name.contains(" ")) {
                toast("Please enter your full name with space in between");
            } else if (!txt_gender.equals("male") && !txt_gender.equals("female")) {
                toast("Gender must be either male or female");
            }
            else if (MessageFilter.containsInappropriateContent(txt_myskill + txt_goalskill + txt_username + txt_name)) {
                toast("Can't use inappropriate words");
            } else {
                try {
                    int ageValue = Integer.parseInt(txt_age);
                    if (ageValue < 0 || ageValue > 150) {
                        toast("Age must be a valid number");
                    } else {
                        termsDialog = new AlertDialog.Builder(this)
                                .setTitle("Terms & Conditions")
                                .setMessage("By continuing, you agree to our Terms & Conditions")
                                .setCancelable(false)
                                .setPositiveButton("I agree", (dialog, which) -> {
                                    registerUser(txt_email, txt_password, txt_name, txt_username, txt_myskill, txt_goalskill, txt_age, txt_gender);
                                })
                                .setNegativeButton("Exit", (dialog, which) -> forceAppExit())
                                .setNeutralButton("Read T&C", (dialog, which) -> {
                                    String url = "https://sites.google.com/view/connectra-usage-terms/home";
                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                    startActivity(browserIntent);
                                })
                                .create();

                        termsDialog.show();

                    }
                } catch (NumberFormatException e) {
                    toast("Please enter a valid age");
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
            map.put("profileApproved", false);
            map.put("cerfApproved", false);
            map.put("banned", false);

            databaseRef.child(userId).setValue(map).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    uploadCertificate(userId);
                } else {
                    pd.dismiss();
                    toast("Registration Failed");
                }
            });
        }).addOnFailureListener(e -> {
            pd.dismiss();
            toast(e.getMessage());
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

            toast("Certificate selected and saved locally.");
        } catch (Exception e) {
            e.printStackTrace();
            toast("Failed to save certificate locally.");
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
                    toast("Failed to get certificate URL");
                });
            } else {
                pd.dismiss();
                toast("Certificate upload failed");
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
            toast("Registration Successful!");
            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } else {
            toast("Error completing registration.");
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
        ProgressDialog initDialog = new ProgressDialog(this);
        initDialog.setMessage("Initializing...");
        initDialog.setCancelable(false);
        initDialog.show();

        new Handler().post(new Runnable() {
            private int attempts = 0;

            @Override
            public void run() {
                try {
                    // First ensure we clean up any existing instances
                    MyApplication.cleanupFirebaseInstances();
                    // Then initialize a fresh instance
                    FirebaseApp secondaryApp = MyApplication.initializeSecondaryApp();
                    storage = FirebaseStorage.getInstance(secondaryApp);
                    initDialog.dismiss();
                } catch (Exception e) {
                    attempts++;
                    if (attempts < 3) {
                        // Retry after 1 second
                        new Handler().postDelayed(this, 1000);
                    } else {
                        initDialog.dismiss();
                        new AlertDialog.Builder(RegisterActivity.this)
                                .setTitle("Initialization Error")
                                .setMessage("Failed to initialize storage. Please restart the app.")
                                .setPositiveButton("OK", (dialog, which) -> finish())
                                .setCancelable(false)
                                .show();
                    }
                }
            }
        });
    }

    private void forceAppExit() {
        this.finishAffinity();

        handler.postDelayed(() -> {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
            }, 100);
    }

    private void toast(String msg){
        Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

}
