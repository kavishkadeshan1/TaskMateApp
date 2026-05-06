package com.example.taskmateapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Optional: set splash layout (or remove if not needed)
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {

            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                // User already logged in
                startActivity(new Intent(SplashActivity.this, TodoActivity.class));
            } else {
                // Not logged in
                startActivity(new Intent(SplashActivity.this, SignInActivity.class));
            }

            finish();

        }, 1500); // 1.5 sec splash delay
    }
}