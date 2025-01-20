package com.example.connectra;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    TextView register, forgotPass;
    Button loginBtn;
    EditText email, password;
    ImageView togglePassword;
    private int RC_SIGN_IN = 11;
    private GoogleSignInClient mGoogleSignInClient;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Google Sign-In Options
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // UI Components
        register = findViewById(R.id.txtSignUpNow);
        forgotPass = findViewById(R.id.txtForgotPassword);
        loginBtn = findViewById(R.id.btnLoginNow);
        email = findViewById(R.id.txtEmailAddress);
        password = findViewById(R.id.txtPassword);
        togglePassword = findViewById(R.id.togglePassword);

        // Password toggle visibility
        togglePassword.setOnClickListener(v -> togglePasswordVisibility(password, togglePassword));

        // Register new user
        register.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });

        // Forgot password
        forgotPass.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPassword.class));
        });

        // Login with email/password
        loginBtn.setOnClickListener(v -> {
            String txtEmail = email.getText().toString();
            String txtPassword = password.getText().toString();
            loginUser(txtEmail, txtPassword);
        });

        // Google Sign-In
        findViewById(R.id.loginBtn).setOnClickListener(v -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });

        // Check for internet connection
        if (!isInternetAvailable()) {
            showNoInternetDialog();
        }
    }

    private void loginUser(String txtEmail, String txtPassword) {
        if (TextUtils.isEmpty(txtEmail) || TextUtils.isEmpty(txtPassword)) {
            Toast.makeText(LoginActivity.this, "E-mail or Password can't be Empty", Toast.LENGTH_SHORT).show();
        } else if (txtPassword.length() < 6) {
            Toast.makeText(LoginActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
        } else {
            auth.signInWithEmailAndPassword(txtEmail, txtPassword)
                    .addOnSuccessListener(authResult -> {
                        Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(LoginActivity.this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            GoogleSignIn.getSignedInAccountFromIntent(data)
                    .addOnSuccessListener(account -> firebaseAuthWithGoogle(account))
                    .addOnFailureListener(e -> Toast.makeText(LoginActivity.this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show());
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Check if this is a new user
                    boolean isNewUser = authResult.getAdditionalUserInfo() != null && authResult.getAdditionalUserInfo().isNewUser();

                    if (isNewUser) {
                        // Redirect new user to ExtraDetailsActivity
                        Intent intent = new Intent(LoginActivity.this, ExtraDetailsActivity.class);
                        intent.putExtra("email", user.getEmail());
                        intent.putExtra("name", user.getDisplayName());
                        startActivity(intent);
                    } else {
                        // Redirect existing user to MainActivity
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this, "Google Authentication Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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

    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
        }
        return false;
    }

    private void showNoInternetDialog() {
        new AlertDialog.Builder(this)
                .setTitle("No Internet Connection")
                .setMessage("Please check your internet connection and try again.")
                .setCancelable(false)
                .setPositiveButton("Retry", (dialog, which) -> {
                    if (!isInternetAvailable()) {
                        showNoInternetDialog();
                    } else {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .show();
    }
}
