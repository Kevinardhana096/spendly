package com.example.spendly.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.spendly.fragment.BudgetFragment;
import com.example.spendly.fragment.HistoryFragment;
import com.example.spendly.fragment.HomeFragment;
import com.example.spendly.fragment.SavingFragment;
import com.example.spendly.R;
import com.example.spendly.utils.DataMigrationUtils;
import com.example.spendly.utils.PermissionUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    // Fragment caching for better performance
    private HomeFragment homeFragment;
    private SavingFragment savingFragment;
    private BudgetFragment budgetFragment;
    private HistoryFragment historyFragment;    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check and request permissions automatically
        checkAndRequestPermissions();

        // Run data migration for existing savings images (one-time operation)
        runDataMigrationIfNeeded();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        FloatingActionButton fab = findViewById(R.id.fab_add);// Set default fragment with caching
        if (savedInstanceState == null) {
            homeFragment = new HomeFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, homeFragment)
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                if (homeFragment == null) {
                    homeFragment = new HomeFragment();
                }
                selectedFragment = homeFragment;
            } else if (itemId == R.id.nav_savings) {
                if (savingFragment == null) {
                    savingFragment = new SavingFragment();
                }
                selectedFragment = savingFragment;
            } else if (itemId == R.id.nav_budgeting) {
                if (budgetFragment == null) {
                    budgetFragment = new BudgetFragment();
                }
                selectedFragment = budgetFragment;
            } else if (itemId == R.id.nav_history) {
                if (historyFragment == null) {
                    historyFragment = new HistoryFragment();
                }
                selectedFragment = historyFragment;
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, selectedFragment)
                        .commit();
            }
            return true;
        });

        fab.setOnClickListener(v -> {
            // Navigate to AddTransactionActivity
            Intent intent = new Intent(MainActivity.this, AddTransactionActivity.class);
            startActivity(intent);        });
    }

    /**
     * Check and request camera and gallery permissions automatically
     */
    private void checkAndRequestPermissions() {
        android.util.Log.d("MainActivity", "🔐 Checking camera and gallery permissions...");
        
        // Log current permission status
        PermissionUtils.logPermissionStatus(this);
        
        if (PermissionUtils.areAllPermissionsGranted(this)) {
            android.util.Log.d("MainActivity", "✅ All permissions already granted");
            return;
        }
        
        // Show explanation dialog first
        showPermissionExplanationDialog();
    }
    
    /**
     * Show dialog explaining why permissions are needed
     */
    private void showPermissionExplanationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("📷 Camera & Gallery Access")
            .setMessage(PermissionUtils.getPermissionExplanation())
            .setPositiveButton("Grant Permissions", (dialog, which) -> {
                // Request all missing permissions
                PermissionUtils.requestAllPermissions(this);
            })
            .setNegativeButton("Skip", (dialog, which) -> {
                android.util.Log.d("MainActivity", "User chose to skip permissions");
                Toast.makeText(this, "You can grant permissions later in Settings", Toast.LENGTH_LONG).show();
            })
            .setCancelable(false)
            .show();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        android.util.Log.d("MainActivity", "📝 Permission result received for request code: " + requestCode);
        
        switch (requestCode) {
            case PermissionUtils.REQUEST_ALL_PERMISSIONS:
                handleAllPermissionsResult(permissions, grantResults);
                break;
            case PermissionUtils.REQUEST_CAMERA_PERMISSION:
                handleCameraPermissionResult(grantResults);
                break;
            case PermissionUtils.REQUEST_STORAGE_PERMISSION:
                handleStoragePermissionResult(grantResults);
                break;
        }
        
        // Log updated permission status
        PermissionUtils.logPermissionStatus(this);
    }
    
    /**
     * Handle result of all permissions request
     */
    private void handleAllPermissionsResult(String[] permissions, int[] grantResults) {
        int grantedCount = 0;
        int deniedCount = 0;
        
        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                grantedCount++;
                android.util.Log.d("MainActivity", "✅ Permission granted: " + permissions[i]);
            } else {
                deniedCount++;
                android.util.Log.w("MainActivity", "❌ Permission denied: " + permissions[i]);
            }
        }
        
        if (grantedCount == permissions.length) {
            // All permissions granted
            android.util.Log.d("MainActivity", "🎉 All permissions granted successfully!");
            Toast.makeText(this, "✅ Camera and gallery access granted!", Toast.LENGTH_SHORT).show();
        } else if (grantedCount > 0) {
            // Partial permissions granted
            android.util.Log.d("MainActivity", "⚠️ Partial permissions granted: " + grantedCount + "/" + permissions.length);
            Toast.makeText(this, "Some permissions granted. You can enable others in Settings.", Toast.LENGTH_LONG).show();
        } else {
            // No permissions granted
            android.util.Log.w("MainActivity", "❌ No permissions granted");
            showPermissionDeniedDialog();
        }
    }
    
    /**
     * Handle camera permission result
     */
    private void handleCameraPermissionResult(int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            android.util.Log.d("MainActivity", "✅ Camera permission granted");
            Toast.makeText(this, "Camera access granted!", Toast.LENGTH_SHORT).show();
        } else {
            android.util.Log.w("MainActivity", "❌ Camera permission denied");
            Toast.makeText(this, "Camera access denied. You can enable it in Settings.", Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Handle storage permission result
     */
    private void handleStoragePermissionResult(int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            android.util.Log.d("MainActivity", "✅ Storage permission granted");
            Toast.makeText(this, "Gallery access granted!", Toast.LENGTH_SHORT).show();
        } else {
            android.util.Log.w("MainActivity", "❌ Storage permission denied");
            Toast.makeText(this, "Gallery access denied. You can enable it in Settings.", Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Show dialog when permissions are denied
     */
    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
            .setTitle("⚠️ Permissions Required")
            .setMessage("Camera and gallery permissions are needed for adding images to savings goals and profile photos.\n\n" +
                       "You can manually enable these permissions in Settings > Apps > Spendly > Permissions.")
            .setPositiveButton("OK", null)
            .setNeutralButton("Try Again", (dialog, which) -> {
                checkAndRequestPermissions();
            })
            .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUserAuthentication();
    }

    private void checkUserAuthentication() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // If not logged in, redirect to sign in activity
            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }    private void runDataMigrationIfNeeded() {
        // Check if migration has been run before
        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean migrationCompleted = prefs.getBoolean("image_migration_completed", false);
        
        if (!migrationCompleted && mAuth.getCurrentUser() != null) {
            android.util.Log.d("MainActivity", "🔄 Starting one-time data migration for existing images...");
            
            // Run migration in background thread
            new Thread(() -> {
                try {
                    DataMigrationUtils.migrateExistingSavingsImages(this);
                    
                    // Mark migration as completed
                    prefs.edit()
                        .putBoolean("image_migration_completed", true)
                        .putLong("migration_date", System.currentTimeMillis())
                        .apply();
                    
                    android.util.Log.d("MainActivity", "✅ Data migration completed and marked as done");
                } catch (Exception e) {
                    android.util.Log.e("MainActivity", "❌ Error during data migration", e);
                }
            }).start();
        } else if (migrationCompleted) {
            android.util.Log.d("MainActivity", "✅ Image migration already completed, skipping");
        }
    }
}