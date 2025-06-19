package com.example.spendly.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.spendly.R;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileSettingsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Button btnEditProfile;
    private Button btnLogout;
    private LinearLayout layoutChangePin, layoutChangePassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnLogout = findViewById(R.id.btn_logout);
        layoutChangePin = findViewById(R.id.layout_change_pin);
        layoutChangePassword = findViewById(R.id.layout_change_password);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            // Finish current activity to go back to previous screen
            finish();
        });

        btnEditProfile.setOnClickListener(v -> {
            // Open edit profile activity
        });

        layoutChangePin.setOnClickListener(v -> {
            // Open change pin activity
        });

        layoutChangePassword.setOnClickListener(v -> {
            // Open change password activity
        });

        btnLogout.setOnClickListener(v -> {
            showLogoutConfirmationDialog();
        });
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes", (dialog, which) -> {
                logoutUser();
            })
            .setNegativeButton("No", (dialog, which) -> {
                dialog.dismiss();
            })
            .show();
    }

    private void logoutUser() {
        // Sign out from Firebase
        mAuth.signOut();

        // Show success message
        Toast.makeText(this, "Logout successful", Toast.LENGTH_SHORT).show();

        // Clear all activities in the stack and go to Sign In page
        Intent intent = new Intent(this, SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}