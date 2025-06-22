package com.example.spendly.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.spendly.R;
import com.example.spendly.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignInActivity extends AppCompatActivity {

    private static final String TAG = "SignInActivity";
      private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView signUpTextView;
    private TextView forgotPasswordTextView;

    // Firebase Auth
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        Log.d(TAG, "=== SignInActivity Created ===");

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        // Hide the action bar if it exists
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize views
        initializeViews();
        
        // Setup input validation
        setupInputValidation();

        // Check if user is already signed in
        checkCurrentUser();
    }    /**
     * Initialize all views
     */
    private void initializeViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        signUpTextView = findViewById(R.id.signUpText);
        forgotPasswordTextView = findViewById(R.id.forgotPasswordText);

        // Set login button click listener
        loginButton.setOnClickListener(v -> attemptLogin());

        // Set sign up text click listener
        signUpTextView.setOnClickListener(v -> {
            Log.d(TAG, "Navigate to SignUp clicked");
            Intent intent = new Intent(getApplicationContext(), SignUpActivity.class);
            startActivity(intent);
        });

        // Set forgot password text click listener
        forgotPasswordTextView.setOnClickListener(v -> {
            Log.d(TAG, "Forgot password clicked");
            showForgotPasswordDialog();
        });
    }

    /**
     * Setup real-time input validation
     */
    private void setupInputValidation() {
        // Email validation
        emailEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateEmail();
            }
        });

        // Password validation
        passwordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validatePassword();
            }
        });
    }

    /**
     * Validate email input
     */
    private boolean validateEmail() {
        String email = emailEditText.getText().toString().trim();
        
        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            return false;
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email address");
            return false;
        }
        
        emailEditText.setError(null);
        return true;
    }

    /**
     * Validate password input
     */
    private boolean validatePassword() {
        String password = passwordEditText.getText().toString().trim();
        
        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            return false;
        }
        
        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            return false;
        }
        
        passwordEditText.setError(null);
        return true;
    }

    /**
     * Attempt to login with validation
     */
    private void attemptLogin() {
        Log.d(TAG, "Login attempt started");

        // Validate inputs
        boolean isEmailValid = validateEmail();
        boolean isPasswordValid = validatePassword();

        if (!isEmailValid || !isPasswordValid) {
            Toast.makeText(getApplicationContext(), "Please correct the errors in the form", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        Log.d(TAG, "Attempting login for email: " + email);

        // Show loading state
        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");

        // Authenticate with Firebase
        signInWithEmailPassword(email, password);
    }    /**
     * Check if user is already signed in and redirect if necessary
     */
    private void checkCurrentUser() {
        // Check if user is signed in (non-null)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            Log.d(TAG, "Current user found: " + currentUser.getUid());
            // User is already signed in, verify data and check PIN
            verifyAndEnsureUserDataExists(currentUser, currentUser.getEmail());
        } else {
            Log.d(TAG, "No current user found");
        }
    }/**
     * Sign in with email and password using Firebase Authentication
     */
    private void signInWithEmailPassword(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    // Reset button state
                    loginButton.setEnabled(true);
                    loginButton.setText(R.string.log_in);

                    if (task.isSuccessful()) {
                        // Sign in success
                        Log.d(TAG, "✅ Authentication successful");
                        Toast.makeText(getApplicationContext(), "Login Successful", Toast.LENGTH_SHORT).show();

                        // Verify and ensure user data exists in Firestore
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "User authenticated with UID: " + user.getUid());
                            verifyAndEnsureUserDataExists(user, email);
                        }
                    } else {
                        // If sign in fails, display a message to the user
                        Log.e(TAG, "❌ Authentication failed", task.getException());
                        
                        if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                            Toast.makeText(getApplicationContext(), "User does not exist", Toast.LENGTH_SHORT).show();
                        } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(getApplicationContext(), "Invalid password", Toast.LENGTH_SHORT).show();
                        } else {
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            Log.e(TAG, "Authentication error: " + errorMessage);
                            Toast.makeText(getApplicationContext(), "Authentication failed: " + errorMessage,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Verify user data exists in Firestore and create if missing
     */
    private void verifyAndEnsureUserDataExists(FirebaseUser firebaseUser, String email) {
        String userId = firebaseUser.getUid();
        Log.d(TAG, "Verifying user data exists in Firestore for UID: " + userId);
        
        mFirestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "✅ User data found in Firestore");
                        // User data exists, check PIN and redirect
                        checkPinCodeAndRedirect(documentSnapshot);
                    } else {
                        Log.w(TAG, "⚠️ User data not found in Firestore, creating minimal user document");
                        // User data doesn't exist, create minimal user document
                        createMinimalUserDocument(firebaseUser, email);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error checking user data in Firestore", e);
                    // On error, try to create user document as fallback
                    Toast.makeText(getApplicationContext(), "Error accessing user data, creating profile...", 
                            Toast.LENGTH_SHORT).show();
                    createMinimalUserDocument(firebaseUser, email);
                });
    }

    /**
     * Create minimal user document for users who don't have one in Firestore
     */
    private void createMinimalUserDocument(FirebaseUser firebaseUser, String email) {
        String userId = firebaseUser.getUid();
        Log.d(TAG, "Creating minimal user document for UID: " + userId);
        
        // Create minimal user object with default values
        User minimalUser = new User(
                userId,
                email,
                "", // Empty phone number - user can update later
                "", // Empty gender - user can update later  
                "", // Empty date of birth - user can update later
                0.0 // Zero initial balance - user can update later
        );
        
        mFirestore.collection("users")
                .document(userId)
                .set(minimalUser)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Minimal user document created successfully");
                    Toast.makeText(getApplicationContext(), "Profile created successfully", Toast.LENGTH_SHORT).show();
                    // Now redirect to PIN setup since this is a new user document
                    goToSetPinCodeActivity();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to create minimal user document", e);
                    Toast.makeText(getApplicationContext(), 
                            "Error creating user profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Still redirect to PIN setup as fallback
                    goToSetPinCodeActivity();
                });
    }    /**
     * Check if user has set a PIN code and redirect accordingly (using DocumentSnapshot)
     */
    private void checkPinCodeAndRedirect(com.google.firebase.firestore.DocumentSnapshot documentSnapshot) {
        Log.d(TAG, "Checking PIN code status from DocumentSnapshot");
        
        if (documentSnapshot.contains("pinCodeSet") && 
            Boolean.TRUE.equals(documentSnapshot.getBoolean("pinCodeSet"))) {
            Log.d(TAG, "PIN is set, redirecting to MainActivity");
            // PIN is already set, go to main activity
            goToMainActivity();
        } else {
            Log.d(TAG, "PIN not set, redirecting to SetPinCodeActivity");
            // PIN is not set, go to SetPinCodeActivity
            goToSetPinCodeActivity();
        }
    }

    /**
     * Check if user has set a PIN code and redirect accordingly (legacy method for backward compatibility)
     */
    private void checkPinCodeAndRedirect(String userId) {
        Log.d(TAG, "Legacy PIN check for userId: " + userId);
        mFirestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        checkPinCodeAndRedirect(documentSnapshot);
                    } else {
                        Log.w(TAG, "User document not found in legacy check, redirecting to PIN setup");
                        goToSetPinCodeActivity();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error in legacy PIN check", e);
                    Toast.makeText(getApplicationContext(), "Error checking user data: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    goToSetPinCodeActivity();
                });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Close this activity so user can't go back
    }    private void goToSetPinCodeActivity() {
        Intent intent = new Intent(getApplicationContext(), SetPinCodeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Close this activity so user can't go back
    }

    /**
     * Show forgot password dialog
     */
    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.reset_password);
        
        // Create EditText for email input
        final EditText emailInput = new EditText(this);
        emailInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setHint(R.string.enter_email_reset);
        
        // Pre-fill with current email if available
        String currentEmail = emailEditText.getText().toString().trim();
        if (!currentEmail.isEmpty()) {
            emailInput.setText(currentEmail);
        }
        
        builder.setView(emailInput);
        
        builder.setPositiveButton(R.string.reset_password, (dialog, which) -> {
            String email = emailInput.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Please enter your email address", Toast.LENGTH_SHORT).show();
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(getApplicationContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            } else {
                sendPasswordResetEmail(email);
            }
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        
        builder.show();
    }

    /**
     * Send password reset email using Firebase Auth
     */
    private void sendPasswordResetEmail(String email) {
        Log.d(TAG, "Sending password reset email to: " + email);
        
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "✅ Password reset email sent successfully");
                        Toast.makeText(getApplicationContext(), 
                                getString(R.string.reset_email_sent), 
                                Toast.LENGTH_LONG).show();
                    } else {
                        Log.e(TAG, "❌ Failed to send password reset email", task.getException());
                        String errorMessage = task.getException() != null ? 
                                task.getException().getMessage() : 
                                getString(R.string.reset_email_error);
                        Toast.makeText(getApplicationContext(), 
                                getString(R.string.reset_email_error) + ": " + errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
