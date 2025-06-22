package com.example.spendly.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.spendly.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileSettingsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Button btnEditProfile;
    private Button btnLogout;
    private LinearLayout layoutChangePin, layoutChangePassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    // TextViews to display user info
    private TextView tvNickname;
    private TextView tvEmail;
    private TextView tvPhone;
    private ImageView imgProfile;

    // Loading indicator
    private View progressOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        initViews();
        setupClickListeners();
        loadUserProfile();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnLogout = findViewById(R.id.btn_logout);
        layoutChangePin = findViewById(R.id.layout_change_pin);
        layoutChangePassword = findViewById(R.id.layout_change_password);

        // Initialize TextViews
        tvNickname = findViewById(R.id.tv_nickname);
        tvEmail = findViewById(R.id.tv_email);
        tvPhone = findViewById(R.id.tv_phone);
        imgProfile = findViewById(R.id.img_profile);

        // Initialize loading indicator
        progressOverlay = findViewById(R.id.progress_overlay);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            // Finish current activity to go back to previous screen
            finish();
        });

        btnEditProfile.setOnClickListener(v -> {
            // Open edit profile activity
            Intent intent = new Intent(ProfileSettingsActivity.this, EditProfileActivity.class);
            startActivityForResult(intent, 1001);
        });

        layoutChangePin.setOnClickListener(v -> {
            // Open change pin activity
            Intent intent = new Intent(ProfileSettingsActivity.this, EditPinCodeActivity.class);
            startActivity(intent);
        });

        layoutChangePassword.setOnClickListener(v -> {
            // Open change password activity
            Intent intent = new Intent(ProfileSettingsActivity.this, EditPasswordActivity.class);
            startActivity(intent);
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

    private void loadUserProfile() {
        // Get current user
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Show loading indicator
            progressOverlay.setVisibility(View.VISIBLE);

            // Get user document from Firestore
            mFirestore.collection("users").document(user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    // Hide loading indicator
                    progressOverlay.setVisibility(View.GONE);

                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        // Check if document exists
                        if (document.exists()) {
                            // Update UI with user data

                            // Set email (we'll use this as nickname since there's no nickname field)
                            String email = document.getString("email");
                            if (email != null) {
                                tvNickname.setText(email.substring(0, email.indexOf('@')));
                                tvEmail.setText(email);
                            }

                            // Set phone number using correct field name "phoneNumber"
                            String phoneNumber = document.getString("phoneNumber");
                            if (phoneNumber != null) {
                                tvPhone.setText(phoneNumber);
                            }

                            // Load profile image using Glide or Picasso
                            String profileImageUrl = document.getString("profileImage");
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                // Load image using your preferred image loading library
                            }
                        } else {
                            Toast.makeText(ProfileSettingsActivity.this, "User profile not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(ProfileSettingsActivity.this, "Failed to load user profile", Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // Profile was updated, reload user profile
            loadUserProfile();
            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
        }
    }
}