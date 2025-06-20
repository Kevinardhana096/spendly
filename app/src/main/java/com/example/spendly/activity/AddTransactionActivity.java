package com.example.spendly.activity;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spendly.R;
import com.example.spendly.interfaces.OnCategorySelectedListener;
import com.example.spendly.model.Transaction;
import com.example.spendly.repository.BudgetRepository;
import com.example.spendly.repository.TransactionRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddTransactionActivity extends AppCompatActivity {

    private static final String TAG = "AddTransactionActivity";
    private ImageView btnBack;
    private CardView btnExpenses, btnIncome, btnDate;
    private TextView tvDateSelected;
    private EditText etAmount;
    private CardView categoryFood, categoryTransport, categoryShopping, categoryBills;
    private CardView btnAddCustomCategory;
    private Button btnAddMore, btnDone;

    // Data variables
    private String selectedCategory = "Food & Beverages"; // Default
    private String transactionType = "expense"; // Default
    private Date selectedDate = new Date(); // Default to today

    // Custom categories list
    private List<String> customCategories = new ArrayList<>();

    // Repositories
    private TransactionRepository transactionRepository;
    private BudgetRepository budgetRepository;

    // Format for currency display
    private NumberFormat currencyFormatter;

    // Date formatter
    private SimpleDateFormat dateFormatter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        // Initialize repositories
        transactionRepository = TransactionRepository.getInstance(this);
        budgetRepository = BudgetRepository.getInstance(this);

        // Initialize formatters
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        dateFormatter = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());

        initViews();
        setupClickListeners();
        setupAmountFormatting();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnExpenses = findViewById(R.id.btn_expenses);
        btnIncome = findViewById(R.id.btn_income);
        btnDate = findViewById(R.id.btn_date);
        tvDateSelected = findViewById(R.id.tv_date_selected);
        etAmount = findViewById(R.id.et_amount);

        // Categories
        categoryFood = findViewById(R.id.category_food);
        categoryTransport = findViewById(R.id.category_transport);
        categoryShopping = findViewById(R.id.category_shopping);
        categoryBills = findViewById(R.id.category_bills);
        btnAddCustomCategory = findViewById(R.id.btn_add_custom_category);

        btnAddMore = findViewById(R.id.btn_add_more);
        btnDone = findViewById(R.id.btn_done);

        // Set initial date display
        tvDateSelected.setText(dateFormatter.format(selectedDate));
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Transaction type selection
        btnExpenses.setOnClickListener(v -> selectTransactionType("expense"));
        btnIncome.setOnClickListener(v -> selectTransactionType("income"));

        // Date picker
        btnDate.setOnClickListener(v -> showDatePickerDialog());

        // Category selection
        categoryFood.setOnClickListener(v -> selectCategory("Food & Beverages"));
        categoryTransport.setOnClickListener(v -> selectCategory("Transport"));
        categoryShopping.setOnClickListener(v -> selectCategory("Shopping"));
        categoryBills.setOnClickListener(v -> selectCategory("Bills & Utilities"));

        // Custom category button
        btnAddCustomCategory.setOnClickListener(v -> showAddCategoryDialog());

        btnAddMore.setOnClickListener(v -> {
            // Add more transactions
            saveTransaction(true);
        });

        btnDone.setOnClickListener(v -> {
            // Save and finish
            saveTransaction(false);
        });
    }

    private void setupAmountFormatting() {
        // Remove any existing TextWatcher first
        if (etAmount.getTag() instanceof TextWatcher) {
            etAmount.removeTextChangedListener((TextWatcher) etAmount.getTag());
        }

        // Set up a simple TextWatcher with minimal processing
        TextWatcher watcher = new TextWatcher() {
            // Prevent recursive calls
            private boolean isEditing = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Nothing needed here
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Nothing needed here
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isEditing) return;

                try {
                    isEditing = true;

                    String input = s.toString();
                    if (input.isEmpty()) {
                        isEditing = false;
                        return;
                    }

                    // Remove any non-digit characters
                    String digitsOnly = input.replaceAll("[^0-9]", "");

                    // Convert to a number
                    long value = digitsOnly.isEmpty() ? 0 : Long.parseLong(digitsOnly);

                    // Format number with period as thousand separators
                    String formatted = String.format(Locale.getDefault(), "%,d", value)
                            .replace(",", ".");

                    // Only update if the text would actually change
                    if (!formatted.equals(input)) {
                        s.replace(0, s.length(), formatted);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error formatting amount: ", e);
                } finally {
                    isEditing = false;
                }
            }
        };

        // Save the TextWatcher as a tag on the EditText so we can remove it later if needed
        etAmount.setTag(watcher);
        etAmount.addTextChangedListener(watcher);
    }

    private void selectTransactionType(String type) {
        this.transactionType = type;

        // Update UI to reflect the selected type
        if ("expense".equals(type)) {
            btnExpenses.setBackgroundResource(R.drawable.transaction_type_selected_background);
            btnIncome.setBackgroundResource(R.drawable.transaction_type_unselected_background);
            Toast.makeText(this, "Expense transaction selected", Toast.LENGTH_SHORT).show();
        } else {
            btnExpenses.setBackgroundResource(R.drawable.transaction_type_unselected_background);
            btnIncome.setBackgroundResource(R.drawable.transaction_type_selected_background);
            Toast.makeText(this, "Income transaction selected", Toast.LENGTH_SHORT).show();
        }
    }

    private void selectCategory(String category) {
        this.selectedCategory = category;

        // Reset all category backgrounds
        resetCategoryBackgrounds();

        // Update selected category background
        if ("Food & Beverages".equals(category)) {
            categoryFood.setForeground(getDrawable(R.drawable.category_selected_foreground));
        } else if ("Transport".equals(category)) {
            categoryTransport.setForeground(getDrawable(R.drawable.category_selected_foreground));
        } else if ("Shopping".equals(category)) {
            categoryShopping.setForeground(getDrawable(R.drawable.category_selected_foreground));
        } else if ("Bills & Utilities".equals(category)) {
            categoryBills.setForeground(getDrawable(R.drawable.category_selected_foreground));
        }

        Toast.makeText(this, category + " selected", Toast.LENGTH_SHORT).show();
    }

    private void resetCategoryBackgrounds() {
        categoryFood.setForeground(null);
        categoryTransport.setForeground(null);
        categoryShopping.setForeground(null);
        categoryBills.setForeground(null);

        // Reset any custom category views if they exist
        RecyclerView customCategoryRecyclerView = findViewById(R.id.custom_categories_recycler_view);
        if (customCategoryRecyclerView != null) {
            for (int i = 0; i < customCategoryRecyclerView.getChildCount(); i++) {
                View childView = customCategoryRecyclerView.getChildAt(i);
                if (childView instanceof CardView) {
                    childView.setForeground(null);
                }
            }
        }
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(selectedDate);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year1, month1, dayOfMonth) -> {
                    calendar.set(year1, month1, dayOfMonth);
                    selectedDate = calendar.getTime();
                    tvDateSelected.setText(dateFormatter.format(selectedDate));
                },
                year, month, day);

        datePickerDialog.show();
    }

    private void showAddCategoryDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_category);

        EditText etCategoryName = dialog.findViewById(R.id.et_category_name);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel);
        Button btnAdd = dialog.findViewById(R.id.btn_add);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String categoryName = etCategoryName.getText().toString().trim();
            if (!categoryName.isEmpty()) {
                addCustomCategory(categoryName);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void addCustomCategory(String categoryName) {
        // Add to the list of custom categories
        customCategories.add(categoryName);

        // Create or update the custom categories section
        updateCustomCategoriesUI();

        // Select this new category
        selectCategory(categoryName);
    }

    private void updateCustomCategoriesUI() {
        // Find or create the RecyclerView for custom categories
        RecyclerView customCategoryRecyclerView = findViewById(R.id.custom_categories_recycler_view);

        if (customCategoryRecyclerView == null) {
            // This is the first custom category, we need to inflate the layout
            View customCategoriesSection = getLayoutInflater().inflate(
                    R.layout.layout_custom_categories,
                    findViewById(R.id.categories_container),
                    true);

            customCategoryRecyclerView = customCategoriesSection.findViewById(R.id.custom_categories_recycler_view);
            customCategoryRecyclerView.setLayoutManager(new LinearLayoutManager(
                    this, LinearLayoutManager.HORIZONTAL, false));
        }

        // Create or update the adapter
        CustomCategoryAdapter adapter = (CustomCategoryAdapter) customCategoryRecyclerView.getAdapter();
        if (adapter == null) {
            adapter = new CustomCategoryAdapter(customCategories, category -> selectCategory(category));
            customCategoryRecyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }

        // Make sure the section is visible
        findViewById(R.id.custom_categories_section).setVisibility(View.VISIBLE);
    }

    private void saveTransaction(boolean addMore) {
        String amountText = etAmount.getText().toString().trim();

        if (amountText.isEmpty()) {
            Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Parse amount from formatted string - just remove all non-numeric chars
            String cleanString = amountText.replaceAll("[^0-9]", "");
            double amount = Double.parseDouble(cleanString);

            // Create transaction object without account type
            Transaction transaction = new Transaction(
                    FirebaseAuth.getInstance().getUid(),
                    amount,
                    selectedCategory,
                    "", // Empty account type since we removed account selection
                    selectedDate,
                    transactionType
            );

            // Format as currency for display
            String formattedAmount = currencyFormatter.format(amount);
            transaction.setFormattedAmount(formattedAmount);

            // Save transaction and update budget
            transactionRepository.saveTransaction(transaction, new TransactionRepository.TransactionCallback() {
                @Override
                public void onSuccess(Map<String, Object> data) {
                    // Now update the budget for this transaction
                    transactionRepository.updateBudgetForTransaction(
                            transaction,
                            budgetRepository,
                            new TransactionRepository.TransactionCallback() {
                                @Override
                                public void onSuccess(Map<String, Object> data) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(AddTransactionActivity.this,
                                                "Transaction saved and budget updated",
                                                Toast.LENGTH_SHORT).show();

                                        if (!addMore) {
                                            finish();
                                        } else {
                                            // Clear form for next transaction
                                            etAmount.setText("");
                                            // Reset to default date (today)
                                            selectedDate = new Date();
                                            tvDateSelected.setText(dateFormatter.format(selectedDate));
                                            // Reset category selection
                                            resetCategoryBackgrounds();
                                            selectedCategory = "Food & Beverages";
                                            selectCategory(selectedCategory);
                                        }
                                    });
                                }

                                @Override
                                public void onError(Exception e) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(AddTransactionActivity.this,
                                                "Transaction saved but budget update failed",
                                                Toast.LENGTH_SHORT).show();

                                        if (!addMore) {
                                            finish();
                                        }
                                    });
                                }
                            });
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        Toast.makeText(AddTransactionActivity.this,
                                "Error saving transaction: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount format", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Adapter for custom categories
     */
    private class CustomCategoryAdapter extends RecyclerView.Adapter<CustomCategoryAdapter.ViewHolder> {

        private List<String> categories;
        private OnCategorySelectedListener listener;

        CustomCategoryAdapter(List<String> categories, OnCategorySelectedListener listener) {
            this.categories = categories;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_custom_category, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.bind(categories.get(position));
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            CardView cardView;
            TextView textView;

            ViewHolder(View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.category_card);
                textView = itemView.findViewById(R.id.category_name);
            }

            void bind(String category) {
                textView.setText(category);
                cardView.setOnClickListener(v -> listener.onCategorySelected(category));
            }
        }
    }
}
