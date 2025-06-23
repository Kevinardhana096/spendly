package com.example.spendly.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spendly.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class SetPinCodeActivity extends AppCompatActivity {
    private View[] pinDots;
    private TextView[] numberButtons;
    private ImageView btnDelete, btnFingerprint;
    private Button btnSet;
    private TextView tvTitle, tvSubtitle;

    private StringBuilder currentPin = new StringBuilder();
    private String confirmedPin = "";
    private boolean isConfirmingPin = false;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_pin_code);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // If the user is not logged in, redirect to login screen
        if (currentUser == null) {
            Toast.makeText(this, "You need to be logged in to set a PIN code", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(SetPinCodeActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        initViews();
        setupClickListeners();
        updatePinDots();
    }

    private void initViews() {
        btnSet = findViewById(R.id.btn_set);
        btnDelete = findViewById(R.id.btn_delete);
        btnFingerprint = findViewById(R.id.btn_fingerprint);

        // Initialize PIN dots
        pinDots = new View[6];
        pinDots[0] = findViewById(R.id.pin_dot_1);
        pinDots[1] = findViewById(R.id.pin_dot_2);
        pinDots[2] = findViewById(R.id.pin_dot_3);
        pinDots[3] = findViewById(R.id.pin_dot_4);
        pinDots[4] = findViewById(R.id.pin_dot_5);
        pinDots[5] = findViewById(R.id.pin_dot_6);

        // Initialize number buttons
        numberButtons = new TextView[10];
        numberButtons[0] = findViewById(R.id.btn_0);
        numberButtons[1] = findViewById(R.id.btn_1);
        numberButtons[2] = findViewById(R.id.btn_2);
        numberButtons[3] = findViewById(R.id.btn_3);
        numberButtons[4] = findViewById(R.id.btn_4);
        numberButtons[5] = findViewById(R.id.btn_5);
        numberButtons[6] = findViewById(R.id.btn_6);
        numberButtons[7] = findViewById(R.id.btn_7);
        numberButtons[8] = findViewById(R.id.btn_8);
        numberButtons[9] = findViewById(R.id.btn_9);

        // Find title and subtitle TextViews using their IDs
        tvTitle = findViewById(R.id.tv_pin_title);
        tvSubtitle = findViewById(R.id.tv_pin_subtitle);
    }

    private void setupClickListeners() {
        // Number button click listeners
        for (int i = 0; i < numberButtons.length; i++) {
            final int number = i;
            numberButtons[i].setOnClickListener(v -> {
                animateButtonPress(v);
                onNumberClicked(number);
            });
        }

        btnDelete.setOnClickListener(v -> {
            animateButtonPress(v);
            onDeleteClicked();
        });

        btnFingerprint.setOnClickListener(v -> {
            animateButtonPress(v);
            Toast.makeText(this, "Fingerprint setup not implemented yet", Toast.LENGTH_SHORT).show();
        });

        btnSet.setOnClickListener(v -> onSetClicked());
    }

    private void animateButtonPress(View button) {
        // Change text color to white when pressed
        if (button instanceof TextView) {
            TextView textView = (TextView) button;
            textView.setTextColor(getResources().getColor(R.color.white, getTheme()));

            // Reset text color after delay
            button.postDelayed(() -> {
                textView.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
            }, 150);
        } else if (button instanceof ImageView) {
            ImageView imageView = (ImageView) button;
            imageView.setColorFilter(getResources().getColor(R.color.white, getTheme()));

            // Reset tint after delay
            button.postDelayed(() -> {
                imageView.setColorFilter(getResources().getColor(R.color.text_primary, getTheme()));
            }, 150);
        }
    }

    private void onNumberClicked(int number) {
        if (currentPin.length() < 6) {
            currentPin.append(number);
            updatePinDots();
            updateSetButton();

            // Auto-proceed when 6 digits entered
            if (currentPin.length() == 6) {
                if (!isConfirmingPin) {
                    // First PIN entry completed
                    confirmedPin = currentPin.toString();
                    isConfirmingPin = true;
                    currentPin.setLength(0);

                    // Update UI for confirmation
                    updateHeaderForConfirmation();
                    updatePinDots();
                    updateSetButton();

                    Toast.makeText(this, "Please confirm your PIN", Toast.LENGTH_SHORT).show();
                } else {
                    // PIN confirmation completed
                    if (currentPin.toString().equals(confirmedPin)) {
                        // PINs match, save and proceed
                        savePinAndProceed();
                    } else {
                        // PINs don't match, reset
                        resetPinEntry();
                        Toast.makeText(this, "PINs don't match. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void onDeleteClicked() {
        if (currentPin.length() > 0) {
            currentPin.deleteCharAt(currentPin.length() - 1);
            updatePinDots();
            updateSetButton();
        }
    }

    private void onSetClicked() {
        if (currentPin.length() == 6) {
            if (!isConfirmingPin) {
                // First PIN entry completed
                confirmedPin = currentPin.toString();
                isConfirmingPin = true;
                currentPin.setLength(0);

                updateHeaderForConfirmation();
                updatePinDots();
                updateSetButton();

                Toast.makeText(this, "Please confirm your PIN", Toast.LENGTH_SHORT).show();
            } else {
                // PIN confirmation
                if (currentPin.toString().equals(confirmedPin)) {
                    savePinAndProceed();
                } else {
                    resetPinEntry();
                    Toast.makeText(this, "PINs don't match. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void updatePinDots() {
        for (int i = 0; i < pinDots.length; i++) {
            if (i < currentPin.length()) {
                pinDots[i].setBackgroundResource(R.drawable.pin_dot_filled);
            } else {
                pinDots[i].setBackgroundResource(R.drawable.pin_dot_empty);
            }
        }
    }

    private void updateSetButton() {
        boolean isEnabled = currentPin.length() == 6;
        btnSet.setEnabled(isEnabled);
        btnSet.setAlpha(isEnabled ? 1.0f : 0.5f);
    }

    private void updateHeaderForConfirmation() {
        // Update header text to show "Confirm PIN" if the views exist
        if (tvTitle != null) {
            tvTitle.setText("Confirm PIN Code");
        }
        if (tvSubtitle != null) {
            tvSubtitle.setText("Please enter the same PIN again to confirm");
        }
    }

    private void resetPinEntry() {
        currentPin.setLength(0);
        confirmedPin = "";
        isConfirmingPin = false;
        updatePinDots();
        updateSetButton();
    }

    private void savePinAndProceed() {
        // Show progress or disable buttons during the save operation
        btnSet.setEnabled(false);
        btnSet.setText("Setting PIN...");

        // Hash the PIN for security
        String hashedPin = hashPin(confirmedPin);

        // Save to SharedPreferences for local access
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit()
                .putString("user_pin", hashedPin)
                .putBoolean("pin_set", true)
                .putLong("pin_set_date", System.currentTimeMillis())
                .apply();

        // Save to Firestore in the user's document
        Map<String, Object> pinData = new HashMap<>();
        pinData.put("userPin", hashedPin);
        pinData.put("pinCodeSet", true);
        pinData.put("pinSetDate", System.currentTimeMillis());

        mFirestore.collection("users")
                .document(currentUser.getUid())
                .update(pinData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "PIN set successfully", Toast.LENGTH_SHORT).show();

                    // Navigate to main app
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save PIN: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSet.setEnabled(true);
                    btnSet.setText("Set");
                });
    }

    private String hashPin(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(pin.getBytes());

            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return pin; // Fallback to plain text (not recommended for production)
        }
    }

    public static boolean verifyPin(String enteredPin, String savedHashedPin) {
        String hashedEnteredPin = hashPinStatic(enteredPin);
        return hashedEnteredPin.equals(savedHashedPin);
    }

    private static String hashPinStatic(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(pin.getBytes());

            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return pin;
        }
    }
}
