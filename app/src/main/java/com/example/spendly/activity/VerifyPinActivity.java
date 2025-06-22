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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class VerifyPinActivity extends AppCompatActivity {
    
    public static final String EXTRA_VERIFICATION_TYPE = "verification_type";
    public static final String EXTRA_VERIFICATION_DATA = "verification_data";
    public static final String TYPE_ADD_MONEY = "add_money";
    public static final String TYPE_ADD_TRANSACTION = "add_transaction";
    
    private View[] pinDots;
    private TextView[] numberButtons;
    private ImageView btnBack, btnDelete;
    private Button btnVerify;
    private TextView tvTitle, tvSubtitle;

    private StringBuilder currentPin = new StringBuilder();
    private String verificationType;
    private Bundle verificationData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_pin);

        // Get verification type and data from intent
        Intent intent = getIntent();
        verificationType = intent.getStringExtra(EXTRA_VERIFICATION_TYPE);
        verificationData = intent.getBundleExtra(EXTRA_VERIFICATION_DATA);

        initViews();
        setupNumberPad();
        setupClickListeners();
        updateUI();
    }

    private void initViews() {
        // Header views
        btnBack = findViewById(R.id.btn_back);
        tvTitle = findViewById(R.id.tv_title);
        tvSubtitle = findViewById(R.id.tv_subtitle);

        // PIN dots
        pinDots = new View[6];
        pinDots[0] = findViewById(R.id.pin_dot_1);
        pinDots[1] = findViewById(R.id.pin_dot_2);
        pinDots[2] = findViewById(R.id.pin_dot_3);
        pinDots[3] = findViewById(R.id.pin_dot_4);
        pinDots[4] = findViewById(R.id.pin_dot_5);
        pinDots[5] = findViewById(R.id.pin_dot_6);

        // Number buttons
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

        // Action buttons
        btnDelete = findViewById(R.id.btn_delete);
        btnVerify = findViewById(R.id.btn_verify);
    }

    private void setupNumberPad() {
        for (int i = 0; i < numberButtons.length; i++) {
            final int number = i;
            numberButtons[i].setOnClickListener(v -> onNumberClicked(number));
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        btnDelete.setOnClickListener(v -> onDeleteClicked());
        btnVerify.setOnClickListener(v -> onVerifyClicked());
    }

    private void updateUI() {
        // Update title based on verification type
        if (TYPE_ADD_MONEY.equals(verificationType)) {
            tvTitle.setText("Verify PIN");
            tvSubtitle.setText("Enter your PIN to add money to savings");
        } else if (TYPE_ADD_TRANSACTION.equals(verificationType)) {
            tvTitle.setText("Verify PIN");
            tvSubtitle.setText("Enter your PIN to complete transaction");
        } else {
            tvTitle.setText("Verify PIN");
            tvSubtitle.setText("Enter your PIN to continue");
        }
    }

    private void onNumberClicked(int number) {
        if (currentPin.length() < 6) {
            currentPin.append(number);
            updatePinDots();
            updateVerifyButton();

            // Auto-verify when 6 digits entered
            if (currentPin.length() == 6) {
                verifyPin();
            }
        }
    }

    private void onDeleteClicked() {
        if (currentPin.length() > 0) {
            currentPin.deleteCharAt(currentPin.length() - 1);
            updatePinDots();
            updateVerifyButton();
        }
    }

    private void onVerifyClicked() {
        if (currentPin.length() == 6) {
            verifyPin();
        } else {
            Toast.makeText(this, "Please enter complete PIN", Toast.LENGTH_SHORT).show();
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

    private void updateVerifyButton() {
        boolean isComplete = currentPin.length() == 6;
        btnVerify.setEnabled(isComplete);
        btnVerify.setAlpha(isComplete ? 1.0f : 0.5f);
    }

    private void verifyPin() {
        // Show loading state
        btnVerify.setEnabled(false);
        btnVerify.setText("Verifying...");

        // Get saved PIN from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String savedHashedPin = prefs.getString("user_pin", "");

        if (savedHashedPin.isEmpty()) {
            // No PIN set, redirect to set PIN
            Toast.makeText(this, "Please set up your PIN first", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, SetPinCodeActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Verify PIN
        String enteredPin = currentPin.toString();
        if (verifyPinHash(enteredPin, savedHashedPin)) {
            // PIN verified successfully
            Toast.makeText(this, "PIN verified successfully", Toast.LENGTH_SHORT).show();
            
            // Return success with verification data
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_VERIFICATION_TYPE, verificationType);
            if (verificationData != null) {
                resultIntent.putExtras(verificationData);
            }
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            // Wrong PIN
            currentPin.setLength(0);
            updatePinDots();
            updateVerifyButton();
            btnVerify.setEnabled(true);
            btnVerify.setText("Verify");
            Toast.makeText(this, "Incorrect PIN. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean verifyPinHash(String enteredPin, String savedHashedPin) {
        String hashedEnteredPin = hashPin(enteredPin);
        return hashedEnteredPin.equals(savedHashedPin);
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
}
