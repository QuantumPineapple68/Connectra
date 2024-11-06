package com.example.connectra;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Objects;

public class  RegisterActivity extends AppCompatActivity {

        EditText email;
        EditText password;
        Button register;
        EditText name;
        EditText username;
        EditText myskill;
        EditText goalskill;
        EditText age;
        EditText gender;

        FirebaseAuth auth;
        FirebaseFirestore mRootRef;

        ProgressDialog pd;

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

        email=findViewById(R.id.editTextTextEmailAddress);
        password=findViewById(R.id.editTextTextPassword);
        register=findViewById(R.id.button2);
        name=findViewById(R.id.name);
        username=findViewById(R.id.username);
        myskill=findViewById(R.id.myskill);
        goalskill=findViewById(R.id.goalskill);
        age=findViewById(R.id.age);
        gender=findViewById(R.id.gender);


        auth = FirebaseAuth.getInstance();
        mRootRef= FirebaseFirestore.getInstance();

        pd=new ProgressDialog(this);


        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String txt_email = email.getText().toString();
                String txt_password = password.getText().toString();
                String txt_name = name.getText().toString();
                String txt_username = username.getText().toString();
                String txt_myskill = myskill.getText().toString();
                String txt_goalskill=goalskill.getText().toString();
                String txt_age=age.getText().toString();
                String txt_gender = gender.getText().toString();

                if (TextUtils.isEmpty(txt_email) || TextUtils.isEmpty(txt_password) || TextUtils.isEmpty(txt_name) || TextUtils.isEmpty(txt_username)){
                    Toast.makeText(RegisterActivity.this, "Fields can't be Empty", Toast.LENGTH_SHORT).show();
                }
                else if (txt_password.length() < 6){
                    Toast.makeText(RegisterActivity.this, "Password must be atleast 6 Digits", Toast.LENGTH_SHORT).show();
                }
                else{
                    registerUser(txt_email ,txt_password, txt_name, txt_username, txt_myskill, txt_goalskill, txt_age, txt_gender);
                }
            }
        });
    }

    private void registerUser(String Email, String Password, String name, String username, String myskill, String goalskill, String age, String gender) {
        pd.setMessage("Please Wait ...");
        pd.show();
        auth.createUserWithEmailAndPassword(Email,Password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {

            @Override
            public void onSuccess(AuthResult authResult) {

                        HashMap<String, Object> map = new HashMap<>();
                        map.put("name", name);
                        map.put("username", username);
                        map.put("email", Email);
                        map.put("id", auth.getCurrentUser().getUid());
                        map.put("myskill", myskill);
                        map.put("goalskill", goalskill);
                        map.put("age", age);
                        map.put("gender", gender);

                        mRootRef.collection("Users").document(auth.getCurrentUser().getUid()).set(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()){
                                    pd.dismiss();
                                    Toast.makeText(RegisterActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(RegisterActivity.this , MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(intent);
                                    finish();
                                }
                            }
                        });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                pd.dismiss();
                Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}