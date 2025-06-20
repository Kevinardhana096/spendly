package com.example.spendly.repository;

import android.content.Context;
import android.util.Log;

import com.example.spendly.model.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionRepository {
    private static final String TAG = "TransactionRepository";
    private static TransactionRepository instance;
    private final FirebaseFirestore db;
    private final Context context;

    // Singleton pattern
    private TransactionRepository(Context context) {
        this.context = context.getApplicationContext();
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized TransactionRepository getInstance(Context context) {
        if (instance == null) {
            instance = new TransactionRepository(context);
        }
        return instance;
    }

    /**
     * Interface for transaction operation callbacks
     */
    public interface TransactionCallback {
        void onSuccess(Map<String, Object> data);
        void onError(Exception e);
    }

    /**
     * Save a transaction to Firestore
     */
    public void saveTransaction(Transaction transaction, final TransactionCallback callback) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            callback.onError(new Exception("User not authenticated"));
            return;
        }

        // Convert transaction to Map for Firestore
        Map<String, Object> transactionData = new HashMap<>();
        transactionData.put("userId", transaction.getUserId());
        transactionData.put("amount", transaction.getAmount());
        transactionData.put("category", transaction.getCategory());
        transactionData.put("account", transaction.getAccount());
        transactionData.put("date", transaction.getDate());
        transactionData.put("type", transaction.getType());
        transactionData.put("formattedAmount", transaction.getFormattedAmount());

        // Add transaction to Firestore
        db.collection("transactions")
                .add(transactionData)
                .addOnSuccessListener(documentReference -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", documentReference.getId());
                    callback.onSuccess(result);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving transaction", e);
                    callback.onError(e);
                });
    }

    /**
     * Get all transactions for the current user
     * Note: This requires a compound index on userId and date
     */
    public void getUserTransactions(final TransactionCallback callback) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            callback.onError(new Exception("User not authenticated"));
            return;
        }

        try {
            // Use a simple query first to avoid index issues
            db.collection("transactions")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<Transaction> transactions = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Transaction transaction = documentSnapshotToTransaction(document);
                                if (transaction != null) {
                                    transactions.add(transaction);
                                }
                            }

                            // Sort transactions by date (most recent first) in memory
                            transactions.sort((t1, t2) -> t2.getDate().compareTo(t1.getDate()));

                            Map<String, Object> result = new HashMap<>();
                            result.put("transactions", transactions);
                            callback.onSuccess(result);
                        } else {
                            Log.e(TAG, "Error getting transactions", task.getException());
                            callback.onError(task.getException());
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in getUserTransactions", e);
            callback.onError(e);
        }
    }

    /**
     * Update budget for a given transaction
     */
    public void updateBudgetForTransaction(Transaction transaction, BudgetRepository budgetRepository,
                                           final TransactionCallback callback) {
        if ("expense".equals(transaction.getType())) {
            // First update category spent
            budgetRepository.updateCategorySpent(
                transaction.getCategory(),
                transaction.getAmount(),
                new BudgetRepository.BudgetCallback() {
                    @Override
                    public void onSuccess(Map<String, Object> data) {
                        // Now also update the total remaining budget for expenses
                        budgetRepository.getTotalBudget(new BudgetRepository.BudgetCallback() {
                            @Override
                            public void onSuccess(Map<String, Object> totalBudgetData) {
                                // Extract current remaining budget
                                double currentRemainingBudget = 0.0;
                                if (totalBudgetData.containsKey("remaining_budget")) {
                                    if (totalBudgetData.get("remaining_budget") instanceof Double) {
                                        currentRemainingBudget = (Double) totalBudgetData.get("remaining_budget");
                                    } else if (totalBudgetData.get("remaining_budget") instanceof Long) {
                                        currentRemainingBudget = ((Long) totalBudgetData.get("remaining_budget")).doubleValue();
                                    }
                                }

                                // Calculate new remaining budget
                                double newRemainingBudget = currentRemainingBudget - transaction.getAmount();

                                // Format for display
                                java.text.NumberFormat formatter = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("in", "ID"));
                                String formattedRemaining = formatter.format(newRemainingBudget)
                                        .replace("Rp", "")
                                        .trim();

                                // Update the remaining budget
                                budgetRepository.updateRemainingBudget(
                                    newRemainingBudget,
                                    formattedRemaining,
                                    new BudgetRepository.BudgetCallback() {
                                        @Override
                                        public void onSuccess(Map<String, Object> data) {
                                            callback.onSuccess(new HashMap<>());
                                        }

                                        @Override
                                        public void onError(Exception e) {
                                            Log.e(TAG, "Error updating remaining budget: " + e.getMessage());
                                            // Still return success since the category was updated
                                            callback.onSuccess(new HashMap<>());
                                        }
                                    });
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "Error getting total budget: " + e.getMessage());
                                // Still return success since the category was updated
                                callback.onSuccess(new HashMap<>());
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        callback.onError(e);
                    }
                }
            );
        } else {
            // For income transactions, no budget update needed
            callback.onSuccess(new HashMap<>());
        }
    }

    /**
     * Get transactions within a date range
     */
    public void getTransactionsByDateRange(long startDate, long endDate, final TransactionCallback callback) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            callback.onError(new Exception("User not authenticated"));
            return;
        }

        try {
            // Use a simple query with in-memory filtering to avoid index requirements
            db.collection("transactions")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<Transaction> transactions = new ArrayList<>();
                            Date startDateObj = new Date(startDate);
                            Date endDateObj = new Date(endDate);

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Transaction transaction = documentSnapshotToTransaction(document);
                                if (transaction != null) {
                                    // Filter by date range in memory
                                    Date transDate = transaction.getDate();
                                    if (transDate != null &&
                                        transDate.compareTo(startDateObj) >= 0 &&
                                        transDate.compareTo(endDateObj) <= 0) {
                                        transactions.add(transaction);
                                    }
                                }
                            }

                            // Sort by date in memory
                            transactions.sort((t1, t2) -> t2.getDate().compareTo(t1.getDate()));

                            Map<String, Object> result = new HashMap<>();
                            result.put("transactions", transactions);
                            callback.onSuccess(result);
                        } else {
                            Log.e(TAG, "Error getting transactions by date range", task.getException());
                            callback.onError(task.getException());
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in getTransactionsByDateRange", e);
            callback.onError(e);
        }
    }

    /**
     * Get transactions for a specific month
     */
    public void getMonthlyTransactions(Date startDate, Date endDate, final TransactionCallback callback) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            callback.onError(new Exception("User not authenticated"));
            return;
        }

        try {
            // Use a simple query with in-memory filtering to avoid index requirements
            db.collection("transactions")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<Transaction> transactions = new ArrayList<>();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Transaction transaction = documentSnapshotToTransaction(document);
                                if (transaction != null) {
                                    // Filter by date range in memory
                                    Date transDate = transaction.getDate();
                                    if (transDate != null &&
                                        transDate.compareTo(startDate) >= 0 &&
                                        transDate.compareTo(endDate) <= 0) {
                                        transactions.add(transaction);
                                    }
                                }
                            }

                            // Sort by date in memory
                            transactions.sort((t1, t2) -> t2.getDate().compareTo(t1.getDate()));

                            Map<String, Object> result = new HashMap<>();
                            result.put("transactions", transactions);
                            callback.onSuccess(result);
                        } else {
                            Log.e(TAG, "Error getting monthly transactions", task.getException());
                            callback.onError(task.getException());
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception in getMonthlyTransactions", e);
            callback.onError(e);
        }
    }

    /**
     * Delete a transaction
     */
    public void deleteTransaction(String transactionId, final TransactionCallback callback) {
        db.collection("transactions")
                .document(transactionId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    callback.onSuccess(result);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting transaction", e);
                    callback.onError(e);
                });
    }

    /**
     * Convert a Firestore document to a Transaction object
     */
    private Transaction documentSnapshotToTransaction(DocumentSnapshot document) {
        try {
            String userId = document.getString("userId");
            double amount = document.getDouble("amount") != null ? document.getDouble("amount") : 0.0;
            String category = document.getString("category");
            String account = document.getString("account");
            java.util.Date date = document.getDate("date");
            String type = document.getString("type");
            String formattedAmount = document.getString("formattedAmount");

            Transaction transaction = new Transaction(userId, amount, category, account, date, type);
            transaction.setId(document.getId());

            if (formattedAmount != null) {
                transaction.setFormattedAmount(formattedAmount);
            }

            return transaction;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to transaction", e);
            return null;
        }
    }
}
