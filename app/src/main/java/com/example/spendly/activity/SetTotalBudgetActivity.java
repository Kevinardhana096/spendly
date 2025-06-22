package com.example.spendly.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spendly.R;
import com.example.spendly.repository.BudgetRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SetTotalBudgetActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText etIncome, etBudget;
    private Button btnNext;
    private ProgressBar progressBar;

    private double monthlyIncome = 0.0;
    private double monthlyBudget = 0.0;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private BudgetRepository budgetRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_total_budget);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        budgetRepository = BudgetRepository.getInstance(this);

        initViews();
        setupClickListeners();
        setupTextWatchers();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        etIncome = findViewById(R.id.et_income);
        etBudget = findViewById(R.id.et_budget);
        btnNext = findViewById(R.id.btn_next);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnNext.setOnClickListener(v -> proceedToNext());
    }

    private void setupTextWatchers() {
        etIncome.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateIncomeAndButton();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etBudget.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateBudgetAndButton();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateIncomeAndButton() {
        String incomeText = etIncome.getText().toString().trim();

        if (!incomeText.isEmpty()) {
            try {
                monthlyIncome = Double.parseDouble(incomeText.replace(",", "").replace(".", ""));
            } catch (NumberFormatException e) {
                monthlyIncome = 0.0;
            }
        } else {
            monthlyIncome = 0.0;
        }

        updateNextButton();
    }

    private void updateBudgetAndButton() {
        String budgetText = etBudget.getText().toString().trim();

        if (!budgetText.isEmpty()) {
            try {
                monthlyBudget = Double.parseDouble(budgetText.replace(",", "").replace(".", ""));
            } catch (NumberFormatException e) {
                monthlyBudget = 0.0;
            }
        } else {
            monthlyBudget = 0.0;
        }

        updateNextButton();
    }

    private void updateNextButton() {
        if (monthlyIncome > 0 && monthlyBudget > 0) {
            btnNext.setEnabled(true);
            btnNext.setAlpha(1.0f);
        } else {
            btnNext.setEnabled(false);
            btnNext.setAlpha(0.5f);
        }
    }

    private void proceedToNext() {
        if (monthlyIncome <= 0 || monthlyBudget <= 0) {
            Toast.makeText(this, "Please enter both income and budget", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate budget vs income
        if (monthlyBudget > monthlyIncome) {
            Toast.makeText(this, "Budget cannot exceed your income", Toast.LENGTH_LONG).show();
            return;
        }

        // Show warning if budget is too high (>80% of income)
        double budgetPercentage = (monthlyBudget / monthlyIncome) * 100;
        if (budgetPercentage > 80) {
            showHighBudgetWarning();
            return;
        }

        // Save data and proceed
        saveBudgetData();
    }

    private void showHighBudgetWarning() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("High Budget Warning");
        builder.setMessage("Your budget is more than 80% of your income. This may not leave enough room for savings. Do you want to continue?");

        builder.setPositiveButton("Continue", (dialog, which) -> saveBudgetData());
        builder.setNegativeButton("Review", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void saveBudgetData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You need to be logged in to set a budget", Toast.LENGTH_SHORT).show();
            return;
        }

        android.util.Log.d("SetTotalBudgetActivity", "Starting to save budget data...");
        android.util.Log.d("SetTotalBudgetActivity", "Monthly Income: " + monthlyIncome);
        android.util.Log.d("SetTotalBudgetActivity", "Monthly Budget: " + monthlyBudget);
        android.util.Log.d("SetTotalBudgetActivity", "User ID: " + currentUser.getUid());

        // Show loading indicator
        progressBar.setVisibility(View.VISIBLE);
        btnNext.setEnabled(false);

        // Create budget data
        Map<String, Object> budgetData = new HashMap<>();
        budgetData.put("monthly_income", monthlyIncome);
        budgetData.put("monthly_budget", monthlyBudget);
        budgetData.put("remaining_budget", monthlyBudget); // Initially all budget is remaining
        budgetData.put("income_formatted", formatNumber((int) monthlyIncome));
        budgetData.put("budget_formatted", formatNumber((int) monthlyBudget));
        budgetData.put("remaining_formatted", formatNumber((int) monthlyBudget));
        budgetData.put("setup_date", getCurrentDate());
        budgetData.put("last_updated", new Date().toString());

        // Save to both Firebase and SQLite using repository
        android.util.Log.d("SetTotalBudgetActivity", "Calling budgetRepository.saveTotalBudget()...");
        budgetRepository.saveTotalBudget(budgetData, new BudgetRepository.BudgetCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                android.util.Log.d("SetTotalBudgetActivity", "✅ Budget save successful!");
                progressBar.setVisibility(View.GONE);

                // Check if data was saved offline-only
                boolean isOfflineOnly = data.containsKey("offline_only") && (boolean) data.get("offline_only");
                if (isOfflineOnly) {
                    Toast.makeText(SetTotalBudgetActivity.this,
                            "Budget saved locally. Will sync when connection is available.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SetTotalBudgetActivity.this,
                            "Total budget set successfully",
                            Toast.LENGTH_SHORT).show();
                }

                // Navigate back to MainActivity instead of going to SetBudgetActivity
                Intent intent = new Intent(SetTotalBudgetActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                // Finish this activity
                finish();
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("SetTotalBudgetActivity", "❌ Budget save failed!");
                android.util.Log.e("SetTotalBudgetActivity", "Error message: " + error);
                progressBar.setVisibility(View.GONE);
                btnNext.setEnabled(true);
                Toast.makeText(SetTotalBudgetActivity.this,
                        "Failed to save budget: " + error,
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {

            }
        });
    }

    private String getCurrentDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    private String formatNumber(int number) {
        return String.format("%,d", number).replace(",", ".");
    }
}
