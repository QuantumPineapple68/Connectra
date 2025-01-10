package com.example.connectra.Fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;

import com.bumptech.glide.Glide;
import com.example.connectra.ChangeSkillActivity;
import com.example.connectra.R;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.connectra.LoginActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Objects;

public class ProfileFragment extends Fragment {

    private TextView fullNameTextView, emailTextView, genderTextView, bioTextView;
    private ImageView profileImageView;
    private Button logoutButton;
    private TextView welcome;
    private Button changeSkill;
    private TextView mySkillTextView, goalSkillTextView;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseStorage secondaryStorage;
    private Uri imageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize Views
        fullNameTextView = view.findViewById(R.id.full_name);
        emailTextView = view.findViewById(R.id.show_email);
        genderTextView = view.findViewById(R.id.show_gender);
        mySkillTextView = view.findViewById(R.id.myskill_change);
        goalSkillTextView = view.findViewById(R.id.goalskill_change);
        bioTextView = view.findViewById(R.id.show_bio);
        welcome=view.findViewById(R.id.show_welcome);
        profileImageView = view.findViewById(R.id.imageView_profile_dp);
        logoutButton = view.findViewById(R.id.logout);
        changeSkill = view.findViewById(R.id.skillChange);

        // Firebase initialization
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        secondaryStorage = FirebaseStorage.getInstance(FirebaseApp.getInstance("secondary"));

        // Fetch and display user data
        fetchUserData();

        // Logout functionality
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(getActivity(), "You have been signed out.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });

        changeSkill.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ChangeSkillActivity.class);
            startActivity(intent);
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
        return view;
    }

    private void fetchUserData() {
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            firestore.collection("Users").document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot snapshot = task.getResult();
                            if (snapshot != null && snapshot.exists()) {
                                String userName = snapshot.getString("username");
                                String email = snapshot.getString("email");
                                String gender = snapshot.getString("gender");
                                String fullName = snapshot.getString("name");
                                String mySkill = snapshot.getString("myskill");
                                String goalSkill = snapshot.getString("goalskill");
                                String bio = snapshot.getString("bio");
                                String profileImage = snapshot.getString("profileImage"); // Fetch profile image URL

                                // Update UI
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        if (fullName != null) welcome.setText("Welcome, " + fullName);
                                        if (userName != null) fullNameTextView.setText(userName);
                                        if (email != null) emailTextView.setText(email);
                                        if (gender != null) genderTextView.setText(gender);
                                        if (mySkill != null) mySkillTextView.setText("I know " + mySkill);
                                        if (goalSkill != null) goalSkillTextView.setText("I want to learn " + goalSkill);
                                        if (bio != null) bioTextView.setText(bio);

                                        // Load profile image or display placeholder
                                        if (profileImage != null && !profileImage.isEmpty()) {
                                            Glide.with(requireContext())
                                                    .load(profileImage)
                                                    .placeholder(R.drawable.no_profile_pic)
                                                    .error(R.drawable.no_profile_pic)
                                                    .into(profileImageView);
                                        } else {
                                            // Show placeholder if no image is uploaded
                                            profileImageView.setImageResource(R.drawable.no_profile_pic);
                                        }
                                    });
                                }
                            } else {
                                Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "Failed to fetch data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }



    private void openImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        imagePickerLauncher.launch(intent);
    }

    private void uploadImage() {
        ProgressDialog pd = new ProgressDialog(getActivity());
        pd.setMessage("Uploading...");
        pd.show();

        if (imageUri != null) {
            StorageReference fireRef = secondaryStorage.getReference().child("connectra").child(System.currentTimeMillis() + "." + getFileExtension(imageUri));

            fireRef.putFile(imageUri).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    fireRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String url = uri.toString();
                        Log.d("DownloadUrl", url);
                        pd.dismiss();
                        Toast.makeText(getActivity(), "Image Upload Successful!", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    pd.dismiss();
                    Toast.makeText(getActivity(), "Image upload failed", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = requireContext().getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }
}
