package com.example.spendly.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.spendly.R;
import com.example.spendly.utils.ImageUtils;
import com.example.spendly.utils.PermissionUtils;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private ImageView btnBack, imgProfile;
    private CardView btnEditPhoto;
    private TextInputEditText etNickname, etEmail, etPhone;
    private View btnSaveChanges;
    
    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private FirebaseUser currentUser;
    
    // Profile image handling
    private Uri selectedImageUri;
    private String profileImageBase64;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        initViews();
        setupClickListeners();
        loadUserData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        imgProfile = findViewById(R.id.img_profile);
        btnEditPhoto = findViewById(R.id.btn_edit_photo);
        etNickname = findViewById(R.id.et_nickname);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        btnSaveChanges = findViewById(R.id.btn_save_changes);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnEditPhoto.setOnClickListener(v -> {
            if (checkPermission()) {
                openImagePicker();
            } else {
                requestPermission();
            }
        });

        btnSaveChanges.setOnClickListener(v -> saveChanges());
    }

    private void loadUserData() {
        // Load existing user data from Firebase
        if (currentUser != null) {
            mFirestore.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Load nickname (extract from email if no separate field exists)
                        String email = documentSnapshot.getString("email");
                        if (email != null) {
                            etEmail.setText(email);
                            // Disable email editing as requested
                            etEmail.setEnabled(false);
                            etEmail.setAlpha(0.6f);
                            
                            // Extract nickname from email
                            String nickname = email.substring(0, email.indexOf('@'));
                            etNickname.setText(nickname);
                        }
                        
                        // Load phone number
                        String phoneNumber = documentSnapshot.getString("phoneNumber");
                        if (phoneNumber != null) {
                            etPhone.setText(phoneNumber);
                        }
                        
                        // Load other profile data if available
                        String fullName = documentSnapshot.getString("fullName");
                        if (fullName != null) {
                            etNickname.setText(fullName);
                        }
                        
                        // Load profile image with Base64 support and fallback to URI
                        loadProfileImage(documentSnapshot);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load profile data: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                });
        }
    }

    private boolean checkPermission() {
        return PermissionUtils.isStoragePermissionGranted(this);
    }

    private void requestPermission() {
        PermissionUtils.requestStoragePermission(this);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        switch (requestCode) {
            case PermissionUtils.REQUEST_STORAGE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openImagePicker();
                } else {
                    Toast.makeText(this, "Gallery permission denied", Toast.LENGTH_SHORT).show();
                }
                break;
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openImagePicker();
                } else {
                    Toast.makeText(this, "Permission denied to access gallery", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                // Display the selected image immediately
                imgProfile.setImageURI(selectedImageUri);
                
                // Convert to Base64 in background
                convertImageToBase64(selectedImageUri);
            }
        }
    }

    /**
     * Convert selected image URI to Base64 string
     */
    private void convertImageToBase64(Uri imageUri) {
        new Thread(() -> {
            try {
                String base64 = ImageUtils.uriToBase64(this, imageUri);
                if (base64 != null && !base64.isEmpty()) {
                    // Update on main thread
                    runOnUiThread(() -> {
                        profileImageBase64 = base64;
                        android.util.Log.d("EditProfile", "✅ Profile image converted to Base64 successfully");
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
                        android.util.Log.e("EditProfile", "❌ Failed to convert image to Base64");
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("EditProfile", "❌ Error converting image to Base64", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    /**
     * Load profile image from Firestore with Base64 support and URI fallback
     */
    private void loadProfileImage(com.google.firebase.firestore.DocumentSnapshot document) {
        if (imgProfile == null) {
            return;
        }

        try {
            // First try to load from Base64
            String profilePhotoBase64 = document.getString("profilePhotoBase64");
            if (profilePhotoBase64 != null && !profilePhotoBase64.isEmpty()) {
                android.graphics.Bitmap bitmap = ImageUtils.base64ToBitmap(profilePhotoBase64);
                if (bitmap != null) {
                    imgProfile.setImageBitmap(bitmap);
                    android.util.Log.d("EditProfile", "✅ Profile image loaded from Base64");
                    return;
                }
            }

            // Fallback to URI-based loading (for legacy images)
            String profileImageUrl = document.getString("profileImage");
            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                loadProfileImageFromUri(profileImageUrl);
            } else {
                // Set default profile image if no image is available
                imgProfile.setImageResource(R.drawable.ic_profile);
            }
        } catch (Exception e) {
            android.util.Log.e("EditProfile", "❌ Error loading profile image", e);
            imgProfile.setImageResource(R.drawable.ic_profile);
        }
    }

    /**
     * Load profile image from URI with error handling (fallback for legacy images)
     */
    private void loadProfileImageFromUri(String imageUrl) {
        try {
            // Use simple URI loading for edit profile
            Uri imageUri = Uri.parse(imageUrl);
            imgProfile.setImageURI(imageUri);
            android.util.Log.d("EditProfile", "✅ Profile image loaded from URI (fallback)");
        } catch (SecurityException e) {
            android.util.Log.w("EditProfile", "⚠️ SecurityException loading image from URI - using default", e);
            imgProfile.setImageResource(R.drawable.ic_profile);
        } catch (Exception e) {
            android.util.Log.e("EditProfile", "❌ Error loading profile image from URI", e);
            imgProfile.setImageResource(R.drawable.ic_profile);
        }
    }

    private void saveChanges() {
        String nickname = etNickname.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (validateInputs(nickname, phone)) {
            if (currentUser != null) {
                // Prepare data to update (excluding email as requested)
                Map<String, Object> updates = new HashMap<>();
                updates.put("fullName", nickname);
                updates.put("phoneNumber", phone);
                
                // Add profile image Base64 if a new image was selected
                if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                    updates.put("profilePhotoBase64", profileImageBase64);
                    android.util.Log.d("EditProfile", "✅ Including Base64 profile image in update");
                }
                
                // Update user document in Firestore
                mFirestore.collection("users").document(currentUser.getUid())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK); // Signal that changes were made
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update profile: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                    });
            }
        }
    }

    private boolean validateInputs(String nickname, String phone) {
        if (nickname.isEmpty()) {
            etNickname.setError("Nickname is required");
            etNickname.requestFocus();
            return false;
        }

        if (phone.isEmpty()) {
            etPhone.setError("Phone number is required");
            etPhone.requestFocus();
            return false;
        }

        // Validate phone number format
        if (phone.length() < 10) {
            etPhone.setError("Phone number must be at least 10 digits");
            etPhone.requestFocus();
            return false;
        }

        return true;
    }
}