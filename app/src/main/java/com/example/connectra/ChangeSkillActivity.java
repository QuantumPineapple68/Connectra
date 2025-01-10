package com.example.connectra;

import android.content.Intent;
import android.os.Bundle;
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
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.connectra.Fragments.ProfileFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ChangeSkillActivity extends AppCompatActivity {

    EditText new_myskill, new_goalskill, new_bio;
    Button save;

    FirebaseFirestore mRootRef = FirebaseFirestore.getInstance();
    FirebaseAuth auth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_skill);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        new_myskill = findViewById(R.id.new_myskill);
        new_goalskill = findViewById(R.id.new_goalskill);
        new_bio = findViewById(R.id.new_bio);
        save = findViewById(R.id.save);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String txt_myskill = new_myskill.getText().toString();
                String txt_goalskill = new_goalskill.getText().toString();
                String txt_bio = new_bio.getText().toString();

                boolean hasUpdates = false;

                if (!txt_myskill.isEmpty()) {
                    mRootRef.collection("Users")
                            .document(auth.getCurrentUser().getUid())
                            .update("myskill", txt_myskill)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(ChangeSkillActivity.this, "Your Skill has been Updated", Toast.LENGTH_SHORT).show();
                                }
                            });
                    hasUpdates = true;
                }

                if (!txt_goalskill.isEmpty()) {
                    mRootRef.collection("Users")
                            .document(auth.getCurrentUser().getUid())
                            .update("goalskill", txt_goalskill)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(ChangeSkillActivity.this, "Your Interest has been Updated", Toast.LENGTH_SHORT).show();
                                }
                            });
                    hasUpdates = true;
                }

                if (!txt_bio.isEmpty()) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("bio", txt_bio);

                    mRootRef.collection("Users")
                            .document(auth.getCurrentUser().getUid())
                            .set(data, SetOptions.merge())
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(ChangeSkillActivity.this, "Bio Updated", Toast.LENGTH_SHORT).show();
                                }
                            });
                    hasUpdates = true;
                }

                if (!hasUpdates) {
                    Toast.makeText(ChangeSkillActivity.this, "No fields updated", Toast.LENGTH_SHORT).show();
                }

                Intent intent = new Intent(ChangeSkillActivity.this, MainActivity.class);
                intent.putExtra("refreshProfile", true);
                startActivity(intent);
                finish();
            }
        });


    }
}