package com.example.spendly.activity;

import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.spendly.R;

public class EditPasswordActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private ImageView btnToggleCurrentPassword, btnToggleNewPassword, btnToggleConfirmPassword;
    private Button btnSavePassword;

    private boolean isCurrentPasswordVisible = false;
    private boolean isNewPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_password);

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

        if (validatePasswords(currentPassword, newPassword, confirmPassword)) {
            // Save new password to database/API
            // For now, just show success message
            Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private boolean validatePasswords(String currentPassword, String newPassword, String confirmPassword) {
        // Validate current password
        if (currentPassword.isEmpty()) {
            etCurrentPassword.setError("Current password is required");
            etCurrentPassword.requestFocus();
            return false;
        }

        // Validate new password
        if (newPassword.isEmpty()) {
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
        if (confirmPassword.isEmpty()) {
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