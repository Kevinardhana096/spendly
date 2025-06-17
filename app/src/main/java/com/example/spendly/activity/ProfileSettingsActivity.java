package com.example.spendly.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.spendly.R;

public class ProfileSettingsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Button btnEditProfile;
    private LinearLayout layoutChangePin, layoutChangePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnEditProfile = findViewById(R.id.btn_edit_profile);
        layoutChangePin = findViewById(R.id.layout_change_pin);
        layoutChangePassword = findViewById(R.id.layout_change_password);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnEditProfile.setOnClickListener(v -> {
            // Open edit profile activity
        });

        layoutChangePin.setOnClickListener(v -> {
            // Open change pin activity
        });

        layoutChangePassword.setOnClickListener(v -> {
            // Open change password activity
        });
    }
}