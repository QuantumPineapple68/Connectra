package com.example.connectra;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import android.widget.ImageView;
import android.widget.Toast;
import com.example.connectra.R;


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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    EditText email, password, name, username, myskill, goalskill, age, gender, confirmPass;
    Button register;
    TextView login;

    FirebaseAuth auth;
    DatabaseReference databaseRef;
    ProgressDialog pd;
    ImageView togglePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register2);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        email = findViewById(R.id.txtEmailAddress);
        password = findViewById(R.id.txtPassword);
        register = findViewById(R.id.btnRegNow);
        name = findViewById(R.id.fulltxtname);
        username = findViewById(R.id.fulltxtusername);
        myskill = findViewById(R.id.mytxtskill);
        goalskill = findViewById(R.id.goaltxtskill);
        age = findViewById(R.id.mytxtage);
        gender = findViewById(R.id.mytxtgender);
        confirmPass = findViewById(R.id.comfirmtxtPassword);
        togglePassword=findViewById(R.id.togglePassword);
        login = findViewById(R.id.txtLoginNow);


        auth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("Users");

        pd = new ProgressDialog(this);

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String txt_email = email.getText().toString();
                String txt_password = password.getText().toString();
                String txt_name = name.getText().toString();
                String txt_username = username.getText().toString();
                String txt_myskill = myskill.getText().toString();
                String txt_goalskill = goalskill.getText().toString();
                String txt_age = age.getText().toString();
                String txt_gender = gender.getText().toString();
                String txt_confirmPass = confirmPass.getText().toString();

                if (TextUtils.isEmpty(txt_email) || TextUtils.isEmpty(txt_password) || TextUtils.isEmpty(txt_name) || TextUtils.isEmpty(txt_username)) {
                    Toast.makeText(RegisterActivity.this, "Fields can't be Empty", Toast.LENGTH_SHORT).show();
                } else if (txt_password.length() < 6) {
                    Toast.makeText(RegisterActivity.this, "Password must be at least 6 Digits", Toast.LENGTH_SHORT).show();
                } else if (!txt_password.equals(txt_confirmPass)) {
                    Toast.makeText(RegisterActivity.this, "Password doesn't match", Toast.LENGTH_SHORT).show();
                } else if (txt_myskill.length()>30 || txt_goalskill.length()>30) {
                    Toast.makeText(RegisterActivity.this, "Describe skill sets in brief (30 letters)", Toast.LENGTH_SHORT).show();
                } else if (!txt_email.endsWith(".com")) {
                    Toast.makeText(RegisterActivity.this, "Enter Valid e-mail", Toast.LENGTH_SHORT).show();
                } else {
                    registerUser(txt_email, txt_password, txt_name, txt_username, txt_myskill, txt_goalskill, txt_age, txt_gender);
                }
            }
        });
        togglePassword.setOnClickListener(v -> togglePasswordVisibility(password, togglePassword));

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this , LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                finish();
            }
        });
    }

    private void registerUser(String Email, String Password, String name, String username, String myskill, String goalskill, String age, String gender) {
        pd.setMessage("Please Wait ...");
        pd.show();

        auth.createUserWithEmailAndPassword(Email, Password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                String userId = auth.getCurrentUser().getUid();

                HashMap<String, Object> map = new HashMap<>();
                map.put("name", name);
                map.put("username", username);
                map.put("email", Email);
                map.put("id", userId);
                map.put("myskill", myskill);
                map.put("goalskill", goalskill);
                map.put("age", age);
                map.put("gender", gender);

                databaseRef.child(userId).setValue(map).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        pd.dismiss();
                        Toast.makeText(RegisterActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    } else {
                        pd.dismiss();
                        Toast.makeText(RegisterActivity.this, "Registration Failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(e -> {
            pd.dismiss();
            Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
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
        // Set cursor to end of text after toggling visibility
        passwordEditText.setSelection(passwordEditText.getText().length());
    }
}