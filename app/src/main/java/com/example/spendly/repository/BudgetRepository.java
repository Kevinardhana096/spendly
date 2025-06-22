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
    private final LruCache<String, Boolean> existsCache;
    
    // Cache keys
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
        void onError(String error);

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
            callback.onError("User not logged in");
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
                            mainHandler.post(() -> callback.onError(e.getMessage()));
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
                            mainHandler.post(() -> callback.onError(ex.getMessage()));
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
            callback.onError("User not logged in");
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
                                mainHandler.post(() -> callback.onError("No budget data found"));
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
                            mainHandler.post(() -> callback.onError(e.getMessage()));
                        }
                    });
                });
    }

    /**
     * Check if budget categories exist in Firebase or local cache
     */
    public void checkCategoriesExist(final BudgetCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("User not logged in");
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
            callback.onError("User not logged in");
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
                                mainHandler.post(() -> callback.onError("No budget categories found"));
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
                            mainHandler.post(() -> callback.onError(e.getMessage()));
                        }
                    });
                });
    }

    /**
     * Update the remaining budget in both Firebase and local SQLite database
     */
    public void updateRemainingBudget(double newRemainingBudget, String formattedRemaining, final BudgetCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("User not logged in");
            return;
        }

        final String userId = currentUser.getUid();
        
        // Create update data
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("remaining_budget", newRemainingBudget);
        updateData.put("formatted_remaining", formattedRemaining);
        updateData.put("last_updated", System.currentTimeMillis());

        // Clear cache for total budget since we're updating it
        budgetCache.remove(getCacheKey(userId, TOTAL_BUDGET_KEY));
        existsCache.remove(getCacheKey(userId, BUDGET_EXISTS_KEY));

        // Update Firebase first
        db.collection("users").document(userId)
                .collection("budget").document("total_budget")
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    // If Firebase update succeeds, update SQLite on background thread
                    executor.execute(() -> {
                        try {
                            dbHelper.updateRemainingBudget(userId, newRemainingBudget, formattedRemaining);
                            
                            // Update cache with new data
                            Map<String, Object> cachedData = budgetCache.get(getCacheKey(userId, TOTAL_BUDGET_KEY));
                            if (cachedData != null) {
                                cachedData.put("remaining_budget", newRemainingBudget);
                                cachedData.put("formatted_remaining", formattedRemaining);
                                cachedData.put("last_updated", updateData.get("last_updated"));
                                budgetCache.put(getCacheKey(userId, TOTAL_BUDGET_KEY), cachedData);
                            }

                            mainHandler.post(() -> callback.onSuccess(updateData));
                        } catch (Exception e) {
                            mainHandler.post(() -> callback.onError(e.getMessage()));
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    // Try to update SQLite anyway for offline access
                    executor.execute(() -> {
                        try {
                            dbHelper.updateRemainingBudget(userId, newRemainingBudget, formattedRemaining);
                            Map<String, Object> result = new HashMap<>(updateData);
                            result.put("offline_only", true);
                            mainHandler.post(() -> callback.onSuccess(result));
                        } catch (Exception ex) {
                            mainHandler.post(() -> callback.onError(ex.getMessage()));
                        }
                    });
                });
    }

    /**
     * Update the spent amount for a specific category in both Firebase and local SQLite database
     */
    public void updateCategorySpent(String category, double transactionAmount, final BudgetCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("User not logged in");
            return;
        }

        final String userId = currentUser.getUid();
        
        // First, get the current budget categories to find the specific category
        getBudgetCategories(new BudgetCallback() {
            @Override
            public void onSuccess(Map<String, Object> categoriesData) {
                // Find the category data
                Map<String, Object> categoryData = null;
                if (categoriesData.containsKey(category)) {
                    Object categoryObj = categoriesData.get(category);
                    if (categoryObj instanceof Map) {
                        categoryData = new HashMap<>((Map<String, Object>) categoryObj);
                    }
                }

                if (categoryData == null) {
                    callback.onError("Category not found: " + category);
                    return;
                }

                // Update the spent amount
                double currentSpent = 0.0;
                if (categoryData.containsKey("spent")) {
                    Object spentObj = categoryData.get("spent");
                    if (spentObj instanceof Double) {
                        currentSpent = (Double) spentObj;
                    } else if (spentObj instanceof Long) {
                        currentSpent = ((Long) spentObj).doubleValue();
                    }
                }

                final double newSpent = currentSpent + transactionAmount;
                categoryData.put("spent", newSpent);

                // Format the spent amount for display
                java.text.NumberFormat formatter = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("in", "ID"));
                final String formattedSpent = formatter.format(newSpent).replace("Rp", "").trim();
                categoryData.put("formatted_spent", formattedSpent);
                categoryData.put("last_updated", System.currentTimeMillis());

                // Update the category in the full categories data
                categoriesData.put(category, categoryData);

                // Make variables effectively final for lambda usage
                final Map<String, Object> finalCategoryData = categoryData;
                final Map<String, Object> finalCategoriesData = categoriesData;

                // Clear cache since we're updating
                budgetCache.remove(getCacheKey(userId, BUDGET_CATEGORIES_KEY));
                existsCache.remove(getCacheKey(userId, CATEGORIES_EXISTS_KEY));

                // Update Firebase first
                db.collection("users").document(userId)
                        .collection("budget").document("categories")
                        .set(finalCategoriesData)
                        .addOnSuccessListener(aVoid -> {
                            // If Firebase update succeeds, update SQLite on background thread
                            executor.execute(() -> {
                                try {
                                    dbHelper.saveBudgetCategory(userId, category, finalCategoryData);
                                    
                                    // Update cache
                                    budgetCache.put(getCacheKey(userId, BUDGET_CATEGORIES_KEY), finalCategoriesData);
                                    existsCache.put(getCacheKey(userId, CATEGORIES_EXISTS_KEY), true);

                                    Map<String, Object> result = new HashMap<>();
                                    result.put("category", category);
                                    result.put("new_spent", newSpent);
                                    result.put("formatted_spent", formattedSpent);

                                    mainHandler.post(() -> callback.onSuccess(result));
                                } catch (Exception e) {
                                    mainHandler.post(() -> callback.onError(e.getMessage()));
                                }
                            });
                        })
                        .addOnFailureListener(e -> {
                            // Try to update SQLite anyway for offline access
                            executor.execute(() -> {
                                try {
                                    dbHelper.saveBudgetCategory(userId, category, finalCategoryData);
                                    
                                    Map<String, Object> result = new HashMap<>();
                                    result.put("category", category);
                                    result.put("new_spent", newSpent);
                                    result.put("formatted_spent", formattedSpent);
                                    result.put("offline_only", true);

                                    mainHandler.post(() -> callback.onSuccess(result));
                                } catch (Exception ex) {
                                    mainHandler.post(() -> callback.onError(ex.getMessage()));
                                }
                            });
                        });
            }

            @Override
            public void onError(String error) {
                callback.onError("Failed to get categories for update: " + error);
            }

            @Override
            public void onError(Exception e) {

            }
        });
    }

    /**
     * Check if total budget exists in Firebase or local cache
     */
    public void checkBudgetExists(final BudgetCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("User not logged in");
            return;
        }

        final String userId = currentUser.getUid();
        final String cacheKey = getCacheKey(userId, BUDGET_EXISTS_KEY);

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
                .collection("budget").document("total_budget")
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
                        Map<String, Object> data = dbHelper.getTotalBudget(userId);
                        boolean exists = !data.isEmpty();
                        existsCache.put(cacheKey, exists);
                        
                        Map<String, Object> result = new HashMap<>();
                        result.put("exists", exists);
                        result.put("offline_only", true);
                        mainHandler.post(() -> callback.onSuccess(result));
                    });
                });
    }

    /**
     * Save a budget category to both Firebase and local SQLite database
     */
    public void saveBudgetCategory(String category, Map<String, Object> categoryData, final BudgetCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("User not logged in");
            return;
        }

        final String userId = currentUser.getUid();
        
        // Clear cache since we're updating
        budgetCache.remove(getCacheKey(userId, BUDGET_CATEGORIES_KEY));
        existsCache.remove(getCacheKey(userId, CATEGORIES_EXISTS_KEY));

        // First get existing categories to merge with new one
        getBudgetCategories(new BudgetCallback() {
            @Override
            public void onSuccess(Map<String, Object> existingCategories) {
                // Add/update the new category
                existingCategories.put(category, categoryData);
                
                // Save the complete categories to Firebase
                db.collection("users").document(userId)
                        .collection("budget").document("categories")
                        .set(existingCategories)
                        .addOnSuccessListener(aVoid -> {
                            // If Firebase save succeeds, save to SQLite on background thread
                            executor.execute(() -> {
                                try {
                                    dbHelper.saveBudgetCategory(userId, category, categoryData);
                                    
                                    // Update cache
                                    budgetCache.put(getCacheKey(userId, BUDGET_CATEGORIES_KEY), existingCategories);
                                    existsCache.put(getCacheKey(userId, CATEGORIES_EXISTS_KEY), true);

                                    Map<String, Object> result = new HashMap<>();
                                    result.put("category", category);
                                    result.put("data", categoryData);

                                    mainHandler.post(() -> callback.onSuccess(result));
                                } catch (Exception e) {
                                    mainHandler.post(() -> callback.onError(e.getMessage()));
                                }
                            });
                        })
                        .addOnFailureListener(e -> {
                            // Try to save SQLite anyway for offline access
                            executor.execute(() -> {
                                try {
                                    dbHelper.saveBudgetCategory(userId, category, categoryData);
                                    
                                    Map<String, Object> result = new HashMap<>();
                                    result.put("category", category);
                                    result.put("data", categoryData);
                                    result.put("offline_only", true);

                                    mainHandler.post(() -> callback.onSuccess(result));
                                } catch (Exception ex) {
                                    mainHandler.post(() -> callback.onError(ex.getMessage()));
                                }
                            });
                        });
            }

            @Override
            public void onError(String error) {
                // If can't get existing categories, create new map
                Map<String, Object> newCategories = new HashMap<>();
                newCategories.put(category, categoryData);
                
                // Save to Firebase
                db.collection("users").document(userId)
                        .collection("budget").document("categories")
                        .set(newCategories)
                        .addOnSuccessListener(aVoid -> {
                            executor.execute(() -> {
                                try {
                                    dbHelper.saveBudgetCategory(userId, category, categoryData);
                                    
                                    Map<String, Object> result = new HashMap<>();
                                    result.put("category", category);
                                    result.put("data", categoryData);

                                    mainHandler.post(() -> callback.onSuccess(result));
                                } catch (Exception ex) {
                                    mainHandler.post(() -> callback.onError(ex.getMessage()));
                                }
                            });
                        })
                        .addOnFailureListener(ex -> {
                            executor.execute(() -> {
                                try {
                                    dbHelper.saveBudgetCategory(userId, category, categoryData);
                                    
                                    Map<String, Object> result = new HashMap<>();
                                    result.put("category", category);
                                    result.put("data", categoryData);
                                    result.put("offline_only", true);

                                    mainHandler.post(() -> callback.onSuccess(result));
                                } catch (Exception exc) {
                                    mainHandler.post(() -> callback.onError(exc.getMessage()));
                                }
                            });
                        });
            }

            @Override
            public void onError(Exception e) {

            }
        });
    }

    /**
     * Sync offline data with Firebase when connection is restored
     */
    public void syncWithFirebase(final BudgetCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError("User not logged in");
            return;
        }

        final String userId = currentUser.getUid();
        
        executor.execute(() -> {
            try {
                // Get all local data
                Map<String, Object> localBudget = dbHelper.getTotalBudget(userId);
                Map<String, Object> localCategories = dbHelper.getBudgetCategories(userId);
                
                if (!localBudget.isEmpty()) {
                    // Sync total budget
                    db.collection("users").document(userId)
                            .collection("budget").document("total_budget")
                            .set(localBudget)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Total budget synced to Firebase");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to sync total budget", e);
                            });
                }
                
                if (!localCategories.isEmpty()) {
                    // Sync categories
                    db.collection("users").document(userId)
                            .collection("budget").document("categories")
                            .set(localCategories)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Categories synced to Firebase");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to sync categories", e);
                            });
                }
                
                // Clear cache to force fresh fetch
                budgetCache.evictAll();
                existsCache.evictAll();
                
                Map<String, Object> result = new HashMap<>();
                result.put("synced", true);
                result.put("total_budget_synced", !localBudget.isEmpty());
                result.put("categories_synced", !localCategories.isEmpty());
                
                mainHandler.post(() -> callback.onSuccess(result));
                
            } catch (Exception e) {
                Log.e(TAG, "Error during sync", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
}
