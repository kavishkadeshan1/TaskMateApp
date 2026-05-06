package com.example.taskmateapp;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private MaterialButton btnDeveloperInfo, btnEditProfile, btnSignOut;

    private TextView textProfileName, textUsername, textEmail;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String uid;
    private String profileName = "";
    private String username = "";
    private String email = "";
    private String phone = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            goToLogin();
            return;
        }

        uid = auth.getCurrentUser().getUid();

        btnBack = findViewById(R.id.btnBack);
        btnDeveloperInfo = findViewById(R.id.btnDeveloperInfo);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnSignOut = findViewById(R.id.btnSignOut);

        textProfileName = findViewById(R.id.textProfileName);
        textUsername = findViewById(R.id.textUsername);
        textEmail = findViewById(R.id.textEmail);

        textProfileName.setText("Loading...");
        textUsername.setText("");
        textEmail.setText("");

        btnBack.setOnClickListener(v -> finish());

        btnDeveloperInfo.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, DeveloperInfoActivity.class));
        });

        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        btnSignOut.setOnClickListener(v -> showSignOutDialog());

        loadProfile();
    }

    private void loadProfile() {
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        profileName = document.getString("name");
                        username = document.getString("username");
                        email = document.getString("email");
                        phone = document.getString("phone");

                        if (profileName == null) profileName = "";
                        if (username == null) username = "";
                        if (email == null) email = "";
                        if (phone == null) phone = "";

                        updateProfileUI();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Load failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void updateProfileUI() {
        textProfileName.setText(profileName);
        textUsername.setText(username);
        textEmail.setText(email);
    }

    private void showEditProfileDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_edit_profile);

        TextView btnClose = dialog.findViewById(R.id.btnClose);
        TextView btnSave = dialog.findViewById(R.id.btnSave);

        TextInputEditText etEditName = dialog.findViewById(R.id.etEditName);
        TextInputEditText etEditEmail = dialog.findViewById(R.id.etEditEmail);
        TextInputEditText etEditPhone = dialog.findViewById(R.id.etEditPhone);

        etEditName.setText(profileName);
        etEditEmail.setText(email);
        etEditPhone.setText(phone);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newName = etEditName.getText() != null ? etEditName.getText().toString().trim() : "";
            String newEmail = etEditEmail.getText() != null ? etEditEmail.getText().toString().trim() : "";
            String newPhone = etEditPhone.getText() != null ? etEditPhone.getText().toString().trim() : "";

            if (newName.isEmpty()) {
                etEditName.setError("Enter name");
                return;
            }

            if (newEmail.isEmpty()) {
                etEditEmail.setError("Enter email");
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("name", newName);
            updates.put("username", newName);
            updates.put("email", newEmail);
            updates.put("phone", newPhone);

            db.collection("users").document(uid)
                    .update(updates)
                    .addOnSuccessListener(unused -> {
                        profileName = newName;
                        username = newName;
                        email = newEmail;
                        phone = newPhone;

                        updateProfileUI();
                        Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.92),
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void showSignOutDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_sign_out);

        MaterialButton btnCancelSignOut = dialog.findViewById(R.id.btnCancelSignOut);
        MaterialButton btnConfirmSignOut = dialog.findViewById(R.id.btnConfirmSignOut);

        btnCancelSignOut.setOnClickListener(v -> dialog.dismiss());

        btnConfirmSignOut.setOnClickListener(v -> {
            auth.signOut();
            dialog.dismiss();
            goToLogin();
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.90),
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void goToLogin() {
        Intent intent = new Intent(ProfileActivity.this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}