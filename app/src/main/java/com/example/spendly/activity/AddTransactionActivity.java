package com.example.spendly.activity;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
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
import android.widget.LinearLayout;
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
    private String selectedCategory = "General"; // Default fallback category
    private String transactionType = "expense"; // Default
    private Date selectedDate = new Date(); // Default to today

    // Budget categories
    private List<BudgetCategory> budgetCategories = new ArrayList<>();
    private RecyclerView budgetCategoriesRecyclerView;
    private BudgetCategoryAdapter budgetCategoryAdapter;

    // Category section views
    private LinearLayout categoriesContainer;
    private View emptyStateView;
    private TextView tvEmptyStateTitle, tvEmptyStateSubtitle;
    private Button btnSetupBudget;

    // Budget state
    private boolean hasBudgetCategories = false;

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
    private TextView tvPinTitle;

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
                            // Show empty state if error loading categories
                            Log.e(TAG, "Error loading budget categories: " + e.getMessage());
                            showEmptyState();
                        }
                    });
                } else {
                    Log.i(TAG, "No budget categories found, showing empty state");
                    showEmptyState();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error checking budget categories: " + e.getMessage());
                showEmptyState();
            }
        });
    }

    /**
     * Show empty state when no budget categories are available
     */
    private void showEmptyState() {
        runOnUiThread(() -> {
            hasBudgetCategories = false;

            // Hide categories container
            if (categoriesContainer != null) {
                categoriesContainer.setVisibility(View.GONE);
            }

            // Show empty state
            if (emptyStateView != null) {
                emptyStateView.setVisibility(View.VISIBLE);
            }

            // Set default category for transactions without budget
            selectedCategory = "General";
        });
    }

    /**
     * Show budget categories section
     */
    private void showBudgetCategories() {
        runOnUiThread(() -> {
            hasBudgetCategories = true;

            // Show categories container
            if (categoriesContainer != null) {
                categoriesContainer.setVisibility(View.VISIBLE);
            }

            // Hide empty state
            if (emptyStateView != null) {
                emptyStateView.setVisibility(View.GONE);
            }
        });
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

        // Initialize category section views
        categoriesContainer = findViewById(R.id.categories_container);
        emptyStateView = findViewById(R.id.empty_state_view);
        tvEmptyStateTitle = findViewById(R.id.tv_empty_state_title);
        tvEmptyStateSubtitle = findViewById(R.id.tv_empty_state_subtitle);
        btnSetupBudget = findViewById(R.id.btn_setup_budget);

        // Set initial date display
        tvDateSelected.setText(dateFormatter.format(selectedDate));

        // Initially hide both sections until we know the budget state
        if (categoriesContainer != null) {
            categoriesContainer.setVisibility(View.GONE);
        }
        if (emptyStateView != null) {
            emptyStateView.setVisibility(View.GONE);
        }
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

        // Setup budget button click
        if (btnSetupBudget != null) {
            btnSetupBudget.setOnClickListener(v -> {
                // Navigate to budget setup activity
                // Ganti dengan activity yang sudah ada atau buat placeholder
                Toast.makeText(AddTransactionActivity.this,
                        "Budget setup will be implemented soon",
                        Toast.LENGTH_SHORT).show();

                // Atau jika Anda punya activity lain untuk budget:
                // Intent intent = new Intent(AddTransactionActivity.this, MainActivity.class);
                // startActivityForResult(intent, 100);
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // Refresh budget categories after setup
            loadBudgetCategories();
        }
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

        // Validate amount
        if (amountText.isEmpty()) {
            etAmount.setError("Please enter amount");
            etAmount.requestFocus();
            return;
        }

        try {
            // Parse amount from formatted string
            String cleanString = amountText.replaceAll("[^0-9]", "");
            if (cleanString.isEmpty()) {
                etAmount.setError("Please enter a valid amount");
                etAmount.requestFocus();
                return;
            }

            double amount = Double.parseDouble(cleanString);

            // Validate minimum amount
            if (amount <= 0) {
                etAmount.setError("Amount must be greater than 0");
                etAmount.requestFocus();
                return;
            }

            // Use selected category or default if no budget is set
            String categoryToUse = hasBudgetCategories ? selectedCategory : "General";

            // Create transaction object
            Transaction transaction = new Transaction(
                    FirebaseAuth.getInstance().getUid(),
                    amount,
                    categoryToUse,
                    "", // Empty account type
                    selectedDate,
                    transactionType
            );

            // Format as currency for display
            String formattedAmount = currencyFormatter.format(amount);
            transaction.setFormattedAmount(formattedAmount);

            // Check if PIN is required
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            boolean isPinRequired = prefs.getBoolean("pin_set", false);
            String savedPin = prefs.getString("user_pin", "");

            if (isPinRequired && !savedPin.isEmpty()) {
                // Show PIN verification dialog
                showPinVerificationDialog(transaction, addMore);
            } else {
                // No PIN required, save directly
                processSaveTransaction(transaction, addMore);
            }

        } catch (NumberFormatException e) {
            etAmount.setError("Invalid amount format");
            etAmount.requestFocus();
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
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

        // Initialize and set the title for transaction verification
        tvPinTitle = pinVerificationDialog.findViewById(R.id.tv_pin_title);
        if (tvPinTitle != null) {
            tvPinTitle.setText("Verify PIN for Transaction");
        }

        // Initialize and set subtitle for transaction info
        TextView tvPinSubtitle = pinVerificationDialog.findViewById(R.id.tv_pin_subtitle);
        if (tvPinSubtitle != null) {
            String transactionTypeText = "expense".equals(transaction.getType()) ? "expense" : "income";
            String amountText = transaction.getFormattedAmount();
            if (amountText == null || amountText.isEmpty()) {
                // Format the amount if it's not already formatted
                NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
                amountText = formatter.format(transaction.getAmount());
            }

            String message = String.format("Confirm %s transaction of %s for %s",
                    transactionTypeText,
                    amountText,
                    transaction.getCategory());
            tvPinSubtitle.setText(message);
        }

        // Set up click listeners for number buttons
        for (int i = 0; i < numberButtons.length; i++) {
            if (numberButtons[i] != null) {
                final int number = i;
                numberButtons[i].setOnClickListener(v -> onPinNumberClicked(number, transaction, addMore));
            }
        }

        // Set up delete button
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                if (enteredPin.length() > 0) {
                    enteredPin.deleteCharAt(enteredPin.length() - 1);
                    updatePinDots();
                }
            });
        }

        // Set up cancel button
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                enteredPin.setLength(0);
                pinVerificationDialog.dismiss();
                Toast.makeText(AddTransactionActivity.this, "Transaction cancelled", Toast.LENGTH_SHORT).show();
            });
        }

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
                // Add a small delay for better UX
                new android.os.Handler().postDelayed(() -> {
                    verifyPin(transaction, addMore);
                }, 200);
            }
        }
    }

    /**
     * Update the PIN dots display based on entered PIN length
     */
    private void updatePinDots() {
        for (int i = 0; i < pinDots.length; i++) {
            if (pinDots[i] != null) {
                if (i < enteredPin.length()) {
                    pinDots[i].setBackgroundResource(R.drawable.pin_dot_filled);
                    // Fix animation with proper final variable
                    final View currentDot = pinDots[i];
                    currentDot.animate()
                            .scaleX(1.2f)
                            .scaleY(1.2f)
                            .setDuration(100)
                            .withEndAction(() -> {
                                if (currentDot != null) {
                                    currentDot.animate()
                                            .scaleX(1f)
                                            .scaleY(1f)
                                            .setDuration(100);
                                }
                            });
                } else {
                    pinDots[i].setBackgroundResource(R.drawable.pin_dot_empty);
                    // Reset scale for empty dots
                    pinDots[i].setScaleX(1f);
                    pinDots[i].setScaleY(1f);
                }
            }
        }
    }

    /**
     * Verify the entered PIN against the saved PIN
     */
    private void verifyPin(Transaction transaction, boolean addMore) {
        String pin = enteredPin.toString();
        String savedPin = getSavedPinHash();

        // If no PIN is saved yet, show error
        if (savedPin.isEmpty()) {
            Toast.makeText(this, "No PIN is set. Please set a PIN first.", Toast.LENGTH_LONG).show();
            enteredPin.setLength(0);
            pinVerificationDialog.dismiss();

            // Redirect to PIN setup
            Intent pinIntent = new Intent(this, SetPinCodeActivity.class);
            pinIntent.putExtra("requirePin", true);
            startActivity(pinIntent);
            return;
        }

        // Verify the PIN using the method from SetPinCodeActivity
        boolean isCorrect = SetPinCodeActivity.verifyPin(pin, savedPin);

        if (isCorrect) {
            // PIN is correct, proceed with saving transaction
            enteredPin.setLength(0);
            pinVerificationDialog.dismiss();

            // Show success message
            Toast.makeText(this, "PIN verified successfully", Toast.LENGTH_SHORT).show();

            // Proceed with transaction
            processSaveTransaction(transaction, addMore);
        } else {
            // PIN is incorrect, show error and clear entry
            Toast.makeText(this, "Incorrect PIN. Please try again.", Toast.LENGTH_SHORT).show();

            // Add vibration feedback with proper API handling
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    android.os.VibrationEffect effect = android.os.VibrationEffect.createOneShot(300, android.os.VibrationEffect.DEFAULT_AMPLITUDE);
                    android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        vibrator.vibrate(effect);
                    }
                } else {
                    @SuppressWarnings("deprecation")
                    android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        vibrator.vibrate(300);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Vibration not available: " + e.getMessage());
            }

            // Clear PIN entry and allow retry
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
        // Save transaction and update budget (only if budget exists)
        transactionRepository.saveTransaction(transaction, new TransactionRepository.TransactionCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                // Update user's available balance
                updateUserBalance(transaction, new TransactionRepository.TransactionCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> data) {
                        // Only update budget if budget categories exist
                        if (hasBudgetCategories) {
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
                                                handleTransactionSaveComplete(addMore);
                                            });
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            runOnUiThread(() -> {
                                                Toast.makeText(AddTransactionActivity.this,
                                                        "Transaction saved but budget update failed",
                                                        Toast.LENGTH_SHORT).show();
                                                handleTransactionSaveComplete(addMore);
                                            });
                                        }
                                    });
                        } else {
                            // No budget to update, just show success
                            runOnUiThread(() -> {
                                Toast.makeText(AddTransactionActivity.this,
                                        "Transaction saved successfully",
                                        Toast.LENGTH_SHORT).show();
                                handleTransactionSaveComplete(addMore);
                            });
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> {
                            Toast.makeText(AddTransactionActivity.this,
                                    "Transaction saved but balance update failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            handleTransactionSaveComplete(addMore);
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
     * Handle completion of transaction save
     */
    private void handleTransactionSaveComplete(boolean addMore) {
        if (!addMore) {
            finish();
        } else {
            // Clear form for next transaction
            etAmount.setText("");
            // Reset to default date (today)
            selectedDate = new Date();
            tvDateSelected.setText(dateFormatter.format(selectedDate));
            // Reset category selection if budget exists
            if (hasBudgetCategories && !budgetCategories.isEmpty()) {
                selectedCategory = budgetCategories.get(0).getCategoryName();
                if (budgetCategoryAdapter != null) {
                    budgetCategoryAdapter.resetSelection();
                }
            } else {
                selectedCategory = "General";
            }
        }
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
            showEmptyState();
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
            showBudgetCategories();
            // Set first category as default selected
            selectedCategory = budgetCategories.get(0).getCategoryName();
        } else {
            showEmptyState();
        }
    }

    /**
     * Set up the RecyclerView for budget categories
     */
    private void setupBudgetCategoriesRecyclerView() {
        // Find or create the RecyclerView for budget categories
        if (budgetCategoriesRecyclerView == null) {
            // Create RecyclerView programmatically
            budgetCategoriesRecyclerView = new RecyclerView(this);
            budgetCategoriesRecyclerView.setId(View.generateViewId()); // Generate unique ID

            // Set layout params
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            budgetCategoriesRecyclerView.setLayoutParams(layoutParams);

            // Use GridLayoutManager with 3 columns
            int numberOfColumns = 3;
            budgetCategoriesRecyclerView.setLayoutManager(new GridLayoutManager(
                    this, numberOfColumns));

            // Add to categories container (now it's LinearLayout, so addView works)
            if (categoriesContainer != null) {
                categoriesContainer.addView(budgetCategoriesRecyclerView);
            }
        }

        if (budgetCategoriesRecyclerView != null) {
            // Create new adapter with our budget categories
            budgetCategoryAdapter = new BudgetCategoryAdapter(budgetCategories,
                    categoryName -> {
                        selectedCategory = categoryName;
                        Toast.makeText(this, categoryName + " selected", Toast.LENGTH_SHORT).show();
                    });
            budgetCategoriesRecyclerView.setAdapter(budgetCategoryAdapter);
        }
    }

    /**
     * Adapter for budget categories
     */
    private class BudgetCategoryAdapter extends RecyclerView.Adapter<BudgetCategoryAdapter.ViewHolder> {

        private List<BudgetCategory> categories;
        private OnCategorySelectedListener listener;
        private int selectedPosition = 0; // Default first item selected

        public BudgetCategoryAdapter(List<BudgetCategory> categories, OnCategorySelectedListener listener) {
            this.categories = categories;
            this.listener = listener;
        }

        public void resetSelection() {
            selectedPosition = 0;
            notifyDataSetChanged();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pinVerificationDialog != null && pinVerificationDialog.isShowing()) {
            pinVerificationDialog.dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        if (pinVerificationDialog != null && pinVerificationDialog.isShowing()) {
            // Show confirmation dialog before cancelling transaction
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Cancel Transaction")
                    .setMessage("Are you sure you want to cancel this transaction?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        enteredPin.setLength(0);
                        pinVerificationDialog.dismiss();
                        super.onBackPressed();
                    })
                    .setNegativeButton("No", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }
}