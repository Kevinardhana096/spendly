package com.example.spendly.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;

import com.example.spendly.database.BudgetDatabaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BudgetRepository {
    private static final String TAG = "BudgetRepository";
    
    // Singleton instance
    private static BudgetRepository instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final BudgetDatabaseHelper dbHelper;

    // For background threading
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Cache
    private final LruCache<String, Map<String, Object>> budgetCache;
    private final LruCache<String, Boolean> existsCache;    // Cache keys
    private static final String TOTAL_BUDGET_KEY = "total_budget";
    private static final String BUDGET_EXISTS_KEY = "budget_exists";
    private static final String BUDGET_CATEGORIES_KEY = "budget_categories";
    private static final String CATEGORIES_EXISTS_KEY = "categories_exists";
    private static final int CACHE_SIZE = 4 * 1024 * 1024; // 4MB cache

    private BudgetRepository(Context context) {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        dbHelper = new BudgetDatabaseHelper(context.getApplicationContext());
        executor = Executors.newFixedThreadPool(3);
        budgetCache = new LruCache<>(CACHE_SIZE);
        existsCache = new LruCache<>(CACHE_SIZE);
    }

    public static synchronized BudgetRepository getInstance(Context context) {
        if (instance == null) {
            instance = new BudgetRepository(context);
        }
        return instance;
    }

    // Interface for callbacks
    public interface BudgetCallback {
        void onSuccess(Map<String, Object> data);
        void onError(Exception e);
    }

    private String getCacheKey(String userId, String type) {
        return userId + "_" + type;
    }

    /**
     * Saves total budget to both Firebase and local SQLite database
     */
    public void saveTotalBudget(Map<String, Object> budgetData, final BudgetCallback callback) {
        android.util.Log.d(TAG, "saveTotalBudget() called");
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            android.util.Log.e(TAG, "User not logged in");
            callback.onError(new Exception("User not logged in"));
            return;
        }

        final String userId = currentUser.getUid();
        android.util.Log.d(TAG, "Saving budget for user: " + userId);
        android.util.Log.d(TAG, "Budget data: " + budgetData.toString());

        // Clear any cached data
        budgetCache.remove(getCacheKey(userId, TOTAL_BUDGET_KEY));
        existsCache.remove(getCacheKey(userId, BUDGET_EXISTS_KEY));

        // Save to Firebase first
        android.util.Log.d(TAG, "Attempting to save to Firebase...");
        db.collection("users").document(userId)
                .collection("budget").document("total_budget")
                .set(budgetData)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d(TAG, "✅ Firebase save successful, now saving to SQLite...");
                    // If Firebase save succeeds, save to SQLite on background thread
                    executor.execute(() -> {
                        try {
                            android.util.Log.d(TAG, "Saving to SQLite database...");
                            dbHelper.saveTotalBudget(userId, budgetData);
                            android.util.Log.d(TAG, "✅ SQLite save successful");
                            // Cache the result
                            budgetCache.put(getCacheKey(userId, TOTAL_BUDGET_KEY), budgetData);
                            existsCache.put(getCacheKey(userId, BUDGET_EXISTS_KEY), true);

                            // Return result on main thread
                            android.util.Log.d(TAG, "Calling success callback...");
                            mainHandler.post(() -> callback.onSuccess(budgetData));
                        } catch (Exception e) {
                            android.util.Log.e(TAG, "❌ Error saving to SQLite: " + e.getMessage(), e);
                            mainHandler.post(() -> callback.onError(e));
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e(TAG, "❌ Error saving to Firebase: " + e.getMessage(), e);
                    // Try to save to SQLite anyway for offline access
                    executor.execute(() -> {
                        try {
                            dbHelper.saveTotalBudget(userId, budgetData);
                            Map<String, Object> result = new HashMap<>(budgetData);
                            result.put("offline_only", true);

                            // Cache the result
                            budgetCache.put(getCacheKey(userId, TOTAL_BUDGET_KEY), result);
                            existsCache.put(getCacheKey(userId, BUDGET_EXISTS_KEY), true);

                            mainHandler.post(() -> callback.onSuccess(result));
                        } catch (Exception ex) {
                            mainHandler.post(() -> callback.onError(ex));
                        }
                    });
                });
    }

    /**
     * Get total budget from Firebase or local cache
     */
    public void getTotalBudget(final BudgetCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError(new Exception("User not logged in"));
            return;
        }

        final String userId = currentUser.getUid();
        final String cacheKey = getCacheKey(userId, TOTAL_BUDGET_KEY);

        // Check cache first
        Map<String, Object> cachedBudget = budgetCache.get(cacheKey);
        if (cachedBudget != null) {
            callback.onSuccess(cachedBudget);
            return;
        }

        // Load from Firebase
        db.collection("users").document(userId)
                .collection("budget").document("total_budget")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.getData() != null) {
                        Map<String, Object> data = documentSnapshot.getData();
                        // Cache the result
                        budgetCache.put(cacheKey, data);
                        existsCache.put(getCacheKey(userId, BUDGET_EXISTS_KEY), true);

                        // Save to SQLite for offline access
                        executor.execute(() -> dbHelper.saveTotalBudget(userId, data));

                        callback.onSuccess(data);
                    } else {
                        // Try loading from SQLite
                        executor.execute(() -> {
                            Map<String, Object> data = dbHelper.getTotalBudget(userId);
                            if (!data.isEmpty()) {
                                // Cache the result
                                budgetCache.put(cacheKey, data);
                                existsCache.put(getCacheKey(userId, BUDGET_EXISTS_KEY), true);
                                
                                mainHandler.post(() -> callback.onSuccess(data));
                            } else {
                                mainHandler.post(() -> callback.onError(new Exception("No budget data found")));
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    // Try loading from SQLite as fallback
                    executor.execute(() -> {
                        Map<String, Object> data = dbHelper.getTotalBudget(userId);
                        if (!data.isEmpty()) {
                            budgetCache.put(cacheKey, data);
                            mainHandler.post(() -> callback.onSuccess(data));
                        } else {
                            mainHandler.post(() -> callback.onError(e));
                        }
                    });                });
    }

    /**
     * Check if budget categories exist in Firebase or local cache
     */
    public void checkCategoriesExist(final BudgetCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError(new Exception("User not logged in"));
            return;
        }

        final String userId = currentUser.getUid();
        final String cacheKey = getCacheKey(userId, CATEGORIES_EXISTS_KEY);

        // Check cache first
        Boolean cachedExists = existsCache.get(cacheKey);
        if (cachedExists != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("exists", cachedExists);
            callback.onSuccess(result);
            return;
        }

        // Check Firebase
        db.collection("users").document(userId)
                .collection("budget").document("categories")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    boolean exists = documentSnapshot.exists() && documentSnapshot.getData() != null;
                    
                    // Cache the result
                    existsCache.put(cacheKey, exists);
                    
                    Map<String, Object> result = new HashMap<>();
                    result.put("exists", exists);
                    callback.onSuccess(result);
                })
                .addOnFailureListener(e -> {
                    // Try checking SQLite as fallback
                    executor.execute(() -> {
                        Map<String, Object> data = dbHelper.getBudgetCategories(userId);
                        boolean exists = !data.isEmpty();
                        existsCache.put(cacheKey, exists);
                        
                        Map<String, Object> result = new HashMap<>();
                        result.put("exists", exists);
                        mainHandler.post(() -> callback.onSuccess(result));
                    });
                });
    }

    /**
     * Get budget categories from Firebase or local cache
     */
    public void getBudgetCategories(final BudgetCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError(new Exception("User not logged in"));
            return;
        }

        final String userId = currentUser.getUid();
        final String cacheKey = getCacheKey(userId, BUDGET_CATEGORIES_KEY);

        // Check cache first
        Map<String, Object> cachedCategories = budgetCache.get(cacheKey);
        if (cachedCategories != null) {
            callback.onSuccess(cachedCategories);
            return;
        }

        // Load from Firebase
        db.collection("users").document(userId)
                .collection("budget").document("categories")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.getData() != null) {
                        Map<String, Object> data = documentSnapshot.getData();
                        // Cache the result
                        budgetCache.put(cacheKey, data);
                        existsCache.put(getCacheKey(userId, CATEGORIES_EXISTS_KEY), true);

                        // Save to SQLite for offline access
                        executor.execute(() -> dbHelper.saveBudgetCategories(userId, data));

                        callback.onSuccess(data);
                    } else {
                        // Try loading from SQLite
                        executor.execute(() -> {
                            Map<String, Object> data = dbHelper.getBudgetCategories(userId);
                            if (!data.isEmpty()) {
                                // Cache the result
                                budgetCache.put(cacheKey, data);
                                existsCache.put(getCacheKey(userId, CATEGORIES_EXISTS_KEY), true);
                                
                                mainHandler.post(() -> callback.onSuccess(data));
                            } else {
                                mainHandler.post(() -> callback.onError(new Exception("No budget categories found")));
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    // Try loading from SQLite as fallback
                    executor.execute(() -> {
                        Map<String, Object> data = dbHelper.getBudgetCategories(userId);
                        if (!data.isEmpty()) {
                            budgetCache.put(cacheKey, data);
                            mainHandler.post(() -> callback.onSuccess(data));
                        } else {
                            mainHandler.post(() -> callback.onError(e));
                        }
                    });
                });
    }
}
