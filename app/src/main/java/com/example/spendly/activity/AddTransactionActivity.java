package com.example.spendly.activity;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spendly.R;
import com.example.spendly.interfaces.OnCategorySelectedListener;
import com.example.spendly.model.BudgetCategory;
import com.example.spendly.model.Transaction;
import com.example.spendly.repository.BudgetRepository;
import com.example.spendly.repository.TransactionRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddTransactionActivity extends AppCompatActivity {

    private static final String TAG = "AddTransactionActivity";
    private ImageView btnBack;
    private CardView btnExpenses, btnIncome, btnDate;
    private TextView tvDateSelected;
    private EditText etAmount;
    private Button btnAddMore, btnDone;

    // Data variables
    private String selectedCategory = "Food & Beverages"; // Default
    private String transactionType = "expense"; // Default
    private Date selectedDate = new Date(); // Default to today

    // Budget categories
    private List<BudgetCategory> budgetCategories = new ArrayList<>();
    private RecyclerView budgetCategoriesRecyclerView;
    private BudgetCategoryAdapter budgetCategoryAdapter;

    // Repositories
    private TransactionRepository transactionRepository;
    private BudgetRepository budgetRepository;

    // Format for currency display
    private NumberFormat currencyFormatter;

    // Date formatter
    private SimpleDateFormat dateFormatter;

    // PIN verification dialog components
    private Dialog pinVerificationDialog;
    private View[] pinDots;
    private TextView[] numberButtons;
    private ImageView btnDelete, btnCancel;
    private StringBuilder enteredPin = new StringBuilder();
    private boolean isPinRequired = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        // Check if PIN verification is required
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        isPinRequired = prefs.getBoolean("pin_set", false);

        // Initialize repositories
        transactionRepository = TransactionRepository.getInstance(this);
        budgetRepository = BudgetRepository.getInstance(this);

        // Initialize formatters
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        dateFormatter = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());

        initViews();
        setupClickListeners();
        setupAmountFormatting();

        // Load budget categories from the repository
        loadBudgetCategories();
    }

    /**
     * Load budget categories from the repository
     */
    private void loadBudgetCategories() {
        // Check if budget categories exist
        budgetRepository.checkCategoriesExist(new BudgetRepository.BudgetCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                boolean categoriesExist = data.containsKey("exists") && (boolean) data.get("exists");

                if (categoriesExist) {
                    // Load categories data
                    budgetRepository.getBudgetCategories(new BudgetRepository.BudgetCallback() {
                        @Override
                        public void onSuccess(Map<String, Object> categoriesData) {
                            // Convert the categories data to a list of BudgetCategory objects
                            processBudgetCategories(categoriesData);
                        }

                        @Override
                        public void onError(Exception e) {
                            // Use default categories if error
                            Log.e(TAG, "Error loading budget categories: " + e.getMessage());
                            Toast.makeText(AddTransactionActivity.this,
                                "Failed to load budget categories. Using default categories.",
                                Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Log.i(TAG, "No budget categories found, using default categories");
                    // Add some default categories
                    addDefaultCategories();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error checking budget categories: " + e.getMessage());
                addDefaultCategories();
            }
        });
    }

    /**
     * Add default budget categories if none exist
     */
    private void addDefaultCategories() {
        budgetCategories.clear();

        // Add default categories
        budgetCategories.add(new BudgetCategory("Food & Beverages", "", 0, 0, "0", "0", new Date().toString()));
        budgetCategories.add(new BudgetCategory("Transport", "", 0, 0, "0", "0", new Date().toString()));
        budgetCategories.add(new BudgetCategory("Shopping", "", 0, 0, "0", "0", new Date().toString()));
        budgetCategories.add(new BudgetCategory("Bills & Utilities", "", 0, 0, "0", "0", new Date().toString()));
        budgetCategories.add(new BudgetCategory("Health", "", 0, 0, "0", "0", new Date().toString()));
        budgetCategories.add(new BudgetCategory("Entertainment", "", 0, 0, "0", "0", new Date().toString()));
        budgetCategories.add(new BudgetCategory("Education", "", 0, 0, "0", "0", new Date().toString()));
        budgetCategories.add(new BudgetCategory("Other", "", 0, 0, "0", "0", new Date().toString()));

        // Setup the RecyclerView with default categories
        setupBudgetCategoriesRecyclerView();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnExpenses = findViewById(R.id.btn_expenses);
        btnIncome = findViewById(R.id.btn_income);
        btnDate = findViewById(R.id.btn_date);
        tvDateSelected = findViewById(R.id.tv_date_selected);
        etAmount = findViewById(R.id.et_amount);
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

    private void saveTransaction(boolean addMore) {
        String amountText = etAmount.getText().toString().trim();

        if (amountText.isEmpty()) {
            Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategory == null || selectedCategory.isEmpty()) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
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

            // Check if PIN verification is required before saving the transaction
            if (isPinRequired) {
                showPinVerificationDialog(transaction, addMore);
            } else {
                // If PIN is not required, save transaction directly
                processSaveTransaction(transaction, addMore);
            }

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount format", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show PIN verification dialog before saving transaction
     */
    private void showPinVerificationDialog(Transaction transaction, boolean addMore) {
        pinVerificationDialog = new Dialog(this, R.style.DialogFullWidth);
        pinVerificationDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        pinVerificationDialog.setContentView(R.layout.dialog_verify_pin);
        pinVerificationDialog.setCancelable(false);

        // Initialize PIN dots
        pinDots = new View[6];
        pinDots[0] = pinVerificationDialog.findViewById(R.id.pin_dot_1);
        pinDots[1] = pinVerificationDialog.findViewById(R.id.pin_dot_2);
        pinDots[2] = pinVerificationDialog.findViewById(R.id.pin_dot_3);
        pinDots[3] = pinVerificationDialog.findViewById(R.id.pin_dot_4);
        pinDots[4] = pinVerificationDialog.findViewById(R.id.pin_dot_5);
        pinDots[5] = pinVerificationDialog.findViewById(R.id.pin_dot_6);

        // Initialize number buttons
        numberButtons = new TextView[10];
        numberButtons[0] = pinVerificationDialog.findViewById(R.id.btn_0);
        numberButtons[1] = pinVerificationDialog.findViewById(R.id.btn_1);
        numberButtons[2] = pinVerificationDialog.findViewById(R.id.btn_2);
        numberButtons[3] = pinVerificationDialog.findViewById(R.id.btn_3);
        numberButtons[4] = pinVerificationDialog.findViewById(R.id.btn_4);
        numberButtons[5] = pinVerificationDialog.findViewById(R.id.btn_5);
        numberButtons[6] = pinVerificationDialog.findViewById(R.id.btn_6);
        numberButtons[7] = pinVerificationDialog.findViewById(R.id.btn_7);
        numberButtons[8] = pinVerificationDialog.findViewById(R.id.btn_8);
        numberButtons[9] = pinVerificationDialog.findViewById(R.id.btn_9);

        btnDelete = pinVerificationDialog.findViewById(R.id.btn_delete);
        btnCancel = pinVerificationDialog.findViewById(R.id.btn_cancel);

        // Set up click listeners for number buttons
        for (int i = 0; i < numberButtons.length; i++) {
            final int number = i;
            numberButtons[i].setOnClickListener(v -> onPinNumberClicked(number, transaction, addMore));
        }

        // Set up delete button
        btnDelete.setOnClickListener(v -> {
            if (enteredPin.length() > 0) {
                enteredPin.deleteCharAt(enteredPin.length() - 1);
                updatePinDots();
            }
        });

        // Set up cancel button
        btnCancel.setOnClickListener(v -> {
            enteredPin.setLength(0);
            pinVerificationDialog.dismiss();
        });

        // Reset pin entry
        enteredPin.setLength(0);
        updatePinDots();

        pinVerificationDialog.show();
    }

    /**
     * Handle pin number button click
     */
    private void onPinNumberClicked(int number, Transaction transaction, boolean addMore) {
        if (enteredPin.length() < 6) {
            enteredPin.append(number);
            updatePinDots();

            // Automatically verify when 6 digits are entered
            if (enteredPin.length() == 6) {
                verifyPin(transaction, addMore);
            }
        }
    }

    /**
     * Update the PIN dots display based on entered PIN length
     */
    private void updatePinDots() {
        for (int i = 0; i < pinDots.length; i++) {
            if (i < enteredPin.length()) {
                pinDots[i].setBackgroundResource(R.drawable.pin_dot_filled);
            } else {
                pinDots[i].setBackgroundResource(R.drawable.pin_dot_empty);
            }
        }
    }

    /**
     * Verify the entered PIN against the saved PIN
     */
    private void verifyPin(Transaction transaction, boolean addMore) {
        String pin = enteredPin.toString();
        String savedPin = getSavedPinHash();

        // If no PIN is saved yet, default to allow transaction
        if (savedPin.isEmpty()) {
            enteredPin.setLength(0);
            pinVerificationDialog.dismiss();
            processSaveTransaction(transaction, addMore);
            return;
        }

        // Use the verification method from SetPinCodeActivity
        boolean isCorrect = SetPinCodeActivity.verifyPin(pin, savedPin);

        if (isCorrect) {
            // PIN is correct, proceed with saving transaction
            enteredPin.setLength(0);
            pinVerificationDialog.dismiss();
            processSaveTransaction(transaction, addMore);
        } else {
            // PIN is incorrect, show error and clear entry
            Toast.makeText(this, "Incorrect PIN. Please try again.", Toast.LENGTH_SHORT).show();
            enteredPin.setLength(0);
            updatePinDots();
        }
    }

    /**
     * Get the saved PIN hash from SharedPreferences
     */
    private String getSavedPinHash() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        return prefs.getString("user_pin", "");
    }

    /**
     * Process saving the transaction after PIN verification
     */
    private void processSaveTransaction(Transaction transaction, boolean addMore) {
        // Save transaction and update budget
        transactionRepository.saveTransaction(transaction, new TransactionRepository.TransactionCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                // Update user's available balance
                updateUserBalance(transaction, new TransactionRepository.TransactionCallback() {
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
                                                selectedCategory = "Food & Beverages";
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
                                    "Transaction saved but balance update failed: " + e.getMessage(),
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
    }

    /**
     * Update the user's available balance based on the transaction
     */
    private void updateUserBalance(Transaction transaction, final TransactionRepository.TransactionCallback callback) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            callback.onError(new Exception("User not logged in"));
            return;
        }

        // Get the current balance first
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            callback.onError(new Exception("User not logged in"));
            return;
        }

        FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get current balance
                        Double currentBalance = documentSnapshot.getDouble("currentBalance");
                        if (currentBalance == null) currentBalance = 0.0;

                        // Calculate new balance based on transaction type
                        double newBalance;
                        if ("expense".equalsIgnoreCase(transaction.getType())) {
                            newBalance = currentBalance - transaction.getAmount();
                        } else { // Income transaction
                            newBalance = currentBalance + transaction.getAmount();
                        }

                        // Update the balance in Firestore
                        FirebaseFirestore.getInstance().collection("users")
                                .document(userId)
                                .update("currentBalance", newBalance)
                                .addOnSuccessListener(aVoid -> {
                                    Map<String, Object> result = new HashMap<>();
                                    result.put("newBalance", newBalance);
                                    callback.onSuccess(result);
                                })
                                .addOnFailureListener(e -> callback.onError(e));
                    } else {
                        callback.onError(new Exception("User document not found"));
                    }
                })
                .addOnFailureListener(e -> callback.onError(e));
    }

    /**
     * Process budget categories data and update UI
     */
    private void processBudgetCategories(Map<String, Object> categoriesData) {
        budgetCategories.clear();

        // Skip if no categories or contains only offline flag
        if (categoriesData.isEmpty() ||
           (categoriesData.size() == 1 && categoriesData.containsKey("offline_only"))) {
            addDefaultCategories();
            return;
        }

        // Convert Firestore data to BudgetCategory objects
        for (Map.Entry<String, Object> entry : categoriesData.entrySet()) {
            String categoryName = entry.getKey();
            if (categoryName.equals("offline_only")) continue;

            @SuppressWarnings("unchecked")
            Map<String, Object> categoryData = (Map<String, Object>) entry.getValue();

            double amount = 0;
            double spent = 0;
            String formattedAmount = "0";
            String formattedSpent = "0";
            String dateAdded = new Date().toString();

            if (categoryData.containsKey("amount")) {
                if (categoryData.get("amount") instanceof Double) {
                    amount = (Double) categoryData.get("amount");
                } else if (categoryData.get("amount") instanceof Long) {
                    amount = ((Long) categoryData.get("amount")).doubleValue();
                }
            }

            if (categoryData.containsKey("spent")) {
                if (categoryData.get("spent") instanceof Double) {
                    spent = (Double) categoryData.get("spent");
                } else if (categoryData.get("spent") instanceof Long) {
                    spent = ((Long) categoryData.get("spent")).doubleValue();
                }
            }

            if (categoryData.containsKey("formatted_amount")) {
                formattedAmount = categoryData.get("formatted_amount").toString();
            }

            if (categoryData.containsKey("formatted_spent")) {
                formattedSpent = categoryData.get("formatted_spent").toString();
            }

            if (categoryData.containsKey("date_added")) {
                dateAdded = categoryData.get("date_added").toString();
            }

            BudgetCategory category = new BudgetCategory(
                categoryName,
                "", // Will set icon based on name in the adapter
                amount,
                spent,
                formattedAmount,
                formattedSpent,
                dateAdded
            );

            budgetCategories.add(category);
        }

        // If we have budget categories, set up the recycler view
        if (!budgetCategories.isEmpty()) {
            setupBudgetCategoriesRecyclerView();
        } else {
            addDefaultCategories();
        }
    }

    /**
     * Set up the RecyclerView for budget categories
     */
    private void setupBudgetCategoriesRecyclerView() {
        // Find or create the RecyclerView for budget categories
        if (budgetCategoriesRecyclerView == null) {
            android.widget.LinearLayout categoriesContainer = findViewById(R.id.categories_container);

            // First check if there is already a budget categories recycler view
            budgetCategoriesRecyclerView = findViewById(R.id.budget_categories_recycler_view);

            if (budgetCategoriesRecyclerView == null) {
                // Inflate the budget categories section
                View budgetCategoriesSection = getLayoutInflater().inflate(
                        R.layout.layout_budget_categories,
                        categoriesContainer,
                        true);

                budgetCategoriesRecyclerView = budgetCategoriesSection.findViewById(R.id.budget_categories_recycler_view);
            }

            // Use GridLayoutManager with 3 columns
            int numberOfColumns = 3;
            budgetCategoriesRecyclerView.setLayoutManager(new GridLayoutManager(
                    this, numberOfColumns));
        }

        // Create new adapter with our budget categories
        budgetCategoryAdapter = new BudgetCategoryAdapter(budgetCategories,
            categoryName -> {
                selectedCategory = categoryName;
                Toast.makeText(this, categoryName + " selected", Toast.LENGTH_SHORT).show();
            });
        budgetCategoriesRecyclerView.setAdapter(budgetCategoryAdapter);

        // Make sure the section is visible
        View budgetCategoriesSection = findViewById(R.id.budget_categories_section);
        if (budgetCategoriesSection != null) {
            budgetCategoriesSection.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Adapter for budget categories
     */
    private class BudgetCategoryAdapter extends RecyclerView.Adapter<BudgetCategoryAdapter.ViewHolder> {

        private List<BudgetCategory> categories;
        private OnCategorySelectedListener listener;
        private int selectedPosition = -1;

        public BudgetCategoryAdapter(List<BudgetCategory> categories, OnCategorySelectedListener listener) {
            this.categories = categories;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_budget_category, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.bind(categories.get(position), position == selectedPosition);
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            CardView cardView;
            TextView textView;
            ImageView imageView;

            ViewHolder(View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.category_card);
                textView = itemView.findViewById(R.id.category_name);
                imageView = itemView.findViewById(R.id.category_icon);
            }

            void bind(BudgetCategory category, boolean isSelected) {
                textView.setText(category.getCategoryName());
                setCategoryIcon(imageView, category.getCategoryName());

                if (isSelected) {
                    cardView.setForeground(getDrawable(R.drawable.category_selected_foreground));
                } else {
                    cardView.setForeground(null);
                }

                cardView.setOnClickListener(v -> {
                    int oldPosition = selectedPosition;
                    selectedPosition = getAdapterPosition();

                    // Update UI
                    notifyItemChanged(oldPosition);
                    notifyItemChanged(selectedPosition);

                    // Notify listener
                    listener.onCategorySelected(category.getCategoryName());
                });
            }

            private void setCategoryIcon(ImageView imageView, String categoryName) {
                int iconRes = R.drawable.ic_food;
                int colorRes = R.color.orange_primary;

                if (categoryName.contains("Food") || categoryName.contains("food")) {
                    iconRes = R.drawable.ic_food;
                    colorRes = R.color.orange_primary;
                } else if (categoryName.contains("Transport") || categoryName.contains("transport")) {
                    iconRes = R.drawable.ic_transportation;
                    colorRes = R.color.blue_primary;
                } else if (categoryName.contains("Shop") || categoryName.contains("shop")) {
                    iconRes = R.drawable.ic_shopping;
                    colorRes = R.color.pink_primary;
                } else if (categoryName.contains("Bill") || categoryName.contains("bill") ||
                           categoryName.contains("Util") || categoryName.contains("util")) {
                    iconRes = R.drawable.ic_bills;
                    colorRes = R.color.purple_primary;
                } else if (categoryName.contains("Health") || categoryName.contains("health")) {
                    iconRes = R.drawable.ic_health;
                    colorRes = R.color.green_primary;
                } else {
                    iconRes = R.drawable.ic_other;
                    colorRes = R.color.purple_primary;
                }

                imageView.setImageResource(iconRes);
                imageView.setColorFilter(ContextCompat.getColor(AddTransactionActivity.this, colorRes));
            }
        }
    }
}
