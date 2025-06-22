package com.example.spendly.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.spendly.database.SpendlyDatabase;
import com.example.spendly.database.dao.TransactionDao;
import com.example.spendly.database.entity.TransactionEntity;
import com.example.spendly.model.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RealtimeTransactionRepository {
    private static final String TAG = "RealtimeTransactionRepo";
    private static RealtimeTransactionRepository instance;
    
    private final TransactionDao transactionDao;
    private final FirebaseFirestore firestore;
    private final Executor executor;
    private ListenerRegistration transactionsListener;
    
    // LiveData for real-time updates
    private final MutableLiveData<List<Transaction>> transactionsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Double> totalIncomeLiveData = new MutableLiveData<>();
    private final MutableLiveData<Double> totalExpensesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private RealtimeTransactionRepository(Context context) {
        SpendlyDatabase database = SpendlyDatabase.getDatabase(context);
        transactionDao = database.transactionDao();
        firestore = FirebaseFirestore.getInstance();
        executor = Executors.newFixedThreadPool(4);
    }

    public static RealtimeTransactionRepository getInstance(Context context) {
        if (instance == null) {
            instance = new RealtimeTransactionRepository(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Get transactions with real-time updates and offline caching
     */
    public LiveData<List<Transaction>> getTransactions(String userId) {
        // First load from local database (offline support)
        loadFromLocalDatabase(userId);
        
        // Then setup real-time listener for Firebase updates
        setupRealtimeTransactionsListener(userId);
        
        return transactionsLiveData;
    }

    /**
     * Get recent transactions with limit
     */
    public LiveData<List<Transaction>> getRecentTransactions(String userId, int limit) {
        // Load from local first
        executor.execute(() -> {
            try {
                List<TransactionEntity> entities = transactionDao.getAllTransactionsSync(userId);
                List<Transaction> transactions = new ArrayList<>();
                
                int maxItems = Math.min(entities.size(), limit);
                for (int i = 0; i < maxItems; i++) {
                    transactions.add(convertToTransaction(entities.get(i)));
                }
                
                if (!transactions.isEmpty()) {
                    transactionsLiveData.postValue(transactions);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading recent transactions", e);
            }
        });
        
        return transactionsLiveData;
    }

    /**
     * Setup real-time listener for transactions
     */
    private void setupRealtimeTransactionsListener(String userId) {
        if (transactionsListener != null) {
            transactionsListener.remove();
        }

        Log.d(TAG, "Setting up real-time transactions listener for user: " + userId);

        transactionsListener = firestore.collection("users")
                .document(userId)
                .collection("transactions")
                .orderBy("date", Query.Direction.DESCENDING)
                .limit(100) // Limit for performance
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Transactions listener error", error);
                        errorMessage.postValue("Failed to sync transactions: " + error.getMessage());
                        return;
                    }

                    if (querySnapshot != null) {
                        Log.d(TAG, "Real-time transactions update: " + querySnapshot.size() + " items");
                        
                        List<Transaction> transactions = new ArrayList<>();
                        List<TransactionEntity> entities = new ArrayList<>();
                        double totalIncome = 0.0;
                        double totalExpenses = 0.0;
                          for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            // Manual conversion to handle Long to Date conversion
                            Transaction transaction = convertDocumentToTransaction(document);
                            if (transaction != null) {
                                transaction.setId(document.getId());
                                transactions.add(transaction);
                                
                                // Calculate totals
                                if ("income".equals(transaction.getType())) {
                                    totalIncome += transaction.getAmount();
                                } else if ("expense".equals(transaction.getType())) {
                                    totalExpenses += transaction.getAmount();
                                }
                                
                                // Convert to entity for local storage
                                TransactionEntity entity = convertToEntity(transaction, userId);
                                entities.add(entity);
                            }
                        }
                        
                        // Update LiveData
                        transactionsLiveData.postValue(transactions);
                        totalIncomeLiveData.postValue(totalIncome);
                        totalExpensesLiveData.postValue(totalExpenses);
                        
                        // Update local database in background
                        final double finalTotalIncome = totalIncome;
                        final double finalTotalExpenses = totalExpenses;
                        
                        executor.execute(() -> {
                            try {
                                transactionDao.deleteAllTransactions(userId);
                                transactionDao.insertTransactions(entities);
                                Log.d(TAG, "Local transactions updated: " + entities.size() + 
                                     " items, Income: " + finalTotalIncome + 
                                     ", Expenses: " + finalTotalExpenses);
                            } catch (Exception e) {
                                Log.e(TAG, "Error updating local transactions database", e);
                            }
                        });
                    }
                });
    }

    /**
     * Load transactions from local database (offline support)
     */
    private void loadFromLocalDatabase(String userId) {
        executor.execute(() -> {
            try {
                List<TransactionEntity> entities = transactionDao.getAllTransactionsSync(userId);
                List<Transaction> transactions = new ArrayList<>();
                double totalIncome = 0.0;
                double totalExpenses = 0.0;
                
                for (TransactionEntity entity : entities) {
                    Transaction transaction = convertToTransaction(entity);
                    transactions.add(transaction);
                    
                    if ("income".equals(transaction.getType())) {
                        totalIncome += transaction.getAmount();
                    } else if ("expense".equals(transaction.getType())) {
                        totalExpenses += transaction.getAmount();
                    }
                }
                
                if (!transactions.isEmpty()) {
                    Log.d(TAG, "Loaded " + transactions.size() + " transactions from local database");
                    transactionsLiveData.postValue(transactions);
                    totalIncomeLiveData.postValue(totalIncome);
                    totalExpensesLiveData.postValue(totalExpenses);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading from local database", e);
            }
        });
    }

    /**
     * Add new transaction
     */
    public void addTransaction(Transaction transaction, OnTransactionOperationListener listener) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            listener.onError("User not authenticated");
            return;
        }

        isLoading.postValue(true);
          // Add to Firebase - convert to Map to handle Date properly
        Map<String, Object> transactionData = convertTransactionToMap(transaction);
        firestore.collection("users")
                .document(userId)
                .collection("transactions")
                .add(transactionData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Transaction added to Firebase: " + documentReference.getId());
                    
                    // Update local database
                    transaction.setId(documentReference.getId());
                    TransactionEntity entity = convertToEntity(transaction, userId);
                    
                    executor.execute(() -> {
                        transactionDao.insertTransaction(entity);
                        Log.d(TAG, "Transaction added to local database");
                    });
                    
                    isLoading.postValue(false);
                    listener.onSuccess("Transaction added successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding transaction", e);
                    isLoading.postValue(false);
                    listener.onError("Failed to add transaction: " + e.getMessage());
                });
    }

    /**
     * Get live data for totals
     */
    public LiveData<Double> getTotalIncome() {
        return totalIncomeLiveData;
    }

    public LiveData<Double> getTotalExpenses() {
        return totalExpensesLiveData;
    }

    public LiveData<Boolean> getLoadingState() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        if (transactionsListener != null) {
            transactionsListener.remove();
            transactionsListener = null;
        }
        Log.d(TAG, "RealtimeTransactionRepository cleaned up");
    }

    // Helper conversion methods
    private TransactionEntity convertToEntity(Transaction transaction, String userId) {
        return new TransactionEntity(
                transaction.getId(),
                userId,
                transaction.getType(),                transaction.getAmount(),
                transaction.getCategory(),
                transaction.getAccount(), // Use account instead of description
                transaction.getDate() != null ? transaction.getDate().getTime() : System.currentTimeMillis(), // Convert Date to long
                System.currentTimeMillis(), // createdAt
                System.currentTimeMillis()  // updatedAt
        );
    }

    private Transaction convertToTransaction(TransactionEntity entity) {
        Transaction transaction = new Transaction();
        transaction.setId(entity.getId());
        transaction.setType(entity.getType());
        transaction.setAmount(entity.getAmount());
        transaction.setCategory(entity.getCategory());
        transaction.setAccount(entity.getDescription()); // Map description back to account        transaction.setDate(new java.util.Date(entity.getDate())); // Convert long to Date
        return transaction;
    }    private Transaction convertDocumentToTransaction(DocumentSnapshot document) {
        try {
            Transaction transaction = new Transaction();
            
            // Handle string fields
            if (document.contains("userId")) transaction.setUserId(document.getString("userId"));
            if (document.contains("category")) transaction.setCategory(document.getString("category"));
            if (document.contains("account")) transaction.setAccount(document.getString("account"));
            if (document.contains("type")) transaction.setType(document.getString("type"));
            
            // Handle numeric fields
            if (document.contains("amount")) {
                Object amountObj = document.get("amount");
                if (amountObj instanceof Number) {
                    transaction.setAmount(((Number) amountObj).doubleValue());
                }
            }
              // Handle date field - convert Long timestamp to Date
            if (document.contains("date")) {
                Object dateObj = document.get("date");
                if (dateObj instanceof Long) {
                    transaction.setDate(new java.util.Date((Long) dateObj));
                } else if (dateObj instanceof com.google.firebase.Timestamp) {
                    transaction.setDate(((com.google.firebase.Timestamp) dateObj).toDate());
                } else if (dateObj instanceof java.util.Date) {
                    transaction.setDate((java.util.Date) dateObj);
                } else {
                    // Handle null or invalid date objects
                    Log.w(TAG, "Invalid date object in transaction document: " + dateObj);
                    transaction.setDate(new java.util.Date(System.currentTimeMillis()));
                }
            } else {
                // No date field found, use current time
                Log.w(TAG, "No date field found in transaction document, using current time");
                transaction.setDate(new java.util.Date(System.currentTimeMillis()));
            }
            
            return transaction;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to transaction", e);
            return null;
        }
    }

    private Map<String, Object> convertTransactionToMap(Transaction transaction) {
        Map<String, Object> map = new HashMap<>();
        
        if (transaction.getUserId() != null) map.put("userId", transaction.getUserId());
        if (transaction.getCategory() != null) map.put("category", transaction.getCategory());
        if (transaction.getAccount() != null) map.put("account", transaction.getAccount());
        if (transaction.getType() != null) map.put("type", transaction.getType());
        
        map.put("amount", transaction.getAmount());
        
        // Convert Date to timestamp (Long) for Firebase storage
        if (transaction.getDate() != null) {
            map.put("date", transaction.getDate().getTime());
        } else {
            map.put("date", System.currentTimeMillis());
        }
        
        return map;
    }

    // Callback interface
    public interface OnTransactionOperationListener {
        void onSuccess(String message);
        void onError(String error);
    }
}
