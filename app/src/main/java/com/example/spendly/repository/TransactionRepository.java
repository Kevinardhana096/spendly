package com.example.spendly.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.spendly.model.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransactionRepository {
    private static final String TAG = "TransactionRepository";
    private static TransactionRepository instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    // For background operations
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private TransactionRepository(Context context) {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        executor = Executors.newFixedThreadPool(2);
    }

    public static synchronized TransactionRepository getInstance(Context context) {
        if (instance == null) {
            instance = new TransactionRepository(context);
        }
        return instance;
    }

    // Interface for callbacks
    public interface TransactionCallback {
        void onSuccess(Map<String, Object> data);
        void onError(Exception e);
    }

    /**
     * Saves a new transaction and updates budget data
     */
    public void saveTransaction(Transaction transaction, final TransactionCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError(new Exception("User not logged in"));
            return;
        }

        transaction.setUserId(currentUser.getUid());

        // Save to Firestore
        db.collection("users").document(currentUser.getUid())
                .collection("transactions")
                .add(transaction)
                .addOnSuccessListener(documentReference -> {
                    // Set the ID from Firestore
                    String id = documentReference.getId();
                    transaction.setId(id);

                    // Update the document with its ID
                    documentReference.update("id", id)
                            .addOnSuccessListener(aVoid -> {
                                Map<String, Object> result = new HashMap<>();
                                result.put("transaction", transaction);
                                result.put("success", true);
                                callback.onSuccess(result);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating transaction ID: " + e.getMessage());
                                // Still consider it a success since the transaction was saved
                                Map<String, Object> result = new HashMap<>();
                                result.put("transaction", transaction);
                                result.put("success", true);
                                callback.onSuccess(result);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving transaction: " + e.getMessage());
                    callback.onError(e);
                });
    }

    /**
     * Gets recent transactions for the current user
     */
    public void getRecentTransactions(int limit, final TransactionCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError(new Exception("User not logged in"));
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .collection("transactions")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Transaction> transactions = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Transaction transaction = document.toObject(Transaction.class);
                            transactions.add(transaction);
                        }

                        Map<String, Object> result = new HashMap<>();
                        result.put("transactions", transactions);
                        callback.onSuccess(result);
                    } else {
                        callback.onError(task.getException());
                    }
                });
    }

    /**
     * Gets transactions for the current month
     */
    public void getMonthlyTransactions(Date startDate, Date endDate, final TransactionCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError(new Exception("User not logged in"));
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .collection("transactions")
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Transaction> transactions = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Transaction transaction = document.toObject(Transaction.class);
                            transactions.add(transaction);
                        }

                        Map<String, Object> result = new HashMap<>();
                        result.put("transactions", transactions);
                        callback.onSuccess(result);
                    } else {
                        callback.onError(task.getException());
                    }
                });
    }

    /**
     * Updates budget data based on a transaction
     */
    public void updateBudgetForTransaction(Transaction transaction,
                                          BudgetRepository budgetRepository,
                                          final TransactionCallback callback) {
        if (transaction == null || budgetRepository == null) {
            callback.onError(new Exception("Invalid transaction or budget repository"));
            return;
        }

        // Only update budget for expense transactions
        if (!"expense".equalsIgnoreCase(transaction.getType())) {
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Not an expense - no budget update needed");
            callback.onSuccess(result);
            return;
        }

        // Get current budget data
        budgetRepository.getTotalBudget(new BudgetRepository.BudgetCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                // Calculate new remaining budget
                double remainingBudget = 0;
                if (data.containsKey("remaining_budget")) {
                    if (data.get("remaining_budget") instanceof Double) {
                        remainingBudget = (Double) data.get("remaining_budget");
                    } else if (data.get("remaining_budget") instanceof Long) {
                        remainingBudget = ((Long) data.get("remaining_budget")).doubleValue();
                    }
                }

                // Subtract transaction amount from remaining budget
                double newRemainingBudget = remainingBudget - transaction.getAmount();

                // Format the new remaining budget
                String formattedRemaining = formatNumber((int) newRemainingBudget);

                // Update the budget
                budgetRepository.updateRemainingBudget(newRemainingBudget, formattedRemaining,
                        new BudgetRepository.BudgetCallback() {
                            @Override
                            public void onSuccess(Map<String, Object> data) {
                                // Now update the category budget if we have category data
                                updateCategoryBudget(transaction, budgetRepository, callback);
                            }

                            @Override
                            public void onError(Exception e) {
                                callback.onError(e);
                            }
                        });
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    private void updateCategoryBudget(Transaction transaction,
                                     BudgetRepository budgetRepository,
                                     final TransactionCallback callback) {
        // Skip if no category
        if (transaction.getCategory() == null || transaction.getCategory().isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Budget updated, no category specified");
            callback.onSuccess(result);
            return;
        }

        // Get budget categories
        budgetRepository.getBudgetCategories(new BudgetRepository.BudgetCallback() {
            @Override
            public void onSuccess(Map<String, Object> categoriesData) {
                String category = transaction.getCategory();

                // Check if this category exists in the budget
                if (!categoriesData.containsKey(category)) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("message", "Total budget updated, category not found in budget");
                    callback.onSuccess(result);
                    return;
                }

                // Get category data
                @SuppressWarnings("unchecked")
                Map<String, Object> categoryData = (Map<String, Object>) categoriesData.get(category);

                // Calculate new spent amount for this category
                double spent = 0;
                if (categoryData.containsKey("spent")) {
                    if (categoryData.get("spent") instanceof Double) {
                        spent = (Double) categoryData.get("spent");
                    } else if (categoryData.get("spent") instanceof Long) {
                        spent = ((Long) categoryData.get("spent")).doubleValue();
                    }
                }

                double newSpent = spent + transaction.getAmount();
                String formattedSpent = formatNumber((int) newSpent);

                // Update category data
                categoryData.put("spent", newSpent);
                categoryData.put("formatted_spent", formattedSpent);

                // Save updated category
                budgetRepository.saveBudgetCategory(category, categoryData,
                        new BudgetRepository.BudgetCallback() {
                            @Override
                            public void onSuccess(Map<String, Object> data) {
                                Map<String, Object> result = new HashMap<>();
                                result.put("message", "Budget and category updated successfully");
                                callback.onSuccess(result);
                            }

                            @Override
                            public void onError(Exception e) {
                                callback.onError(e);
                            }
                        });
            }

            @Override
            public void onError(Exception e) {
                // If error getting categories, consider the operation successful anyway
                // since we already updated the total budget
                Map<String, Object> result = new HashMap<>();
                result.put("message", "Total budget updated, but failed to update category");
                callback.onSuccess(result);
            }
        });
    }

    private String formatNumber(int number) {
        return String.format(java.util.Locale.getDefault(), "%,d", number).replace(",", ".");
    }

    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
