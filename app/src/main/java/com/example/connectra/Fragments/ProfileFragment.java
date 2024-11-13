package com.example.connectra.Fragments;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.connectra.LoginActivity;
import com.example.connectra.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


public class ProfileFragment extends Fragment {

    FirebaseStorage secondaryStorage = FirebaseStorage.getInstance(FirebaseApp.getInstance("secondary"));
    Button logout;
    ImageView add;
    Uri imageUri;
    static final int IMAGE_REQUEST =2;
    ActivityResultLauncher<Intent> imagePickerLauncher;




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
//        return inflater.inflate(R.layout.fragment_profile, container, false);
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        logout = view.findViewById(R.id.logout);
        add=view.findViewById(R.id.imageView_profile_dp);

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Toast.makeText(getActivity(), "You have been signed out.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
                getActivity().finish();
            }
        });

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImage();
            }
        });
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

        if (imageUri != null){
            StorageReference fireRef = secondaryStorage.getReference().child("connectra").child(System.currentTimeMillis() + "." + getFileExtension(imageUri));

            fireRef.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    fireRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String url = uri.toString();
                            Log.d("DownloadUrl", url);
                            pd.dismiss();
                            Toast.makeText(getActivity(), "Image Upload Successful !", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = requireContext().getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return  mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }
}