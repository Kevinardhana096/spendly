package com.example.spendly.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.spendly.R;
import com.example.spendly.repository.BudgetRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SetBudgetActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Spinner spinnerCategory;
    private LinearLayout categoryTransportation, categoryShopping, categoryHealth, btnAddNewCategory;
    private EditText etTotalBudget;
    private TextView tvRemainingBudget;
    private Button btnAddBudget;
    private ProgressBar progressBar;

    // New fields for improved dropdown
    private LinearLayout selectedCategoryContainer;
    private LinearLayout categoriesList;
    private TextView selectedCategoryName;
    private ImageView selectedCategoryIcon;
    private ImageView dropdownIcon;
    private LinearLayout categoryFood, categoryBills;
    private boolean isDropdownOpen = false;

    private String selectedCategory = "Food & Beverages";
    private double totalBudget = 0.0;
    private double remainingBudget = 0.0; // From total monthly budget
    private double monthlyBudget = 0.0;   // Total monthly budget

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private BudgetRepository budgetRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_budget);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        budgetRepository = BudgetRepository.getInstance(this);

        // Get data from intent
        if (getIntent().hasExtra("monthly_budget")) {
            monthlyBudget = getIntent().getDoubleExtra("monthly_budget", 0.0);
        }

        if (getIntent().hasExtra("remaining_budget")) {
            remainingBudget = getIntent().getDoubleExtra("remaining_budget", 0.0);
        } else {
            remainingBudget = monthlyBudget;
        }

        initViews();
        setupSpinner();
        setupClickListeners();
        setupTextWatcher();
        updateRemainingBudget();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        spinnerCategory = findViewById(R.id.spinner_category);

        // Initialize new dropdown UI elements
        selectedCategoryContainer = findViewById(R.id.selected_category_container);
        categoriesList = findViewById(R.id.categories_list);
        selectedCategoryName = findViewById(R.id.selected_category_name);
        selectedCategoryIcon = findViewById(R.id.selected_category_icon);
        dropdownIcon = findViewById(R.id.dropdown_icon);

        categoryFood = findViewById(R.id.category_food);
        categoryTransportation = findViewById(R.id.category_transportation);
        categoryShopping = findViewById(R.id.category_shopping);
        categoryHealth = findViewById(R.id.category_health);
        categoryBills = findViewById(R.id.category_bills);

        btnAddNewCategory = findViewById(R.id.btn_add_new_category);
        etTotalBudget = findViewById(R.id.et_total_budget);
        tvRemainingBudget = findViewById(R.id.tv_remaining_budget);
        btnAddBudget = findViewById(R.id.btn_add_budget);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupSpinner() {
        String[] categories = {"Food & Beverages", "Transportation", "Shopping", "Health and Sport", "Bills & Utilities"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        // Set default category in dropdown UI
        selectedCategoryName.setText(selectedCategory);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Custom dropdown toggle
        selectedCategoryContainer.setOnClickListener(v -> toggleCategoryDropdown());

        // Category selection click listeners
        categoryFood.setOnClickListener(v -> selectCategory("Food & Beverages"));
        categoryTransportation.setOnClickListener(v -> selectCategory("Transportation"));
        categoryShopping.setOnClickListener(v -> selectCategory("Shopping"));
        categoryHealth.setOnClickListener(v -> selectCategory("Health and Sport"));
        categoryBills.setOnClickListener(v -> selectCategory("Bills & Utilities"));

        btnAddNewCategory.setOnClickListener(v -> showAddCategoryDialog());

        btnAddBudget.setOnClickListener(v -> saveBudget());
    }

    /**
     * Toggle the category dropdown visibility
     */
    private void toggleCategoryDropdown() {
        isDropdownOpen = !isDropdownOpen;

        // Show or hide the categories list
        categoriesList.setVisibility(isDropdownOpen ? View.VISIBLE : View.GONE);

        // Rotate dropdown arrow icon
        float startDegrees = isDropdownOpen ? 0f : 180f;
        float endDegrees = isDropdownOpen ? 180f : 0f;

        RotateAnimation rotateAnimation = new RotateAnimation(
                startDegrees, endDegrees,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );

        rotateAnimation.setDuration(300);
        rotateAnimation.setFillAfter(true);
        dropdownIcon.startAnimation(rotateAnimation);
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

        // Update visual state of dropdown
        selectedCategoryName.setText(category);

        // Update category icon based on selection
        int iconRes = R.drawable.ic_food;
        int colorRes = R.color.orange_primary;

        switch (category) {
            case "Food & Beverages":
                iconRes = R.drawable.ic_food;
                colorRes = R.color.orange_primary;
                break;
            case "Transportation":
                iconRes = R.drawable.ic_transportation;
                colorRes = R.color.blue_primary;
                break;
            case "Shopping":
                iconRes = R.drawable.ic_shopping;
                colorRes = R.color.pink_primary;
                break;
            case "Health and Sport":
                iconRes = R.drawable.ic_health;
                colorRes = R.color.green_primary;
                break;
            case "Bills & Utilities":
                iconRes = R.drawable.ic_bills;
                colorRes = R.color.purple_primary;
                break;
        }

        selectedCategoryIcon.setImageResource(iconRes);
        selectedCategoryIcon.setColorFilter(getResources().getColor(colorRes));

        // Update spinner selection for backward compatibility
        String[] categories = {"Food & Beverages", "Transportation", "Shopping", "Health and Sport", "Bills & Utilities"};
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(category)) {
                spinnerCategory.setSelection(i);
                break;
            }
        }

        // Close dropdown after selection
        if (isDropdownOpen) {
            toggleCategoryDropdown();
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
                double remaining = remainingBudget - enteredBudget;

                if (remaining >= 0) {
                    tvRemainingBudget.setText("Rp" + formatNumber((int) remaining));
                    tvRemainingBudget.setTextColor(getResources().getColor(R.color.black));
                } else {
                    tvRemainingBudget.setText("Rp" + formatNumber((int) Math.abs(remaining)) + " over budget");
                    tvRemainingBudget.setTextColor(getResources().getColor(R.color.red_primary));
                }
            } catch (NumberFormatException e) {
                tvRemainingBudget.setText("Rp" + formatNumber((int) remainingBudget));
                tvRemainingBudget.setTextColor(getResources().getColor(R.color.black));
            }
        } else {
            tvRemainingBudget.setText("Rp" + formatNumber((int) remainingBudget));
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

        // Validate if entered budget doesn't exceed remaining budget
        if (totalBudget > remainingBudget) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Budget Exceeded");
            builder.setMessage("The budget amount exceeds your remaining monthly budget. Do you want to adjust your total monthly budget?");

            builder.setPositiveButton("Yes, Adjust", (dialog, which) -> {
                // Navigate back to SetTotalBudgetActivity
                Intent intent = new Intent(this, SetTotalBudgetActivity.class);
                startActivity(intent);
                finish();
            });

            builder.setNegativeButton("No, I'll Change This", (dialog, which) -> dialog.dismiss());

            builder.show();
            return;
        }

        // Get selected category from spinner
        String category = spinnerCategory.getSelectedItem().toString();

        // Save to Firebase and SQLite
        saveToRepository(category, totalBudget);
    }

    private void saveToRepository(String category, double amount) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You need to be logged in to set a budget", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading indicator
        progressBar.setVisibility(View.VISIBLE);
        btnAddBudget.setEnabled(false);

        String formattedAmount = formatNumber((int) amount);

        // Create category budget data
        Map<String, Object> categoryData = new HashMap<>();
        categoryData.put("amount", amount);
        categoryData.put("formatted_amount", formattedAmount);
        // Initialize spent to 0 (important - don't set it to the budget amount)
        categoryData.put("spent", 0.0);
        categoryData.put("formatted_spent", "0");
        categoryData.put("date_added", new Date().toString());

        // Save using repository pattern
        budgetRepository.saveBudgetCategory(category, categoryData, new BudgetRepository.BudgetCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                // Also update the remaining budget
                updateRemainingBudgetInRepository(remainingBudget - amount);
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                btnAddBudget.setEnabled(true);
                Toast.makeText(SetBudgetActivity.this,
                        "Failed to save category: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRemainingBudgetInRepository(final double newRemainingBudget) {
        String formattedRemaining = formatNumber((int) newRemainingBudget);

        // Instead of updating the remaining budget value, just check budget completion
        // The remaining budget won't change when setting up category budgets
        checkBudgetCompletion();

        progressBar.setVisibility(View.GONE);
        Toast.makeText(SetBudgetActivity.this,
                "Budget category added successfully",
                Toast.LENGTH_SHORT).show();
    }

    private void checkBudgetCompletion() {
        // Mark budget setup as complete using the repository
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("users").document(userId)
                    .update("budget_setup_completed", true)
                    .addOnCompleteListener(task -> {
                        // Show success message before finishing
                        Toast.makeText(this, "Budget category added successfully", Toast.LENGTH_SHORT).show();

                        // Simply finish this activity to return to BudgetFragment
                        finish();
                    });
        } else {
            Toast.makeText(this, "Budget category added successfully", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private String formatNumber(int number) {
        return String.format("%,d", number).replace(",", ".");
    }
}
