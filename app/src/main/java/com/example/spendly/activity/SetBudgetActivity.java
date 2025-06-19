package com.example.spendly.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.spendly.R;

public class SetBudgetActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Spinner spinnerCategory;
    private LinearLayout categoryTransportation, categoryShopping, categoryHealth, btnAddNewCategory;
    private EditText etTotalBudget;
    private TextView tvRemainingBudget;
    private Button btnAddBudget;

    private String selectedCategory = "Food & Beverages";
    private double totalBudget = 0.0;
    private double existingBudget = 500000.0; // Example existing budget

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_budget);

        initViews();
        setupSpinner();
        setupClickListeners();
        setupTextWatcher();
        updateRemainingBudget();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        spinnerCategory = findViewById(R.id.spinner_category);
        categoryTransportation = findViewById(R.id.category_transportation);
        categoryShopping = findViewById(R.id.category_shopping);
        categoryHealth = findViewById(R.id.category_health);
        btnAddNewCategory = findViewById(R.id.btn_add_new_category);
        etTotalBudget = findViewById(R.id.et_total_budget);
        tvRemainingBudget = findViewById(R.id.tv_remaining_budget);
        btnAddBudget = findViewById(R.id.btn_add_budget);
    }

    private void setupSpinner() {
        String[] categories = {"Food & Beverages", "Transportation", "Shopping", "Health and Sport"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        categoryTransportation.setOnClickListener(v -> selectCategory("Transportation"));
        categoryShopping.setOnClickListener(v -> selectCategory("Shopping"));
        categoryHealth.setOnClickListener(v -> selectCategory("Health and Sport"));

        btnAddNewCategory.setOnClickListener(v -> showAddCategoryDialog());

        btnAddBudget.setOnClickListener(v -> saveBudget());
    }

    private void setupTextWatcher() {
        etTotalBudget.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateBudgetButton();
                updateRemainingBudget();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void selectCategory(String category) {
        selectedCategory = category;

        // Update spinner selection
        String[] categories = {"Food & Beverages", "Transportation", "Shopping", "Health and Sport"};
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(category)) {
                spinnerCategory.setSelection(i);
                break;
            }
        }

        Toast.makeText(this, category + " selected", Toast.LENGTH_SHORT).show();
    }

    private void updateBudgetButton() {
        String budgetText = etTotalBudget.getText().toString().trim();

        if (!budgetText.isEmpty()) {
            try {
                totalBudget = Double.parseDouble(budgetText.replace(",", "").replace(".", ""));
                btnAddBudget.setEnabled(true);
                btnAddBudget.setAlpha(1.0f);
            } catch (NumberFormatException e) {
                btnAddBudget.setEnabled(false);
                btnAddBudget.setAlpha(0.5f);
            }
        } else {
            btnAddBudget.setEnabled(false);
            btnAddBudget.setAlpha(0.5f);
        }
    }

    private void updateRemainingBudget() {
        String budgetText = etTotalBudget.getText().toString().trim();

        if (!budgetText.isEmpty()) {
            try {
                double enteredBudget = Double.parseDouble(budgetText.replace(",", "").replace(".", ""));
                double remaining = existingBudget - enteredBudget;

                if (remaining >= 0) {
                    tvRemainingBudget.setText("Rp" + formatNumber((int) remaining));
                    tvRemainingBudget.setTextColor(getResources().getColor(R.color.black));
                } else {
                    tvRemainingBudget.setText("Rp" + formatNumber((int) Math.abs(remaining)) + " over budget");
                    tvRemainingBudget.setTextColor(getResources().getColor(R.color.red_primary));
                }
            } catch (NumberFormatException e) {
                tvRemainingBudget.setText("Rp" + formatNumber((int) existingBudget));
                tvRemainingBudget.setTextColor(getResources().getColor(R.color.black));
            }
        } else {
            tvRemainingBudget.setText("Rp" + formatNumber((int) existingBudget));
            tvRemainingBudget.setTextColor(getResources().getColor(R.color.black));
        }
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Category");

        final EditText input = new EditText(this);
        input.setHint("Enter category name");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String categoryName = input.getText().toString().trim();
            if (!categoryName.isEmpty()) {
                addNewCategory(categoryName);
                Toast.makeText(this, "Category added: " + categoryName, Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void addNewCategory(String categoryName) {
        // Add new category to database/shared preferences
        // Update spinner with new category
        String[] currentCategories = {"Food & Beverages", "Transportation", "Shopping", "Health and Sport", categoryName};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currentCategories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        // Select the new category
        spinnerCategory.setSelection(currentCategories.length - 1);
        selectedCategory = categoryName;
    }

    private void saveBudget() {
        if (totalBudget <= 0) {
            Toast.makeText(this, "Please enter a valid budget amount", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get selected category from spinner
        String spinnerCategory = this.spinnerCategory.getSelectedItem().toString();

        // Save budget to database/shared preferences
        // For now, just show success message

        // Create result intent
        Intent resultIntent = new Intent();
        resultIntent.putExtra("category", spinnerCategory);
        resultIntent.putExtra("budget_amount", totalBudget);
        resultIntent.putExtra("budget_formatted", formatNumber((int) totalBudget));

        setResult(RESULT_OK, resultIntent);

        Toast.makeText(this, "Budget set for " + spinnerCategory + ": Rp" + formatNumber((int) totalBudget), Toast.LENGTH_SHORT).show();
        finish();
    }

    private String formatNumber(int number) {
        return String.format("%,d", number).replace(",", ".");
    }

    public static class BudgetData {
        public String category;
        public double amount;
        public String formattedAmount;

        public BudgetData(String category, double amount, String formattedAmount) {
            this.category = category;
            this.amount = amount;
            this.formattedAmount = formattedAmount;
        }
    }
}