package com.example.spendly.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spendly.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPhone, tilGender, tilDob, tilPassword, tilConfirmPassword, tilCurrentBalance;
    private TextInputEditText etEmail, etPhone, etDob, etPassword, etConfirmPassword, etCurrentBalance;
    private AutoCompleteTextView actGender;
    private MaterialButton btnSignUp;
    private TextView tvLoginLink;

    private Calendar calendar;
    private SimpleDateFormat dateFormatter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Hide the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize calendar and date formatter
        calendar = Calendar.getInstance();
        dateFormatter = new SimpleDateFormat("dd MMMM yyyy", Locale.US);

        // Initialize views
        initializeViews();

        // Setup gender dropdown
        setupGenderDropdown();

        // Setup date picker
        setupDatePicker();

        // Setup listeners for validation
        setupValidationListeners();

        // Setup buttons
        setupButtons();
    }

    private void initializeViews() {
        tilEmail = findViewById(R.id.til_email);
        etEmail = findViewById(R.id.et_email);

        tilPhone = findViewById(R.id.til_phone);
        etPhone = findViewById(R.id.et_phone);

        tilGender = findViewById(R.id.til_gender);
        actGender = findViewById(R.id.act_gender);

        tilDob = findViewById(R.id.til_dob);
        etDob = findViewById(R.id.et_dob);

        tilPassword = findViewById(R.id.til_password);
        etPassword = findViewById(R.id.et_password);

        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);

        tilCurrentBalance = findViewById(R.id.til_current_balance);
        etCurrentBalance = findViewById(R.id.et_current_balance);

        btnSignUp = findViewById(R.id.btn_sign_up);
        tvLoginLink = findViewById(R.id.tv_login_link);
    }

    private void setupGenderDropdown() {
        String[] genderOptions = new String[]{"Male", "Female", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                genderOptions);
        actGender.setAdapter(adapter);
    }

    private void setupDatePicker() {
        etDob.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    SignUpActivity.this,
                    (view, year, month, day) -> {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, day);
                        updateDateInView();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }

    private void updateDateInView() {
        etDob.setText(dateFormatter.format(calendar.getTime()));
    }

    private void setupValidationListeners() {
        // Email validation
        etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateEmail(s.toString());
            }
        });

        // Password validation
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validatePassword(s.toString());
            }
        });

        // Confirm password validation
        etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateConfirmPassword(etPassword.getText().toString(), s.toString());
            }
        });
    }

    private boolean validateEmail(String email) {
        boolean isValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
        tilEmail.setEndIconVisible(isValid);
        if (!isValid && !email.isEmpty()) {
            tilEmail.setError("Please enter a valid email address");
        } else {
            tilEmail.setError(null);
        }
        return isValid;
    }

    private boolean validatePassword(String password) {
        // Password must be at least 8 characters with both letters and numbers
        boolean isValid = password.length() >= 8 &&
                Pattern.compile("[a-zA-Z]").matcher(password).find() &&
                Pattern.compile("[0-9]").matcher(password).find();

        if (!isValid && !password.isEmpty()) {
            tilPassword.setError("Password must be at least 8 characters with both letters and numbers");
        } else {
            tilPassword.setError(null);
        }
        return isValid;
    }

    private boolean validateConfirmPassword(String password, String confirmPassword) {
        boolean isValid = password.equals(confirmPassword);
        if (!isValid && !confirmPassword.isEmpty()) {
            tilConfirmPassword.setError("Passwords do not match");
        } else {
            tilConfirmPassword.setError(null);
        }
        return isValid;
    }

    private boolean validateAllFields() {
        boolean isEmailValid = validateEmail(etEmail.getText().toString());
        boolean isPasswordValid = validatePassword(etPassword.getText().toString());
        boolean isConfirmPasswordValid = validateConfirmPassword(
                etPassword.getText().toString(),
                etConfirmPassword.getText().toString()
        );

        boolean isPhoneValid = !etPhone.getText().toString().isEmpty();
        if (!isPhoneValid) {
            tilPhone.setError("Phone number is required");
        } else {
            tilPhone.setError(null);
        }

        boolean isGenderValid = !actGender.getText().toString().isEmpty();
        if (!isGenderValid) {
            tilGender.setError("Please select a gender");
        } else {
            tilGender.setError(null);
        }

        boolean isDobValid = !etDob.getText().toString().isEmpty();
        if (!isDobValid) {
            tilDob.setError("Date of birth is required");
        } else {
            tilDob.setError(null);
        }

        boolean isBalanceValid = !etCurrentBalance.getText().toString().isEmpty();
        if (!isBalanceValid) {
            tilCurrentBalance.setError("Current balance is required");
        } else {
            tilCurrentBalance.setError(null);
        }

        return isEmailValid && isPasswordValid && isConfirmPasswordValid &&
               isPhoneValid && isGenderValid && isDobValid && isBalanceValid;
    }

    private void setupButtons() {
        // Sign up button click listener
        btnSignUp.setOnClickListener(v -> {
            if (validateAllFields()) {
                // TODO: Register user with backend service
                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
                // Go to sign in screen
                Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Please correct the errors in the form", Toast.LENGTH_SHORT).show();
            }
        });

        // Login link click listener
        tvLoginLink.setOnClickListener(v -> {
            // Go back to sign in screen
            finish();
        });
    }
}
