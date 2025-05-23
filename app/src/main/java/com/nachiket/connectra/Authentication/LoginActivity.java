package com.nachiket.connectra.Authentication;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;

import com.google.android.material.color.MaterialColors;
import com.nachiket.connectra.ExtraDetailsActivity;
import com.nachiket.connectra.MainActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.nachiket.connectra.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    TextView register, credits;
    Button login;
    EditText email;
    EditText password;
    TextView forgotPass;
    ImageView togglePassword;
    ProgressDialog pd;
    private AlertDialog DeletionDialog;

    FirebaseAuth auth;
    private LinearLayout googleSignInBtn;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 123;
    private DatabaseReference usersRef;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean terms, banned, suspended;
    private ProgressBar loginProgress;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "ConnectraPrefs";
    private static final String TERMS_ACCEPTED_KEY = "TermsAccepted";

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

        Window window = getWindow();
        int statusBarColor = getResources().getColor(R.color.login_bar, getTheme());
        window.setStatusBarColor(statusBarColor);
        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(window, window.getDecorView());
        boolean isLightBackground = MaterialColors.isColorLight(statusBarColor);
        windowInsetsController.setAppearanceLightStatusBars(isLightBackground);

        banned = getIntent().getBooleanExtra("banned", false);
        suspended = getIntent().getBooleanExtra("suspended", false);

        if (suspended){
            DeletionDialog = new AlertDialog.Builder(this)
                    .setTitle("Account Suspended")
                    .setMessage("Your account has been Suspended by admin for violating terms of use")
                    .setCancelable(false)
                    .setNegativeButton("Exit", (dialog, which) -> forceAppExit())
                    .create();
            DeletionDialog.show();
        }
        else if (banned){
            DeletionDialog = new AlertDialog.Builder(this)
                    .setTitle("Account Banned Permanently")
                    .setMessage("Your account has been Banned by admin for violating terms of use")
                    .setCancelable(false)
                    .setNegativeButton("Exit", (dialog, which) -> forceAppExit())
                    .create();
            DeletionDialog.show();
        }


        register = findViewById(R.id.txtSignUpNow);
        login=findViewById(R.id.btnLoginNow);
        email=findViewById(R.id.txtEmailAddress);
        password=findViewById(R.id.txtPassword);
        forgotPass=findViewById(R.id.txtForgotPassword);
        togglePassword=findViewById(R.id.togglePassword);
        credits=findViewById(R.id.toLinkedIn);
        loginProgress = findViewById(R.id.loginProgress);
        pd = new ProgressDialog(this);

        auth=FirebaseAuth.getInstance();

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        terms = sharedPreferences.getBoolean(TERMS_ACCEPTED_KEY, false);

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
                String txt_email=email.getText().toString().trim();
                String txt_password=password.getText().toString().trim();
                loginUser(txt_email,txt_password);
            }
        });

        credits.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/nachiket-jadhav-pune/"));
                startActivity(browserIntent);
            }
        });


        togglePassword.setOnClickListener(v -> togglePasswordVisibility(password, togglePassword));
        googleSignInBtn = findViewById(R.id.loginBtn);
        usersRef = FirebaseDatabase.getInstance().getReference("Users");
        initializeGoogleSignIn();

        googleSignInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (terms){
                    signIn();
                }
                else {
                    AlertDialog termsDialog = new AlertDialog.Builder(LoginActivity.this)
                            .setTitle("Terms & Conditions")
                            .setMessage("By continuing, you agree to our Terms & Conditions")
                            .setCancelable(false)
                            .setPositiveButton("I agree", (dialog, which) -> {
                                terms = true;
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putBoolean(TERMS_ACCEPTED_KEY, true);
                                editor.apply();
                                signIn();
                            })
                            .setNegativeButton("Exit", (dialog, which) -> forceAppExit())
                            .setNeutralButton("Read T&C", (dialog, which) -> {
                                String url = "https://sites.google.com/view/connectra-usage-terms/home";
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                startActivity(browserIntent);
                            })
                            .create();

                    termsDialog.show();
                }
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            runOnUiThread(() -> {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }
    }

    private void loginUser(String txtEmail, String txtPassword) {

        if (TextUtils.isEmpty(txtEmail) || TextUtils.isEmpty(txtPassword)){
            toast("E-mail or Password can't be Empty");
        }
        else if (txtPassword.length() < 6){
            toast("Password must be atleast 6 Digits");
        }
        else{
            loginProgress.setVisibility(View.VISIBLE);
            login.setText("");
            login.setEnabled(false);
            auth.signInWithEmailAndPassword(txtEmail,txtPassword)
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            toast("Login Successful");
                            if (vibrator != null && vibrator.hasVibrator()) {
                                vibrator.vibrate(VibrationEffect.createOneShot(20, 110));
                            }
                            startActivity(new Intent(LoginActivity.this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                            finish();
                        }
                    })      .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            toast("Invalid Credentials");
                            loginProgress.setVisibility(View.GONE);
                            login.setText("Login");
                            login.setEnabled(true);
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


    private void initializeGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void signIn() {
        pd.setMessage("Signing In...");
        pd.show();
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                toast("Google sign in failed: " + e.getMessage());
            }
        }
    }
    private void firebaseAuthWithGoogle(String idToken) {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = auth.getCurrentUser().getUid();
                        checkUserExistence(userId);
                    } else {
                        toast("Authentication failed.");
                    }
                });
    }

    private void checkUserExistence(String userId) {
        usersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    pd.dismiss();
                    // User exists, proceed to MainActivity
                    startActivity(new Intent(LoginActivity.this, MainActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    finish();
                } else {
                    pd.dismiss();
                    // New user, proceed to ExtraDetailsActivity
                    Intent intent = new Intent(LoginActivity.this, ExtraDetailsActivity.class);
                    intent.putExtra("email", auth.getCurrentUser().getEmail());
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                toast("Database error: " + error.getMessage());
            }
        });
    }

    private void forceAppExit() {
        this.finishAffinity();

        handler.postDelayed(() -> {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }, 100);
    }

    private void toast(String msg){
        Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
    }
}