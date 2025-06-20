package com.example.spendly.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;

import androidx.annotation.NonNull;

import com.example.spendly.database.BudgetDatabaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BudgetRepository {
    private static final String TAG = "BudgetRepository";
    // Singleton instance
    private static BudgetRepository instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final BudgetDatabaseHelper dbHelper;

    // For background threading - using a thread pool for better performance
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Add caching to improve performance
    private final LruCache<String, Map<String, Object>> budgetCache;
    private final LruCache<String, Map<String, Object>> categoriesCache;
    private final LruCache<String, Boolean> existsCache;

    // Cache keys
    private static final String TOTAL_BUDGET_KEY = "total_budget";
    private static final String CATEGORIES_KEY = "categories";
    private static final String BUDGET_EXISTS_KEY = "budget_exists";
    private static final String CATEGORIES_EXISTS_KEY = "categories_exists";

    private static final int CACHE_SIZE = 4 * 1024 * 1024; // 4MB cache

    private BudgetRepository(Context context) {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        dbHelper = new BudgetDatabaseHelper(context.getApplicationContext());

        // Create a fixed thread pool for optimized performance
        executor = Executors.newFixedThreadPool(3); // Use 3 threads for database operations

        // Initialize caches
        budgetCache = new LruCache<>(CACHE_SIZE);
        categoriesCache = new LruCache<>(CACHE_SIZE);
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
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError(new Exception("User not logged in"));
            return;
        }

        final String userId = currentUser.getUid();

        // Clear any cached data
        budgetCache.remove(getCacheKey(userId, TOTAL_BUDGET_KEY));
        existsCache.remove(getCacheKey(userId, BUDGET_EXISTS_KEY));

        // Save to Firebase first
        db.collection("users").document(userId)
                .collection("budget").document("total_budget")
                .set(budgetData)
                .addOnSuccessListener(aVoid -> {
                    // If Firebase save succeeds, save to SQLite on background thread
                    executor.execute(() -> {
                        try {
                            dbHelper.saveTotalBudget(userId, budgetData);
                            // Cache the result
                            budgetCache.put(getCacheKey(userId, TOTAL_BUDGET_KEY), budgetData);
                            existsCache.put(getCacheKey(userId, BUDGET_EXISTS_KEY), true);

                            // Return result on main thread
                            mainHandler.post(() -> callback.onSuccess(budgetData));
                        } catch (Exception e) {
                            Log.e(TAG, "Error saving to SQLite: " + e.getMessage());
                            mainHandler.post(() -> callback.onError(e));
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving to Firebase: " + e.getMessage());
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
     * Saves budget category to both Firebase and local SQLite database
     */
    public void saveBudgetCategory(String category, Map<String, Object> categoryData,
                                   final BudgetCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError(new Exception("User not logged in"));
            return;
        }

        final String userId = currentUser.getUid();

        // Clear cached categories data
        categoriesCache.remove(getCacheKey(userId, CATEGORIES_KEY));
        existsCache.remove(getCacheKey(userId, CATEGORIES_EXISTS_KEY));

        // Save to Firebase first
        db.collection("users").document(userId)
                .collection("budget").document("categories")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Object> categories = new HashMap<>();
                        DocumentSnapshot document = task.getResult();

                        if (document != null && document.exists() && document.getData() != null) {
                            categories = document.getData();
                        }

                        // Add or update this category
                        categories.put(category, categoryData);

                        // Need to create a final copy of categories for use in lambda
                        final Map<String, Object> finalCategories = categories;

                        // Save updated categories to Firebase
                        db.collection("users").document(userId)
                                .collection("budget").document("categories")
                                .set(finalCategories)
                                .addOnSuccessListener(aVoid -> {
                                    // Save to SQLite in background
                                    executor.execute(() -> {
                                        try {
                                            // Use a more performant bulk save
                                            dbHelper.saveBudgetCategories(userId, finalCategories);

                                            // Cache the categories
                                            categoriesCache.put(getCacheKey(userId, CATEGORIES_KEY), finalCategories);
                                            existsCache.put(getCacheKey(userId, CATEGORIES_EXISTS_KEY), true);

                                            mainHandler.post(() -> callback.onSuccess(categoryData));
                                        } catch (Exception e) {
                                            mainHandler.post(() -> callback.onError(e));
                                        }
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    // Try to save to SQLite anyway
                                    executor.execute(() -> {
                                        try {
                                            dbHelper.saveBudgetCategory(userId, category, categoryData);
                                            Map<String, Object> result = new HashMap<>(categoryData);
                                            result.put("offline_only", true);

                                            // Update cache
                                            Map<String, Object> cachedCategories = categoriesCache.get(getCacheKey(userId, CATEGORIES_KEY));
                                            if (cachedCategories != null) {
                                                cachedCategories.put(category, result);
                                            } else {
                                                Map<String, Object> newCachedCategories = new HashMap<>();
                                                newCachedCategories.put(category, result);
                                                cachedCategories = newCachedCategories;
                                            }
                                            categoriesCache.put(getCacheKey(userId, CATEGORIES_KEY), cachedCategories);
                                            existsCache.put(getCacheKey(userId, CATEGORIES_EXISTS_KEY), true);

                                            mainHandler.post(() -> callback.onSuccess(result));
                                        } catch (Exception ex) {
                                            mainHandler.post(() -> callback.onError(ex));
                                        }
                                    });
                                });
                    } else {
                        callback.onError(task.getException());
                    }
                });
    }

    /**
     * Updates the remaining budget in Firebase and SQLite
     */
    public void updateRemainingBudget(double newRemainingBudget, String formattedRemaining,
                                      final BudgetCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError(new Exception("User not logged in"));
            return;
        }

        final String userId = currentUser.getUid();

        // Clear budget cache
        budgetCache.remove(getCacheKey(userId, TOTAL_BUDGET_KEY));

        // Update in Firebase
        db.collection("users").document(userId)
                .collection("budget").document("total_budget")
                .update("remaining_budget", newRemainingBudget,
                        "remaining_formatted", formattedRemaining)
                .addOnSuccessListener(aVoid -> {
                    // Update in SQLite on background thread
                    executor.execute(() -> {
                        try {
                            dbHelper.updateRemainingBudget(userId, newRemainingBudget, formattedRemaining);
                            Map<String, Object> result = new HashMap<>();
                            result.put("remaining_budget", newRemainingBudget);
                            result.put("remaining_formatted", formattedRemaining);

                            // Update cache
                            Map<String, Object> cachedBudget = budgetCache.get(getCacheKey(userId, TOTAL_BUDGET_KEY));
                            if (cachedBudget != null) {
                                cachedBudget.put("remaining_budget", newRemainingBudget);
                                cachedBudget.put("remaining_formatted", formattedRemaining);
                                budgetCache.put(getCacheKey(userId, TOTAL_BUDGET_KEY), cachedBudget);
                            }

                            mainHandler.post(() -> callback.onSuccess(result));
                        } catch (Exception e) {
                            mainHandler.post(() -> callback.onError(e));
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    // Try to update SQLite anyway
                    executor.execute(() -> {
                        try {
                            dbHelper.updateRemainingBudget(userId, newRemainingBudget, formattedRemaining);
                            Map<String, Object> result = new HashMap<>();
                            result.put("remaining_budget", newRemainingBudget);
                            result.put("remaining_formatted", formattedRemaining);
                            result.put("offline_only", true);

                            // Update cache
                            Map<String, Object> cachedBudget = budgetCache.get(getCacheKey(userId, TOTAL_BUDGET_KEY));
                            if (cachedBudget != null) {
                                cachedBudget.put("remaining_budget", newRemainingBudget);
                                cachedBudget.put("remaining_formatted", formattedRemaining);
                                cachedBudget.put("offline_only", true);
                                budgetCache.put(getCacheKey(userId, TOTAL_BUDGET_KEY), cachedBudget);
                            }

                            mainHandler.post(() -> callback.onSuccess(result));
                        } catch (Exception ex) {
                            mainHandler.post(() -> callback.onError(ex));
                        }
                    });
                });
    }

    /**
     * Gets total budget data - checks cache first, then tries Firebase, then falls back to SQLite
     */
    public void getTotalBudget(final BudgetCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError(new Exception("User not logged in"));
            return;
        }

        final String userId = currentUser.getUid();
        final String cacheKey = getCacheKey(userId, TOTAL_BUDGET_KEY);

        // First check if we have this in cache
        Map<String, Object> cachedBudget = budgetCache.get(cacheKey);
        if (cachedBudget != null) {
            callback.onSuccess(cachedBudget);
            return;
        }

        // Try to get from Firebase first
        db.collection("users").document(userId)
                .collection("budget").document("total_budget")
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.getData() != null) {
                        Map<String, Object> data = document.getData();

                        // Cache the result
                        budgetCache.put(cacheKey, data);
                        existsCache.put(getCacheKey(userId, BUDGET_EXISTS_KEY), true);

                        // Save the data to SQLite in the background for offline access
                        executor.execute(() -> dbHelper.saveTotalBudget(userId, data));

                        callback.onSuccess(data);
                    } else {
                        // If not in Firebase, try to get from SQLite
                        executor.execute(() -> {
                            Map<String, Object> data = dbHelper.getTotalBudget(userId);

                            if (!data.isEmpty()) {
                                data.put("offline_only", true);

                                // Cache the result
                                budgetCache.put(cacheKey, data);
                                existsCache.put(getCacheKey(userId, BUDGET_EXISTS_KEY), true);
                            }

                            mainHandler.post(() -> {
                                if (!data.isEmpty()) {
                                    callback.onSuccess(data);
                                } else {
                                    callback.onError(new Exception("No budget data found"));
                                }
                            });
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting data from Firebase: " + e.getMessage());

                    // Try to get from SQLite as fallback
                    executor.execute(() -> {
                        Map<String, Object> data = dbHelper.getTotalBudget(userId);

                        if (!data.isEmpty()) {
                            data.put("offline_only", true);

                            // Cache the result
                            budgetCache.put(cacheKey, data);
                            existsCache.put(getCacheKey(userId, BUDGET_EXISTS_KEY), true);
                        }

                        mainHandler.post(() -> {
                            if (!data.isEmpty()) {
                                callback.onSuccess(data);
                            } else {
                                callback.onError(e);
                            }
                        });
                    });
                });
    }

    /**
     * Gets budget categories - checks cache first, then tries Firebase, falls back to SQLite
     */
    public void getBudgetCategories(final BudgetCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError(new Exception("User not logged in"));
            return;
        }

        final String userId = currentUser.getUid();
        final String cacheKey = getCacheKey(userId, CATEGORIES_KEY);

        // First check if we have this in cache
        Map<String, Object> cachedCategories = categoriesCache.get(cacheKey);
        if (cachedCategories != null) {
            callback.onSuccess(cachedCategories);
            return;
        }

        // Try to get from Firebase first
        db.collection("users").document(userId)
                .collection("budget").document("categories")
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.getData() != null) {
                        Map<String, Object> data = document.getData();

                        // Cache the result
                        categoriesCache.put(cacheKey, data);
                        existsCache.put(getCacheKey(userId, CATEGORIES_EXISTS_KEY), true);

                        // Save the data to SQLite in the background for offline access
                        executor.execute(() -> dbHelper.saveBudgetCategories(userId, data));

                        callback.onSuccess(data);
                    } else {
                        // If not in Firebase, try to get from SQLite
                        executor.execute(() -> {
                            Map<String, Object> data = dbHelper.getBudgetCategories(userId);

                            if (!data.isEmpty()) {
                                data.put("offline_only", true);

                                // Cache the result
                                categoriesCache.put(cacheKey, data);
                                existsCache.put(getCacheKey(userId, CATEGORIES_EXISTS_KEY), true);
                            }

                            mainHandler.post(() -> {
                                if (!data.isEmpty()) {
                                    callback.onSuccess(data);
                                } else {
                                    callback.onError(new Exception("No budget categories found"));
                                }
                            });
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting categories from Firebase: " + e.getMessage());

                    // Try to get from SQLite as fallback
                    executor.execute(() -> {
                        Map<String, Object> data = dbHelper.getBudgetCategories(userId);

                        if (!data.isEmpty()) {
                            data.put("offline_only", true);

                            // Cache the result
                            categoriesCache.put(cacheKey, data);
                            existsCache.put(getCacheKey(userId, CATEGORIES_EXISTS_KEY), true);
                        }

                        mainHandler.post(() -> {
                            if (!data.isEmpty()) {
                                callback.onSuccess(data);
                            } else {
                                callback.onError(e);
                            }
                        });
                    });
                });
    }

    /**
     * Checks if the user has budget data - checks cache first, uses optimized query otherwise
     */
    public void checkBudgetExists(@NonNull final BudgetCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError(new Exception("User not logged in"));
            return;
        }

        final String userId = currentUser.getUid();
        final String cacheKey = getCacheKey(userId, BUDGET_EXISTS_KEY);

        // Check cache first
        Boolean existsInCache = existsCache.get(cacheKey);
        if (existsInCache != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("exists", existsInCache);
            callback.onSuccess(result);
            return;
        }

        // Use optimized exists query rather than fetching the whole document
        db.collection("users").document(userId)
                .collection("budget").document("total_budget")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        boolean docExists = document != null && document.exists() && document.getData() != null;

                        // Cache the result
                        existsCache.put(cacheKey, docExists);

                        Map<String, Object> result = new HashMap<>();
                        result.put("exists", docExists);

                        if (docExists) {
                            callback.onSuccess(result);
                        } else {
                            // Check SQLite as fallback
                            executor.execute(() -> {
                                boolean localExists = dbHelper.hasBudgetData(userId);

                                // Update cache
                                existsCache.put(cacheKey, localExists);

                                Map<String, Object> localResult = new HashMap<>();
                                localResult.put("exists", localExists);
                                if (localExists) {
                                    localResult.put("offline_only", true);
                                }

                                mainHandler.post(() -> callback.onSuccess(localResult));
                            });
                        }
                    } else {
                        executor.execute(() -> {
                            boolean localExists = dbHelper.hasBudgetData(userId);

                            // Update cache
                            existsCache.put(cacheKey, localExists);

                            Map<String, Object> result = new HashMap<>();
                            result.put("exists", localExists);
                            if (localExists) {
                                result.put("offline_only", true);
                            }

                            mainHandler.post(() -> callback.onSuccess(result));
                        });
                    }
                });
    }

    /**
     * Checks if the user has budget categories
     */
    public void checkCategoriesExist(@NonNull final BudgetCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError(new Exception("User not logged in"));
            return;
        }

        final String userId = currentUser.getUid();
        final String cacheKey = getCacheKey(userId, CATEGORIES_EXISTS_KEY);

        // Check cache first
        Boolean existsInCache = existsCache.get(cacheKey);
        if (existsInCache != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("exists", existsInCache);
            callback.onSuccess(result);
            return;
        }

        // Use optimized exists query
        db.collection("users").document(userId)
                .collection("budget").document("categories")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        boolean docExists = document != null && document.exists() && document.getData() != null;

                        // Cache the result
                        existsCache.put(cacheKey, docExists);

                        Map<String, Object> result = new HashMap<>();
                        result.put("exists", docExists);

                        if (docExists) {
                            callback.onSuccess(result);
                        } else {
                            // Check SQLite as fallback
                            executor.execute(() -> {
                                boolean localExists = dbHelper.hasBudgetCategories(userId);

                                // Update cache
                                existsCache.put(cacheKey, localExists);

                                Map<String, Object> localResult = new HashMap<>();
                                localResult.put("exists", localExists);
                                if (localExists) {
                                    localResult.put("offline_only", true);
                                }

                                mainHandler.post(() -> callback.onSuccess(localResult));
                            });
                        }
                    } else {
                        executor.execute(() -> {
                            boolean localExists = dbHelper.hasBudgetCategories(userId);

                            // Update cache
                            existsCache.put(cacheKey, localExists);

                            Map<String, Object> result = new HashMap<>();
                            result.put("exists", localExists);
                            if (localExists) {
                                result.put("offline_only", true);
                            }

                            mainHandler.post(() -> callback.onSuccess(result));
                        });
                    }
                });
    }

    /**
     * Syncs local data with Firebase when device comes online
     */
    public void syncWithFirebase(final BudgetCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError(new Exception("User not logged in"));
            return;
        }

        final String userId = currentUser.getUid();

        executor.execute(() -> {
            try {
                // Get data from local database
                Map<String, Object> totalBudget = dbHelper.getTotalBudget(userId);
                Map<String, Object> categories = dbHelper.getBudgetCategories(userId);

                if (!totalBudget.isEmpty()) {
                    // Sync total budget to Firebase
                    db.collection("users").document(userId)
                            .collection("budget").document("total_budget")
                            .set(totalBudget);

                    // Update cache
                    budgetCache.put(getCacheKey(userId, TOTAL_BUDGET_KEY), totalBudget);
                }

                if (!categories.isEmpty()) {
                    // Sync categories to Firebase
                    db.collection("users").document(userId)
                            .collection("budget").document("categories")
                            .set(categories);

                    // Update cache
                    categoriesCache.put(getCacheKey(userId, CATEGORIES_KEY), categories);
                }

                mainHandler.post(() -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("sync_success", true);
                    callback.onSuccess(result);
                });

            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    /**
     * Clears all cache to free memory
     */
    public void clearCache() {
        budgetCache.evictAll();
        categoriesCache.evictAll();
        existsCache.evictAll();
    }

    /**
     * Shutdown the executor service when app is closing to prevent memory leaks
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}

