package com.example.spendly.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.spendly.R;
import com.example.spendly.activity.ProfileSettingsActivity;
import com.example.spendly.model.Transaction;
import com.example.spendly.model.User;
import com.example.spendly.repository.BudgetRepository;
import com.example.spendly.repository.TransactionRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private TextView tvGreeting;
    private TextView tvBalance;
    private TextView tvRemainingBudget;
    private TextView tvUsedAmount;
    private TextView tvTotalBudget;
    private TextView tvOutcome;
    private TextView tvIncome;
    private TextView tvUsedPercentage;
    private View progressIndicator;
    private ProgressBar loadingProgressBar;

    // Firebase components
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private FirebaseUser currentUser;

    // Repositories
    private BudgetRepository budgetRepository;
    private TransactionRepository transactionRepository;

    // Data values
    private double availableBalance = 0.0;
    private double totalIncome = 0.0;
    private double totalExpenses = 0.0;
    private double remainingBudget = 0.0;
    private double totalBudget = 0.0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize repositories
        if (getActivity() != null) {
            budgetRepository = BudgetRepository.getInstance(getActivity());
            transactionRepository = TransactionRepository.getInstance(getActivity());
        }

        // Initialize views
        initViews(view);

        // Show loading state
        showLoadingState(true);

        // Load user data and financial information
        loadUserData();
    }

    /**
     * Initialize views from the layout
     */
    private void initViews(View view) {
        tvGreeting = view.findViewById(R.id.tv_greeting);
        tvBalance = view.findViewById(R.id.tv_balance);
        CardView cardProfile = view.findViewById(R.id.card_profile);

        // Budget views
        tvRemainingBudget = view.findViewById(R.id.tv_remaining_budget);
        tvUsedAmount = view.findViewById(R.id.tv_used_amount);
        tvTotalBudget = view.findViewById(R.id.tv_total_budget);
        tvUsedPercentage = view.findViewById(R.id.tv_used_percentage);
        progressIndicator = view.findViewById(R.id.progress_indicator);

        // Income & Outcome views
        tvOutcome = view.findViewById(R.id.tv_outcome);
        tvIncome = view.findViewById(R.id.tv_income);

        // Loading progress bar
        loadingProgressBar = view.findViewById(R.id.loading_progress_bar);

        // Set click listener for profile card
        cardProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ProfileSettingsActivity.class);
            startActivity(intent);
        });

        // Set current month and year for the greeting
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String currentMonth = sdf.format(new Date());
        tvGreeting.setText("Hi! " + (currentUser != null ? extractName(currentUser.getEmail()) : "User"));
    }

    /**
     * Load user data from Firestore and update UI
     */
    private void loadUserData() {
        if (currentUser == null) {
            showLoadingState(false);
            return;
        }

        // Get user document from Firestore
        mFirestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Convert document to User object
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            // Update available balance from user profile
                            availableBalance = user.getCurrentBalance();

                            // Now load budget and transaction data
                            loadBudgetData();
                        }
                    } else {
                        // Create a default user document if it doesn't exist
                        createDefaultUserDocument();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoadingState(false);
                    showToast("Failed to load user data: " + e.getMessage());
                });
    }

    /**
     * Creates a default user document if it doesn't exist
     */
    private void createDefaultUserDocument() {
        if (currentUser == null) return;

        User newUser = new User();
        newUser.setEmail(currentUser.getEmail());
        newUser.setCurrentBalance(0.0);

        mFirestore.collection("users")
                .document(currentUser.getUid())
                .set(newUser)
                .addOnSuccessListener(aVoid -> {
                    availableBalance = 0.0;
                    loadBudgetData();
                })
                .addOnFailureListener(e -> {
                    showLoadingState(false);
                    showToast("Failed to create user data: " + e.getMessage());
                });
    }

    /**
     * Load budget data from BudgetRepository
     */
    private void loadBudgetData() {
        if (budgetRepository == null) {
            showLoadingState(false);
            updateUI();
            return;
        }

        budgetRepository.checkBudgetExists(new BudgetRepository.BudgetCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                boolean budgetExists = data.containsKey("exists") && (boolean) data.get("exists");

                if (budgetExists) {
                    budgetRepository.getTotalBudget(new BudgetRepository.BudgetCallback() {
                        @Override
                        public void onSuccess(Map<String, Object> budgetData) {
                            // Extract budget information
                            extractBudgetData(budgetData);

                            // Now load transaction data
                            loadTransactionData();
                        }

                        @Override
                        public void onError(Exception e) {
                            loadTransactionData(); // Still try to load transactions
                        }
                    });
                } else {
                    // No budget exists, still load transactions
                    loadTransactionData();
                }
            }

            @Override
            public void onError(Exception e) {
                loadTransactionData(); // Still try to load transactions
            }
        });
    }

    /**
     * Extract budget values from budget data map
     */
    private void extractBudgetData(Map<String, Object> budgetData) {
        // Extract monthly income
        if (budgetData.containsKey("monthly_income")) {
            if (budgetData.get("monthly_income") instanceof Double) {
                totalIncome = (Double) budgetData.get("monthly_income");
            } else if (budgetData.get("monthly_income") instanceof Long) {
                totalIncome = ((Long) budgetData.get("monthly_income")).doubleValue();
            }
        }

        // Extract monthly budget
        if (budgetData.containsKey("monthly_budget")) {
            if (budgetData.get("monthly_budget") instanceof Double) {
                totalBudget = (Double) budgetData.get("monthly_budget");
            } else if (budgetData.get("monthly_budget") instanceof Long) {
                totalBudget = ((Long) budgetData.get("monthly_budget")).doubleValue();
            }
        }

        // Extract remaining budget
        if (budgetData.containsKey("remaining_budget")) {
            if (budgetData.get("remaining_budget") instanceof Double) {
                remainingBudget = (Double) budgetData.get("remaining_budget");
            } else if (budgetData.get("remaining_budget") instanceof Long) {
                remainingBudget = ((Long) budgetData.get("remaining_budget")).doubleValue();
            }
        }
    }

    /**
     * Load transaction data to calculate total expenses
     */
    private void loadTransactionData() {
        if (transactionRepository == null) {
            showLoadingState(false);
            updateUI();
            return;
        }

        // Get all transactions for the current month
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);

        // Set to the first day of the month
        calendar.set(year, month, 1, 0, 0, 0);
        Date startDate = calendar.getTime();

        // Set to the last day of the month
        calendar.set(year, month, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        Date endDate = calendar.getTime();

        transactionRepository.getMonthlyTransactions(startDate, endDate,
                new TransactionRepository.TransactionCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                if (data.containsKey("transactions")) {
                    @SuppressWarnings("unchecked")
                    List<Transaction> transactions = (List<Transaction>) data.get("transactions");

                    // Calculate total expenses
                    totalExpenses = 0.0;
                    for (Transaction transaction : transactions) {
                        if ("expense".equalsIgnoreCase(transaction.getType())) {
                            totalExpenses += transaction.getAmount();
                        }
                    }
                }

                // Update the UI with all the loaded data
                showLoadingState(false);
                updateUI();
            }

            @Override
            public void onError(Exception e) {
                showLoadingState(false);
                updateUI();
                showToast("Error loading transactions: " + e.getMessage());
            }
        });
    }

    /**
     * Update UI with user and financial data
     */
    private void updateUI() {
        if (!isAdded()) return; // Don't update UI if fragment is not attached

        // Update user greeting
        String greeting = "Hi! " + extractName(currentUser != null ? currentUser.getEmail() : "User");
        tvGreeting.setText(greeting);

        // Format and display available balance
        String formattedBalance = formatCurrency(availableBalance);
        tvBalance.setText(formattedBalance);

        // Format and display remaining budget
        String formattedRemainingBudget = formatCurrency(remainingBudget);
        tvRemainingBudget.setText(formattedRemainingBudget);

        // Format and display used amount and total budget
        double usedBudget = totalBudget - remainingBudget;
        tvUsedAmount.setText("Used " + formatCurrency(usedBudget));
        tvTotalBudget.setText("From " + formatCurrency(totalBudget));

        // Calculate and display budget usage percentage
        int usagePercentage = totalBudget > 0 ? (int)((usedBudget / totalBudget) * 100) : 0;
        tvUsedPercentage.setText(usagePercentage + "% used");

        // Update progress bar
        updateProgressBar(usagePercentage);

        // Display total income and outcome
        tvIncome.setText(formatCurrency(totalIncome));
        tvOutcome.setText(formatCurrency(totalExpenses));
    }

    /**
     * Updates the visual progress bar based on budget usage
     */
    private void updateProgressBar(int percentage) {
        if (progressIndicator != null) {
            ViewGroup.LayoutParams params = progressIndicator.getLayoutParams();
            int parentWidth = ((View) progressIndicator.getParent()).getWidth();

            if (parentWidth > 0) {
                int width = (int)(parentWidth * percentage / 100.0);
                params.width = Math.max(width, 10); // Minimum width for visibility
                progressIndicator.setLayoutParams(params);
            }
        }
    }

    /**
     * Extract name from email address (before @ symbol)
     */
    private String extractName(String email) {
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }
        return "User";
    }

    /**
     * Format currency in Indonesian Rupiah
     */
    private String formatCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        return formatter.format(amount);
    }

    /**
     * Show or hide loading state
     */
    private void showLoadingState(boolean isLoading) {
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Show toast message
     */
    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Update data when fragment resumes
     */
    @Override
    public void onResume() {
        super.onResume();
        // Reload data when the fragment becomes visible again
        if (isAdded()) {
            showLoadingState(true);
            loadUserData();
        }
    }
}
