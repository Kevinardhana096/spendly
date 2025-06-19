package com.example.spendly.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.spendly.R;
import com.example.spendly.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

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

    // Firebase components
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Hide the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

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
                    this,
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
                String password = etPassword.getText() != null ? etPassword.getText().toString() : "";
                validateConfirmPassword(password, s.toString());
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
        String emailText = etEmail.getText() != null ? etEmail.getText().toString() : "";
        String passwordText = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String confirmPasswordText = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";
        String phoneText = etPhone.getText() != null ? etPhone.getText().toString() : "";
        String genderText = actGender.getText() != null ? actGender.getText().toString() : "";
        String dobText = etDob.getText() != null ? etDob.getText().toString() : "";
        String currentBalanceText = etCurrentBalance.getText() != null ? etCurrentBalance.getText().toString() : "";

        boolean isEmailValid = validateEmail(emailText);
        boolean isPasswordValid = validatePassword(passwordText);
        boolean isConfirmPasswordValid = validateConfirmPassword(passwordText, confirmPasswordText);

        boolean isPhoneValid = !TextUtils.isEmpty(phoneText);
        if (!isPhoneValid) {
            tilPhone.setError("Phone number is required");
        } else {
            tilPhone.setError(null);
        }

        boolean isGenderValid = !TextUtils.isEmpty(genderText);
        if (!isGenderValid) {
            tilGender.setError("Please select a gender");
        } else {
            tilGender.setError(null);
        }

        boolean isDobValid = !TextUtils.isEmpty(dobText);
        if (!isDobValid) {
            tilDob.setError("Date of birth is required");
        } else {
            tilDob.setError(null);
        }

        boolean isBalanceValid = !TextUtils.isEmpty(currentBalanceText);
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
                registerUser();
            } else {
                Toast.makeText(getApplicationContext(), "Please correct the errors in the form", Toast.LENGTH_SHORT).show();
            }
        });

        // Login link click listener
        tvLoginLink.setOnClickListener(v -> {
            // Go back to sign in screen
            finish();
        });
    }

    /**
     * Register user with Firebase Authentication and store user data in Firestore
     */
    private void registerUser() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String gender = actGender.getText() != null ? actGender.getText().toString().trim() : "";
        String dob = etDob.getText() != null ? etDob.getText().toString().trim() : "";
        String currentBalanceStr = etCurrentBalance.getText() != null ? etCurrentBalance.getText().toString().trim() : "";

        final double currentBalance;
        try {
            currentBalance = Double.parseDouble(currentBalanceStr.replace("Rp ", "").replace(",", ""));
        } catch (NumberFormatException e) {
            Toast.makeText(getApplicationContext(), "Invalid current balance format", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        btnSignUp.setEnabled(false);
        btnSignUp.setText(R.string.creating_account);

        // Create user with email and password
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Sign up success
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Create user object
                            User user = new User(
                                    firebaseUser.getUid(),
                                    email,
                                    phone,
                                    gender,
                                    dob,
                                    currentBalance
                            );

                            // Save user data to Firestore
                            saveUserToFirestore(user);
                        }
                    } else {
                        // If sign up fails, display a message to the user
                        btnSignUp.setEnabled(true);
                        btnSignUp.setText(R.string.sign_up);

                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(getApplicationContext(), "Email already in use. Please use a different email.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            Toast.makeText(getApplicationContext(), "Registration failed: " + errorMessage,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Save user data to Firestore
     * @param user The user object containing data to save
     */
    private void saveUserToFirestore(User user) {
        mFirestore.collection("users")
                .document(user.getUserId())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    btnSignUp.setEnabled(true);
                    btnSignUp.setText(R.string.sign_up);

                    // Registration complete
                    Toast.makeText(getApplicationContext(), "Registration successful!", Toast.LENGTH_SHORT).show();

                    // Go to sign in screen
                    Intent intent = new Intent(getApplicationContext(), SignInActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSignUp.setEnabled(true);
                    btnSignUp.setText(R.string.sign_up);

                    // Handle the error
                    Toast.makeText(getApplicationContext(), "Failed to save user data: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();

                    // Delete the authentication account since we couldn't save the user data
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        firebaseUser.delete();
                    }
                });
    }
}
