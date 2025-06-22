package com.example.spendly.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.spendly.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private ImageView btnBack, imgProfile;
    private CardView btnEditPhoto;
    private TextInputEditText etNickname, etEmail, etPhone;
    private Button btnSaveChanges;
    
    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private FirebaseStorage mStorage;
    private FirebaseUser currentUser;
    
    // Profile image
    private Uri selectedImageUri;
    private String currentProfileImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
        mStorage = FirebaseStorage.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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
        // Show loading state
        btnSaveChanges.setEnabled(false);
        btnSaveChanges.setText("Loading...");

        // Load user data from Firestore
        mFirestore.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Load user data
                        String nickname = documentSnapshot.getString("nickname");
                        String email = documentSnapshot.getString("email");
                        String phone = documentSnapshot.getString("phoneNumber");
                        currentProfileImageUrl = documentSnapshot.getString("profileImageUrl");

                        // Set data to UI
                        if (!TextUtils.isEmpty(nickname)) {
                            etNickname.setText(nickname);
                        } else {
                            // Use email username as default nickname
                            String emailUsername = currentUser.getEmail().split("@")[0];
                            etNickname.setText(emailUsername);
                        }

                        // Email should not be editable (as per requirement)
                        etEmail.setText(currentUser.getEmail());
                        etEmail.setEnabled(false);
                        etEmail.setFocusable(false);

                        if (!TextUtils.isEmpty(phone)) {
                            etPhone.setText(phone);
                        }

                        // Load profile image
                        if (!TextUtils.isEmpty(currentProfileImageUrl)) {
                            Glide.with(this)
                                    .load(currentProfileImageUrl)
                                    .placeholder(R.drawable.ic_profile)
                                    .error(R.drawable.ic_profile)
                                    .circleCrop()
                                    .into(imgProfile);
                        }
                    }

                    // Enable save button
                    btnSaveChanges.setEnabled(true);
                    btnSaveChanges.setText("Save Changes");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnSaveChanges.setEnabled(true);
                    btnSaveChanges.setText("Save Changes");
                });
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_CODE);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission denied to access gallery", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                // Display selected image
                Glide.with(this)
                        .load(selectedImageUri)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(imgProfile);
            }
        }
    }

    private void saveChanges() {
        String nickname = etNickname.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (!validateInputs(nickname, phone)) {
            return;
        }

        // Show loading state
        btnSaveChanges.setEnabled(false);
        btnSaveChanges.setText("Saving...");

        if (selectedImageUri != null) {
            // Upload image first, then save profile data
            uploadImageAndSaveProfile(nickname, phone);
        } else {
            // Save profile data without image upload
            saveProfileData(nickname, phone, currentProfileImageUrl);
        }
    }

    private void uploadImageAndSaveProfile(String nickname, String phone) {
        // Create storage reference
        StorageReference storageRef = mStorage.getReference()
                .child("profile_images")
                .child(currentUser.getUid() + ".jpg");

        // Upload image
        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get download URL
                    storageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String imageUrl = uri.toString();
                                saveProfileData(nickname, phone, imageUrl);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to get image URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                resetSaveButton();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetSaveButton();
                });
    }

    private void saveProfileData(String nickname, String phone, String imageUrl) {
        // Prepare data to save
        Map<String, Object> updates = new HashMap<>();
        updates.put("nickname", nickname);
        updates.put("phoneNumber", phone);
        if (imageUrl != null) {
            updates.put("profileImageUrl", imageUrl);
        }
        updates.put("updatedAt", System.currentTimeMillis());

        // Save to Firestore
        mFirestore.collection("users")
                .document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetSaveButton();
                });
    }

    private void resetSaveButton() {
        btnSaveChanges.setEnabled(true);
        btnSaveChanges.setText("Save Changes");
    }

    private boolean validateInputs(String nickname, String phone) {
        if (TextUtils.isEmpty(nickname)) {
            etNickname.setError("Nickname is required");
            etNickname.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Phone number is required");
            etPhone.requestFocus();
            return false;
        }

        // Validate phone number format (Indonesian phone number)
        if (!phone.matches("^(\\+62|62|0)8[1-9][0-9]{6,9}$")) {
            etPhone.setError("Invalid phone number format");
            etPhone.requestFocus();
            return false;
        }

        return true;
    }
}