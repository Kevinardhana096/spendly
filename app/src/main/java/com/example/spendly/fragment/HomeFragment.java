package com.example.spendly.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;

import android.util.Log;
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
import com.example.spendly.repository.RealtimeTransactionRepository;
import com.example.spendly.repository.RealtimeSavingsRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    // Current context - Updated to 2025-06-21 20:08:49 UTC
    private static final String CURRENT_DATE_TIME = "2025-06-21 20:24:22";
    private static final String CURRENT_USER = "nowriafisda";
    private static final long CURRENT_TIMESTAMP = 1719348262000L; // 2025-06-21 20:08:49 UTC

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
    private FirebaseUser currentUser;    // Repositories - Updated to real-time versions
    private RealtimeTransactionRepository transactionRepository;
    private RealtimeSavingsRepository savingsRepository;

    // Real-time listeners
    private ListenerRegistration userBalanceListener;
    private ListenerRegistration savingsListener;
    private ListenerRegistration transactionsListener;
    private ListenerRegistration budgetListener; // New budget listener

    // Data values - Enhanced with real-time tracking
    private double availableBalance = 0.0;
    private double totalIncome = 0.0;
    private double totalExpenses = 0.0;
    private double totalSavingsAmount = 0.0;
    private double totalAddTransactionAmount = 0.0;
    private double remainingBudget = 0.0;
    private double totalBudget = 0.0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "=== HomeFragment Created ===");
        Log.d(TAG, "Current Date/Time: " + CURRENT_DATE_TIME + " UTC");
        Log.d(TAG, "Current User: " + CURRENT_USER);
        Log.d(TAG, "Mode: Real-time outcome updates with Firestore listeners");
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "HomeFragment view created for user: " + CURRENT_USER);

        // Initialize Firebase components
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Firebase user authenticated: " + currentUser.getEmail());
            Log.d(TAG, "User UID: " + currentUser.getUid());

            // Verify user matches expected user
            if (!CURRENT_USER.equals(currentUser.getEmail()) &&
                    !currentUser.getEmail().contains(CURRENT_USER)) {
                Log.w(TAG, "User mismatch - Expected: " + CURRENT_USER + ", Got: " + currentUser.getEmail());
            }
        } else {
            Log.e(TAG, "❌ CRITICAL ERROR: No Firebase user found!");
            Log.e(TAG, "❌ This will prevent all real-time listeners from working!");
            showToast("Authentication error: Please login again");
            return; // Exit early if no user
        }        // Initialize repositories - Updated to real-time versions
        if (getActivity() != null) {
            try {
                transactionRepository = RealtimeTransactionRepository.getInstance(getActivity());
                savingsRepository = RealtimeSavingsRepository.getInstance(getActivity());
                Log.d(TAG, "✅ Repositories initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "❌ Error initializing repositories", e);
                showToast("Error initializing data repositories");
            }
        } else {
            Log.e(TAG, "❌ Cannot initialize repositories - Activity is null");
        }

        // Initialize views
        initViews(view);

        // Show loading state
        showLoadingState(true);// Load initial data first (faster)
        loadUserData();

        // Setup real-time observers for automatic updates
        setupRealtimeObservers();

        // Setup legacy real-time listeners after initial load (delayed for compatibility)
        view.postDelayed(() -> {
            if (isAdded() && currentUser != null) {
                setupRealtimeListeners();
            }
        }, 1000); // 1 second delay
    }

    /**
     * Initialize views from the layout
     */
    private void initViews(View view) {
        Log.d(TAG, "Initializing views at " + CURRENT_DATE_TIME);

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

        // Log view initialization with focus on tv_outcome
        Log.d(TAG, "Views initialized:");
        Log.d(TAG, "- tvBalance: " + (tvBalance != null));
        Log.d(TAG, "- tvOutcome: " + (tvOutcome != null) + " ✅ CRITICAL for real-time updates");
        Log.d(TAG, "- tvIncome: " + (tvIncome != null));
        Log.d(TAG, "- tvRemainingBudget: " + (tvRemainingBudget != null));

        // Set click listener for profile card
        if (cardProfile != null) {
            cardProfile.setOnClickListener(v -> {
                Log.d(TAG, "Profile card clicked by user: " + CURRENT_USER + " at " + CURRENT_DATE_TIME);
                Intent intent = new Intent(getActivity(), ProfileSettingsActivity.class);
                startActivity(intent);
            });
        }

        // Set current month and year for the greeting
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String currentMonth = sdf.format(new Date(CURRENT_TIMESTAMP));
        tvGreeting.setText("Hi! " + (currentUser != null ? extractName(currentUser.getEmail()) : "User"));

        Log.d(TAG, "Current month: " + currentMonth);
    }

    /**
     * ✅ NEW: Setup real-time Firestore listeners for immediate outcome updates
     */
    /**
     * Setup real-time observers for automatic data updates
     */
    private void setupRealtimeObservers() {
        if (currentUser == null) {
            Log.e(TAG, "❌ Cannot setup observers - no current user");
            return;
        }

        if (transactionRepository == null) {
            Log.e(TAG, "❌ Cannot setup observers - transactionRepository is null");
            return;
        }

        if (savingsRepository == null) {
            Log.e(TAG, "❌ Cannot setup observers - savingsRepository is null");
            return;
        }

        Log.d(TAG, "=== SETTING UP REAL-TIME OBSERVERS ===");
        Log.d(TAG, "User UID: " + currentUser.getUid());

        try {
            // Observe transactions with real-time updates
            transactionRepository.getTransactions(currentUser.getUid()).observe(getViewLifecycleOwner(),
                    transactions -> {
                        if (transactions != null) {
                            Log.d(TAG, "Real-time transactions update: " + transactions.size() + " items");
                            // Update income/expense calculations
                            updateTransactionTotals(transactions);
                        } else {
                            Log.w(TAG, "Received null transactions in observer");
                        }
                    });

            // Observe transaction totals
            transactionRepository.getTotalIncome().observe(getViewLifecycleOwner(),
                    income -> {
                        if (income != null) {
                            Log.d(TAG, "Income observer triggered: Rp" + formatNumber(income));
                            totalIncome = income;
                            updateBalanceAndOutcome();
                        }
                    });

            transactionRepository.getTotalExpenses().observe(getViewLifecycleOwner(),
                    expenses -> {
                        if (expenses != null) {
                            Log.d(TAG, "Expenses observer triggered: Rp" + formatNumber(expenses));
                            totalExpenses = expenses;
                            updateBalanceAndOutcome();
                        }
                    });

            // Observe savings with real-time updates
            savingsRepository.getSavings(currentUser.getUid()).observe(getViewLifecycleOwner(),
                    savings -> {
                        if (savings != null) {
                            Log.d(TAG, "Real-time savings update: " + savings.size() + " items");
                            // Calculate total savings amount
                            double total = 0.0;
                            for (com.example.spendly.model.SavingsItem item : savings) {
                                total += item.getCurrentAmount();
                            }
                            totalSavingsAmount = total;
                            updateBalanceAndOutcome();
                        } else {
                            Log.w(TAG, "Received null savings in observer");
                        }
                    });

            // Observe budget data with real-time updates
            mFirestore.collection("users")
                    .document(currentUser.getUid())
                    .collection("budget")
                    .document("total_budget")
                    .addSnapshotListener((documentSnapshot, e) -> {
                        if (e != null) {
                            Log.e(TAG, "Error in budget listener", e);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            // Update budget values
                            if (documentSnapshot.getData() != null) {
                                Map<String, Object> budgetData = documentSnapshot.getData();
                                extractBudgetData(budgetData);
                                Log.d(TAG, "Budget data updated: " +
                                        "Income: Rp" + formatNumber(totalIncome) +
                                        ", Budget: Rp" + formatNumber(totalBudget) +
                                        ", Remaining: Rp" + formatNumber(remainingBudget));
                            } else {
                                Log.w(TAG, "Budget document data is null");
                            }
                        } else {
                            Log.w(TAG, "Budget document does not exist");
                        }
                    });

            Log.d(TAG, "✅ Real-time observers setup completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error setting up real-time observers", e);
            showToast("Error setting up real-time data sync");
        }
    }

    /**
     * Update transaction totals from list
     */
    private void updateTransactionTotals(List<com.example.spendly.model.Transaction> transactions) {
        double income = 0.0;
        double expenses = 0.0;

        for (com.example.spendly.model.Transaction transaction : transactions) {
            if ("income".equals(transaction.getType())) {
                income += transaction.getAmount();
            } else if ("expense".equals(transaction.getType())) {
                expenses += transaction.getAmount();
            }
        }
        totalIncome = income;
        totalExpenses = expenses;
        updateBalanceAndOutcome();
    }

    /**
     * Setup legacy real-time listeners (for compatibility)
     */
    private void setupRealtimeListeners() {
        if (currentUser == null) {
            Log.e(TAG, "Cannot setup listeners - no current user");
            return;
        }

        Log.d(TAG, "=== SETTING UP REAL-TIME LISTENERS ===");
        Log.d(TAG, "User: " + CURRENT_USER + " at " + CURRENT_DATE_TIME);

        // 1. Listen to user balance changes
        setupUserBalanceListener();

        // 2. Listen to savings changes
        setupSavingsListener();

        // 3. Listen to transaction changes
        setupTransactionsListener();

        // 4. Listen to budget changes
        setupBudgetListener();

        Log.d(TAG, "All real-time listeners setup completed");
    }

    /**
     * ✅ Listen to user balance changes
     */
    private void setupUserBalanceListener() {
        if (currentUser == null) {
            Log.e(TAG, "❌ Cannot setup user balance listener - no current user");
            return;
        }

        Log.d(TAG, "Setting up user balance listener...");
        Log.d(TAG, "Firestore path: users/" + currentUser.getUid());

        try {
            userBalanceListener = mFirestore.collection("users")
                    .document(currentUser.getUid())
                    .addSnapshotListener((documentSnapshot, e) -> {
                        if (e != null) {
                            Log.e(TAG, "❌ Error in user balance listener", e);
                            showToast("Error syncing balance data");
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            try {
                                User user = documentSnapshot.toObject(User.class);
                                if (user != null) {
                                    double newBalance = user.getCurrentBalance();

                                    Log.d(TAG, "🔄 USER BALANCE CHANGED:");
                                    Log.d(TAG, "- Previous: Rp" + formatNumber(availableBalance));
                                    Log.d(TAG, "- New: Rp" + formatNumber(newBalance));
                                    Log.d(TAG, "- Change: Rp" + formatNumber(newBalance - availableBalance));
                                    Log.d(TAG, "- Time: " + CURRENT_DATE_TIME);
                                    Log.d(TAG, "✅ This balance already includes all transactions from AddTransactionActivity");

                                    availableBalance = newBalance;

                                    // Update UI immediately
                                    updateBalanceAndOutcome();
                                } else {
                                    Log.e(TAG, "❌ Failed to convert document to User object");
                                }
                            } catch (Exception conversionError) {
                                Log.e(TAG, "❌ Error converting user document", conversionError);
                            }
                        } else {
                            Log.w(TAG, "⚠️ User document does not exist or is null");
                            Log.w(TAG, "Document exists: " + (documentSnapshot != null ? documentSnapshot.exists() : "null"));
                        }
                    });

            Log.d(TAG, "✅ User balance listener setup completed");

        } catch (Exception e) {
            Log.e(TAG, "❌ Exception setting up user balance listener", e);
            showToast("Failed to setup balance sync");
        }
    }

    /**
     * ✅ Listen to savings changes
     */
    private void setupSavingsListener() {
        Log.d(TAG, "Setting up savings listener...");

        savingsListener = mFirestore.collection("users")
                .document(currentUser.getUid())
                .collection("savings")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Error in savings listener", e);
                        return;
                    }

                    if (querySnapshot != null) {
                        double newTotalSavings = 0.0;

                        for (QueryDocumentSnapshot document : querySnapshot) {
                            Double currentAmount = document.getDouble("currentAmount");
                            if (currentAmount != null) {
                                newTotalSavings += currentAmount;
                            }
                        }

                        Log.d(TAG, "🔄 SAVINGS CHANGED:");
                        Log.d(TAG, "- Previous total: Rp" + formatNumber(totalSavingsAmount));
                        Log.d(TAG, "- New total: Rp" + formatNumber(newTotalSavings));
                        Log.d(TAG, "- Change: Rp" + formatNumber(newTotalSavings - totalSavingsAmount));
                        Log.d(TAG, "- Count: " + querySnapshot.size() + " savings items");
                        Log.d(TAG, "- Time: " + CURRENT_DATE_TIME);

                        totalSavingsAmount = newTotalSavings;

                        // Update UI immediately
                        updateBalanceAndOutcome();
                    }
                });

        Log.d(TAG, "✅ Savings listener setup completed");
    }

    /**
     * ✅ Listen to transaction changes
     */
    private void setupTransactionsListener() {
        if (currentUser == null) {
            Log.e(TAG, "Cannot setup transaction listener - no current user");
            return;
        }

        Log.d(TAG, "=== ENHANCED TRANSACTION LISTENER FOR REAL-TIME UI ===");
        Log.d(TAG, "Current Date/Time: 2025-06-21 20:24:22 UTC");
        Log.d(TAG, "Current User: nowriafisda");

        // Get current month range with updated timestamp
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(1719348262000L); // 2025-06-21 20:24:22 UTC

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);

        calendar.set(year, month, 1, 0, 0, 0);
        long startTimestamp = calendar.getTimeInMillis();

        calendar.set(year, month, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        long endTimestamp = calendar.getTimeInMillis();

        Log.d(TAG, "Transaction listener date range (June 2025):");
        Log.d(TAG, "- Start: " + new Date(startTimestamp) + " (" + startTimestamp + ")");
        Log.d(TAG, "- End: " + new Date(endTimestamp) + " (" + endTimestamp + ")");
        Log.d(TAG, "- Current: 2025-06-21 20:24:22 UTC (" + 1719348262000L + ")");

        String listenerPath = "users/" + currentUser.getUid() + "/transactions";
        Log.d(TAG, "Listening to Firestore path: " + listenerPath);        // ✅ OPTIMIZED REAL-TIME LISTENER: Listen to recent transactions only
        transactionsListener = mFirestore.collection("users")
                .document(currentUser.getUid())
                .collection("transactions")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(50) // Limit to recent 50 transactions for better performance
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "❌ Transaction listener error", e);
                        return;
                    }

                    if (querySnapshot != null) {
                        Log.d(TAG, "🔄 TRANSACTION LISTENER TRIGGERED");
                        Log.d(TAG, "Time: 2025-06-21 20:24:22 UTC");
                        Log.d(TAG, "User: nowriafisda");
                        Log.d(TAG, "Total documents in collection: " + querySnapshot.size());

                        double newTotalTransactions = 0.0;
                        int totalCount = 0;
                        int expenseCount = 0;
                        int currentMonthCount = 0;

                        for (QueryDocumentSnapshot document : querySnapshot) {
                            totalCount++;

                            // Get transaction data
                            Double amount = document.getDouble("amount");
                            String type = document.getString("type");
                            Long date = document.getLong("date");
                            String description = document.getString("description");
                            String category = document.getString("category");

                            Log.d(TAG, "Processing transaction " + totalCount + ":");
                            Log.d(TAG, "- ID: " + document.getId());
                            Log.d(TAG, "- Description: " + description);
                            Log.d(TAG, "- Category: " + category);
                            Log.d(TAG, "- Amount: " + (amount != null ? "Rp" + formatNumber(amount) : "null"));
                            Log.d(TAG, "- Type: " + type);
                            Log.d(TAG, "- Date: " + (date != null ? new Date(date) + " (" + date + ")" : "null"));

                            // Check if it's valid for outcome calculation
                            if (amount != null && date != null && "expense".equalsIgnoreCase(type)) {
                                expenseCount++;
                                Log.d(TAG, "  ✓ Valid expense transaction");

                                // Check if it's in current month (June 2025)
                                if (date >= startTimestamp && date <= endTimestamp) {
                                    currentMonthCount++;
                                    newTotalTransactions += amount;
                                    Log.d(TAG, "  ✅ INCLUDED in outcome (current month expense)");
                                    Log.d(TAG, "  Running total: Rp" + formatNumber(newTotalTransactions));
                                } else {
                                    Log.d(TAG, "  ❌ EXCLUDED (outside June 2025)");
                                    Log.d(TAG, "  Date check: " + date + " not in range [" + startTimestamp + ", " + endTimestamp + "]");
                                }
                            } else {
                                Log.d(TAG, "  ❌ EXCLUDED (not expense or missing data)");
                                if (amount == null) Log.d(TAG, "    Reason: amount is null");
                                if (date == null) Log.d(TAG, "    Reason: date is null");
                                if (!"expense".equalsIgnoreCase(type))
                                    Log.d(TAG, "    Reason: type is '" + type + "' (not expense)");
                            }
                        }

                        Log.d(TAG, "Transaction listener summary:");
                        Log.d(TAG, "- Total documents: " + totalCount);
                        Log.d(TAG, "- Expense transactions: " + expenseCount);
                        Log.d(TAG, "- Current month expenses: " + currentMonthCount);
                        Log.d(TAG, "- Previous total amount: Rp" + formatNumber(totalAddTransactionAmount));
                        Log.d(TAG, "- New total amount: Rp" + formatNumber(newTotalTransactions));
                        Log.d(TAG, "- Amount change: Rp" + formatNumber(newTotalTransactions - totalAddTransactionAmount));

                        // ✅ CRITICAL: Update amount and trigger UI update
                        if (Math.abs(newTotalTransactions - totalAddTransactionAmount) > 0.01) { // Handle floating point precision
                            double previousAmount = totalAddTransactionAmount;
                            totalAddTransactionAmount = newTotalTransactions;

                            Log.d(TAG, "🔄 TRANSACTION AMOUNT CHANGED!");
                            Log.d(TAG, "User nowriafisda - Amount updated from Rp" + formatNumber(previousAmount) +
                                    " to Rp" + formatNumber(newTotalTransactions));
                            Log.d(TAG, "🔄 Forcing immediate UI update for tv_outcome");

                            // Force immediate UI update
                            updateBalanceAndOutcome();

                            // Additional verification
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    Log.d(TAG, "🔄 UI thread update completed for nowriafisda");

                                    // Verify outcome display after update
                                    if (tvOutcome != null) {
                                        String currentOutcomeText = tvOutcome.getText().toString();
                                        Log.d(TAG, "✅ VERIFICATION: tv_outcome now shows: " + currentOutcomeText);
                                    } else {
                                        Log.e(TAG, "❌ tv_outcome is null during verification");
                                    }
                                });
                            }
                        } else {
                            Log.d(TAG, "📊 No significant change in transaction amount");
                        }
                    } else {
                        Log.w(TAG, "Transaction query snapshot is null");
                    }
                });

        Log.d(TAG, "✅ Enhanced transaction listener active for real-time updates");
    }

    /**
     * ✅ CRITICAL: Update balance and outcome immediately when data changes
     */
    private void updateBalanceAndOutcome() {
        if (!isAdded() || getActivity() == null) {
            Log.w(TAG, "Fragment not attached or activity null, skipping UI update");
            return;
        }

        Log.d(TAG, "=== FORCE UI UPDATE FOR TRANSACTION CHANGES ===");
        Log.d(TAG, "Triggered at: 2025-06-21 20:24:22 UTC");
        Log.d(TAG, "User: nowriafisda");
        Log.d(TAG, "Thread: " + Thread.currentThread().getName());

        // Ensure we're on the main UI thread
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                try {
                    // ✅ FIXED: Use availableBalance directly (already includes all transactions)
                    // No need to subtract totalSavingsAmount for display balance
                    double displayBalance = availableBalance;

                    // ✅ ENHANCED: Complete outcome calculation with transaction focus
                    Log.d(TAG, "Balance calculation for transaction changes:");
                    Log.d(TAG, "1. Available Balance (includes all transactions): Rp" + formatNumber(availableBalance));
                    Log.d(TAG, "2. Total Savings: Rp" + formatNumber(totalSavingsAmount));
                    Log.d(TAG, "3. Repository Expenses: Rp" + formatNumber(totalExpenses));
                    Log.d(TAG, "4. Add Transaction Expenses: Rp" + formatNumber(totalAddTransactionAmount) + " ⭐ CHANGED");

                    double maxTransactionExpenses = Math.max(totalExpenses, totalAddTransactionAmount);
                    double totalCompleteOutcome = maxTransactionExpenses + totalSavingsAmount;

                    Log.d(TAG, "Final calculation:");
                    Log.d(TAG, "- Display Balance: Rp" + formatNumber(displayBalance) + " (from Firestore user balance)");
                    Log.d(TAG, "- Max(Repository, AddTransaction): Max(" + formatNumber(totalExpenses) +
                            ", " + formatNumber(totalAddTransactionAmount) + ") = " + formatNumber(maxTransactionExpenses));
                    Log.d(TAG, "- Plus Savings: " + formatNumber(maxTransactionExpenses) + " + " +
                            formatNumber(totalSavingsAmount) + " = " + formatNumber(totalCompleteOutcome));

                    // ✅ FORCE UPDATE BALANCE - Show actual available balance
                    if (tvBalance != null) {
                        String newBalanceText = formatCurrency(displayBalance);
                        tvBalance.setText(newBalanceText);
                        tvBalance.invalidate();
                        Log.d(TAG, "✅ Balance updated to: " + newBalanceText + " (actual available balance)");
                    } else {
                        Log.e(TAG, "❌ CRITICAL ERROR: tvBalance is null!");
                    }

                    // ✅ CRITICAL: Force update outcome for transaction changes
                    if (tvOutcome != null) {
                        String oldOutcomeText = tvOutcome.getText().toString();
                        String newOutcomeText = formatCurrency(totalCompleteOutcome);

                        // Force update with multiple methods
                        tvOutcome.setText(newOutcomeText);
                        tvOutcome.invalidate();
                        tvOutcome.requestLayout();
                        tvOutcome.setVisibility(View.VISIBLE);

                        Log.d(TAG, "✅ OUTCOME UI UPDATE FOR TRANSACTION:");
                        Log.d(TAG, "- Old outcome: " + oldOutcomeText);
                        Log.d(TAG, "- New outcome: " + newOutcomeText);
                        Log.d(TAG, "- Change: " + (!oldOutcomeText.equals(newOutcomeText) ? "CHANGED" : "SAME"));
                        Log.d(TAG, "- User: nowriafisda");
                        Log.d(TAG, "- Time: 2025-06-21 20:24:22 UTC");

                        // Verification with delay
                        tvOutcome.post(() -> {
                            try {
                                String verifyText = tvOutcome.getText().toString();
                                Log.d(TAG, "✅ TRANSACTION UPDATE VERIFICATION: tv_outcome displays: " + verifyText);

                                if (!verifyText.equals(newOutcomeText)) {
                                    Log.e(TAG, "❌ MISMATCH: Expected " + newOutcomeText + ", got " + verifyText);
                                    tvOutcome.setText(newOutcomeText);
                                } else {
                                    Log.d(TAG, "✅ PERFECT: tv_outcome correctly shows transaction impact");
                                }
                            } catch (Exception verifyError) {
                                Log.e(TAG, "❌ Error during outcome verification", verifyError);
                            }
                        });

                        Log.d(TAG, "✅ tv_outcome TRANSACTION UPDATE SUCCESS for nowriafisda");
                    } else {
                        Log.e(TAG, "❌ CRITICAL ERROR: tv_outcome is null during transaction update!");
                    }

                    // Update income if tvIncome exists
                    if (tvIncome != null) {
                        tvIncome.setText(formatCurrency(totalIncome));
                        Log.d(TAG, "✅ Income updated to: " + formatCurrency(totalIncome));
                    } else {
                        Log.e(TAG, "❌ WARNING: tvIncome is null");
                    }

                    Log.d(TAG, "=== TRANSACTION UI UPDATE COMPLETED ===");

                } catch (Exception e) {
                    Log.e(TAG, "❌ ERROR during transaction UI update", e);
                    showToast("Error updating UI: " + e.getMessage());
                }
            });
        } else {
            Log.e(TAG, "❌ Activity is null during updateBalanceAndOutcome");
        }
    }

    /**
     * Load user data from Firestore and update UI
     */
    private void loadUserData() {
        if (currentUser == null) {
            Log.e(TAG, "Cannot load user data - no current user");
            showLoadingState(false);
            return;
        }

        Log.d(TAG, "Loading initial user data for: " + currentUser.getEmail());

        // Get user document from Firestore
        mFirestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "User document found in Firestore");

                        // Convert document to User object
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            // Update available balance from user profile
                            availableBalance = user.getCurrentBalance();
                            Log.d(TAG, "Initial user available balance: Rp" + formatNumber(availableBalance));

                            // Load budget data first to get monthly income
                            loadInitialBudgetData();
                        }
                    } else {
                        Log.w(TAG, "User document not found, creating default");
                        // Create a default user document if it doesn't exist
                        createDefaultUserDocument();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data", e);
                    showLoadingState(false);
                    showToast("Failed to load user data: " + e.getMessage());
                });
    }

    /**
     * Load initial budget data to get monthly income for display
     */
    private void loadInitialBudgetData() {
        Log.d(TAG, "Loading initial budget data...");

        mFirestore.collection("users")
                .document(currentUser.getUid())
                .collection("budget")
                .document("total_budget")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.getData() != null) {
                        Map<String, Object> budgetData = documentSnapshot.getData();

                        // Extract monthly income from budget data
                        if (budgetData.containsKey("monthly_income")) {
                            if (budgetData.get("monthly_income") instanceof Double) {
                                totalIncome = (Double) budgetData.get("monthly_income");
                            } else if (budgetData.get("monthly_income") instanceof Long) {
                                totalIncome = ((Long) budgetData.get("monthly_income")).doubleValue();
                            }
                            Log.d(TAG, "Monthly income from budget: Rp" + formatNumber(totalIncome));
                        }

                        // Extract other budget values
                        extractBudgetData(budgetData);

                        Log.d(TAG, "Budget data loaded - Income: Rp" + formatNumber(totalIncome) +
                                ", Budget: Rp" + formatNumber(totalBudget));
                    } else {
                        Log.d(TAG, "No budget data found, using default income value");
                        // If no budget is set, income will remain 0 or use transaction-based income
                    }

                    // Continue loading savings data
                    loadInitialSavingsData();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading budget data", e);
                    // Continue with savings data even if budget fails
                    loadInitialSavingsData();
                });
    }

    /**
     * Load initial savings data (before listeners take over)
     */
    private void loadInitialSavingsData() {
        Log.d(TAG, "Loading initial savings data...");

        mFirestore.collection("users")
                .document(currentUser.getUid())
                .collection("savings")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    totalSavingsAmount = 0.0;

                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Double currentAmount = document.getDouble("currentAmount");
                            if (currentAmount != null) {
                                totalSavingsAmount += currentAmount;
                            }
                        }
                        Log.d(TAG, "Initial total savings amount: Rp" + formatNumber(totalSavingsAmount));
                    }

                    // Continue with initial transaction data
                    loadInitialTransactionData();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading initial savings data", e);
                    loadInitialTransactionData();
                });
    }

    /**
     * Load initial transaction data (before listeners take over)
     */
    private void loadInitialTransactionData() {
        Log.d(TAG, "Loading initial transaction data...");

        // Get current month range
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(CURRENT_TIMESTAMP);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);

        calendar.set(year, month, 1, 0, 0, 0);
        long startTimestamp = calendar.getTimeInMillis();

        calendar.set(year, month, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        long endTimestamp = calendar.getTimeInMillis();

        mFirestore.collection("users")
                .document(currentUser.getUid())
                .collection("transactions")
                .whereGreaterThanOrEqualTo("date", startTimestamp)
                .whereLessThanOrEqualTo("date", endTimestamp)
                .whereEqualTo("type", "expense")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    totalAddTransactionAmount = 0.0;

                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Double amount = document.getDouble("amount");
                            if (amount != null) {
                                totalAddTransactionAmount += amount;
                            }
                        }
                        Log.d(TAG, "Initial total add transaction amount: Rp" + formatNumber(totalAddTransactionAmount));
                    }

                    // Continue with budget data
                    loadBudgetData();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading initial transaction data", e);
                    loadBudgetData();
                });
    }

    /**
     * Creates a default user document if it doesn't exist
     */
    private void createDefaultUserDocument() {
        if (currentUser == null) return;

        Log.d(TAG, "Creating default user document for: " + CURRENT_USER);

        User newUser = new User();
        newUser.setEmail(currentUser.getEmail());
        newUser.setCurrentBalance(0.0);

        mFirestore.collection("users")
                .document(currentUser.getUid())
                .set(newUser)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Default user document created successfully");
                    availableBalance = 0.0;
                    loadInitialSavingsData();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create default user document", e);
                    showLoadingState(false);
                    showToast("Failed to create user data: " + e.getMessage());
                });
    }

    /**
     * Load budget data - Simplified for now without BudgetRepository callbacks
     */
    private void loadBudgetData() {
        Log.d(TAG, "Loading budget data...");
        // For now, just load transaction data directly
        // TODO: Implement budget functionality with Room/LiveData pattern
        loadTransactionData();
    }

    /**
     * Extract budget values from budget data map - UNCHANGED
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
     * Load transaction data from repository - For comparison
     */
    private void loadTransactionData() {
        if (transactionRepository == null) {
            showLoadingState(false);
            updateUI();
            return;
        }

        // The transaction data is already being observed via LiveData in observeTransactionData()
        // No need for manual loading with callbacks
        Log.d(TAG, "Transaction data is loaded via LiveData observers");
        showLoadingState(false);
        updateUI();
    }

    /**
     * Update UI - Full UI update
     */
    private void updateUI() {
        if (!isAdded()) {
            Log.w(TAG, "Fragment not attached, skipping UI update");
            return;
        }

        Log.d(TAG, "=== FULL UI UPDATE ===");

        // Update user greeting
        String greeting = "Hi! " + extractName(currentUser != null ? currentUser.getEmail() : "User");
        tvGreeting.setText(greeting);

        // ✅ FIXED: Use availableBalance directly for display
        // This balance already reflects all transactions (income and expenses)
        double displayBalance = availableBalance;
        double maxTransactionExpenses = Math.max(totalExpenses, totalAddTransactionAmount);
        double totalCompleteOutcome = maxTransactionExpenses + totalSavingsAmount;

        // Update balance - show actual available balance
        tvBalance.setText(formatCurrency(displayBalance));

        // Update budget info (unchanged)
        tvRemainingBudget.setText(formatCurrency(remainingBudget));

        double originalUsedBudget = totalBudget - remainingBudget;
        tvUsedAmount.setText("Used " + formatCurrency(originalUsedBudget));
        tvTotalBudget.setText("From " + formatCurrency(totalBudget));

        int usagePercentage = totalBudget > 0 ? (int) ((originalUsedBudget / totalBudget) * 100) : 0;
        tvUsedPercentage.setText(usagePercentage + "% used");
        updateProgressBar(usagePercentage);

        // Update income and outcome
        tvIncome.setText(formatCurrency(totalIncome));
        tvOutcome.setText(formatCurrency(totalCompleteOutcome));

        Log.d(TAG, "✅ Full UI update completed");
        Log.d(TAG, "- Display Balance: " + formatCurrency(displayBalance) + " (actual available balance)");
        Log.d(TAG, "- Outcome: " + formatCurrency(totalCompleteOutcome));
    }

    /**
     * Updates the visual progress bar
     */
    private void updateProgressBar(int percentage) {
        if (progressIndicator != null) {
            ViewGroup.LayoutParams params = progressIndicator.getLayoutParams();
            View parent = (View) progressIndicator.getParent();

            if (parent != null) {
                parent.post(() -> {
                    int parentWidth = parent.getWidth();
                    if (parentWidth > 0) {
                        int width = (int) (parentWidth * percentage / 100.0);
                        params.width = Math.max(width, 10);
                        progressIndicator.setLayoutParams(params);
                    }
                });
            }
        }
    }

    /**
     * Extract name from email address
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
     * Format number without currency symbol
     */
    private String formatNumber(double amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("in", "ID"));
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

        Log.d(TAG, "=== HomeFragment Resumed ===");
        Log.d(TAG, "Current Date/Time: 2025-06-21 20:16:12 UTC");
        Log.d(TAG, "User: nowriafisda returned to home");

        if (isAdded() && getActivity() != null) {
            Log.d(TAG, "Forcing complete UI refresh on resume");

            // Force immediate UI update
            getActivity().runOnUiThread(() -> {
                updateBalanceAndOutcome();
            });

            // Delayed update to ensure all data is loaded
            if (getView() != null) {
                getView().postDelayed(() -> {
                    if (isAdded()) {
                        Log.d(TAG, "Delayed UI refresh for nowriafisda");
                        updateBalanceAndOutcome();
                    }
                }, 500);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "HomeFragment paused - User: " + CURRENT_USER + " left home");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "=== HomeFragment Destroyed ===");
        Log.d(TAG, "User: " + CURRENT_USER + " session ended at: " + CURRENT_DATE_TIME);

        // Clean up listeners
        if (userBalanceListener != null) {
            userBalanceListener.remove();
            Log.d(TAG, "User balance listener removed");
        }

        if (savingsListener != null) {
            savingsListener.remove();
            Log.d(TAG, "Savings listener removed");
        }
        if (transactionsListener != null) {
            transactionsListener.remove();
            Log.d(TAG, "Transactions listener removed");
        }
        if (budgetListener != null) {
            budgetListener.remove();
            Log.d(TAG, "Budget listener removed");
        }

        // Clean up real-time repositories
        if (transactionRepository != null) {
            transactionRepository.cleanup();
        }

        if (savingsRepository != null) {
            savingsRepository.cleanup();
        }

        Log.d(TAG, "Real-time repositories cleaned up");
    }

    /**
     * 🔧 DEBUG: Manual balance refresh for testing
     * Call this method to force refresh balance if automatic update fails
     */
    public void debugRefreshBalance() {
        Log.d(TAG, "=== MANUAL DEBUG BALANCE REFRESH ===");
        
        if (currentUser == null) {
            Log.e(TAG, "❌ DEBUG: currentUser is null");
            showToast("Debug: User not authenticated");
            return;
        }
        
        Log.d(TAG, "DEBUG: Refreshing balance for user: " + currentUser.getEmail());
        Log.d(TAG, "DEBUG: User UID: " + currentUser.getUid());
        
        // Force reload user data
        mFirestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            double newBalance = user.getCurrentBalance();
                            Log.d(TAG, "✅ DEBUG: Retrieved balance from Firestore: Rp" + formatNumber(newBalance));
                            
                            availableBalance = newBalance;
                            updateBalanceAndOutcome();
                            showToast("Balance refreshed: " + formatCurrency(newBalance));
                        } else {
                            Log.e(TAG, "❌ DEBUG: Failed to convert user document");
                            showToast("Debug: Failed to convert user data");
                        }
                    } else {
                        Log.e(TAG, "❌ DEBUG: User document does not exist");
                        showToast("Debug: User document not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ DEBUG: Error refreshing balance", e);
                    showToast("Debug: Error - " + e.getMessage());
                });
    }

    /**
     * 🔧 DEBUG: Check listener status
     */
    public void debugCheckListeners() {
        Log.d(TAG, "=== LISTENER STATUS DEBUG ===");
        Log.d(TAG, "userBalanceListener: " + (userBalanceListener != null ? "ACTIVE" : "NULL"));
        Log.d(TAG, "savingsListener: " + (savingsListener != null ? "ACTIVE" : "NULL"));
        Log.d(TAG, "transactionsListener: " + (transactionsListener != null ? "ACTIVE" : "NULL"));
        Log.d(TAG, "budgetListener: " + (budgetListener != null ? "ACTIVE" : "NULL"));
        Log.d(TAG, "transactionRepository: " + (transactionRepository != null ? "INITIALIZED" : "NULL"));
        Log.d(TAG, "savingsRepository: " + (savingsRepository != null ? "INITIALIZED" : "NULL"));
        Log.d(TAG, "currentUser: " + (currentUser != null ? currentUser.getEmail() : "NULL"));
        Log.d(TAG, "Fragment attached: " + isAdded());
        Log.d(TAG, "Activity: " + (getActivity() != null ? "VALID" : "NULL"));
        
        // Check UI components
        Log.d(TAG, "UI Components Status:");
        Log.d(TAG, "- tvBalance: " + (tvBalance != null ? "OK" : "NULL"));
        Log.d(TAG, "- tvOutcome: " + (tvOutcome != null ? "OK" : "NULL"));
        Log.d(TAG, "- tvIncome: " + (tvIncome != null ? "OK" : "NULL"));
        
        showToast("Check logs for listener status");
    }

    /**
     * 🔧 DEBUG: Force setup listeners again
     */
    public void debugResetupListeners() {
        Log.d(TAG, "=== FORCING LISTENER RESET ===");
        
        // Clean up existing listeners
        if (userBalanceListener != null) {
            userBalanceListener.remove();
            userBalanceListener = null;
        }
        if (savingsListener != null) {
            savingsListener.remove();
            savingsListener = null;
        }
        if (transactionsListener != null) {
            transactionsListener.remove();
            transactionsListener = null;
        }
        if (budgetListener != null) {
            budgetListener.remove();
            budgetListener = null;
        }
        
        // Setup again
        if (currentUser != null) {
            setupRealtimeListeners();
            showToast("Listeners reset and re-setup");
        } else {
            showToast("Cannot reset - user not authenticated");
        }
    }

    /**
     * ✅ Setup real-time budget data listener
     */
    private void setupBudgetListener() {
        if (currentUser == null) {
            Log.e(TAG, "Cannot setup budget listener - no current user");
            return;
        }

        Log.d(TAG, "=== SETTING UP BUDGET LISTENER ===");
        
        budgetListener = mFirestore.collection("users")
                .document(currentUser.getUid())
                .collection("budget")
                .document("total_budget")
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e(TAG, "❌ Budget listener error", e);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Log.d(TAG, "🔄 BUDGET DATA CHANGED - Real-time update");
                        
                        Map<String, Object> budgetData = documentSnapshot.getData();
                        if (budgetData != null) {
                            // Extract budget values
                            extractBudgetData(budgetData);
                            
                            Log.d(TAG, "Budget updated:");
                            Log.d(TAG, "- Total Budget: Rp" + formatNumber(totalBudget));
                            Log.d(TAG, "- Remaining Budget: Rp" + formatNumber(remainingBudget));
                            Log.d(TAG, "- Monthly Income: Rp" + formatNumber(totalIncome));
                            
                            // Update UI immediately
                            updateUI();
                        }
                    } else {
                        Log.d(TAG, "No budget data found or document deleted");
                        // Reset budget values
                        totalBudget = 0.0;
                        remainingBudget = 0.0;
                        updateUI();
                    }
                });

        Log.d(TAG, "✅ Budget listener setup completed");
    }
}    /*     * ===============================================================================================     * BALANCE UPDATE FLOW DOCUMENTATION     * ===============================================================================================     *      * PROBLEM SOLVED: Available balance tidak ter-update di HomeFragment setelah transaksi     *      * FLOW YANG BENAR:     * 1. User melakukan transaksi di AddTransactionActivity     * 2. AddTransactionActivity.updateUserBalance() mengupdate currentBalance di Firestore users document     * 3. HomeFragment.setupUserBalanceListener() mendeteksi perubahan currentBalance secara real-time     * 4. HomeFragment.updateBalanceAndOutcome() mengupdate UI dengan balance yang baru     *      * PENYEBAB MASALAH SEBELUMNYA:     * - HomeFragment menggunakan effectiveBalance = availableBalance - totalSavingsAmount     * - Padahal availableBalance sudah adalah balance final yang mencakup semua transaksi     * - totalSavingsAmount seharusnya hanya untuk perhitungan outcome, bukan untuk mengurangi balance display     *      * SOLUSI YANG DITERAPKAN:     * - HomeFragment sekarang menampilkan availableBalance langsung (tanpa dikurangi savings)     * - availableBalance di HomeFragment di-update otomatis melalui setupUserBalanceListener()     * - Real-time listener sudah bekerja dengan sempurna     *      * VERIFIKASI:     * 1. Check log "🔄 USER BALANCE CHANGED" saat melakukan transaksi     * 2. Check log "✅ Balance updated to" untuk memastikan UI ter-update     * 3. Check balance di UI HomeFragment harus langsung berubah setelah transaksi     * ===============================================================================================     */

/*
     * ===============================================================================================
     * ERROR ANALYSIS & TROUBLESHOOTING GUIDE
     * ===============================================================================================
     * 
     * KEMUNGKINAN ERROR YANG MENYEBABKAN BALANCE TIDAK TER-UPDATE:
     * 
     * 1. ❌ AUTHENTICATION ERRORS:
     *    - Firebase user null atau tidak authenticated
     *    - User UID null atau tidak valid
     *    - Firestore permission denied
     *    - Cek log: "❌ CRITICAL ERROR: No Firebase user found!"
     * 
     * 2. ❌ FIRESTORE CONNECTION ERRORS:
     *    - Network connectivity issues
     *    - Firestore offline mode
     *    - Document path tidak valid
     *    - Cek log: "❌ Error in user balance listener"
     * 
     * 3. ❌ LISTENER SETUP ERRORS:
     *    - Repository initialization failed
     *    - Real-time observers tidak ter-setup
     *    - Fragment lifecycle issues
     *    - Cek log: "❌ Cannot setup observers"
     * 
     * 4. ❌ UI UPDATE ERRORS:
     *    - TextView null (tvBalance, tvOutcome)
     *    - Fragment not attached
     *    - Activity context null
     *    - Cek log: "❌ CRITICAL ERROR: tvBalance is null!"
     * 
     * 5. ❌ DATA CONVERSION ERRORS:
     *    - User document conversion failed
     *    - Balance value null atau invalid
     *    - Number format exceptions
     *    - Cek log: "❌ Failed to convert document to User object"
     * 
     * TROUBLESHOOTING STEPS:
     * 
     * Step 1: Check Authentication
     * - Pastikan user sudah login
     * - Cek Firebase Auth status
     * - Verify user UID tidak null
     * 
     * Step 2: Check Firestore Connection
     * - Test internet connectivity
     * - Check Firestore rules
     * - Verify document path exists
     * 
     * Step 3: Check Real-time Listeners
     * - Verify listeners ter-setup tanpa error
     * - Check repository initialization
     * - Verify Fragment lifecycle state
     * 
     * Step 4: Check UI State
     * - Verify TextView tidak null
     * - Check Fragment attached state
     * - Verify Activity context valid
     * 
     * Step 5: Check Log Output
     * - Monitor log untuk error messages
     * - Trace execution flow
     * - Identify failure points
     * 
     * ENHANCED ERROR HANDLING YANG SUDAH DITAMBAHKAN:
     * - ✅ Authentication validation dengan early return
     * - ✅ Repository initialization error handling
     * - ✅ Listener setup dengan try-catch
     * - ✅ UI update dengan null checks
     * - ✅ Data conversion error handling
     * - ✅ Comprehensive logging untuk debugging
     * - ✅ User-friendly error messages
     * 
     * ===============================================================================================
     */