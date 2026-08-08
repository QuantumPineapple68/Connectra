package com.nachiket.connectra.Fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.nachiket.connectra.utility.SystemBarsUtil;
import com.nachiket.connectra.ChangeSkillActivity;
import com.nachiket.connectra.Authentication.LoginActivity;
import com.nachiket.connectra.ReportActivity;
import com.nachiket.connectra.model.MyApplication;
import com.nachiket.connectra.R;
import com.nachiket.connectra.utility.ConnectraApi;
import com.nachiket.connectra.utility.ImageUtil;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

public class ProfileFragment extends Fragment {

    private TextView fullNameTextView, emailTextView, genderTextView, bioTextView;
    private ImageView profileImageView;
    private TextView welcome;
    private Button changeSkill, childReport, logoutButton;
    private TextView mySkillTextView, goalSkillTextView, credit;
    private AlertDialog verificationDialog;
    private ImageView displayRating, removeProfile;
    private TextView numbRev;
    private String userId;

    // Skill DNA preview card (tap to share)
    private View shareDnaCard;
    private ImageView dnaPreviewImage;
    private ShimmerFrameLayout dnaPreviewShimmer;
    private TextView dnaShareLabel;
    private byte[] dnaCardBytes;
    private boolean dnaProfileLoaded, dnaRatingLoaded, dnaPreviewRequested;

    // Raw values kept for the Skill DNA card
    private String dnaName = "", dnaUsername = "", dnaMySkill = "", dnaGoalSkill = "";
    private boolean dnaAiVerified = false;
    private float dnaRating = 0f;
    private long dnaReviews = 0;

    private FirebaseAuth auth;
    private DatabaseReference userRef;
    private Uri imageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cropImageLauncher;

    private ValueEventListener userValueEventListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        SystemBarsUtil.apply(requireActivity(),
                getResources().getColor(R.color.white, requireContext().getTheme()));

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
        displayRating = view.findViewById(R.id.display_rating);
        numbRev = view.findViewById(R.id.numb_revs);
        removeProfile = view.findViewById(R.id.removeProfile);
        shareDnaCard = view.findViewById(R.id.share_dna_card);
        dnaPreviewImage = view.findViewById(R.id.dna_preview_image);
        dnaPreviewShimmer = view.findViewById(R.id.dna_preview_shimmer);
        dnaShareLabel = view.findViewById(R.id.dna_share_label);

        // Firebase initialization
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);
            setupUserValueEventListener();
            fetchAndDisplayRating();
        } else {
            toast("User is not authenticated");
        }


        // Logout functionality
        logoutButton.setOnClickListener(v -> logout());

        bioTextView.setOnClickListener(v -> navigateToChangeSkill());

        // Change Skill functionality
        changeSkill.setOnClickListener(v -> navigateToChangeSkill());

        removeProfile.setOnClickListener(v -> remove_Profile());

        shareDnaCard.setOnClickListener(v -> shareSkillDna());

        childReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ReportActivity.class);
                startActivity(intent);
            }
        });

        // Initialize Image Picker launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        startCrop(selectedImageUri);
                    }
                }
        );

        // Initialize UCrop result launcher
        cropImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        final Uri resultUri = UCrop.getOutput(result.getData());
                        if (resultUri != null) {
                            imageUri = resultUri;
                            uploadImage();
                        } else {
                            toast("Failed to crop image");
                        }
                    } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                        final Throwable cropError = UCrop.getError(result.getData());
                        toast("Image cropping failed: " + (cropError != null ? cropError.getMessage() : "Unknown error"));
                    }
                }
        );

        // Image upload functionality
        profileImageView.setOnClickListener(v -> openImage());

        credit.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/QuantumPineapple68"));
            startActivity(browserIntent);
        });

        return view;
    }

    private void remove_Profile() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Remove Profile Picture")
                .setMessage("Are you sure you want to remove your profile picture?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    userRef.child("profileImage").removeValue();
                    userRef.child("profileApproved").setValue(false);
                    profileImageView.setImageResource(R.drawable.prof);
                    removeProfile.setVisibility(View.GONE);
                    snackbar("Profile picture removed successfully");
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void startCrop(Uri sourceUri) {
        if (sourceUri == null || getContext() == null) return;

        // Create destination URI for the cropped image
        String destinationFileName = UUID.randomUUID().toString() + ".jpg";
        Uri destinationUri = Uri.fromFile(new File(requireContext().getCacheDir(), destinationFileName));

        // Configure UCrop options
        UCrop.Options options = new UCrop.Options();
        options.setCircleDimmedLayer(true); // Enable circular crop area
        options.setShowCropFrame(false); // Hide crop frame since we're using circle
        options.setShowCropGrid(false); // Hide crop grid since we're using circle
        options.setStatusBarColor(requireContext().getResources().getColor(R.color.white, requireContext().getTheme()));
        options.setToolbarColor(requireContext().getResources().getColor(R.color.white, requireContext().getTheme()));
        options.setToolbarTitle("Crop Profile Image");
        options.setCompressionQuality(85); // Adjust quality as needed

        // Start UCrop activity
        UCrop uCrop = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1) // Square aspect ratio
                .withOptions(options);

        cropImageLauncher.launch(uCrop.getIntent(requireContext()));
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

                    // Keep raw values around for the Skill DNA card
                    dnaName = fullName != null ? fullName : "";
                    dnaUsername = userName != null ? userName : "";
                    dnaMySkill = mySkill != null ? mySkill : "";
                    dnaGoalSkill = goalSkill != null ? goalSkill : "";
                    dnaAiVerified = dataSnapshot.child("aiVerified").getValue(Boolean.class) != null
                            ? dataSnapshot.child("aiVerified").getValue(Boolean.class)
                            : false;

                    dnaProfileLoaded = true;
                    maybeLoadDnaPreview();

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
                                removeProfile.setVisibility(View.VISIBLE);
                                Glide.with(requireContext())
                                        .load(profileImage)
                                        .placeholder(R.drawable.prof)
                                        .error(R.drawable.prof)
                                        .into(profileImageView);
                            } else {
                                profileImageView.setImageResource(R.drawable.prof);
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

        ProgressDialog pd = new ProgressDialog(getActivity());
        pd.setMessage("Checking image with AI ...");
        pd.setCancelable(false);
        pd.show();

        // Compress + encode off the main thread, then let the AI moderate it
        new Thread(() -> {
            final String dataUri;
            try {
                dataUri = ImageUtil.uriToDataUri(requireContext(), imageUri);
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        pd.dismiss();
                        toast("Could not process image: " + e.getMessage());
                    });
                }
                return;
            }

            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> ConnectraApi.moderateImage(dataUri, new ConnectraApi.ModerationCallback() {
                @Override
                public void onResult(boolean nsfw, String reason) {
                    if (!isAdded()) return;
                    if (nsfw) {
                        pd.dismiss();
                        new AlertDialog.Builder(getContext())
                                .setTitle("Image Rejected")
                                .setMessage("Our AI safety check flagged this picture"
                                        + (reason != null && !reason.isEmpty() ? " (" + reason + ")" : "")
                                        + ". Please choose a different image.")
                                .setCancelable(true)
                                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                                .show();
                        return;
                    }
                    saveProfileImage(pd, dataUri, true);
                }

                @Override
                public void onError(String message) {
                    if (!isAdded()) return;
                    // Moderation unavailable — fall back to manual admin review
                    Log.e("ProfileFragment", "Moderation failed: " + message);
                    saveProfileImage(pd, dataUri, false);
                }
            }));
        }).start();
    }

    private void saveProfileImage(ProgressDialog pd, String dataUri, boolean aiApproved) {
        userRef.child("profileImage").setValue(dataUri)
                .addOnSuccessListener(aVoid -> {
                    userRef.child("profileApproved").setValue(aiApproved);
                    pd.dismiss();
                    if (!isAdded()) return;
                    if (aiApproved) {
                        removeProfile.setVisibility(View.VISIBLE);
                        Glide.with(requireContext())
                                .load(dataUri)
                                .placeholder(R.drawable.prof)
                                .error(R.drawable.prof)
                                .into(profileImageView);
                        snackbar("Image Upload Successful! AI-approved ✓");
                    } else {
                        verificationDialog = new AlertDialog.Builder(getContext())
                                .setTitle("Image sent for Verification")
                                .setMessage("Our AI check is temporarily unavailable, so your Profile picture has been sent to the admin for NSFW verification. It will appear to everyone once verified.")
                                .setCancelable(true)
                                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                                .create();
                        verificationDialog.show();
                    }
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    toast("Failed to update profile: " + e.getMessage());
                });
    }

    /** Generates the card preview once the profile and rating data have both arrived. */
    private void maybeLoadDnaPreview() {
        if (!dnaProfileLoaded || !dnaRatingLoaded || dnaPreviewRequested) return;
        dnaPreviewRequested = true;
        loadDnaPreview();
    }

    private void loadDnaPreview() {
        dnaPreviewShimmer.setVisibility(View.VISIBLE);
        dnaPreviewShimmer.startShimmer();
        dnaShareLabel.setText("Generating your Skill DNA ...");

        ConnectraApi.fetchSkillDna(dnaName, dnaUsername, dnaMySkill, dnaGoalSkill,
                dnaRating, dnaReviews, dnaAiVerified, new ConnectraApi.BytesCallback() {
                    @Override
                    public void onResult(byte[] bytes) {
                        if (!isAdded()) return;
                        dnaCardBytes = bytes;
                        showDnaPreview(bytes);
                    }

                    @Override
                    public void onError(String message) {
                        Log.e("ProfileFragment", "DNA preview failed: " + message);
                        if (!isAdded()) return;
                        dnaPreviewShimmer.stopShimmer();
                        dnaPreviewShimmer.setVisibility(View.GONE);
                        dnaShareLabel.setText("Preview unavailable — tap to share your Skill DNA ✨");
                    }
                });
    }

    private void showDnaPreview(byte[] bytes) {
        new Thread(() -> {
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                dnaPreviewShimmer.stopShimmer();
                dnaPreviewShimmer.setVisibility(View.GONE);
                if (bitmap != null) dnaPreviewImage.setImageBitmap(bitmap);
                dnaShareLabel.setText("Tap to share your Skill DNA ✨");
            });
        }).start();
    }

    private void shareSkillDna() {
        // The preview already holds the rendered card — share it right away
        if (dnaCardBytes != null) {
            shareCardBytes(dnaCardBytes, null);
            return;
        }

        ProgressDialog pd = new ProgressDialog(getActivity());
        pd.setMessage("Generating your Skill DNA card ...");
        pd.setCancelable(false);
        pd.show();

        ConnectraApi.fetchSkillDna(dnaName, dnaUsername, dnaMySkill, dnaGoalSkill,
                dnaRating, dnaReviews, dnaAiVerified, new ConnectraApi.BytesCallback() {
                    @Override
                    public void onResult(byte[] bytes) {
                        if (!isAdded()) {
                            pd.dismiss();
                            return;
                        }
                        dnaCardBytes = bytes;
                        showDnaPreview(bytes);
                        shareCardBytes(bytes, pd);
                    }

                    @Override
                    public void onError(String message) {
                        pd.dismiss();
                        toast("Card generation failed: " + message);
                    }
                });
    }

    private void shareCardBytes(byte[] bytes, @Nullable ProgressDialog pd) {
        new Thread(() -> {
            try {
                File shareDir = new File(requireContext().getCacheDir(), "share");
                if (!shareDir.exists()) shareDir.mkdirs();
                File cardFile = new File(shareDir, "skill_dna.png");
                FileOutputStream out = new FileOutputStream(cardFile);
                out.write(bytes);
                out.close();

                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (pd != null) pd.dismiss();
                    Uri contentUri = FileProvider.getUriForFile(requireContext(),
                            "com.nachiket.connectra.fileprovider", cardFile);
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("image/png");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    // ClipData is what the share sheet uses to render the image preview
                    shareIntent.setClipData(ClipData.newRawUri("Skill DNA", contentUri));
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, "Share your Skill DNA"));
                });
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (pd != null) pd.dismiss();
                        toast("Could not share card: " + e.getMessage());
                    });
                }
            }
        }).start();
    }

    private void fetchAndDisplayRating() {
        DatabaseReference revRef = userRef.child("rev");
        DatabaseReference ratingsRef = userRef.child("ratings");

        ratingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return; // Check if fragment is still attached

                long totalReviews = snapshot.getChildrenCount();
                numbRev.setText("(" + totalReviews + ")");
                dnaReviews = totalReviews;

                if (totalReviews == 0) {
                    displayRating.setImageResource(R.drawable.r0);
                    dnaRatingLoaded = true;
                    maybeLoadDnaPreview();
                    return;
                }

                revRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;

                        float totalRev = snapshot.exists() ? snapshot.getValue(Float.class) : 0f;
                        float average = totalRev / totalReviews;
                        dnaRating = average;

                        int imageResId = getRatingImageResource(average);
                        displayRating.setImageResource(imageResId);

                        dnaRatingLoaded = true;
                        maybeLoadDnaPreview();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (isAdded()) {
                            toast("Error fetching rating: " + error.getMessage());
                            dnaRatingLoaded = true;
                            maybeLoadDnaPreview();
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    toast("Error: " + error.getMessage());
                    dnaRatingLoaded = true;
                    maybeLoadDnaPreview();
                }
            }
        });
    }

    private int getRatingImageResource(float average) {
        if (average <= 0) return R.drawable.r0;
        else if (average <= 0.5) return R.drawable.r0_5;
        else if (average <= 1) return R.drawable.r1;
        else if (average <= 1.5) return R.drawable.r1_5;
        else if (average <= 2) return R.drawable.r2;
        else if (average <= 2.5) return R.drawable.r2_5;
        else if (average <= 3) return R.drawable.r3;
        else if (average <= 3.5) return R.drawable.r3_5;
        else if (average <= 4) return R.drawable.r4;
        else if (average <= 4.5) return R.drawable.r4_5;
        else return R.drawable.r5;
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