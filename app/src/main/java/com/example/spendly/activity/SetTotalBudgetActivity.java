package com.example.spendly.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.spendly.R;

public class SetTotalBudgetActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText etIncome, etBudget;
    private LinearLayout budgetRecommendation;
    private TextView tvRecommendation, btnUseRecommendation;
    private Button btnNext;

    private double monthlyIncome = 0.0;
    private double monthlyBudget = 0.0;
    private double recommendedBudget = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_total_budget);

        initViews();
        setupClickListeners();
        setupTextWatchers();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        etIncome = findViewById(R.id.et_income);
        etBudget = findViewById(R.id.et_budget);
        budgetRecommendation = findViewById(R.id.budget_recommendation);
        tvRecommendation = findViewById(R.id.tv_recommendation);
        btnUseRecommendation = findViewById(R.id.btn_use_recommendation);
        btnNext = findViewById(R.id.btn_next);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnUseRecommendation.setOnClickListener(v -> useRecommendation());

        btnNext.setOnClickListener(v -> proceedToNext());
    }

    private void setupTextWatchers() {
        etIncome.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateIncomeAndRecommendation();
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

    private void updateIncomeAndRecommendation() {
        String incomeText = etIncome.getText().toString().trim();

        if (!incomeText.isEmpty()) {
            try {
                monthlyIncome = Double.parseDouble(incomeText.replace(",", "").replace(".", ""));

                // Calculate recommended budget (70% of income)
                recommendedBudget = monthlyIncome * 0.7;

                // Show recommendation
                showBudgetRecommendation();

            } catch (NumberFormatException e) {
                hideBudgetRecommendation();
            }
        } else {
            hideBudgetRecommendation();
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

    private void showBudgetRecommendation() {
        budgetRecommendation.setVisibility(View.VISIBLE);

        String formattedRecommendation = formatNumber((int) recommendedBudget);
        tvRecommendation.setText("Based on your income, we recommend setting your budget to 70% of your income (Rp" + formattedRecommendation + ")");
    }

    private void hideBudgetRecommendation() {
        budgetRecommendation.setVisibility(View.GONE);
    }

    private void useRecommendation() {
        etBudget.setText(String.valueOf((int) recommendedBudget));
        monthlyBudget = recommendedBudget;
        updateNextButton();

        Toast.makeText(this, "Recommendation applied", Toast.LENGTH_SHORT).show();
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
        // Save to SharedPreferences or database
        getSharedPreferences("budget_prefs", MODE_PRIVATE)
                .edit()
                .putFloat("monthly_income", (float) monthlyIncome)
                .putFloat("monthly_budget", (float) monthlyBudget)
                .putString("setup_date", getCurrentDate())
                .putBoolean("budget_setup_completed", true)
                .apply();

        // Create result intent
        Intent resultIntent = new Intent();
        resultIntent.putExtra("monthly_income", monthlyIncome);
        resultIntent.putExtra("monthly_budget", monthlyBudget);
        resultIntent.putExtra("income_formatted", formatNumber((int) monthlyIncome));
        resultIntent.putExtra("budget_formatted", formatNumber((int) monthlyBudget));

        setResult(RESULT_OK, resultIntent);

        Toast.makeText(this, "Budget setup completed successfully", Toast.LENGTH_SHORT).show();

        // Navigate to main budget screen or dashboard
        finish();
    }

    private String getCurrentDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }

    private String formatNumber(int number) {
        return String.format("%,d", number).replace(",", ".");
    }

    public static class TotalBudgetData {
        public double monthlyIncome;
        public double monthlyBudget;
        public String incomeFormatted;
        public String budgetFormatted;

        public TotalBudgetData(double monthlyIncome, double monthlyBudget, String incomeFormatted, String budgetFormatted) {
            this.monthlyIncome = monthlyIncome;
            this.monthlyBudget = monthlyBudget;
            this.incomeFormatted = incomeFormatted;
            this.budgetFormatted = budgetFormatted;
        }
    }
}