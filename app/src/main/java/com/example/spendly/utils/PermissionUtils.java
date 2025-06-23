package com.example.spendly.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for handling camera and gallery permissions
 * Automatically requests necessary permissions for image features
 */
public class PermissionUtils {
    
    private static final String TAG = "PermissionUtils";
    
    // Permission request codes
    public static final int REQUEST_CAMERA_PERMISSION = 100;
    public static final int REQUEST_STORAGE_PERMISSION = 101;
    public static final int REQUEST_ALL_PERMISSIONS = 102;
    
    // Required permissions based on Android version
    private static final String[] CAMERA_PERMISSIONS = {
        Manifest.permission.CAMERA
    };
    
    // Storage permissions vary by Android version
    private static String[] getStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            return new String[]{
                Manifest.permission.READ_MEDIA_IMAGES
            };
        } else {
            // Android 12 and below
            return new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }
    }
    
    /**
     * Get all required permissions for image features
     */
    public static String[] getAllRequiredPermissions() {
        List<String> permissions = new ArrayList<>();
        
        // Add camera permission
        for (String permission : CAMERA_PERMISSIONS) {
            permissions.add(permission);
        }
        
        // Add storage permissions
        for (String permission : getStoragePermissions()) {
            permissions.add(permission);
        }
        
        return permissions.toArray(new String[0]);
    }
    
    /**
     * Check if camera permission is granted
     */
    public static boolean isCameraPermissionGranted(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Check if storage/media permission is granted
     */
    public static boolean isStoragePermissionGranted(Context context) {
        String[] storagePermissions = getStoragePermissions();
        for (String permission : storagePermissions) {
            if (ContextCompat.checkSelfPermission(context, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Check if all required permissions are granted
     */
    public static boolean areAllPermissionsGranted(Context context) {
        return isCameraPermissionGranted(context) && isStoragePermissionGranted(context);
    }
    
    /**
     * Get list of missing permissions
     */
    public static List<String> getMissingPermissions(Context context) {
        List<String> missingPermissions = new ArrayList<>();
        
        // Check camera permission
        if (!isCameraPermissionGranted(context)) {
            missingPermissions.add(Manifest.permission.CAMERA);
        }
        
        // Check storage permissions
        String[] storagePermissions = getStoragePermissions();
        for (String permission : storagePermissions) {
            if (ContextCompat.checkSelfPermission(context, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        
        return missingPermissions;
    }
    
    /**
     * Request camera permission only
     */
    public static void requestCameraPermission(Activity activity) {
        Log.d(TAG, "Requesting camera permission...");
        ActivityCompat.requestPermissions(activity, CAMERA_PERMISSIONS, REQUEST_CAMERA_PERMISSION);
    }
    
    /**
     * Request storage permission only
     */
    public static void requestStoragePermission(Activity activity) {
        Log.d(TAG, "Requesting storage permission...");
        String[] storagePermissions = getStoragePermissions();
        ActivityCompat.requestPermissions(activity, storagePermissions, REQUEST_STORAGE_PERMISSION);
    }
    
    /**
     * Request all required permissions at once
     */
    public static void requestAllPermissions(Activity activity) {
        List<String> missingPermissions = getMissingPermissions(activity);
        
        if (missingPermissions.isEmpty()) {
            Log.d(TAG, "All permissions already granted");
            return;
        }
        
        Log.d(TAG, "Requesting " + missingPermissions.size() + " missing permissions: " + missingPermissions);
        
        String[] permissionsArray = missingPermissions.toArray(new String[0]);
        ActivityCompat.requestPermissions(activity, permissionsArray, REQUEST_ALL_PERMISSIONS);
    }
    
    /**
     * Check if the user has denied permission and selected "Don't ask again"
     */
    public static boolean shouldShowPermissionRationale(Activity activity, String permission) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }
    
    /**
     * Log permission status for debugging
     */
    public static void logPermissionStatus(Context context) {
        Log.d(TAG, "=== PERMISSION STATUS ===");
        Log.d(TAG, "Android SDK: " + Build.VERSION.SDK_INT);
        Log.d(TAG, "Camera permission: " + (isCameraPermissionGranted(context) ? "GRANTED" : "DENIED"));
        Log.d(TAG, "Storage permission: " + (isStoragePermissionGranted(context) ? "GRANTED" : "DENIED"));
        
        String[] storagePermissions = getStoragePermissions();
        for (String permission : storagePermissions) {
            boolean granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, "- " + permission + ": " + (granted ? "GRANTED" : "DENIED"));
        }
        
        Log.d(TAG, "All permissions granted: " + areAllPermissionsGranted(context));
        
        List<String> missing = getMissingPermissions(context);
        if (!missing.isEmpty()) {
            Log.d(TAG, "Missing permissions: " + missing);
        }
        Log.d(TAG, "========================");
    }
    
    /**
     * Get user-friendly permission explanation
     */
    public static String getPermissionExplanation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return "Spendly needs access to:\n\n" +
                   "📷 Camera - To capture photos for savings goals and profile\n" +
                   "🖼️ Photos - To select images from your gallery\n\n" +
                   "These permissions help you personalize your savings goals and profile with images.";
        } else {
            return "Spendly needs access to:\n\n" +
                   "📷 Camera - To capture photos for savings goals and profile\n" +
                   "📁 Storage - To access photos from your gallery\n\n" +
                   "These permissions help you personalize your savings goals and profile with images.";
        }
    }
}
