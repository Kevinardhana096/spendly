package com.example.spendly.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.spendly.R;

public class EditPinActivity extends AppCompatActivity {

    private ImageView btnBack, btnDelete;
    private Button btnSaveChanges;

    // PIN dots
    private View[] pinDots;

    // Number buttons
    private TextView[] numberButtons;

    // PIN input
    private StringBuilder currentPin = new StringBuilder();
    private final int PIN_LENGTH = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_pin);

        initViews();
        setupClickListeners();
        updatePinDisplay();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnDelete = findViewById(R.id.btn_delete);
        btnSaveChanges = findViewById(R.id.btn_save_changes);

        // Initialize PIN dots
        pinDots = new View[]{
                findViewById(R.id.dot_1),
                findViewById(R.id.dot_2),
                findViewById(R.id.dot_3),
                findViewById(R.id.dot_4),
                findViewById(R.id.dot_5),
                findViewById(R.id.dot_6)
        };

        // Initialize number buttons
        numberButtons = new TextView[]{
                findViewById(R.id.btn_0),
                findViewById(R.id.btn_1),
                findViewById(R.id.btn_2),
                findViewById(R.id.btn_3),
                findViewById(R.id.btn_4),
                findViewById(R.id.btn_5),
                findViewById(R.id.btn_6),
                findViewById(R.id.btn_7),
                findViewById(R.id.btn_8),
                findViewById(R.id.btn_9)
        };
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSaveChanges.setOnClickListener(v -> savePin());

        btnDelete.setOnClickListener(v -> clearLastDigit());

        // Set click listeners for number buttons
        for (int i = 0; i < numberButtons.length; i++) {
            final int number = i;
            numberButtons[i].setOnClickListener(v -> onNumberClicked(number));
        }
    }

    private void onNumberClicked(int number) {
        if (currentPin.length() < PIN_LENGTH) {
            currentPin.append(number);
            updatePinDisplay();
        }
    }

    private void updatePinDisplay() {
        // Update pin dots
        for (int i = 0; i < pinDots.length; i++) {
            if (i < currentPin.length()) {
                pinDots[i].setBackgroundResource(R.drawable.pin_dot_filled);
            } else {
                pinDots[i].setBackgroundResource(R.drawable.pin_dot_empty);
            }
        }

        // Update save button state
        if (currentPin.length() == PIN_LENGTH) {
            btnSaveChanges.setEnabled(true);
            btnSaveChanges.setAlpha(1.0f);
        } else {
            btnSaveChanges.setEnabled(false);
            btnSaveChanges.setAlpha(0.5f);
        }
    }

    private void savePin() {
        if (currentPin.length() == PIN_LENGTH) {
            String pin = currentPin.toString();

            // Save PIN to SharedPreferences or database
            // For now, just show success message
            Toast.makeText(this, "PIN saved successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Please enter a complete PIN", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearLastDigit() {
        if (currentPin.length() > 0) {
            currentPin.deleteCharAt(currentPin.length() - 1);
            updatePinDisplay();
        }
    }
}