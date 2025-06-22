package com.example.spendly.activity;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spendly.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EditPasswordActivity extends AppCompatActivity {
    private ImageView btnBack;
    private EditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private ImageView btnToggleCurrentPassword, btnToggleNewPassword, btnToggleConfirmPassword;
    private Button btnSavePassword;
    private boolean isCurrentPasswordVisible = false;
    private boolean isNewPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    
    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_password);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        etCurrentPassword = findViewById(R.id.et_current_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnToggleCurrentPassword = findViewById(R.id.btn_toggle_current_password);
        btnToggleNewPassword = findViewById(R.id.btn_toggle_new_password);
        btnToggleConfirmPassword = findViewById(R.id.btn_toggle_confirm_password);
        btnSavePassword = findViewById(R.id.btn_save_password);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnToggleCurrentPassword.setOnClickListener(v -> togglePasswordVisibility(
                etCurrentPassword, btnToggleCurrentPassword, isCurrentPasswordVisible));

        btnToggleNewPassword.setOnClickListener(v -> togglePasswordVisibility(
                etNewPassword, btnToggleNewPassword, isNewPasswordVisible));

        btnToggleConfirmPassword.setOnClickListener(v -> togglePasswordVisibility(
                etConfirmPassword, btnToggleConfirmPassword, isConfirmPasswordVisible));

        btnSavePassword.setOnClickListener(v -> saveNewPassword());
    }

    private void togglePasswordVisibility(EditText editText, ImageView toggleButton, boolean isVisible) {
        if (isVisible) {
            // Hide password
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleButton.setImageResource(R.drawable.ic_visibility_off);
        } else {
            // Show password
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggleButton.setImageResource(R.drawable.ic_visibility_on);
        }

        // Move cursor to end
        editText.setSelection(editText.getText().length());

        // Update visibility state
        if (editText == etCurrentPassword) {
            isCurrentPasswordVisible = !isCurrentPasswordVisible;
        } else if (editText == etNewPassword) {
            isNewPasswordVisible = !isNewPasswordVisible;
        } else if (editText == etConfirmPassword) {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
        }
    }

    private void saveNewPassword() {
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (!validatePasswords(currentPassword, newPassword, confirmPassword)) {
            return;
        }

        // Show loading state
        btnSavePassword.setEnabled(false);
        btnSavePassword.setText("Updating...");

        // Get user email for re-authentication
        String email = currentUser.getEmail();
        if (email == null) {
            Toast.makeText(this, "User email not found", Toast.LENGTH_SHORT).show();
            resetSaveButton();
            return;
        }

        // Create credential with current password
        AuthCredential credential = EmailAuthProvider.getCredential(email, currentPassword);

        // Re-authenticate user
        currentUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // Update password
                    currentUser.updatePassword(newPassword)
                            .addOnSuccessListener(updateVoid -> {
                                Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to update password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                resetSaveButton();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show();
                    etCurrentPassword.setError("Incorrect password");
                    etCurrentPassword.requestFocus();
                    resetSaveButton();
                });
    }

    private void resetSaveButton() {
        btnSavePassword.setEnabled(true);
        btnSavePassword.setText("Save Password");
    }

    private boolean validatePasswords(String currentPassword, String newPassword, String confirmPassword) {
        // Validate current password
        if (TextUtils.isEmpty(currentPassword)) {
            etCurrentPassword.setError("Current password is required");
            etCurrentPassword.requestFocus();
            return false;
        }

        // Validate new password
        if (TextUtils.isEmpty(newPassword)) {
            etNewPassword.setError("New password is required");
            etNewPassword.requestFocus();
            return false;
        }

        if (newPassword.length() < 8) {
            etNewPassword.setError("Password must be at least 8 characters");
            etNewPassword.requestFocus();
            return false;
        }

        if (!isValidPassword(newPassword)) {
            etNewPassword.setError("Password must contain both letters and numbers");
            etNewPassword.requestFocus();
            return false;
        }

        // Validate confirm password
        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("Please confirm your new password");
            etConfirmPassword.requestFocus();
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return false;
        }

        if (currentPassword.equals(newPassword)) {
            etNewPassword.setError("New password must be different from current password");
            etNewPassword.requestFocus();
            return false;
        }

        return true;
    }

    private boolean isValidPassword(String password) {
        boolean hasLetter = false;
        boolean hasNumber = false;

        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasNumber = true;
            }

            if (hasLetter && hasNumber) {
                return true;
            }
        }

        return false;
    }
}