package com.example.spendly.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.spendly.R;
import com.example.spendly.utils.ImageUtils;
import com.example.spendly.utils.DataMigrationUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileSettingsActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Button btnEditProfile;
    private Button btnLogout;
    private LinearLayout layoutChangePin, layoutChangePassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    // TextViews to display user info
    private TextView tvNickname;
    private TextView tvEmail;
    private TextView tvPhone;
    private ImageView imgProfile;

    // Loading indicator
    private View progressOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        
        // Configure Firestore settings to force online mode and prevent offline errors
        configureFirestoreSettings();

        initViews();
        setupClickListeners();
        
        // Run profile image migration (if needed)
        DataMigrationUtils.migrateExistingProfileImages(this);
        
        loadUserProfile();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnLogout = findViewById(R.id.btn_logout);
        layoutChangePin = findViewById(R.id.layout_change_pin);
        layoutChangePassword = findViewById(R.id.layout_change_password);

        // Initialize TextViews
        tvNickname = findViewById(R.id.tv_nickname);
        tvEmail = findViewById(R.id.tv_email);
        tvPhone = findViewById(R.id.tv_phone);
        imgProfile = findViewById(R.id.img_profile);

        // Initialize loading indicator
        progressOverlay = findViewById(R.id.progress_overlay);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            // Finish current activity to go back to previous screen
            finish();
        });

        btnEditProfile.setOnClickListener(v -> {
            // Open edit profile activity
            Intent intent = new Intent(ProfileSettingsActivity.this, EditProfileActivity.class);
            startActivityForResult(intent, REQUEST_EDIT_PROFILE);
        });

        layoutChangePin.setOnClickListener(v -> {
            // Open change pin activity
            Intent intent = new Intent(ProfileSettingsActivity.this, EditPinCodeActivity.class);
            startActivityForResult(intent, REQUEST_EDIT_PIN);
        });

        layoutChangePassword.setOnClickListener(v -> {
            // Open change password activity
            Intent intent = new Intent(ProfileSettingsActivity.this, EditPasswordActivity.class);
            startActivityForResult(intent, REQUEST_EDIT_PASSWORD);
        });

        btnLogout.setOnClickListener(v -> {
            showLogoutConfirmationDialog();
        });
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes", (dialog, which) -> {
                logoutUser();
            })
            .setNegativeButton("No", (dialog, which) -> {
                dialog.dismiss();
            })
            .show();
    }

    private void logoutUser() {
        // Sign out from Firebase
        mAuth.signOut();

        // Show success message
        Toast.makeText(this, "Logout successful", Toast.LENGTH_SHORT).show();

        // Clear all activities in the stack and go to Sign In page
        Intent intent = new Intent(this, SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadUserProfile() {
        // Get current user
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Show loading indicator
            progressOverlay.setVisibility(View.VISIBLE);

            // First, ensure Firestore network is enabled before attempting to load data
            mFirestore.enableNetwork()
                .addOnCompleteListener(networkTask -> {
                    android.util.Log.d("ProfileSettings", "Network enable completed before loading profile: " + networkTask.isSuccessful());
                    
                    // Get user document from Firestore
                    mFirestore.collection("users").document(user.getUid())
                        .get()
                        .addOnCompleteListener(task -> {
                            // Hide loading indicator
                            progressOverlay.setVisibility(View.GONE);

                            if (task.isSuccessful() && task.getResult() != null) {
                                DocumentSnapshot document = task.getResult();
                                // Check if document exists
                                if (document.exists()) {
                                    // Update UI with user data

                                    // Set email and nickname
                                    String email = document.getString("email");
                                    String fullName = document.getString("fullName");
                                    
                                    if (email != null) {
                                        tvEmail.setText(email);
                                        
                                        // Use fullName if available, otherwise extract from email
                                        if (fullName != null && !fullName.isEmpty()) {
                                            tvNickname.setText(fullName);
                                        } else {
                                            tvNickname.setText(email.substring(0, email.indexOf('@')));
                                        }
                                    }

                                    // Set phone number using correct field name "phoneNumber"
                                    String phoneNumber = document.getString("phoneNumber");
                                    if (phoneNumber != null) {
                                        tvPhone.setText(phoneNumber);
                                    }

                                    // Load profile image using Base64 with fallback to URI
                                    String profilePhotoBase64 = document.getString("profilePhotoBase64");
                                    String profileImageUrl = document.getString("profileImage");
                                    
                                    if (profilePhotoBase64 != null && !profilePhotoBase64.isEmpty()) {
                                        loadProfileImageFromBase64(profilePhotoBase64);
                                    } else if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                        loadProfileImage(profileImageUrl);
                                    } else {
                                        // Set default profile image if no image is available
                                        if (imgProfile != null) {
                                            imgProfile.setImageResource(R.drawable.ic_profile);
                                        }
                                    }
                                    
                                    // Sync PIN from Firebase to SharedPreferences
                                    syncPinFromFirebase(document);
                                } else {
                                    Toast.makeText(ProfileSettingsActivity.this, "User profile not found", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                // Enhanced error handling for offline errors
                                Exception exception = task.getException();
                                if (exception != null && exception.getMessage() != null && 
                                    exception.getMessage().contains("client is offline")) {
                                    android.util.Log.e("ProfileSettings", "❌ Firestore client is offline - retrying...");
                                    Toast.makeText(ProfileSettingsActivity.this, "Connection issue. Please check your internet connection.", Toast.LENGTH_LONG).show();
                                    
                                    // Retry after a short delay
                                    new android.os.Handler().postDelayed(() -> {
                                        android.util.Log.d("ProfileSettings", "🔄 Retrying profile load after offline error...");
                                        loadUserProfile();
                                    }, 3000);
                                } else {
                                    Toast.makeText(ProfileSettingsActivity.this, "Failed to load user profile", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh user profile data when returning from edit activities
        loadUserProfile();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_EDIT_PROFILE:
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    // Refresh profile data
                    loadUserProfile();
                    break;
                case REQUEST_EDIT_PIN:
                    Toast.makeText(this, "PIN updated successfully", Toast.LENGTH_SHORT).show();
                    break;
                case REQUEST_EDIT_PASSWORD:
                    Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    /**
     * Sync PIN from Firebase to SharedPreferences for local access
     */
    private void syncPinFromFirebase(DocumentSnapshot document) {
        String firebasePin = document.getString("userPin");
        Boolean pinCodeSet = document.getBoolean("pinCodeSet");
        
        if (firebasePin != null && !firebasePin.isEmpty() && pinCodeSet != null && pinCodeSet) {
            // Save Firebase PIN to SharedPreferences
            android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            String localPin = prefs.getString("user_pin", "");
            
            // Only update if different or if no local PIN exists
            if (!firebasePin.equals(localPin)) {
                prefs.edit()
                    .putString("user_pin", firebasePin)
                    .putBoolean("pin_set", true)
                    .putLong("pin_sync_date", System.currentTimeMillis())
                    .apply();
                
                android.util.Log.d("ProfileSettings", "PIN synced from Firebase to local storage");
            }
        }
    }

    /**
     * Load profile image from Base64 string (preferred method)
     */
    private void loadProfileImageFromBase64(String base64String) {
        if (imgProfile == null) {
            return;
        }

        try {
            android.graphics.Bitmap bitmap = ImageUtils.base64ToBitmap(base64String);
            if (bitmap != null) {
                imgProfile.setImageBitmap(bitmap);
                android.util.Log.d("ProfileSettings", "✅ Profile image loaded from Base64");
            } else {
                android.util.Log.w("ProfileSettings", "⚠️ Failed to decode Base64 image - using default");
                imgProfile.setImageResource(R.drawable.ic_profile);
            }
        } catch (Exception e) {
            android.util.Log.e("ProfileSettings", "❌ Error loading profile image from Base64", e);
            imgProfile.setImageResource(R.drawable.ic_profile);
        }
    }

    /**
     * Load profile image with proper error handling (fallback for URI-based images)
     */
    private void loadProfileImage(String imageUrl) {
        if (imgProfile == null) {
            return;
        }

        try {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_profile) // Default placeholder
                .error(R.drawable.ic_profile) // Error fallback
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache strategy
                .centerCrop()
                .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(@androidx.annotation.Nullable GlideException e, Object model, Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                        android.util.Log.e("ProfileSettings", "Failed to load profile image: " + (e != null ? e.getMessage() : "Unknown error"));
                        if (e != null && e.getCause() instanceof SecurityException) {
                            android.util.Log.w("ProfileSettings", "SecurityException when loading image - URI permission denied: " + imageUrl);
                        }
                        // Glide will automatically show error drawable
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, Target<android.graphics.drawable.Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        return false; // Allow Glide to handle the resource
                    }
                })
                .into(imgProfile);
        } catch (Exception e) {
            // If Glide fails completely, set default image
            android.util.Log.e("ProfileSettings", "Error loading profile image: " + e.getMessage());
            imgProfile.setImageResource(R.drawable.ic_profile);
        }
    }

    /**
     * Configure Firestore settings to force online mode and prevent offline errors
     */
    private void configureFirestoreSettings() {
        try {
            android.util.Log.d("ProfileSettings", "🔧 Configuring Firestore settings to force online mode...");
            
            // Configure Firestore settings BEFORE enabling network
            com.google.firebase.firestore.FirebaseFirestoreSettings settings =
                    new com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                            .setPersistenceEnabled(true)  // Enable offline persistence
                            .setCacheSizeBytes(com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                            .build();
            
            mFirestore.setFirestoreSettings(settings);
            android.util.Log.d("ProfileSettings", "✅ Firestore settings configured with persistence enabled");
            
            // Force enable network after settings configuration
            android.util.Log.d("ProfileSettings", "🌐 Attempting to force enable Firestore network...");
            mFirestore.enableNetwork()
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("ProfileSettings", "✅ Firestore network enabled successfully");
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.w("ProfileSettings", "⚠️ Could not enable Firestore network (may be temporary)", e);
                        // This is not critical - app can still work with persistence
                    });
            
            // Add a delay and retry network enable
            new android.os.Handler().postDelayed(() -> {
                android.util.Log.d("ProfileSettings", "🔄 Retry: Attempting to enable Firestore network again...");
                mFirestore.enableNetwork()
                        .addOnSuccessListener(aVoid -> {
                            android.util.Log.d("ProfileSettings", "✅ Second attempt: Firestore network enabled");
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.w("ProfileSettings", "⚠️ Second attempt failed - will rely on offline handling", e);
                        });
            }, 2000); // 2 second delay
            
        } catch (Exception e) {
            android.util.Log.e("ProfileSettings", "❌ Error configuring Firestore settings", e);
        }
    }

    // Activity result constants
    private static final int REQUEST_EDIT_PROFILE = 1001;
    private static final int REQUEST_EDIT_PIN = 1002;
    private static final int REQUEST_EDIT_PASSWORD = 1003;
}