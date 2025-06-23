package com.example.spendly;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

/**
 * Application class to configure global Firebase settings
 */
public class SpendlyApplication extends Application {
    private static final String TAG = "SpendlyApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.d(TAG, "🚀 Initializing Spendly Application...");
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        
        // Configure Firestore settings globally to prevent offline issues
        configureGlobalFirestoreSettings();
        
        Log.d(TAG, "✅ Spendly Application initialized successfully");
    }

    /**
     * Configure Firestore settings globally to force online mode and prevent offline errors
     */
    private void configureGlobalFirestoreSettings() {
        try {
            Log.d(TAG, "🔧 Configuring global Firestore settings...");
            
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            
            // Configure Firestore settings for optimal connectivity
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)  // Enable offline persistence
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build();
            
            firestore.setFirestoreSettings(settings);
            Log.d(TAG, "✅ Global Firestore settings configured with persistence enabled");
            
            // Force enable network globally
            Log.d(TAG, "🌐 Attempting to enable Firestore network globally...");
            firestore.enableNetwork()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "✅ Global Firestore network enabled successfully");
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "⚠️ Could not enable global Firestore network (may be temporary)", e);
                        
                        // Retry after a delay
                        new android.os.Handler().postDelayed(() -> {
                            Log.d(TAG, "🔄 Retry: Attempting to enable global Firestore network again...");
                            firestore.enableNetwork()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "✅ Second attempt: Global Firestore network enabled");
                                    })
                                    .addOnFailureListener(retryError -> {
                                        Log.w(TAG, "⚠️ Second attempt failed - will rely on individual activity settings", retryError);
                                    });
                        }, 3000); // 3 second delay for retry
                    });
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error configuring global Firestore settings", e);
        }
    }
}
