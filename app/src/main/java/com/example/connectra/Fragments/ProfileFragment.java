package com.example.connectra.Fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import com.bumptech.glide.Glide;
import com.example.connectra.ChangeSkillActivity;
import com.example.connectra.LoginActivity;
import com.example.connectra.R;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuth.AuthStateListener;
import com.google.firebase.auth.FirebaseUser;
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
    private Button logoutButton;
    private TextView welcome;
    private Button changeSkill;
    private TextView mySkillTextView, goalSkillTextView, credit;

    private FirebaseAuth auth;
    private DatabaseReference userRef;
    private FirebaseStorage storage;
    private Uri imageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private ValueEventListener userValueEventListener;

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
        welcome = view.findViewById(R.id.show_welcome);
        profileImageView = view.findViewById(R.id.imageView_profile_dp);
        logoutButton = view.findViewById(R.id.logout);
        changeSkill = view.findViewById(R.id.skillChange);
        credit = view.findViewById(R.id.credits);

        // Firebase initialization
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance(FirebaseApp.getInstance("secondary"));

        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
            setupUserValueEventListener();
        } else {
            Toast.makeText(getContext(), "User is not authenticated", Toast.LENGTH_SHORT).show();
        }

        // Logout functionality
        logoutButton.setOnClickListener(v -> logout());

        // Change Skill functionality
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

        credit.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/QuantumPineapple68"));
            startActivity(browserIntent);
        });

        return view;
    }

    private void setupUserValueEventListener() {
        userValueEventListener = new ValueEventListener() {
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

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (fullName != null) welcome.setText("Welcome, " + fullName);
                            if (userName != null) fullNameTextView.setText(userName);
                            if (email != null) emailTextView.setText(email);
                            if (gender != null) genderTextView.setText(gender);
                            if (mySkill != null) mySkillTextView.setText("I know " + mySkill);
                            if (goalSkill != null) goalSkillTextView.setText("I want to learn " + goalSkill);
                            if (bio != null) bioTextView.setText(bio);

                            if (profileImage != null && !profileImage.isEmpty()) {
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
                    Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("DatabaseError", databaseError.getMessage());
                Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
            }
        };

        userRef.addValueEventListener(userValueEventListener);
    }

    private void logout() {
        Activity activity = getActivity();
        if (activity == null) return;

        // Only need to clean up database listener now
        if (userRef != null && userValueEventListener != null) {
            userRef.removeEventListener(userValueEventListener);
        }

        auth.signOut();

        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        activity.finish();
    }

    private void openImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        imagePickerLauncher.launch(intent);
    }

    private void uploadImage() {
        if (imageUri == null) return;

        ProgressDialog pd = new ProgressDialog(getActivity());
        pd.setMessage("Uploading...");
        pd.show();

        StorageReference fireRef = storage.getReference().child("connectra")
                .child(System.currentTimeMillis() + "." + getFileExtension(imageUri));

        fireRef.putFile(imageUri).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                fireRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String url = uri.toString();
                    userRef.child("profileImage").setValue(url).addOnCompleteListener(task1 -> {
                        pd.dismiss();
                        if (task1.isSuccessful()) {
                            Toast.makeText(getActivity(), "Image Upload Successful!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), "Error updating profile image URL.", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            } else {
                pd.dismiss();
                Toast.makeText(getActivity(), "Image upload failed", Toast.LENGTH_SHORT).show();
            }
        });
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
}
