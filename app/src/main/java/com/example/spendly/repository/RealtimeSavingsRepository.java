package com.example.spendly.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.spendly.database.SpendlyDatabase;
import com.example.spendly.database.dao.SavingsDao;
import com.example.spendly.database.entity.SavingsEntity;
import com.example.spendly.model.SavingsItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class RealtimeSavingsRepository {
    private static final String TAG = "RealtimeSavingsRepository";
    private static RealtimeSavingsRepository instance;
    
    private final SavingsDao savingsDao;
    private final FirebaseFirestore firestore;
    private final Executor executor;
    private ListenerRegistration savingsListener;
    
    // LiveData for real-time updates
    private final MutableLiveData<List<SavingsItem>> savingsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private RealtimeSavingsRepository(Context context) {
        SpendlyDatabase database = SpendlyDatabase.getDatabase(context);
        savingsDao = database.savingsDao();
        firestore = FirebaseFirestore.getInstance();
        executor = Executors.newFixedThreadPool(4);
    }    public static RealtimeSavingsRepository getInstance(Context context) {
        if (instance == null) {
            instance = new RealtimeSavingsRepository(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Get savings with real-time updates and offline caching
     */
    public LiveData<List<SavingsItem>> getSavings(String userId) {
        // First load from local database (offline support)
        loadFromLocalDatabase(userId);
        
        // Then setup real-time listener for Firebase updates
        setupRealtimeSavingsListener(userId);
        
        return savingsLiveData;
    }

    /**
     * Setup real-time listener for savings updates
     */
    private void setupRealtimeSavingsListener(String userId) {
        if (savingsListener != null) {
            savingsListener.remove(); // Remove existing listener
        }

        Log.d(TAG, "Setting up real-time savings listener for user: " + userId);

        savingsListener = firestore.collection("users")
                .document(userId)
                .collection("savings")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Savings listener error", error);
                        errorMessage.postValue("Failed to sync savings: " + error.getMessage());
                        return;
                    }

                    if (querySnapshot != null) {
                        Log.d(TAG, "Real-time savings update received: " + querySnapshot.size() + " items");
                        
                        List<SavingsItem> savingsList = new ArrayList<>();
                        List<SavingsEntity> entities = new ArrayList<>();
                        
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            SavingsItem item = document.toObject(SavingsItem.class);
                            if (item != null) {
                                item.setId(document.getId());
                                savingsList.add(item);
                                
                                // Convert to entity for local storage
                                SavingsEntity entity = convertToEntity(item, userId);
                                entities.add(entity);
                            }
                        }
                        
                        // Update LiveData
                        savingsLiveData.postValue(savingsList);
                        
                        // Update local database in background
                        executor.execute(() -> {
                            try {
                                savingsDao.deleteAllSavings(userId); // Clear old data
                                savingsDao.insertSavingsList(entities); // Insert new data
                                Log.d(TAG, "Local savings database updated with " + entities.size() + " items");
                            } catch (Exception e) {
                                Log.e(TAG, "Error updating local savings database", e);
                            }
                        });
                    }
                });
    }

    /**
     * Load savings from local database (offline support)
     */
    private void loadFromLocalDatabase(String userId) {
        executor.execute(() -> {
            try {
                List<SavingsEntity> entities = savingsDao.getAllSavingsSync(userId);
                List<SavingsItem> savingsList = new ArrayList<>();
                
                for (SavingsEntity entity : entities) {
                    SavingsItem item = convertToSavingsItem(entity);
                    savingsList.add(item);
                }
                
                if (!savingsList.isEmpty()) {
                    Log.d(TAG, "Loaded " + savingsList.size() + " savings from local database");
                    savingsLiveData.postValue(savingsList);
                } else {
                    Log.d(TAG, "No savings found in local database");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading from local database", e);
            }
        });
    }

    /**
     * Add new savings item
     */
    public void addSavings(SavingsItem savingsItem, OnSavingsOperationListener listener) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            listener.onError("User not authenticated");
            return;
        }

        isLoading.postValue(true);
        
        // Add to Firebase
        firestore.collection("users")
                .document(userId)
                .collection("savings")
                .add(savingsItem)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Savings added to Firebase: " + documentReference.getId());
                    
                    // Update local database
                    savingsItem.setId(documentReference.getId());
                    SavingsEntity entity = convertToEntity(savingsItem, userId);
                    
                    executor.execute(() -> {
                        savingsDao.insertSavings(entity);
                        Log.d(TAG, "Savings added to local database");
                    });
                    
                    isLoading.postValue(false);
                    listener.onSuccess("Savings added successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding savings", e);
                    isLoading.postValue(false);
                    listener.onError("Failed to add savings: " + e.getMessage());
                });
    }

    /**
     * Update existing savings item
     */
    public void updateSavings(SavingsItem savingsItem, OnSavingsOperationListener listener) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null || savingsItem.getId() == null) {
            listener.onError("Invalid operation");
            return;
        }

        isLoading.postValue(true);
        
        // Update in Firebase
        firestore.collection("users")
                .document(userId)
                .collection("savings")
                .document(savingsItem.getId())
                .set(savingsItem)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Savings updated in Firebase");
                    
                    // Update local database
                    SavingsEntity entity = convertToEntity(savingsItem, userId);
                    executor.execute(() -> {
                        savingsDao.updateSavings(entity);
                        Log.d(TAG, "Savings updated in local database");
                    });
                    
                    isLoading.postValue(false);
                    listener.onSuccess("Savings updated successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating savings", e);
                    isLoading.postValue(false);
                    listener.onError("Failed to update savings: " + e.getMessage());
                });
    }

    /**
     * Delete savings item
     */
    public void deleteSavings(String savingsId, OnSavingsOperationListener listener) {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            listener.onError("User not authenticated");
            return;
        }

        isLoading.postValue(true);
        
        // Delete from Firebase
        firestore.collection("users")
                .document(userId)
                .collection("savings")
                .document(savingsId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Savings deleted from Firebase");
                    
                    // Delete from local database
                    executor.execute(() -> {
                        SavingsEntity entity = new SavingsEntity();
                        entity.setId(savingsId);
                        savingsDao.deleteSavings(entity);
                        Log.d(TAG, "Savings deleted from local database");
                    });
                    
                    isLoading.postValue(false);
                    listener.onSuccess("Savings deleted successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting savings", e);
                    isLoading.postValue(false);
                    listener.onError("Failed to delete savings: " + e.getMessage());
                });
    }

    /**
     * Get loading state
     */
    public LiveData<Boolean> getLoadingState() {
        return isLoading;
    }

    /**
     * Get error messages
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        if (savingsListener != null) {
            savingsListener.remove();
            savingsListener = null;
        }
        Log.d(TAG, "RealtimeSavingsRepository cleaned up");
    }

    // Helper methods for conversion between entities and models
    private SavingsEntity convertToEntity(SavingsItem item, String userId) {
        return new SavingsEntity(
                item.getId(),
                userId,
                item.getName(),
                item.getCategory(),
                item.getTargetAmount(),
                item.getCurrentAmount(),
                item.getCompletionDate(),
                item.getPhotoUri(),
                item.getCreatedAt(),
                System.currentTimeMillis() // updatedAt
        );
    }

    private SavingsItem convertToSavingsItem(SavingsEntity entity) {
        SavingsItem item = new SavingsItem();
        item.setId(entity.getId());
        item.setName(entity.getName());
        item.setCategory(entity.getCategory());
        item.setTargetAmount(entity.getTargetAmount());
        item.setCurrentAmount(entity.getCurrentAmount());
        item.setCompletionDate(entity.getCompletionDate());
        item.setPhotoUri(entity.getPhotoUri());
        item.setCreatedAt(entity.getCreatedAt());
        return item;
    }

    // Callback interface for operations
    public interface OnSavingsOperationListener {
        void onSuccess(String message);
        void onError(String error);
    }
}
