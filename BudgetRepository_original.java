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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

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

    // Track active operations to prevent premature shutdown
    private final AtomicInteger activeOperations = new AtomicInteger(0);

    private BudgetRepository(Context context) {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        dbHelper = new BudgetDatabaseHelper(context);
        executor = Executors.newFixedThreadPool(3);
        
        // Initialize caches
        budgetCache = new LruCache<String, Map<String, Object>>(CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, Map<String, Object> value) {
                return key.length() + (value != null ? value.size() * 64 : 0);
            }
        };
        
        existsCache = new LruCache<String, Boolean>(CACHE_SIZE / 4) {
            @Override
            protected int sizeOf(String key, Boolean value) {
                return key.length() + 4;
            }
        };
    }

    public static synchronized BudgetRepository getInstance(Context context) {
        if (instance == null) {
            instance = new BudgetRepository(context.getApplicationContext());
        }
        return instance;
    }

    // Interface for callbacks
    public interface BudgetCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String error);
    }

    // Operation tracking methods
    private void startOperation() {
        activeOperations.incrementAndGet();
    }

    private void endOperation() {
        activeOperations.decrementAndGet();
    }

    public boolean hasActiveOperations() {
        return activeOperations.get() > 0;
    }

    // Safe execute wrapper
    private void safeExecute(Runnable task) {
        try {
            executor.execute(task);
        } catch (RejectedExecutionException e) {
            Log.w(TAG, "Task rejected, executor may be shutting down: " + e.getMessage());
            mainHandler.post(() -> {
                // Handle the case where executor is shut down
                Log.w(TAG, "Could not execute background task, executor unavailable");
            });
        }
    }

    // Get current user
    private FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    // Get user ID
    private String getUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // Save total budget to Firestore
    public void saveTotalBudget(double budget, BudgetCallback callback) {
        startOperation();
        
        safeExecute(() -> {
            try {
                String userId = getUserId();
                if (userId == null) {
                    mainHandler.post(() -> {
                        endOperation();
                        callback.onError("User not authenticated");
                    });
                    return;
                }

                Map<String, Object> budgetData = new HashMap<>();
                budgetData.put("totalBudget", budget);
                budgetData.put("userId", userId);
                budgetData.put("timestamp", System.currentTimeMillis());

                db.collection("budgets").document(userId)
                    .set(budgetData)
                    .addOnSuccessListener(aVoid -> {
                        // Clear cache
                        budgetCache.remove(TOTAL_BUDGET_KEY);
                        existsCache.remove(BUDGET_EXISTS_KEY);
                        
                        Log.d(TAG, "Total budget saved successfully: " + budget);
                        mainHandler.post(() -> {
                            endOperation();
                            callback.onSuccess();
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error saving total budget", e);
                        mainHandler.post(() -> {
                            endOperation();
                            callback.onError("Failed to save budget: " + e.getMessage());
                        });
                    });
            } catch (Exception e) {
                Log.e(TAG, "Exception in saveTotalBudget", e);
                mainHandler.post(() -> {
                    endOperation();
                    callback.onError("Exception: " + e.getMessage());
                });
            }
        });
    }

    // Get total budget from Firestore
    public void getTotalBudget(DataCallback<Double> callback) {
        startOperation();
        
        // Check cache first
        Map<String, Object> cachedData = budgetCache.get(TOTAL_BUDGET_KEY);
        if (cachedData != null && cachedData.containsKey("totalBudget")) {
            Double budget = (Double) cachedData.get("totalBudget");
            endOperation();
            callback.onSuccess(budget != null ? budget : 0.0);
            return;
        }

        safeExecute(() -> {
            try {
                String userId = getUserId();
                if (userId == null) {
                    mainHandler.post(() -> {
                        endOperation();
                        callback.onError("User not authenticated");
                    });
                    return;
                }

                db.collection("budgets").document(userId)
                    .get()
                    .addOnSuccessListener(document -> {
                        double budget = 0.0;
                        if (document.exists()) {
                            Object budgetObj = document.get("totalBudget");
                            if (budgetObj instanceof Number) {
                                budget = ((Number) budgetObj).doubleValue();
                            }
                            
                            // Cache the result
                            Map<String, Object> data = new HashMap<>();
                            data.put("totalBudget", budget);
                            budgetCache.put(TOTAL_BUDGET_KEY, data);
                        }
                        
                        final double finalBudget = budget;
                        mainHandler.post(() -> {
                            endOperation();
                            callback.onSuccess(finalBudget);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting total budget", e);
                        mainHandler.post(() -> {
                            endOperation();
                            callback.onError("Failed to get budget: " + e.getMessage());
                        });
                    });
            } catch (Exception e) {
                Log.e(TAG, "Exception in getTotalBudget", e);
                mainHandler.post(() -> {
                    endOperation();
                    callback.onError("Exception: " + e.getMessage());
                });
            }
        });
    }

    // Update remaining budget (subtract spent amount)
    public void updateRemainingBudget(double spentAmount, BudgetCallback callback) {
        startOperation();
        
        safeExecute(() -> {
            try {
                String userId = getUserId();
                if (userId == null) {
                    mainHandler.post(() -> {
                        endOperation();
                        callback.onError("User not authenticated");
                    });
                    return;
                }

                db.collection("budgets").document(userId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            Object totalBudgetObj = document.get("totalBudget");
                            Object currentSpentObj = document.get("totalSpent");
                            
                            double totalBudget = 0.0;
                            double currentSpent = 0.0;
                            
                            if (totalBudgetObj instanceof Number) {
                                totalBudget = ((Number) totalBudgetObj).doubleValue();
                            }
                            if (currentSpentObj instanceof Number) {
                                currentSpent = ((Number) currentSpentObj).doubleValue();
                            }
                            
                            double newTotalSpent = currentSpent + spentAmount;
                            
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("totalSpent", newTotalSpent);
                            updates.put("remainingBudget", totalBudget - newTotalSpent);
                            updates.put("lastUpdated", System.currentTimeMillis());
                            
                            db.collection("budgets").document(userId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    // Clear relevant caches
                                    budgetCache.remove(TOTAL_BUDGET_KEY);
                                    
                                    Log.d(TAG, "Remaining budget updated successfully");
                                    mainHandler.post(() -> {
                                        endOperation();
                                        callback.onSuccess();
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error updating remaining budget", e);
                                    mainHandler.post(() -> {
                                        endOperation();
                                        callback.onError("Failed to update budget: " + e.getMessage());
                                    });
                                });
                        } else {
                            mainHandler.post(() -> {
                                endOperation();
                                callback.onError("Budget not found");
                            });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting budget for update", e);
                        mainHandler.post(() -> {
                            endOperation();
                            callback.onError("Failed to get budget: " + e.getMessage());
                        });
                    });
            } catch (Exception e) {
                Log.e(TAG, "Exception in updateRemainingBudget", e);
                mainHandler.post(() -> {
                    endOperation();
                    callback.onError("Exception: " + e.getMessage());
                });
            }
        });
    }

    // Save budget categories
    public void saveBudgetCategories(Map<String, Double> categoryBudgets, BudgetCallback callback) {
        startOperation();
        
        safeExecute(() -> {
            try {
                String userId = getUserId();
                if (userId == null) {
                    mainHandler.post(() -> {
                        endOperation();
                        callback.onError("User not authenticated");
                    });
                    return;
                }

                Map<String, Object> budgetData = new HashMap<>();
                budgetData.put("categories", categoryBudgets);
                budgetData.put("userId", userId);
                budgetData.put("timestamp", System.currentTimeMillis());

                db.collection("budget_categories").document(userId)
                    .set(budgetData)
                    .addOnSuccessListener(aVoid -> {
                        // Clear cache
                        budgetCache.remove(BUDGET_CATEGORIES_KEY);
                        existsCache.remove(CATEGORIES_EXISTS_KEY);
                        
                        Log.d(TAG, "Budget categories saved successfully");
                        mainHandler.post(() -> {
                            endOperation();
                            callback.onSuccess();
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error saving budget categories", e);
                        mainHandler.post(() -> {
                            endOperation();
                            callback.onError("Failed to save categories: " + e.getMessage());
                        });
                    });
            } catch (Exception e) {
                Log.e(TAG, "Exception in saveBudgetCategories", e);
                mainHandler.post(() -> {
                    endOperation();
                    callback.onError("Exception: " + e.getMessage());
                });
            }
        });
    }

    // Get budget categories
    public void getBudgetCategories(DataCallback<Map<String, Double>> callback) {
        startOperation();
        
        // Check cache first
        Map<String, Object> cachedData = budgetCache.get(BUDGET_CATEGORIES_KEY);
        if (cachedData != null && cachedData.containsKey("categories")) {
            @SuppressWarnings("unchecked")
            Map<String, Double> categories = (Map<String, Double>) cachedData.get("categories");
            endOperation();
            callback.onSuccess(categories != null ? categories : new HashMap<>());
            return;
        }

        safeExecute(() -> {
            try {
                String userId = getUserId();
                if (userId == null) {
                    mainHandler.post(() -> {
                        endOperation();
                        callback.onError("User not authenticated");
                    });
                    return;
                }

                db.collection("budget_categories").document(userId)
                    .get()
                    .addOnSuccessListener(document -> {
                        Map<String, Double> categories = new HashMap<>();
                        if (document.exists()) {
                            Object categoriesObj = document.get("categories");
                            if (categoriesObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> categoriesMap = (Map<String, Object>) categoriesObj;
                                for (Map.Entry<String, Object> entry : categoriesMap.entrySet()) {
                                    if (entry.getValue() instanceof Number) {
                                        categories.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
                                    }
                                }
                            }
                            
                            // Cache the result
                            Map<String, Object> data = new HashMap<>();
                            data.put("categories", categories);
                            budgetCache.put(BUDGET_CATEGORIES_KEY, data);
                        }
                        
                        mainHandler.post(() -> {
                            endOperation();
                            callback.onSuccess(categories);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting budget categories", e);
                        mainHandler.post(() -> {
                            endOperation();
                            callback.onError("Failed to get categories: " + e.getMessage());
                        });
                    });
            } catch (Exception e) {
                Log.e(TAG, "Exception in getBudgetCategories", e);
                mainHandler.post(() -> {
                    endOperation();
                    callback.onError("Exception: " + e.getMessage());
                });
            }
        });
    }

    // Update category spent amount
    public void updateCategorySpent(String category, double amount, BudgetCallback callback) {
        startOperation();
        
        safeExecute(() -> {
            try {
                String userId = getUserId();
                if (userId == null) {
                    mainHandler.post(() -> {
                        endOperation();
                        callback.onError("User not authenticated");
                    });
                    return;
                }

                db.collection("category_spending").document(userId)
                    .get()
                    .addOnSuccessListener(document -> {
                        Map<String, Object> spentData = new HashMap<>();
                        if (document.exists()) {
                            Object existingData = document.getData();
                            if (existingData instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> existingMap = (Map<String, Object>) existingData;
                                spentData.putAll(existingMap);
                            }
                        }
                        
                        // Update the specific category
                        double currentSpent;
                        Object currentSpentObj = spentData.get(category);
                        if (currentSpentObj instanceof Number) {
                            currentSpent = ((Number) currentSpentObj).doubleValue();
                        } else {
                            currentSpent = 0.0;
                        }

                        spentData.put(category, currentSpent + amount);
                        spentData.put("userId", userId);
                        spentData.put("lastUpdated", System.currentTimeMillis());
                        
                        db.collection("category_spending").document(userId)
                            .set(spentData)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Category spending updated: " + category + " -> " + (currentSpent + amount));
                                mainHandler.post(() -> {
                                    endOperation();
                                    callback.onSuccess();
                                });
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating category spending", e);
                                mainHandler.post(() -> {
                                    endOperation();
                                    callback.onError("Failed to update category spending: " + e.getMessage());
                                });
                            });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting category spending data", e);
                        mainHandler.post(() -> {
                            endOperation();
                            callback.onError("Failed to get spending data: " + e.getMessage());
                        });
                    });
            } catch (Exception e) {
                Log.e(TAG, "Exception in updateCategorySpent", e);
                mainHandler.post(() -> {
                    endOperation();
                    callback.onError("Exception: " + e.getMessage());
                });
            }
        });
    }

    // Get category spent amounts
    public void getCategorySpending(DataCallback<Map<String, Double>> callback) {
        startOperation();
        
        safeExecute(() -> {
            try {
                String userId = getUserId();
                if (userId == null) {
                    mainHandler.post(() -> {
                        endOperation();
                        callback.onError("User not authenticated");
                    });
                    return;
                }

                db.collection("category_spending").document(userId)
                    .get()
                    .addOnSuccessListener(document -> {
                        Map<String, Double> spending = new HashMap<>();
                        if (document.exists()) {
                            Map<String, Object> data = document.getData();
                            if (data != null) {
                                for (Map.Entry<String, Object> entry : data.entrySet()) {
                                    if (!entry.getKey().equals("userId") && 
                                        !entry.getKey().equals("lastUpdated") &&
                                        entry.getValue() instanceof Number) {
                                        spending.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
                                    }
                                }
                            }
                        }
                        
                        mainHandler.post(() -> {
                            endOperation();
                            callback.onSuccess(spending);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting category spending", e);
                        mainHandler.post(() -> {
                            endOperation();
                            callback.onError("Failed to get spending: " + e.getMessage());
                        });
                    });
            } catch (Exception e) {
                Log.e(TAG, "Exception in getCategorySpending", e);
                mainHandler.post(() -> {
                    endOperation();
                    callback.onError("Exception: " + e.getMessage());
                });
            }
        });
    }

    // Check if budget exists
    public void checkBudgetExists(DataCallback<Boolean> callback) {
        startOperation();
        
        // Check cache first
        Boolean cachedExists = existsCache.get(BUDGET_EXISTS_KEY);
        if (cachedExists != null) {
            endOperation();
            callback.onSuccess(cachedExists);
            return;
        }

        safeExecute(() -> {
            try {
                String userId = getUserId();
                if (userId == null) {
                    mainHandler.post(() -> {
                        endOperation();
                        callback.onError("User not authenticated");
                    });
                    return;
                }

                db.collection("budgets").document(userId)
                    .get()
                    .addOnSuccessListener(document -> {
                        boolean exists = document.exists() && document.get("totalBudget") != null;
                        
                        // Cache the result
                        existsCache.put(BUDGET_EXISTS_KEY, exists);
                        
                        mainHandler.post(() -> {
                            endOperation();
                            callback.onSuccess(exists);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error checking budget existence", e);
                        mainHandler.post(() -> {
                            endOperation();
                            callback.onError("Failed to check budget existence: " + e.getMessage());
                        });
                    });
            } catch (Exception e) {
                Log.e(TAG, "Exception in checkBudgetExists", e);
                mainHandler.post(() -> {
                    endOperation();
                    callback.onError("Exception: " + e.getMessage());
                });
            }
        });
    }

    // Check if categories exist
    public void checkCategoriesExist(DataCallback<Boolean> callback) {
        startOperation();
        
        // Check cache first
        Boolean cachedExists = existsCache.get(CATEGORIES_EXISTS_KEY);
        if (cachedExists != null) {
            endOperation();
            callback.onSuccess(cachedExists);
            return;
        }

        safeExecute(() -> {
            try {
                String userId = getUserId();
                if (userId == null) {
                    mainHandler.post(() -> {
                        endOperation();
                        callback.onError("User not authenticated");
                    });
                    return;
                }

                db.collection("budget_categories").document(userId)
                    .get()
                    .addOnSuccessListener(document -> {
                        boolean exists = document.exists() && document.get("categories") != null;
                        
                        // Cache the result
                        existsCache.put(CATEGORIES_EXISTS_KEY, exists);
                        
                        mainHandler.post(() -> {
                            endOperation();
                            callback.onSuccess(exists);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error checking categories existence", e);
                        mainHandler.post(() -> {
                            endOperation();
                            callback.onError("Failed to check categories: " + e.getMessage());
                        });
                    });
            } catch (Exception e) {
                Log.e(TAG, "Exception in checkCategoriesExist", e);
                mainHandler.post(() -> {
                    endOperation();
                    callback.onError("Exception: " + e.getMessage());
                });
            }
        });
    }

    // Cleanup method to properly shut down resources
    public void cleanup() {
        if (hasActiveOperations()) {
            Log.w(TAG, "Cleanup called but there are still active operations: " + activeOperations.get());
            return;
        }
        
        Log.d(TAG, "Cleaning up BudgetRepository resources");
        
        if (executor != null && !executor.isShutdown()) {
            try {
                executor.shutdown();
                // Give it a moment to finish gracefully
                if (!executor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
                Log.d(TAG, "Executor shutdown completed");
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
                Log.w(TAG, "Executor shutdown interrupted");
            } catch (Exception e) {
                Log.w(TAG, "Error during executor shutdown: " + e.getMessage());
            }
        }
        
        // Clear caches
        try {
            if (budgetCache != null) {
                budgetCache.evictAll();
                Log.d(TAG, "Budget cache cleared");
            }
            if (existsCache != null) {
                existsCache.evictAll();
                Log.d(TAG, "Exists cache cleared");
            }
        } catch (Exception e) {
            Log.w(TAG, "Error clearing caches: " + e.getMessage());
        }
    }
}
