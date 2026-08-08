package com.nachiket.connectra;

import android.app.AlertDialog;
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

import com.nachiket.connectra.utility.SystemBarsUtil;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.text.HtmlCompat;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.nachiket.connectra.utility.ConnectraApi;
import com.nachiket.connectra.utility.ImageUtil;
import com.nachiket.connectra.utility.MessageFilter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;

public class ExtraDetailsActivity extends AppCompatActivity {
    EditText name, username, myskill, goalskill, age, gender;
    Button register, cerfbtn;
    ImageView cerf;
    AlertDialog verificationDialog;

    FirebaseAuth auth;
    DatabaseReference databaseRef;
    ProgressDialog pd;

    Uri certificateUri;
    File localCertificateFile;
    ActivityResultLauncher<Intent> certificatePickerLauncher;
    String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extra_details);

        SystemBarsUtil.apply(this, getResources().getColor(R.color.white, getTheme()));

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
            else if (MessageFilter.containsInappropriateContent(txt_myskill + txt_goalskill + txt_username + txt_name)) {
                Toast.makeText(ExtraDetailsActivity.this,"Can't use inappropriate words", Toast.LENGTH_SHORT).show();
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
        map.put("aiVerified", false);
        map.put("banned", false);

        databaseRef.child(userId).setValue(map).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                uploadCertificate(userId, myskill);
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

    private void uploadCertificate(String userId, String claimedSkill) {
        if (localCertificateFile == null) {
            pd.dismiss();
            // Registration successful even without certificate
            proceedToMainActivity();
            return;
        }

        pd.setMessage("Checking certificate with AI ...");

        // Compress + encode off the main thread, then run the AI checks
        new Thread(() -> {
            final String dataUri;
            try {
                dataUri = ImageUtil.uriToDataUri(this, Uri.fromFile(localCertificateFile));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    pd.dismiss();
                    Toast.makeText(ExtraDetailsActivity.this, "Could not process certificate",
                            Toast.LENGTH_SHORT).show();
                    proceedToMainActivity();
                });
                return;
            }

            runOnUiThread(() -> ConnectraApi.moderateImage(dataUri, new ConnectraApi.ModerationCallback() {
                @Override
                public void onResult(boolean nsfw, String reason) {
                    if (isFinishing() || isDestroyed()) return;
                    if (nsfw) {
                        pd.dismiss();
                        // Account is created, only the certificate is rejected
                        new AlertDialog.Builder(ExtraDetailsActivity.this)
                                .setTitle("Certificate Rejected")
                                .setMessage("Our AI safety check flagged this image"
                                        + (reason != null && !reason.isEmpty() ? " (" + reason + ")" : "")
                                        + ". You can upload a different certificate later from Edit Profile.")
                                .setCancelable(false)
                                .setPositiveButton("OK", (dialog, which) -> {
                                    dialog.dismiss();
                                    proceedToMainActivity();
                                })
                                .show();
                        return;
                    }
                    verifyAndSaveCertificate(userId, claimedSkill, dataUri);
                }

                @Override
                public void onError(String message) {
                    if (isFinishing() || isDestroyed()) return;
                    // Moderation unavailable — fall back to manual admin review
                    saveCertificate(userId, dataUri, false, false, "");
                }
            }));
        }).start();
    }

    private void verifyAndSaveCertificate(String userId, String claimedSkill, String dataUri) {
        pd.setMessage("Verifying certificate with AI ...");

        ConnectraApi.verifyCertificate(dataUri, claimedSkill, new ConnectraApi.CertificateCallback() {
            @Override
            public void onResult(boolean isCertificate, boolean matchesSkill, String detectedSkill,
                                 String title, String issuer, double confidence) {
                if (isFinishing() || isDestroyed()) return;
                saveCertificate(userId, dataUri, true, isCertificate && matchesSkill, detectedSkill);
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) return;
                // Verification unavailable — keep the certificate but without the AI badge
                saveCertificate(userId, dataUri, true, false, "");
            }
        });
    }

    private void saveCertificate(String userId, String dataUri, boolean aiApproved,
                                 boolean aiVerified, String detectedSkill) {
        HashMap<String, Object> updates = new HashMap<>();
        updates.put("certificateUrl", dataUri);
        updates.put("cerfApproved", aiApproved);
        updates.put("aiVerified", aiVerified);

        databaseRef.child(userId).updateChildren(updates).addOnCompleteListener(task1 -> {
            pd.dismiss();
            if (task1.isSuccessful()) {
                String title;
                CharSequence message;
                if (aiVerified) {
                    title = "AI-Verified ✓";
                    message = "Your certificate passed the AI checks"
                            + (detectedSkill != null && !detectedSkill.isEmpty()
                            ? " and matches your skill \"" + detectedSkill + "\"" : "")
                            + ". It is now visible on your profile with the AI-Verified badge!";
                } else if (aiApproved) {
                    title = "Certificate Uploaded";
                    message = HtmlCompat.fromHtml(
                            "Your certificate passed the safety check and is visible to everyone, but the <b>AI could not verify it against your claimed skill.</b>",
                            HtmlCompat.FROM_HTML_MODE_LEGACY);
                } else {
                    title = "Image sent for Verification";
                    message = "Our AI check is temporarily unavailable, so your certificate has been sent to the admin for NSFW verification. It will appear to everyone once verified.";
                }
                verificationDialog = new AlertDialog.Builder(this)
                        .setTitle(title)
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton("OK", (dialog, which) -> {
                            dialog.dismiss();
                            proceedToMainActivity();
                        })
                        .create();
                verificationDialog.show();
            } else {
                Toast.makeText(ExtraDetailsActivity.this,
                        "Error saving certificate.", Toast.LENGTH_SHORT).show();
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