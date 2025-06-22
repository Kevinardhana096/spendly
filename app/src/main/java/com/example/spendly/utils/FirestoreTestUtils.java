package com.example.spendly.utils;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for testing Firestore connectivity and permissions
 */
public class FirestoreTestUtils {
    private static final String TAG = "FirestoreTestUtils";
    
    /**
     * Test basic Firestore write operation
     */
    public static void testFirestoreWrite() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "❌ No authenticated user for Firestore test");
            return;
        }
        
        Log.d(TAG, "🔍 Testing Firestore write operation...");
        Log.d(TAG, "User ID: " + currentUser.getUid());
        Log.d(TAG, "User Email: " + currentUser.getEmail());
        
        // Create test data
        Map<String, Object> testData = new HashMap<>();
        testData.put("test_field", "test_value");
        testData.put("timestamp", System.currentTimeMillis());
        testData.put("user_email", currentUser.getEmail());
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Try to write to a test collection
        db.collection("test")
                .document("connectivity_test")
                .set(testData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Firestore write test SUCCESSFUL");
                    Log.d(TAG, "✅ User has write permissions to Firestore");
                    
                    // Clean up test document
                    db.collection("test").document("connectivity_test").delete()
                            .addOnSuccessListener(deleteVoid -> {
                                Log.d(TAG, "✅ Test document cleaned up successfully");
                            })
                            .addOnFailureListener(deleteError -> {
                                Log.w(TAG, "⚠️ Failed to clean up test document: " + deleteError.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Firestore write test FAILED", e);
                    Log.e(TAG, "❌ Error message: " + e.getMessage());
                    Log.e(TAG, "❌ Error class: " + e.getClass().getSimpleName());
                    
                    if (e.getMessage() != null && e.getMessage().contains("PERMISSION_DENIED")) {
                        Log.e(TAG, "❌ PERMISSION_DENIED: Check Firestore Security Rules");
                        Log.e(TAG, "❌ Make sure rules allow authenticated users to write");
                    }
                });
    }
    
    /**
     * Test Firestore read operation
     */
    public static void testFirestoreRead() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "❌ No authenticated user for Firestore read test");
            return;
        }
        
        Log.d(TAG, "🔍 Testing Firestore read operation...");
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Try to read from users collection
        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Log.d(TAG, "✅ Firestore read test SUCCESSFUL - Document exists");
                        Log.d(TAG, "✅ Document data: " + document.getData());
                    } else {
                        Log.d(TAG, "✅ Firestore read test SUCCESSFUL - Document doesn't exist (normal for new users)");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Firestore read test FAILED", e);
                    Log.e(TAG, "❌ Error message: " + e.getMessage());
                    
                    if (e.getMessage() != null && e.getMessage().contains("PERMISSION_DENIED")) {
                        Log.e(TAG, "❌ PERMISSION_DENIED: Check Firestore Security Rules for read permissions");
                    }
                });
    }
    
    /**
     * Run comprehensive Firestore connectivity tests
     */
    public static void runConnectivityTests() {
        Log.d(TAG, "🚀 Starting Firestore connectivity tests...");
        
        // Test authentication status
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "❌ User is not authenticated - cannot test Firestore");
            return;
        }
        
        Log.d(TAG, "✅ User is authenticated");
        Log.d(TAG, "User UID: " + currentUser.getUid());
        Log.d(TAG, "User Email: " + currentUser.getEmail());
        
        // Run tests
        testFirestoreRead();
        testFirestoreWrite();
    }
}
