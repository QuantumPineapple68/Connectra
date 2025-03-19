package com.nachiket.connectra.Fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.color.MaterialColors;
import com.nachiket.connectra.ChangeSkillActivity;
import com.nachiket.connectra.Authentication.LoginActivity;
import com.nachiket.connectra.ReportActivity;
import com.nachiket.connectra.model.MyApplication;
import com.nachiket.connectra.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileFragment extends Fragment {

    private TextView fullNameTextView, emailTextView, genderTextView, bioTextView;
    private ImageView profileImageView;
    private TextView welcome;
    private Button changeSkill, childReport, logoutButton;
    private TextView mySkillTextView, goalSkillTextView, credit;
    private AlertDialog verificationDialog;

    private FirebaseAuth auth;
    private DatabaseReference userRef;
    private FirebaseStorage storage;
    private Uri imageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private ValueEventListener userValueEventListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        Window window = requireActivity().getWindow();
        int statusBarColor = getResources().getColor(R.color.white, requireContext().getTheme());
        window.setStatusBarColor(statusBarColor);
        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(window, window.getDecorView());
        boolean isLightBackground = MaterialColors.isColorLight(statusBarColor);
        windowInsetsController.setAppearanceLightStatusBars(isLightBackground);

        // Initialize Views
        fullNameTextView = view.findViewById(R.id.full_name);
        emailTextView = view.findViewById(R.id.show_email);
        genderTextView = view.findViewById(R.id.show_gender);
        mySkillTextView = view.findViewById(R.id.myskill_change);
        goalSkillTextView = view.findViewById(R.id.goalskill_change);
        bioTextView = view.findViewById(R.id.show_bio);
        welcome = view.findViewById(R.id.show_welcome);
        profileImageView = view.findViewById(R.id.imageView_profile_dp);
        logoutButton = view.findViewById(R.id.logout);
        changeSkill = view.findViewById(R.id.skillChange);
        credit = view.findViewById(R.id.credits);
        childReport = view.findViewById(R.id.report);

        // Firebase initialization
        auth = FirebaseAuth.getInstance();
        initializeStorage();

        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
            setupUserValueEventListener();
        } else {
            toast("User is not authenticated");
        }

        // Logout functionality
        logoutButton.setOnClickListener(v -> logout());

        bioTextView.setOnClickListener(v -> navigateToChangeSkill());

        // Change Skill functionality
        changeSkill.setOnClickListener(v -> navigateToChangeSkill());

        childReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ReportActivity.class);
                startActivity(intent);
            }
        });
        // Image upload functionality
        profileImageView.setOnClickListener(v -> openImage());

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        uploadImage();
                    }
                }
        );

        credit.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/QuantumPineapple68"));
            startActivity(browserIntent);
        });

        return view;
    }

    private void initializeStorage() {
        try {
            // Clean up existing instances first
            MyApplication.cleanupFirebaseInstances();
            // Initialize a fresh secondary instance
            FirebaseApp secondaryApp = MyApplication.initializeSecondaryApp();
            storage = FirebaseStorage.getInstance(secondaryApp);
        } catch (Exception e) {
            Log.e("ProfileFragment", "Error initializing storage: " + e.getMessage());
            toast("Error initializing storage. Please try again.");
        }
    }

    private void setupUserValueEventListener() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String fullName = dataSnapshot.child("name").getValue(String.class);
                    String userName = dataSnapshot.child("username").getValue(String.class);
                    String email = dataSnapshot.child("email").getValue(String.class);
                    String gender = dataSnapshot.child("gender").getValue(String.class);
                    String mySkill = dataSnapshot.child("myskill").getValue(String.class);
                    String goalSkill = dataSnapshot.child("goalskill").getValue(String.class);
                    String bio = dataSnapshot.child("bio").getValue(String.class);
                    String profileImage = dataSnapshot.child("profileImage").getValue(String.class);
                    boolean profileApproved = dataSnapshot.child("profileApproved").getValue(Boolean.class) != null
                            ? dataSnapshot.child("profileApproved").getValue(Boolean.class)
                            : false;

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (fullName != null) welcome.setText("Welcome, " + fullName);
                            if (userName != null) fullNameTextView.setText(userName);
                            if (email != null) emailTextView.setText(email);
                            if (gender != null) genderTextView.setText(gender);
                            if (mySkill != null) mySkillTextView.setText("I know " + mySkill);
                            if (goalSkill != null) goalSkillTextView.setText("I want to learn " + goalSkill);
                            if (bio != null) bioTextView.setText(bio);

                            if (profileImage != null && !profileImage.isEmpty() && profileApproved) {
                                Glide.with(requireContext())
                                        .load(profileImage)
                                        .placeholder(R.drawable.no_profile_pic)
                                        .error(R.drawable.no_profile_pic)
                                        .into(profileImageView);
                            } else {
                                profileImageView.setImageResource(R.drawable.no_profile_pic);
                            }
                        });
                    }
                } else {
                    toast("User data not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("DatabaseError", databaseError.getMessage());
                toast("Failed to load user data");
            }
        });
    }

    private void logout() {
        Activity activity = getActivity();
        if (activity == null) return;

        // Clean up database listener
        if (userRef != null && userValueEventListener != null) {
            userRef.removeEventListener(userValueEventListener);
        }

        // Sign out from Firebase Auth
        auth.signOut();

        // Clean up the secondary Firebase instance
        MyApplication.cleanupFirebaseInstances();

        // Sign out from Google if it was a Google sign-in
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(requireActivity(),
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.client_id))
                        .requestEmail()
                        .build());
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent intent = new Intent(activity, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            activity.finish();
        });
    }

    private void openImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        imagePickerLauncher.launch(intent);
    }

    private void uploadImage() {
        if (imageUri == null) return;

        if (storage == null) {
            toast("Storage not initialized. Please try again.");
            return;
        }

        ProgressDialog pd = new ProgressDialog(getActivity());
        pd.setMessage("Uploading...");
        pd.show();

        try {
            StorageReference fireRef = storage.getReference().child("connectra")
                    .child(System.currentTimeMillis() + "." + getFileExtension(imageUri));

            fireRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        userRef.child("profileApproved").setValue(false);
                        fireRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String url = uri.toString();
                            userRef.child("profileImage").setValue(url)
                                    .addOnSuccessListener(aVoid -> {
                                        pd.dismiss();
                                        snackbar("Image Upload Successful!");
                                        verificationDialog = new AlertDialog.Builder(getContext())
                                                .setTitle("Image sent for Verification")
                                                .setMessage("Your Profile picture has been sent to the admin for NSFW verification to check for mature content. It will appear to everyone once verified.")
                                                .setCancelable(true)
                                                .setPositiveButton("OK", (dialog, which) -> {
                                                    dialog.dismiss();
                                                })
                                                .create();

                                        verificationDialog.show();
                                    })
                                    .addOnFailureListener(e -> {
                                        pd.dismiss();
                                        toast("Failed to update profile: " + e.getMessage());
                                    });
                        });
                    })
                    .addOnFailureListener(e -> {
                        pd.dismiss();
                        toast("Upload failed: " + e.getMessage());
                        Log.e("ProfileFragment", "Upload failed: " + e.getMessage());
                    });
        } catch (Exception e) {
            pd.dismiss();
            toast("Error: " + e.getMessage());
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = requireContext().getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userRef != null && userValueEventListener != null) {
            userRef.removeEventListener(userValueEventListener);
        }
    }

    private void navigateToChangeSkill(){
        Intent intent = new Intent(getActivity(), ChangeSkillActivity.class);
        startActivity(intent);
    }

    private void snackbar(String msg){
        Snackbar snackbar = Snackbar.make(requireView(), msg, Snackbar.LENGTH_SHORT);
        View snackbarView = snackbar.getView();
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) snackbarView.getLayoutParams();
        params.bottomMargin = 180; // Adjust the value as needed
        snackbarView.setLayoutParams(params);
        snackbar.show();
    }

    private void toast(String msg){
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
