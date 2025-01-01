package com.example.connectra.Fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
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

    private TextView fullNameTextView, emailTextView, genderTextView;
    private ImageView profileImageView;
    private Button logoutButton;
    private TextView welcome;

    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
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
        welcome=view.findViewById(R.id.show_welcome);
        profileImageView = view.findViewById(R.id.imageView_profile_dp);
        logoutButton = view.findViewById(R.id.logout);

        // Firebase initialization
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
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
        String userEmail = Objects.requireNonNull(auth.getCurrentUser()).getEmail();
        if (userEmail != null) {
            String sanitizedEmail = userEmail.replace(".", ","); // Firebase-friendly key
            databaseReference.child(sanitizedEmail).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String userName = snapshot.child("username").getValue(String.class);
                        String email = snapshot.child("email").getValue(String.class);
                        String gender = snapshot.child("gender").getValue(String.class);
                        String fullName = snapshot.child("name").getValue(String.class);

                        // Update UI
                        requireActivity().runOnUiThread(() -> {
                            if (fullName != null) {
                                welcome.setText("Welcome, " + fullName);
                            }
                            if (userName != null) {
                                fullNameTextView.setText(userName);
                            }
                            if (email != null) {
                                emailTextView.setText(email);
                            }
                            if (gender != null) {
                                genderTextView.setText(gender);
                            }
                        });

                    } else {
                        Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "Failed to fetch data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
