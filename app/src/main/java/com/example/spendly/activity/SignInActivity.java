package com.example.spendly.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spendly.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignInActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView signUpTextView;

    // Firebase Auth
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();

        // Hide the action bar if it exists
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize views
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        signUpTextView = findViewById(R.id.signUpText);

        // Set login button click listener
        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            // Basic validation
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Verify email format
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(getApplicationContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                return;
            }

            // Show loading state
            loginButton.setEnabled(false);
            loginButton.setText(R.string.logging_in);

            // Authenticate with Firebase
            signInWithEmailPassword(email, password);
        });

        // Set sign up text click listener
        signUpTextView.setOnClickListener(v -> {
            // Navigate to SignUp activity
            Intent intent = new Intent(getApplicationContext(), SignUpActivity.class);
            startActivity(intent);
        });

        // Check if user is already signed in
        checkCurrentUser();
    }

    /**
     * Check if user is already signed in and redirect if necessary
     */
    private void checkCurrentUser() {
        // Check if user is signed in (non-null)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            // User is already signed in, check if PIN is set
            checkPinCodeAndRedirect(currentUser.getUid());
        }
    }

    /**
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
                        Toast.makeText(getApplicationContext(), "Login Successful", Toast.LENGTH_SHORT).show();

                        // Check if PIN is set and redirect accordingly
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkPinCodeAndRedirect(user.getUid());
                        }
                    } else {
                        // If sign in fails, display a message to the user
                        if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                            Toast.makeText(getApplicationContext(), "User does not exist", Toast.LENGTH_SHORT).show();
                        } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(getApplicationContext(), "Invalid password", Toast.LENGTH_SHORT).show();
                        } else {
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            Toast.makeText(getApplicationContext(), "Authentication failed: " + errorMessage,
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Check if user has set a PIN code and redirect accordingly
     */
    private void checkPinCodeAndRedirect(String userId) {
        mFirestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("pinCodeSet")
                            && Boolean.TRUE.equals(documentSnapshot.getBoolean("pinCodeSet"))) {
                        // PIN is already set, go to main activity
                        goToMainActivity();
                    } else {
                        // PIN is not set, go to SetPinCodeActivity
                        goToSetPinCodeActivity();
                    }
                })
                .addOnFailureListener(e -> {
                    // On error, go to PIN setup as a safety measure
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
    }

    private void goToSetPinCodeActivity() {
        Intent intent = new Intent(getApplicationContext(), SetPinCodeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Close this activity so user can't go back
    }
}
