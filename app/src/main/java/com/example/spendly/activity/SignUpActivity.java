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
import com.example.spendly.utils.FirestoreTestUtils;
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

    private static final String TAG = "SignUpActivity";
    private static final int REGISTRATION_TIMEOUT_MS = 30000; // 30 seconds timeout

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
    
    // Timeout handler
    private android.os.Handler timeoutHandler;
    private Runnable timeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Hide the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        
        // Configure Firestore settings to handle offline issues
        configureFirestoreSettings();

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
    }    private void setupValidationListeners() {
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

        // Phone number validation
        etPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validatePhone(s.toString());
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

        // Current balance validation
        etCurrentBalance.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateCurrentBalance(s.toString());
            }
        });
    }    private boolean validateEmail(String email) {
        boolean isValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
        tilEmail.setEndIconVisible(isValid);
        if (!isValid && !email.isEmpty()) {
            tilEmail.setError("Please enter a valid email address");
        } else {
            tilEmail.setError(null);
        }
        return isValid;
    }

    private boolean validatePhone(String phone) {
        // Indonesian phone number validation (basic)
        boolean isValid = phone.length() >= 10 && phone.length() <= 15 && 
                         (phone.startsWith("08") || phone.startsWith("+62"));
        
        if (!isValid && !phone.isEmpty()) {
            tilPhone.setError("Please enter a valid Indonesian phone number");
        } else {
            tilPhone.setError(null);
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

    private boolean validateCurrentBalance(String balanceStr) {
        if (balanceStr.isEmpty()) {
            tilCurrentBalance.setError("Current balance is required");
            return false;
        }
        
        try {
            double balance = Double.parseDouble(balanceStr.replace("Rp ", "").replace(",", ""));
            if (balance < 0) {
                tilCurrentBalance.setError("Balance cannot be negative");
                return false;
            }
            tilCurrentBalance.setError(null);
            return true;
        } catch (NumberFormatException e) {
            tilCurrentBalance.setError("Please enter a valid number");
            return false;
        }
    }    private boolean validateAllFields() {
        String emailText = etEmail.getText() != null ? etEmail.getText().toString() : "";
        String passwordText = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String confirmPasswordText = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";
        String phoneText = etPhone.getText() != null ? etPhone.getText().toString() : "";
        String genderText = actGender.getText() != null ? actGender.getText().toString() : "";
        String dobText = etDob.getText() != null ? etDob.getText().toString() : "";
        String currentBalanceText = etCurrentBalance.getText() != null ? etCurrentBalance.getText().toString() : "";

        // Validate each field
        boolean isEmailValid = validateEmail(emailText);
        boolean isPhoneValid = validatePhone(phoneText);
        boolean isPasswordValid = validatePassword(passwordText);
        boolean isConfirmPasswordValid = validateConfirmPassword(passwordText, confirmPasswordText);
        boolean isBalanceValid = validateCurrentBalance(currentBalanceText);

        // Validate gender selection
        boolean isGenderValid = !TextUtils.isEmpty(genderText);
        if (!isGenderValid) {
            tilGender.setError("Please select a gender");
        } else {
            tilGender.setError(null);
        }

        // Validate date of birth
        boolean isDobValid = !TextUtils.isEmpty(dobText);
        if (!isDobValid) {
            tilDob.setError("Date of birth is required");
        } else {
            tilDob.setError(null);
        }

        return isEmailValid && isPhoneValid && isPasswordValid && isConfirmPasswordValid &&
               isGenderValid && isDobValid && isBalanceValid;
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
    }    /**
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
            android.util.Log.e("SignUpActivity", "Invalid balance format: " + currentBalanceStr, e);
            tilCurrentBalance.setError("Invalid balance format");
            Toast.makeText(getApplicationContext(), "Invalid current balance format", Toast.LENGTH_SHORT).show();
            return;
        }        // Show loading state with progress indicator
        btnSignUp.setEnabled(false);
        btnSignUp.setText("Creating Account...");
        
        // Set up timeout handler to prevent getting stuck
        timeoutHandler = new android.os.Handler();
        timeoutRunnable = () -> {
            android.util.Log.e("SignUpActivity", "❌ REGISTRATION TIMEOUT - Process took too long");
            resetButtonState();
            Toast.makeText(getApplicationContext(), "Registration timeout. Please check your connection and try again.", Toast.LENGTH_LONG).show();
        };
        timeoutHandler.postDelayed(timeoutRunnable, REGISTRATION_TIMEOUT_MS);
        
        android.util.Log.d("SignUpActivity", "=== STARTING USER REGISTRATION ===");
        android.util.Log.d("SignUpActivity", "Email: " + email);
        android.util.Log.d("SignUpActivity", "Phone: " + phone);
        android.util.Log.d("SignUpActivity", "Gender: " + gender);
        android.util.Log.d("SignUpActivity", "DOB: " + dob);
        android.util.Log.d("SignUpActivity", "Current Balance: " + currentBalance);
        android.util.Log.d("SignUpActivity", "Timeout set for: " + REGISTRATION_TIMEOUT_MS + "ms");
        
        // Skip Firestore connectivity test to prevent blocking - it might cause the stuck issue
        android.util.Log.d("SignUpActivity", "Proceeding directly to Firebase Auth (skipping connectivity test)");

        // Create user with email and password
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    android.util.Log.d("SignUpActivity", "Firebase Auth task completed. Success: " + task.isSuccessful());
                    
                    if (task.isSuccessful()) {
                        android.util.Log.d("SignUpActivity", "✅ Firebase Authentication successful");
                        
                        // Sign up success
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            android.util.Log.d("SignUpActivity", "Firebase user created with UID: " + firebaseUser.getUid());
                            android.util.Log.d("SignUpActivity", "Firebase user email: " + firebaseUser.getEmail());
                            
                            // Create user object
                            User user = new User(
                                    firebaseUser.getUid(),
                                    email,
                                    phone,
                                    gender,
                                    dob,
                                    currentBalance
                            );

                            // Save user data to Firestore immediately
                            android.util.Log.d("SignUpActivity", "Proceeding to save user data to Firestore...");
                            saveUserToFirestore(user);
                        } else {
                            android.util.Log.e("SignUpActivity", "❌ CRITICAL: Firebase user is null after successful authentication");
                            resetButtonState();
                            Toast.makeText(getApplicationContext(), "Authentication error: User object is null", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        android.util.Log.e("SignUpActivity", "❌ Firebase Authentication failed", task.getException());
                        
                        // If sign up fails, display a message to the user
                        resetButtonState();

                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            tilEmail.setError("Email already in use");
                            Toast.makeText(getApplicationContext(), "Email already in use. Please use a different email.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            android.util.Log.e("SignUpActivity", "Detailed auth error: " + errorMessage);
                            Toast.makeText(getApplicationContext(), "Registration failed: " + errorMessage,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .addOnFailureListener(this, e -> {
                    android.util.Log.e("SignUpActivity", "❌ Firebase Auth onFailure triggered", e);
                    resetButtonState();
                    Toast.makeText(getApplicationContext(), "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }    /**
     * Reset button state to normal and cancel any pending timeouts
     */
    private void resetButtonState() {
        android.util.Log.d("SignUpActivity", "Resetting button state");
        
        // Cancel timeout if it exists
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
            android.util.Log.d("SignUpActivity", "Timeout cancelled");
        }
        
        // Reset button
        btnSignUp.setEnabled(true);
        btnSignUp.setText(R.string.sign_up);
        android.util.Log.d("SignUpActivity", "Button state reset to normal");
    }    /**
     * Save user data to Firestore with comprehensive error handling and improved navigation
     * @param user The user object containing data to save
     */
    private void saveUserToFirestore(User user) {
        android.util.Log.d("SignUpActivity", "=== SAVING USER DATA TO FIRESTORE ===");
        android.util.Log.d("SignUpActivity", "User ID: " + user.getUserId());
        android.util.Log.d("SignUpActivity", "Email: " + user.getEmail());
        android.util.Log.d("SignUpActivity", "Phone: " + user.getPhoneNumber());
        android.util.Log.d("SignUpActivity", "Gender: " + user.getGender());
        android.util.Log.d("SignUpActivity", "DOB: " + user.getDateOfBirth());
        android.util.Log.d("SignUpActivity", "Current Balance: " + user.getCurrentBalance());
        android.util.Log.d("SignUpActivity", "Firestore path: users/" + user.getUserId());
        
        // DEBUG: Print user object details
        android.util.Log.d("SignUpActivity", "🔍 DEBUG - User object validation:");
        android.util.Log.d("SignUpActivity", "- userId null check: " + (user.getUserId() != null));
        android.util.Log.d("SignUpActivity", "- email null check: " + (user.getEmail() != null));
        android.util.Log.d("SignUpActivity", "- phoneNumber null check: " + (user.getPhoneNumber() != null));
        android.util.Log.d("SignUpActivity", "- gender null check: " + (user.getGender() != null));
        android.util.Log.d("SignUpActivity", "- dateOfBirth null check: " + (user.getDateOfBirth() != null));
        
        // Verify Firestore instance
        if (mFirestore == null) {
            android.util.Log.e("SignUpActivity", "❌ CRITICAL: Firestore instance is null");
            resetButtonState();
            Toast.makeText(getApplicationContext(), "Firestore not initialized", Toast.LENGTH_LONG).show();
            return;
        }
          android.util.Log.d("SignUpActivity", "✅ Firestore instance verified, proceeding with save operation");
        
        // Skip connectivity test - proceed directly to save with enhanced error handling
        android.util.Log.d("SignUpActivity", "Skipping connectivity test due to offline issues - proceeding directly to save");
        performActualSave(user);    }
    
    /**
     * Perform the actual save operation with direct approach and enhanced offline handling
     */
    private void performActualSave(User user) {
        android.util.Log.d("SignUpActivity", "🚀 Starting actual Firestore save operation");
        android.util.Log.d("SignUpActivity", "Collection: users");
        android.util.Log.d("SignUpActivity", "Document ID: " + user.getUserId());
        android.util.Log.d("SignUpActivity", "Using SetOptions.merge()");
        
        // First, try to enable network connectivity if offline
        mFirestore.enableNetwork()
                .addOnCompleteListener(networkTask -> {
                    android.util.Log.d("SignUpActivity", "Network enable task completed: " + networkTask.isSuccessful());
                    
                    // Proceed with save operation regardless of network enable result
                    proceedWithFirestoreSave(user);
                });
    }
    
    /**
     * Proceed with the actual Firestore save operation
     */
    private void proceedWithFirestoreSave(User user) {
        android.util.Log.d("SignUpActivity", "📝 Proceeding with Firestore save...");
        
        // Use set with merge option for better performance and conflict handling
        mFirestore.collection("users")
                .document(user.getUserId())
                .set(user, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(this, aVoid -> {
                    android.util.Log.d("SignUpActivity", "✅ SUCCESS: User data saved to Firestore");
                    android.util.Log.d("SignUpActivity", "Document path: users/" + user.getUserId());
                    android.util.Log.d("SignUpActivity", "User can now login and data will be available in HomeFragment");
                    
                    // Verify the save by reading back the data
                    verifyDataSaved(user.getUserId());
                    
                    // Reset button state
                    resetButtonState();

                    // Registration complete - show success message
                    Toast.makeText(getApplicationContext(), "Registration successful! Redirecting to login...", Toast.LENGTH_SHORT).show();

                    // Navigate to SignIn immediately without delay to prevent getting stuck
                    android.util.Log.d("SignUpActivity", "Navigating to SignInActivity...");
                    navigateToSignIn();
                })
                .addOnFailureListener(this, e -> {
                    android.util.Log.e("SignUpActivity", "❌ FAILED to save user data to Firestore", e);
                    android.util.Log.e("SignUpActivity", "Error message: " + e.getMessage());
                    android.util.Log.e("SignUpActivity", "Error class: " + e.getClass().getSimpleName());
                    android.util.Log.e("SignUpActivity", "Error cause: " + (e.getCause() != null ? e.getCause().getMessage() : "null"));
                    
                    // Handle offline-specific errors
                    handleFirestoreError(e, user);
                });
    }
    
    /**
     * Handle different types of Firestore errors, especially offline errors
     */
    private void handleFirestoreError(Exception e, User user) {
        resetButtonState();
        
        String errorMsg = "Failed to save user profile: " + e.getMessage();
        boolean shouldCleanupAuth = true;
        
        if (e.getMessage() != null) {
            String message = e.getMessage().toLowerCase();
            
            if (message.contains("client is offline") || message.contains("failed to get document because the client is offline")) {
                errorMsg = "🌐 OFFLINE ERROR: Cannot connect to Firebase. Please check your internet connection and try again.";
                android.util.Log.e("SignUpActivity", "❌ FIRESTORE CLIENT OFFLINE - Internet connection issue");
                android.util.Log.e("SignUpActivity", "🔧 SOLUTION: Check internet connection, restart app, or try different network");
                shouldCleanupAuth = false; // Don't delete auth user for offline errors
                
                // Try to show helpful message
                Toast.makeText(getApplicationContext(), 
                    "No internet connection. Your account was created but profile data couldn't be saved. Please login and complete your profile.", 
                    Toast.LENGTH_LONG).show();
                
                // Navigate to SignIn anyway since Auth account was created successfully
                android.util.Log.d("SignUpActivity", "Navigating to SignIn despite offline error (Auth account exists)");
                navigateToSignIn();
                return;
                
            } else if (message.contains("permission_denied")) {
                errorMsg = "Permission denied. Please check Firestore security rules.";
                android.util.Log.e("SignUpActivity", "❌ FIRESTORE PERMISSION DENIED - Check security rules");
            } else if (message.contains("unauthenticated")) {
                errorMsg = "Authentication error. Please try again.";
                android.util.Log.e("SignUpActivity", "❌ FIRESTORE UNAUTHENTICATED - Auth token issue");
            } else if (message.contains("unavailable")) {
                errorMsg = "Firestore service unavailable. Please try again later.";
                android.util.Log.e("SignUpActivity", "❌ FIRESTORE UNAVAILABLE - Network or server issue");
                shouldCleanupAuth = false; // Service issue, don't delete auth
            } else if (message.contains("network")) {
                errorMsg = "Network error. Please check your internet connection.";
                android.util.Log.e("SignUpActivity", "❌ FIRESTORE NETWORK ERROR");
                shouldCleanupAuth = false; // Network issue, don't delete auth
            }
        }
        
        Toast.makeText(getApplicationContext(), errorMsg, Toast.LENGTH_LONG).show();        // Clean up authentication account only for non-network related errors
        if (shouldCleanupAuth) {
            cleanupFailedRegistration();
        } else {
            android.util.Log.d("SignUpActivity", "Auth account preserved due to network/service issue");
        }
    }
    
    /**
     * Verify that data was actually saved by reading it back
     */
    private void verifyDataSaved(String userId) {
        android.util.Log.d("SignUpActivity", "🔍 VERIFICATION: Reading back saved data...");
        
        mFirestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(this, documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        android.util.Log.d("SignUpActivity", "✅ VERIFICATION SUCCESS: Document exists in Firestore");
                        android.util.Log.d("SignUpActivity", "Document data: " + documentSnapshot.getData());
                        
                        // Try to convert back to User object
                        try {
                            User savedUser = documentSnapshot.toObject(User.class);
                            if (savedUser != null) {
                                android.util.Log.d("SignUpActivity", "✅ VERIFICATION: User object conversion successful");
                                android.util.Log.d("SignUpActivity", "- Saved Email: " + savedUser.getEmail());
                                android.util.Log.d("SignUpActivity", "- Saved Phone: " + savedUser.getPhoneNumber());
                                android.util.Log.d("SignUpActivity", "- Saved Balance: " + savedUser.getCurrentBalance());
                            } else {
                                android.util.Log.e("SignUpActivity", "❌ VERIFICATION: User object conversion failed");
                            }
                        } catch (Exception e) {
                            android.util.Log.e("SignUpActivity", "❌ VERIFICATION: Error converting document", e);
                        }
                    } else {
                        android.util.Log.e("SignUpActivity", "❌ VERIFICATION FAILED: Document does not exist!");
                        Toast.makeText(getApplicationContext(), "Warning: Data may not have been saved properly", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(this, e -> {
                    android.util.Log.e("SignUpActivity", "❌ VERIFICATION ERROR: Could not read back data", e);
                });
    }

    /**
     * Navigate to SignIn activity with proper flags
     */
    private void navigateToSignIn() {
        try {
            android.util.Log.d("SignUpActivity", "Creating intent for SignInActivity");
            Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            
            android.util.Log.d("SignUpActivity", "Starting SignInActivity");
            startActivity(intent);
            
            android.util.Log.d("SignUpActivity", "Finishing SignUpActivity");
            finish();
            
            android.util.Log.d("SignUpActivity", "✅ Navigation to SignInActivity completed");
        } catch (Exception e) {
            android.util.Log.e("SignUpActivity", "❌ Error navigating to SignInActivity", e);
            Toast.makeText(getApplicationContext(), "Navigation error. Please manually go to login.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Clean up failed registration by deleting the auth user
     */
    private void cleanupFailedRegistration() {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            android.util.Log.d("SignUpActivity", "Cleaning up: Deleting Firebase Auth user due to Firestore save failure");
            firebaseUser.delete().addOnCompleteListener(this, deleteTask -> {
                if (deleteTask.isSuccessful()) {
                    android.util.Log.d("SignUpActivity", "✅ Firebase Auth user deleted successfully");
                    Toast.makeText(getApplicationContext(), "Registration cancelled. Please try again.", Toast.LENGTH_SHORT).show();
                } else {
                    android.util.Log.e("SignUpActivity", "❌ Failed to delete Firebase Auth user", deleteTask.getException());
                    Toast.makeText(getApplicationContext(), "Please contact support - account in inconsistent state", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Clean up timeout handler to prevent memory leaks
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
            android.util.Log.d("SignUpActivity", "Timeout handler cleaned up in onDestroy");
        }
    }
    /**
     * 🔧 DEBUG: Test Firestore manually (for debugging purposes)
     * Call this method to test if Firestore is working
     */
    public void debugTestFirestore() {
        android.util.Log.d("SignUpActivity", "=== MANUAL FIRESTORE DEBUG TEST ===");
        
        if (mAuth == null) {
            android.util.Log.e("SignUpActivity", "❌ DEBUG: mAuth is null");
            Toast.makeText(this, "DEBUG: FirebaseAuth not initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (mFirestore == null) {
            android.util.Log.e("SignUpActivity", "❌ DEBUG: mFirestore is null");
            Toast.makeText(this, "DEBUG: Firestore not initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            android.util.Log.w("SignUpActivity", "⚠️ DEBUG: No current user - this is normal before signup");
            Toast.makeText(this, "DEBUG: No authenticated user (normal before signup)", Toast.LENGTH_SHORT).show();
        } else {
            android.util.Log.d("SignUpActivity", "✅ DEBUG: Current user: " + currentUser.getEmail());
            Toast.makeText(this, "DEBUG: User authenticated: " + currentUser.getEmail(), Toast.LENGTH_SHORT).show();
        }
        
        // Test Firestore write operation
        android.util.Log.d("SignUpActivity", "🔍 DEBUG: Testing Firestore write operation...");
        
        java.util.Map<String, Object> testData = new java.util.HashMap<>();
        testData.put("test_field", "test_value");
        testData.put("timestamp", System.currentTimeMillis());        testData.put("debug_test", true);
        
        mFirestore.collection("debug_test")
                .document("signup_debug_" + System.currentTimeMillis())
                .set(testData)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("SignUpActivity", "✅ DEBUG: Firestore write test successful");
                    Toast.makeText(this, "DEBUG: Firestore write test SUCCESS", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("SignUpActivity", "❌ DEBUG: Firestore write test failed", e);
                    Toast.makeText(this, "DEBUG: Firestore write test FAILED: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /**
     * 🔧 DEBUG: Check current form data
     */
    public void debugCheckFormData() {
        android.util.Log.d("SignUpActivity", "=== FORM DATA DEBUG ===");
        
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String gender = actGender.getText() != null ? actGender.getText().toString().trim() : "";
        String dob = etDob.getText() != null ? etDob.getText().toString().trim() : "";
        String currentBalanceStr = etCurrentBalance.getText() != null ? etCurrentBalance.getText().toString().trim() : "";
        
        android.util.Log.d("SignUpActivity", "Email: '" + email + "'");
        android.util.Log.d("SignUpActivity", "Phone: '" + phone + "'");
        android.util.Log.d("SignUpActivity", "Gender: '" + gender + "'");
        android.util.Log.d("SignUpActivity", "DOB: '" + dob + "'");
        android.util.Log.d("SignUpActivity", "Balance: '" + currentBalanceStr + "'");
        
        Toast.makeText(this, "DEBUG: Check logcat for form data", Toast.LENGTH_SHORT).show();
    }    /**
     * Configure Firestore settings to handle offline connectivity issues
     */
    private void configureFirestoreSettings() {
        try {
            android.util.Log.d("SignUpActivity", "🔧 Configuring Firestore settings for offline handling...");
            
            // Configure Firestore settings BEFORE enabling network
            com.google.firebase.firestore.FirebaseFirestoreSettings settings =
                    new com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                            .setPersistenceEnabled(true)  // Enable offline persistence
                            .setCacheSizeBytes(com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                            .build();
            
            mFirestore.setFirestoreSettings(settings);
            android.util.Log.d("SignUpActivity", "✅ Firestore settings configured with persistence enabled");
            
            // Force enable network after settings configuration
            android.util.Log.d("SignUpActivity", "🌐 Attempting to force enable Firestore network...");
            mFirestore.enableNetwork()
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("SignUpActivity", "✅ Firestore network enabled successfully");
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.w("SignUpActivity", "⚠️ Could not enable Firestore network (may be temporary)", e);
                        // This is not critical - app can still work with persistence
                    });
            
            // Add a delay and retry network enable
            new android.os.Handler().postDelayed(() -> {
                android.util.Log.d("SignUpActivity", "🔄 Retry: Attempting to enable Firestore network again...");
                mFirestore.enableNetwork()
                        .addOnSuccessListener(aVoid -> {
                            android.util.Log.d("SignUpActivity", "✅ Second attempt: Firestore network enabled");
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.w("SignUpActivity", "⚠️ Second attempt failed - will rely on offline handling", e);
                        });
            }, 2000); // 2 second delay
            
        } catch (Exception e) {
            android.util.Log.e("SignUpActivity", "❌ Error configuring Firestore settings", e);
        }
    }    /**
     * 🔧 DEBUG: Test network connectivity and Firebase connection
     * Call this manually for testing - does not interfere with normal signup flow
     */
    public void debugTestConnectivity() {
        android.util.Log.d("SignUpActivity", "=== MANUAL CONNECTIVITY DEBUG TEST ===");
        
        // Test 1: Check internet connectivity
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            
            android.util.Log.d("SignUpActivity", "🌐 Internet Connected: " + isConnected);
            
            if (!isConnected) {
                Toast.makeText(this, "❌ No internet connection detected", Toast.LENGTH_LONG).show();
                return;
            }
        }
        
        // Test 2: Try to enable Firestore network manually
        android.util.Log.d("SignUpActivity", "🔥 Manual test: Attempting Firestore network enable...");
        
        mFirestore.enableNetwork()
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("SignUpActivity", "✅ Manual test: Firestore network enabled successfully");
                    
                    // Test 3: Try simple write operation  
                    testSimpleFirestoreWrite();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("SignUpActivity", "❌ Manual test: Failed to enable Firestore network", e);
                    Toast.makeText(this, "DEBUG: Failed to enable Firestore network: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    
                    // Still try write operation to see what happens
                    testSimpleFirestoreWrite();
                });
    }
    
    /**
     * Test simple Firestore write operation for connectivity
     */
    private void testSimpleFirestoreWrite() {
        android.util.Log.d("SignUpActivity", "📝 Testing simple Firestore write...");
        
        java.util.Map<String, Object> testData = new java.util.HashMap<>();
        testData.put("connectivity_test", true);
        testData.put("timestamp", System.currentTimeMillis());
        testData.put("device_info", android.os.Build.MODEL);
        
        mFirestore.collection("connectivity_test")
                .document("test_" + System.currentTimeMillis())
                .set(testData)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("SignUpActivity", "✅ Simple Firestore write successful - Connection OK");
                    Toast.makeText(this, "✅ Firestore connectivity test PASSED", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("SignUpActivity", "❌ Simple Firestore write failed", e);
                    String errorDetails = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    
                    if (errorDetails.contains("client is offline")) {
                        Toast.makeText(this, "❌ OFFLINE ERROR: Cannot connect to Firebase", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "❌ Firestore error: " + errorDetails, Toast.LENGTH_LONG).show();
                    }                });
    }
    
    /**
     * 🔧 ADVANCED DEBUG: Force Firestore online with multiple strategies
     */
    public void debugForceFirestoreOnline() {
        android.util.Log.d("SignUpActivity", "=== FORCING FIRESTORE ONLINE ===");
        
        Toast.makeText(this, "Attempting to force Firestore online...", Toast.LENGTH_SHORT).show();
        
        // Strategy 1: Disable then re-enable network
        android.util.Log.d("SignUpActivity", "Strategy 1: Disable -> Enable network");
        mFirestore.disableNetwork()
                .addOnCompleteListener(disableTask -> {
                    android.util.Log.d("SignUpActivity", "Network disabled, now re-enabling...");
                    
                    // Wait a moment then re-enable
                    new android.os.Handler().postDelayed(() -> {
                        mFirestore.enableNetwork()
                                .addOnSuccessListener(aVoid -> {
                                    android.util.Log.d("SignUpActivity", "✅ Strategy 1 SUCCESS: Network re-enabled");
                                    testSimpleFirestoreWrite();
                                })
                                .addOnFailureListener(e -> {
                                    android.util.Log.e("SignUpActivity", "❌ Strategy 1 FAILED", e);
                                    tryStrategy2();
                                });
                    }, 1000);
                });
    }
    
    private void tryStrategy2() {
        android.util.Log.d("SignUpActivity", "Strategy 2: Direct write with offline handling");
        
        // Try direct write without connectivity check
        java.util.Map<String, Object> testData = new java.util.HashMap<>();
        testData.put("force_test", true);
        testData.put("timestamp", System.currentTimeMillis());
        testData.put("strategy", "direct_write");
        
        mFirestore.collection("force_test")
                .document("test_" + System.currentTimeMillis())
                .set(testData)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("SignUpActivity", "✅ Strategy 2 SUCCESS: Direct write worked");
                    Toast.makeText(this, "✅ SUCCESS: Firestore is working!", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("SignUpActivity", "❌ Strategy 2 FAILED: Direct write failed", e);
                    Toast.makeText(this, "❌ All strategies failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    
                    // Show detailed error analysis
                    analyzeOfflineError(e);
                });
    }
    
    private void analyzeOfflineError(Exception e) {
        android.util.Log.d("SignUpActivity", "=== OFFLINE ERROR ANALYSIS ===");
        
        String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
        android.util.Log.d("SignUpActivity", "Error message: " + errorMsg);
        android.util.Log.d("SignUpActivity", "Error class: " + e.getClass().getSimpleName());
        
        StringBuilder analysis = new StringBuilder("Error Analysis:\n");
        
        if (errorMsg.contains("client is offline")) {
            analysis.append("• Client is in offline mode\n");
            analysis.append("• Possible causes: No internet, Firebase servers unreachable\n");
            analysis.append("• Solution: Check internet connection, try mobile data\n");
        }
        
        if (errorMsg.contains("Failed to get document")) {
            analysis.append("• Document read operation failed\n");
            analysis.append("• Firestore may be in offline-only mode\n");
        }
        
        android.util.Log.d("SignUpActivity", analysis.toString());
        Toast.makeText(this, "See logcat for detailed error analysis", Toast.LENGTH_LONG).show();
    }
}
