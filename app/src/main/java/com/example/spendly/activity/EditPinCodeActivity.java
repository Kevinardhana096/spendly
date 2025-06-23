package com.example.spendly.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import com.example.spendly.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditPinCodeActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvStepIndicator, tvTitle, tvSubtitle;
    private View[] pinDots;
    private TextView[] numberButtons;
    private ImageView btnDelete, btnFingerprint;
    private Button btnSaveChanges;
    private StringBuilder currentPin = new StringBuilder();
    private String oldPin = "";
    private String newPin = "";
    private String confirmedPin = "";

    // PIN change steps
    private static final int STEP_ENTER_OLD_PIN = 1;
    private static final int STEP_ENTER_NEW_PIN = 2;
    private static final int STEP_CONFIRM_NEW_PIN = 3;

    private int currentStep = STEP_ENTER_OLD_PIN;
    
    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_pin_code);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        initViews();
        setupClickListeners();
        updateUIForCurrentStep();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvStepIndicator = findViewById(R.id.tv_step_indicator);
        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        btnSaveChanges = findViewById(R.id.btn_save_changes);
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
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Number button click listeners
        for (int i = 0; i < numberButtons.length; i++) {
            final int number = i;
            numberButtons[i].setOnClickListener(v -> onNumberClicked(number));
        }

        btnDelete.setOnClickListener(v -> onDeleteClicked());

        btnFingerprint.setOnClickListener(v -> {
            Toast.makeText(this, "Fingerprint authentication not available", Toast.LENGTH_SHORT).show();
        });

        btnSaveChanges.setOnClickListener(v -> onSaveChangesClicked());
    }

    private void onNumberClicked(int number) {
        if (currentPin.length() < 6) {
            currentPin.append(number);
            updatePinDots();
            updateSaveButton();

            // Auto-proceed when 6 digits entered
            if (currentPin.length() == 6) {
                handlePinComplete();
            }
        }
    }

    private void onDeleteClicked() {
        if (currentPin.length() > 0) {
            currentPin.deleteCharAt(currentPin.length() - 1);
            updatePinDots();
            updateSaveButton();
        }
    }

    private void onSaveChangesClicked() {
        if (currentPin.length() == 6) {
            handlePinComplete();
        }
    }

    private void handlePinComplete() {
        switch (currentStep) {
            case STEP_ENTER_OLD_PIN:
                verifyOldPin();
                break;
            case STEP_ENTER_NEW_PIN:
                setNewPin();
                break;
            case STEP_CONFIRM_NEW_PIN:
                confirmNewPin();
                break;
        }
    }

    private void verifyOldPin() {
        String enteredPin = currentPin.toString();
        String savedPin = getSavedPin();

        if (verifyPin(enteredPin, savedPin)) {
            // Old PIN is correct, proceed to next step
            oldPin = enteredPin;
            currentStep = STEP_ENTER_NEW_PIN;
            currentPin.setLength(0);
            updateUIForCurrentStep();
            Toast.makeText(this, "PIN verified. Enter your new PIN.", Toast.LENGTH_SHORT).show();
        } else {
            // Wrong old PIN
            currentPin.setLength(0);
            updatePinDots();
            updateSaveButton();
            Toast.makeText(this, "Incorrect PIN. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setNewPin() {
        newPin = currentPin.toString();

        // Check if new PIN is different from old PIN
        if (newPin.equals(oldPin)) {
            currentPin.setLength(0);
            updatePinDots();
            updateSaveButton();
            Toast.makeText(this, "New PIN must be different from the old PIN.", Toast.LENGTH_SHORT).show();
            return;
        }

        currentStep = STEP_CONFIRM_NEW_PIN;
        currentPin.setLength(0);
        updateUIForCurrentStep();
        Toast.makeText(this, "Please confirm your new PIN.", Toast.LENGTH_SHORT).show();
    }

    private void confirmNewPin() {
        confirmedPin = currentPin.toString();

        if (confirmedPin.equals(newPin)) {
            // PINs match, save the new PIN
            saveNewPin();
        } else {
            // PINs don't match, go back to new PIN entry
            currentStep = STEP_ENTER_NEW_PIN;
            currentPin.setLength(0);
            newPin = "";
            updateUIForCurrentStep();
            Toast.makeText(this, "PINs don't match. Please enter your new PIN again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveNewPin() {
        String hashedPin = hashPin(newPin);

        // Show progress
        btnSaveChanges.setEnabled(false);
        btnSaveChanges.setText("Updating PIN...");

        // Save to SharedPreferences for local access
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit()
                .putString("user_pin", hashedPin)
                .putLong("pin_updated_date", System.currentTimeMillis())
                .apply();

        // Save to Firebase Firestore
        if (currentUser != null) {
            Map<String, Object> pinData = new HashMap<>();
            pinData.put("userPin", hashedPin);
            pinData.put("pinCodeSet", true);
            pinData.put("pinUpdatedDate", System.currentTimeMillis());

            mFirestore.collection("users")
                    .document(currentUser.getUid())
                    .update(pinData)
                    .addOnSuccessListener(aVoid -> {
                        // Firebase update successful
                        Toast.makeText(this, "PIN updated successfully", Toast.LENGTH_SHORT).show();
                        
                        // Create result intent
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("pin_updated_success", true);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        // Firebase update failed, but local storage succeeded
                        Toast.makeText(this, "PIN updated locally. Sync will happen later.", Toast.LENGTH_SHORT).show();
                        
                        // Still return success since local storage worked
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("pin_updated_success", true);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    });
        } else {
            // No user logged in, only local storage
            Toast.makeText(this, "PIN updated successfully", Toast.LENGTH_SHORT).show();
            
            // Create result intent
            Intent resultIntent = new Intent();
            resultIntent.putExtra("pin_updated_success", true);
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }

    private void updateUIForCurrentStep() {
        updatePinDots();
        updateSaveButton();

        switch (currentStep) {
            case STEP_ENTER_OLD_PIN:
                tvStepIndicator.setText("Step 1 of 3");
                tvStepIndicator.setVisibility(View.VISIBLE);
                tvTitle.setText("Enter Current PIN");
                tvSubtitle.setText("Please enter your current PIN to continue");
                btnSaveChanges.setText("Verify PIN");
                break;
            case STEP_ENTER_NEW_PIN:
                tvStepIndicator.setText("Step 2 of 3");
                tvTitle.setText("Enter New PIN");
                tvSubtitle.setText("Choose a new PIN code for your account");
                btnSaveChanges.setText("Set New PIN");
                break;
            case STEP_CONFIRM_NEW_PIN:
                tvStepIndicator.setText("Step 3 of 3");
                tvTitle.setText("Confirm New PIN");
                tvSubtitle.setText("Please confirm your new PIN code");
                btnSaveChanges.setText("Save Changes");
                break;
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

    private void updateSaveButton() {
        boolean isEnabled = currentPin.length() == 6;
        btnSaveChanges.setEnabled(isEnabled);
        btnSaveChanges.setAlpha(isEnabled ? 1.0f : 0.5f);
    }

    private String getSavedPin() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String localPin = prefs.getString("user_pin", "");
        
        // If no local PIN found, try to sync from Firebase
        if (localPin.isEmpty() && currentUser != null) {
            // This is a fallback - normally PIN should be synced during login
            syncPinFromFirebaseIfNeeded();
            // Return local PIN after potential sync
            return prefs.getString("user_pin", "");
        }
        
        return localPin;
    }
    
    /**
     * Fallback method to sync PIN from Firebase if not found locally
     */
    private void syncPinFromFirebaseIfNeeded() {
        if (currentUser != null) {
            mFirestore.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String firebasePin = documentSnapshot.getString("userPin");
                        Boolean pinCodeSet = documentSnapshot.getBoolean("pinCodeSet");
                        
                        if (firebasePin != null && !firebasePin.isEmpty() && pinCodeSet != null && pinCodeSet) {
                            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                            prefs.edit()
                                .putString("user_pin", firebasePin)
                                .putBoolean("pin_set", true)
                                .putLong("pin_sync_date", System.currentTimeMillis())
                                .apply();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Failed to sync from Firebase, continue with empty PIN
                });
        }
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
            return pin; // Fallback (not recommended for production)
        }
    }

    private boolean verifyPin(String enteredPin, String savedHashedPin) {
        String hashedEnteredPin = hashPin(enteredPin);
        return hashedEnteredPin.equals(savedHashedPin);
    }

    @Override
    public void onBackPressed() {
        if (currentStep > STEP_ENTER_OLD_PIN) {
            // Allow going back to previous step
            switch (currentStep) {
                case STEP_ENTER_NEW_PIN:
                    currentStep = STEP_ENTER_OLD_PIN;
                    break;
                case STEP_CONFIRM_NEW_PIN:
                    currentStep = STEP_ENTER_NEW_PIN;
                    break;
            }
            currentPin.setLength(0);
            updateUIForCurrentStep();
        } else {
            super.onBackPressed();
        }
    }
}