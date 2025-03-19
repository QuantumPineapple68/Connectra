package com.nachiket.connectra;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.nachiket.connectra.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ReportActivity extends AppCompatActivity {

    private TextInputEditText editTargetUsername, editOtherLocation, editOtherType, editDescription;
    private TextInputLayout layoutOtherLocation, layoutOtherType;
    private CheckBox checkProfileImage, checkCertificate, checkName, checkUsername, checkBio, checkOtherLocation;
    private CheckBox checkChildSafety, checkNsfw, checkHarassment, checkOtherType;
    private Button buttonSubmitReport, buttonCancel;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        Window window = getWindow();
        int statusBarColor = getResources().getColor(R.color.inverted_top, getTheme());
        window.setStatusBarColor(statusBarColor);
        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(window, window.getDecorView());
        boolean isLightBackground = MaterialColors.isColorLight(statusBarColor);
        windowInsetsController.setAppearanceLightStatusBars(isLightBackground);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize UI components
        initializeViews();

        // Set up listeners
        setupCheckBoxListeners();
        setupButtonListeners();
    }

    private void initializeViews() {
        editTargetUsername = findViewById(R.id.edit_target_username);
        editOtherLocation = findViewById(R.id.edit_other_location);
        editOtherType = findViewById(R.id.edit_other_type);
        editDescription = findViewById(R.id.edit_description);

        layoutOtherLocation = findViewById(R.id.layout_other_location);
        layoutOtherType = findViewById(R.id.layout_other_type);

        // Content Location Checkboxes
        checkProfileImage = findViewById(R.id.check_profile_image);
        checkCertificate = findViewById(R.id.check_certificate);
        checkName = findViewById(R.id.check_name);
        checkUsername = findViewById(R.id.check_username);
        checkBio = findViewById(R.id.check_bio);
        checkOtherLocation = findViewById(R.id.check_other_location);

        // Content Type Checkboxes
        checkChildSafety = findViewById(R.id.check_child_safety);
        checkNsfw = findViewById(R.id.check_nsfw);
        checkHarassment = findViewById(R.id.check_harassment);
        checkOtherType = findViewById(R.id.check_other_type);

        buttonSubmitReport = findViewById(R.id.button_submit_report);
        buttonCancel = findViewById(R.id.button_cancel);
    }

    private void setupCheckBoxListeners() {
        // Show/hide Other text field for location
        checkOtherLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutOtherLocation.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                editOtherLocation.setText("");
            }
        });

        // Show/hide Other text field for type
        checkOtherType.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutOtherType.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                editOtherType.setText("");
            }
        });
    }

    private void setupButtonListeners() {
        // Submit Button
        buttonSubmitReport.setOnClickListener(v -> validateAndSubmitReport());

        // Cancel Button
        buttonCancel.setOnClickListener(v -> finish());
    }

    private void validateAndSubmitReport() {
        // Validate input fields
        String targetUsername = editTargetUsername.getText().toString().trim();
        String description = editDescription.getText().toString().trim();

        if (targetUsername.isEmpty()) {
            editTargetUsername.setError("Username is required");
            editTargetUsername.requestFocus();
            return;
        }

        List<String> contentLocations = getSelectedContentLocations();
        if (contentLocations.isEmpty()) {
            Toast.makeText(this, "Please select at least one location where you saw the content", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> contentTypes = getSelectedContentTypes();
        if (contentTypes.isEmpty()) {
            Toast.makeText(this, "Please select at least one type of content", Toast.LENGTH_SHORT).show();
            return;
        }

        if (description.isEmpty()) {
            editDescription.setError("Please provide more details");
            editDescription.requestFocus();
            return;
        }

        // All validations passed, create and submit report
        submitReport();
    }

    private List<String> getSelectedContentLocations() {
        List<String> selectedLocations = new ArrayList<>();

        if (checkProfileImage.isChecked()) selectedLocations.add("Profile Image");
        if (checkCertificate.isChecked()) selectedLocations.add("Certificate");
        if (checkName.isChecked()) selectedLocations.add("Name");
        if (checkUsername.isChecked()) selectedLocations.add("Username");
        if (checkBio.isChecked()) selectedLocations.add("Bio");
        if (checkOtherLocation.isChecked()) {
            String otherText = editOtherLocation.getText().toString().trim();
            if (!otherText.isEmpty()) {
                selectedLocations.add("Other: " + otherText);
            } else {
                selectedLocations.add("Other");
            }
        }

        return selectedLocations;
    }

    private List<String> getSelectedContentTypes() {
        List<String> selectedTypes = new ArrayList<>();

        if (checkChildSafety.isChecked()) selectedTypes.add("Child Safety Concern");
        if (checkNsfw.isChecked()) selectedTypes.add("NSFW Content");
        if (checkHarassment.isChecked()) selectedTypes.add("Harassment or Bullying");
        if (checkOtherType.isChecked()) {
            String otherText = editOtherType.getText().toString().trim();
            if (!otherText.isEmpty()) {
                selectedTypes.add("Other: " + otherText);
            } else {
                selectedTypes.add("Other");
            }
        }

        return selectedTypes;
    }

    private void submitReport() {
        // Check if user is signed in
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in to submit a report", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get report data
        Map<String, Object> reportData = createReportData();

        // Create a unique ID for the report
        String reportId = UUID.randomUUID().toString();

        // Save to Firebase
        mDatabase.child("ReportedUsers")
                .child("ReportForms")
                .child(mAuth.getCurrentUser().getUid())
                .child(reportId)
                .setValue(reportData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ReportActivity.this, "Report submitted successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ReportActivity.this, "Failed to submit report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private Map<String, Object> createReportData() {
        Map<String, Object> reportData = new HashMap<>();

        // Reporter info
        reportData.put("reporterId", mAuth.getCurrentUser().getUid());
        reportData.put("reporterEmail", mAuth.getCurrentUser().getEmail());

        // Target info
        reportData.put("targetUsername", editTargetUsername.getText().toString().trim());

        // Content locations - store as a map for efficient Firebase storage
        List<String> contentLocations = getSelectedContentLocations();
        Map<String, Boolean> locationsMap = new HashMap<>();
        for (String location : contentLocations) {
            locationsMap.put(location, true);
        }
        reportData.put("contentLocations", locationsMap);

        // Content types - store as a map for efficient Firebase storage
        List<String> contentTypes = getSelectedContentTypes();
        Map<String, Boolean> typesMap = new HashMap<>();
        for (String type : contentTypes) {
            typesMap.put(type, true);
        }
        reportData.put("contentTypes", typesMap);

        // Description
        reportData.put("description", editDescription.getText().toString().trim());

        // Timestamp
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        reportData.put("timestamp", timestamp);

        // Status
        reportData.put("status", "Pending Review");

        return reportData;
    }
}