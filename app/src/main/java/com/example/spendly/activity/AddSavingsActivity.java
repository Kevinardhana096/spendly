package com.example.spendly.activity;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddSavingsActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 1001;
    private static final int GALLERY_REQUEST_CODE = 1002;
    private static final int CAMERA_PERMISSION_CODE = 1003;
    private static final int STORAGE_PERMISSION_CODE = 1004;

    private ImageView btnBack, ivCategoryIcon, ivTargetPhoto;
    private LinearLayout categoryContainer, dateContainer, photoPlaceholder;
    private TextView tvSelectedCategory, tvCompletionDate;
    private EditText etTargetName, etTotalAmount;
    private CardView photoContainer;
    private Button btnAddTarget;

    private Calendar completionCalendar;
    private SimpleDateFormat dateFormat;
    private String selectedCategory = "Food & Beverages";
    private int selectedCategoryIcon = R.drawable.ic_food;
    private int selectedCategoryColor = R.color.orange_primary;
    private Uri selectedPhotoUri;
    private String selectedPhotoBase64; // Base64 encoded image data

    // Edit mode variables
    private boolean isEditMode = false;
    private String editSavingsId;
    private com.example.spendly.model.SavingsItem editSavingsItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_savings);

        initViews();
        setupClickListeners();
        setupTextWatchers();
        initializeData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        ivCategoryIcon = findViewById(R.id.iv_category_icon);
        categoryContainer = findViewById(R.id.category_container);
        tvSelectedCategory = findViewById(R.id.tv_selected_category);
        etTargetName = findViewById(R.id.et_target_name);
        etTotalAmount = findViewById(R.id.et_total_amount);
        dateContainer = findViewById(R.id.date_container);
        tvCompletionDate = findViewById(R.id.tv_completion_date);
        photoContainer = findViewById(R.id.photo_container);
        ivTargetPhoto = findViewById(R.id.iv_target_photo);
        photoPlaceholder = findViewById(R.id.photo_placeholder);
        btnAddTarget = findViewById(R.id.btn_add_target);

        completionCalendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        categoryContainer.setOnClickListener(v -> showCategorySelector());

        dateContainer.setOnClickListener(v -> showDatePicker());

        photoContainer.setOnClickListener(v -> showPhotoOptions());

        btnAddTarget.setOnClickListener(v -> saveTarget());
    }

    private void setupTextWatchers() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateAddButton();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etTargetName.addTextChangedListener(textWatcher);
        etTotalAmount.addTextChangedListener(textWatcher);
    }

    private void initializeData() {
        // Check if this is edit mode
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("edit_mode", false)) {
            isEditMode = true;
            editSavingsId = intent.getStringExtra("savings_id");
            editSavingsItem = (com.example.spendly.model.SavingsItem) intent.getSerializableExtra("savings_item");
            
            if (editSavingsItem != null) {
                setupEditMode();
                return;
            }
        }
        
        // Normal add mode - set default completion date (1 year from now)
        completionCalendar.add(Calendar.YEAR, 1);
        tvCompletionDate.setText(dateFormat.format(completionCalendar.getTime()));
        tvCompletionDate.setTextColor(getResources().getColor(R.color.black));
    }

    private void setupEditMode() {
        // Update title and button text
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Edit Savings Goal");
        }
        btnAddTarget.setText("Update Goal");

        // Fill in existing data
        etTargetName.setText(editSavingsItem.getName());
        etTotalAmount.setText(String.valueOf((int) editSavingsItem.getTargetAmount()));
        selectedCategory = editSavingsItem.getCategory();
        
        // Set completion date
        if (editSavingsItem.getCompletionDate() > 0) {
            completionCalendar.setTimeInMillis(editSavingsItem.getCompletionDate());
            tvCompletionDate.setText(dateFormat.format(completionCalendar.getTime()));
            tvCompletionDate.setTextColor(getResources().getColor(R.color.black));
        }
        
        // Load existing photo if available
        if (editSavingsItem.getPhotoBase64() != null && !editSavingsItem.getPhotoBase64().isEmpty()) {
            // Load from Base64
            selectedPhotoBase64 = editSavingsItem.getPhotoBase64();
            loadImageFromBase64(selectedPhotoBase64);
        } else if (editSavingsItem.getPhotoUri() != null && !editSavingsItem.getPhotoUri().isEmpty()) {
            // Fallback to URI (for backward compatibility)
            selectedPhotoUri = Uri.parse(editSavingsItem.getPhotoUri());
            loadImageFromUri(selectedPhotoUri);
        }
        
        // Update category display
        updateCategoryDisplay();
    }

    private void loadImageFromUri(Uri uri) {
        try {
            // First try direct URI loading
            if (uri.toString().startsWith("http")) {
                // Firebase Storage URL - use Glide
                com.bumptech.glide.Glide.with(this)
                        .load(uri.toString())
                        .placeholder(R.drawable.placeholder_green)
                        .error(R.drawable.placeholder_green)
                        .into(ivTargetPhoto);
            } else {
                // Local URI - use Glide with better error handling
                com.bumptech.glide.Glide.with(this)
                        .load(uri)
                        .placeholder(R.drawable.placeholder_green)
                        .error(R.drawable.placeholder_green)
                        .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                            @Override
                            public boolean onLoadFailed(com.bumptech.glide.load.engine.GlideException e, Object model, 
                                    com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                    boolean isFirstResource) {
                                // If Glide fails, try loading with MediaStore or fallback
                                loadImageWithFallback(uri);
                                return true;
                            }

                            @Override
                            public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, 
                                    com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                    com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                                return false;
                            }
                        })
                        .into(ivTargetPhoto);
            }
            
            ivTargetPhoto.setVisibility(View.VISIBLE);
            photoPlaceholder.setVisibility(View.GONE);
            
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to placeholder
            loadImageWithFallback(uri);
        }
    }

    private void loadImageWithFallback(Uri uri) {
        try {
            // Try with MediaStore first
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            ivTargetPhoto.setImageBitmap(bitmap);
            ivTargetPhoto.setVisibility(View.VISIBLE);
            photoPlaceholder.setVisibility(View.GONE);
        } catch (SecurityException se) {
            // SecurityException - show placeholder and inform user
            android.util.Log.w("AddSavingsActivity", "SecurityException accessing image: " + se.getMessage());
            showImagePlaceholder();
            Toast.makeText(this, "Cannot access selected image due to permissions. Please try another image.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // Other exceptions - try direct URI as last resort
            try {
                ivTargetPhoto.setImageURI(uri);
                ivTargetPhoto.setVisibility(View.VISIBLE);
                photoPlaceholder.setVisibility(View.GONE);
            } catch (Exception ex) {
                // Final fallback to placeholder
                showImagePlaceholder();
                Toast.makeText(this, "Failed to load selected image. Please try another image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showImagePlaceholder() {
        ivTargetPhoto.setImageResource(R.drawable.placeholder_green);
        ivTargetPhoto.setVisibility(View.VISIBLE);
        photoPlaceholder.setVisibility(View.GONE);
        selectedPhotoUri = null; // Clear the URI since we can't use it
        selectedPhotoBase64 = null; // Clear Base64 data too
    }

    private void loadImageFromBase64(String base64String) {
        try {
            Bitmap bitmap = ImageUtils.base64ToBitmap(base64String);
            if (bitmap != null) {
                ivTargetPhoto.setImageBitmap(bitmap);
                ivTargetPhoto.setVisibility(View.VISIBLE);
                photoPlaceholder.setVisibility(View.GONE);
            } else {
                showImagePlaceholder();
                Toast.makeText(this, "Failed to load image from stored data", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            showImagePlaceholder();
            Toast.makeText(this, "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showCategorySelector() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Target Category");

        String[] categories = {
                "Food & Beverages",
                "Transportation",
                "Shopping",
                "Health and Sport",
                "Entertainment",
                "Education",
                "Investment",
                "Travel",
                "Gadgets",
                "Others"
        };

        int[] icons = {
                R.drawable.ic_food,
                R.drawable.ic_transportation,
                R.drawable.ic_shopping,
                R.drawable.ic_health,
                R.drawable.ic_others,
                R.drawable.ic_others, // Education
                R.drawable.ic_others, // Investment
                R.drawable.ic_others, // Travel
                R.drawable.ic_others, // Gadgets
                R.drawable.ic_others  // Others
        };

        int[] colors = {
                R.color.orange_primary,
                R.color.blue_primary,
                R.color.pink_primary,
                R.color.green_primary,
                R.color.purple_primary,
                R.color.red_primary,
                R.color.orange_primary, // Investment
                R.color.blue_primary,   // Travel
                R.color.pink_primary,   // Gadgets
                R.color.purple_primary  // Others
        };

        builder.setItems(categories, (dialog, which) -> {
            selectedCategory = categories[which];
            selectedCategoryIcon = icons[which];
            selectedCategoryColor = colors[which];

            tvSelectedCategory.setText(selectedCategory);
            ivCategoryIcon.setImageResource(selectedCategoryIcon);
            ivCategoryIcon.setColorFilter(ContextCompat.getColor(this, selectedCategoryColor));

            updateAddButton();
        });

        builder.show();
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    completionCalendar.set(Calendar.YEAR, year);
                    completionCalendar.set(Calendar.MONTH, month);
                    completionCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    tvCompletionDate.setText(dateFormat.format(completionCalendar.getTime()));
                    tvCompletionDate.setTextColor(getResources().getColor(R.color.black));
                    updateAddButton();
                },
                completionCalendar.get(Calendar.YEAR),
                completionCalendar.get(Calendar.MONTH),
                completionCalendar.get(Calendar.DAY_OF_MONTH)
        );

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showPhotoOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Photo Source");

        String[] options = {"Camera", "Gallery"};

        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                openCamera();
            } else {
                openGallery();
            }
        });

        builder.show();
    }

    private void openCamera() {
        if (!PermissionUtils.isCameraPermissionGranted(this)) {
            PermissionUtils.requestCameraPermission(this);
        } else {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
            }
        }
    }

    private void openGallery() {
        if (!PermissionUtils.isStoragePermissionGranted(this)) {
            PermissionUtils.requestStoragePermission(this);
            return;
        }

        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Add flags to ensure we can read the URI
        galleryIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            galleryIntent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        }
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE && data != null) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    if (imageBitmap != null) {
                        // Convert bitmap to Base64
                        selectedPhotoBase64 = ImageUtils.bitmapToBase64(imageBitmap);
                        if (selectedPhotoBase64 != null) {
                            ivTargetPhoto.setImageBitmap(imageBitmap);
                            ivTargetPhoto.setVisibility(View.VISIBLE);
                            photoPlaceholder.setVisibility(View.GONE);
                            selectedPhotoUri = null; // Clear URI since we're using Base64
                            updateAddButton();
                        } else {
                            Toast.makeText(this, "Failed to process camera image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            } else if (requestCode == GALLERY_REQUEST_CODE && data != null) {
                selectedPhotoUri = data.getData();
                if (selectedPhotoUri != null) {
                    // Convert URI to Base64
                    selectedPhotoBase64 = ImageUtils.uriToBase64(this, selectedPhotoUri);
                    if (selectedPhotoBase64 != null) {
                        // Load the image from Base64 to display
                        loadImageFromBase64(selectedPhotoBase64);
                        updateAddButton();
                    } else {
                        // Fallback to URI loading if Base64 conversion fails
                        loadImageFromUri(selectedPhotoUri);
                        updateAddButton();
                        Toast.makeText(this, "Image converted to compatible format", Toast.LENGTH_SHORT).show();
                    }
                    
                    // Take persistent permission for the URI (for fallback compatibility)
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                            getContentResolver().takePersistableUriPermission(selectedPhotoUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        }
                    } catch (SecurityException e) {
                        // Permission not available for this URI, continue anyway
                        android.util.Log.w("AddSavingsActivity", "Could not take persistent permission: " + e.getMessage());
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Storage permission required to access gallery", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateAddButton() {
        String targetName = etTargetName.getText().toString().trim();
        String amountText = etTotalAmount.getText().toString().trim();
        String dateText = tvCompletionDate.getText().toString();

        boolean isNameValid = !targetName.isEmpty();
        boolean isAmountValid = !amountText.isEmpty();
        boolean isDateValid = !dateText.equals("DD/MM/YYYY");
        boolean hasPhoto = ivTargetPhoto.getVisibility() == View.VISIBLE;

        boolean isFormValid = isNameValid && isAmountValid && isDateValid;

        btnAddTarget.setEnabled(isFormValid);
        btnAddTarget.setAlpha(isFormValid ? 1.0f : 0.5f);
    }

    private void saveTarget() {
        String targetName = etTargetName.getText().toString().trim();
        String amountText = etTotalAmount.getText().toString().trim();

        if (targetName.isEmpty() || amountText.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amount = Double.parseDouble(amountText.replace(",", "").replace(".", ""));

            if (amount <= 0) {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isEditMode) {
                // Update existing savings in Firestore
                updateSavingsInFirestore(targetName, amount);
            } else {
                // Create new target (original functionality)
                createNewTarget(targetName, amount);
            }

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSavingsInFirestore(String targetName, double amount) {
        if (editSavingsId == null) {
            Toast.makeText(this, "Error: No savings ID found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        btnAddTarget.setEnabled(false);
        btnAddTarget.setText("Updating...");

        // Prepare update data
        java.util.Map<String, Object> updateData = new java.util.HashMap<>();
        updateData.put("name", targetName);
        updateData.put("category", selectedCategory);
        updateData.put("targetAmount", amount);
        updateData.put("completionDate", completionCalendar.getTimeInMillis());
        
        // Update photo data
        if (selectedPhotoBase64 != null && !selectedPhotoBase64.isEmpty()) {
            updateData.put("photoBase64", selectedPhotoBase64);
            updateData.put("photoUri", null); // Clear old URI
        } else if (selectedPhotoUri != null) {
            updateData.put("photoUri", selectedPhotoUri.toString());
            // Keep existing photoBase64 if no new image was selected
        }

        // Update in Firestore
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("savings")
                .document(editSavingsId)
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Savings goal updated successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnAddTarget.setEnabled(true);
                    btnAddTarget.setText("Update Goal");
                });
    }

    private void createNewTarget(String targetName, double amount) {
        // Create target data with Base64 image support
        SavingsTarget savingsTarget = new SavingsTarget(
                targetName,
                selectedCategory,
                amount,
                completionCalendar.getTimeInMillis(),
                selectedPhotoUri != null ? selectedPhotoUri.toString() : null,
                System.currentTimeMillis()
        );
        
        // Set Base64 image data if available
        if (selectedPhotoBase64 != null && !selectedPhotoBase64.isEmpty()) {
            savingsTarget.setPhotoBase64(selectedPhotoBase64);
        }

        // Prepare result intent with all necessary data
        Intent resultIntent = new Intent();
        resultIntent.putExtra("target_name", targetName);
        resultIntent.putExtra("target_category", selectedCategory);
        resultIntent.putExtra("target_amount", amount);
        resultIntent.putExtra("completion_date", completionCalendar.getTimeInMillis());
        resultIntent.putExtra("completion_date_str", dateFormat.format(completionCalendar.getTime()));
        resultIntent.putExtra("photo_uri", selectedPhotoUri != null ? selectedPhotoUri.toString() : null);
        resultIntent.putExtra("photo_base64", selectedPhotoBase64); // Add Base64 data
        resultIntent.putExtra("amount_formatted", formatNumber((long) amount));
        resultIntent.putExtra("created_at", System.currentTimeMillis());

        // Set the result and finish
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private String formatNumber(long number) {
        return String.format("%,d", number).replace(",", ".");
    }

    // Data class for savings target
    public static class SavingsTarget {
        public String name;
        public String category;
        public double targetAmount;
        public long completionDate;
        public String photoUri;
        public String photoBase64; // Base64 encoded image data
        public long createdAt;

        public SavingsTarget(String name, String category, double targetAmount, long completionDate, String photoUri, long createdAt) {
            this.name = name;
            this.category = category;
            this.targetAmount = targetAmount;
            this.completionDate = completionDate;
            this.photoUri = photoUri;
            this.createdAt = createdAt;
        }
        
        public void setPhotoBase64(String photoBase64) {
            this.photoBase64 = photoBase64;
        }
        
        public String getPhotoBase64() {
            return photoBase64;
        }
    }

    private void updateCategoryDisplay() {
        tvSelectedCategory.setText(selectedCategory);
        
        // Update icon and color based on category
        switch (selectedCategory.toLowerCase()) {
            case "food & beverages":
                selectedCategoryIcon = R.drawable.ic_food;
                selectedCategoryColor = R.color.orange_primary;
                break;
            case "transportation":
                selectedCategoryIcon = R.drawable.ic_transportation;
                selectedCategoryColor = R.color.blue_primary;
                break;
            case "shopping":
                selectedCategoryIcon = R.drawable.ic_shopping;
                selectedCategoryColor = R.color.pink_primary;
                break;
            case "entertainment":
                selectedCategoryIcon = R.drawable.ic_others;
                selectedCategoryColor = R.color.red_primary;
                break;
            case "bills & utilities":
                selectedCategoryIcon = R.drawable.ic_bills;
                selectedCategoryColor = R.color.purple_primary;
                break;
            case "health":
                selectedCategoryIcon = R.drawable.ic_health;
                selectedCategoryColor = R.color.green_primary;
                break;
            case "education":
                selectedCategoryIcon = R.drawable.ic_others;
                selectedCategoryColor = R.color.blue_secondary;
                break;
            default:
                selectedCategoryIcon = R.drawable.ic_other;
                selectedCategoryColor = R.color.gray_primary;
                break;
        }
        
        ivCategoryIcon.setImageResource(selectedCategoryIcon);
        ivCategoryIcon.setColorFilter(ContextCompat.getColor(this, selectedCategoryColor));
    }
}