package com.example.spendly.utils;

import android.content.Context;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility to migrate existing savings data from URI to Base64
 * Run this once to convert all existing savings images to Base64
 */
public class DataMigrationUtils {
    private static final String TAG = "DataMigration";
    
    public static void migrateExistingSavingsImages(Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        if (userId == null) {
            Log.e(TAG, "User not authenticated");
            return;
        }
        
        Log.d(TAG, "Starting migration of existing savings images to Base64...");
        
        db.collection("users")
            .document(userId)
            .collection("savings")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                int totalItems = queryDocumentSnapshots.size();
                int processedItems = 0;
                
                Log.d(TAG, "Found " + totalItems + " savings items to check for migration");
                
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    String photoUri = document.getString("photoUri");
                    String photoBase64 = document.getString("photoBase64");
                    
                    // Only migrate if has URI but no Base64
                    if (photoUri != null && !photoUri.isEmpty() && 
                        (photoBase64 == null || photoBase64.isEmpty()) &&
                        photoUri.startsWith("content://")) {
                        
                        Log.d(TAG, "Migrating image for savings: " + document.getString("name"));
                        
                        try {
                            android.net.Uri uri = android.net.Uri.parse(photoUri);
                            String base64 = ImageUtils.uriToBase64(context, uri);
                            
                            if (base64 != null && !base64.isEmpty()) {
                                // Update document with Base64 data
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("photoBase64", base64);
                                
                                document.getReference().update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "✅ Successfully migrated image for: " + document.getString("name"));
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "❌ Failed to update document: " + e.getMessage());
                                    });
                            } else {
                                Log.w(TAG, "⚠️ Failed to convert URI to Base64 for: " + document.getString("name"));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Error processing URI for " + document.getString("name"), e);
                        }
                    }
                    
                    processedItems++;
                    if (processedItems == totalItems) {
                        Log.d(TAG, "Migration check completed for all items");
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to fetch savings for migration", e);
            });    }
    
    /**
     * Migrate existing profile images from URI to Base64
     * Run this once to convert all existing profile images to Base64
     */
    public static void migrateExistingProfileImages(Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        if (userId == null) {
            Log.e(TAG, "User not authenticated for profile image migration");
            return;
        }
        
        Log.d(TAG, "Starting migration of existing profile images to Base64...");
        
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String profileImageUri = documentSnapshot.getString("profileImage");
                    String profilePhotoBase64 = documentSnapshot.getString("profilePhotoBase64");
                    
                    // Only migrate if has URI but no Base64
                    if (profileImageUri != null && !profileImageUri.isEmpty() && 
                        (profilePhotoBase64 == null || profilePhotoBase64.isEmpty()) &&
                        profileImageUri.startsWith("content://")) {
                        
                        Log.d(TAG, "Migrating profile image for user: " + userId);
                        
                        try {
                            android.net.Uri uri = android.net.Uri.parse(profileImageUri);
                            String base64 = ImageUtils.uriToBase64(context, uri);
                            
                            if (base64 != null && !base64.isEmpty()) {
                                // Update document with Base64 data
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("profilePhotoBase64", base64);
                                
                                db.collection("users")
                                    .document(userId)
                                    .update(updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "✅ Profile image migrated to Base64 successfully");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "❌ Failed to update profile image with Base64: " + e.getMessage());
                                    });
                            } else {
                                Log.w(TAG, "⚠️ Failed to convert profile image URI to Base64");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "❌ Error during profile image migration: " + e.getMessage());
                        }
                    } else {
                        Log.d(TAG, "No profile image migration needed");
                    }
                } else {
                    Log.d(TAG, "User document not found - no profile image to migrate");
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "❌ Failed to check profile image for migration: " + e.getMessage());
            });
    }
}
