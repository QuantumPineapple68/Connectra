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
import com.example.connectra.R;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    TextView register;
    Button login;
    EditText email;
    EditText password;
    TextView forgotPass;
    ImageView togglePassword;

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        register = findViewById(R.id.txtSignUpNow);
        login=findViewById(R.id.btnLoginNow);
        email=findViewById(R.id.txtEmailAddress);
        password=findViewById(R.id.txtPassword);
        forgotPass=findViewById(R.id.txtForgotPassword);
        togglePassword=findViewById(R.id.togglePassword);

        auth=FirebaseAuth.getInstance();

        if (!isInternetAvailable()) {
            showNoInternetDialog();
        }

        forgotPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, ForgotPassword.class));
            }
        });

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this , RegisterActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String txt_email=email.getText().toString();
                String txt_password=password.getText().toString();
                loginUser(txt_email,txt_password);
            }
        });

        togglePassword.setOnClickListener(v -> togglePasswordVisibility(password, togglePassword));

    }

    @Override
    protected void onStart() {
        super.onStart();

        if (FirebaseAuth.getInstance().getCurrentUser() != null){
            startActivity(new Intent(LoginActivity.this,MainActivity.class));
            finish();
        }
    }

    private void loginUser(String txtEmail, String txtPassword) {

        if (TextUtils.isEmpty(txtEmail) || TextUtils.isEmpty(txtPassword)){
            Toast.makeText(LoginActivity.this, "E-mail or Password can't be Empty", Toast.LENGTH_SHORT).show();
        }
        else if (txtPassword.length() < 6){
            Toast.makeText(LoginActivity.this, "Password must be atleast 6 Digits", Toast.LENGTH_SHORT).show();
        }
        else{
            auth.signInWithEmailAndPassword(txtEmail,txtPassword)
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                            finish();
                        }
                    })      .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(LoginActivity.this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
                        }
                    });
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
        // Set cursor to end of text after toggling visibility
        passwordEditText.setSelection(passwordEditText.getText().length());
    }

    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkCapabilities capabilities =
                    connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            if (capabilities != null) {
                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            }
        }
        return false;
    }

    // Show a popup dialog when there is no internet
    private void showNoInternetDialog() {
        new AlertDialog.Builder(this)
                .setTitle("No Internet Connection")
                .setMessage("Please check your internet connection and try again.")
                .setCancelable(false) // User can't dismiss the dialog by tapping outside
                .setPositiveButton("Retry", (dialog, which) -> {
                    // Retry logic: Check for internet again
                    if (!isInternetAvailable()) {
                        showNoInternetDialog(); // Show the dialog again if still no internet
                    } else {
                        dialog.dismiss(); // Dismiss if internet is available
                    }
                })
                .setNegativeButton("Exit", (dialog, which) -> {
                    finish(); // Exit the app
                })
                .show();
    }
}