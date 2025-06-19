package com.example.spendly.activity;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddSavingsActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 1001;
    private static final int GALLERY_REQUEST_CODE = 1002;
    private static final int CAMERA_PERMISSION_CODE = 1003;

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
        // Set default completion date (1 year from now)
        completionCalendar.add(Calendar.YEAR, 1);
        tvCompletionDate.setText(dateFormat.format(completionCalendar.getTime()));
        tvCompletionDate.setTextColor(getResources().getColor(R.color.black));
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
                R.drawable.ic_others
        };

        int[] colors = {
                R.color.orange_primary,
                R.color.blue_primary,
                R.color.pink_primary,
                R.color.green_primary,
                R.color.purple_primary,
                R.color.red_primary,
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
            }
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
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
                        ivTargetPhoto.setImageBitmap(imageBitmap);
                        ivTargetPhoto.setVisibility(View.VISIBLE);
                        photoPlaceholder.setVisibility(View.GONE);
                        updateAddButton();
                    }
                }
            } else if (requestCode == GALLERY_REQUEST_CODE && data != null) {
                selectedPhotoUri = data.getData();
                if (selectedPhotoUri != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedPhotoUri);
                        ivTargetPhoto.setImageBitmap(bitmap);
                        ivTargetPhoto.setVisibility(View.VISIBLE);
                        photoPlaceholder.setVisibility(View.GONE);
                        updateAddButton();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
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

            // Create target data
            SavingsTarget savingsTarget = new SavingsTarget(
                    targetName,
                    selectedCategory,
                    amount,
                    completionCalendar.getTimeInMillis(),
                    selectedPhotoUri != null ? selectedPhotoUri.toString() : null,
                    System.currentTimeMillis()
            );

            // Save to database or pass back to parent
            Intent resultIntent = new Intent();
            resultIntent.putExtra("target_name", targetName);
            resultIntent.putExtra("target_category", selectedCategory);
            resultIntent.putExtra("target_amount", amount);
            resultIntent.putExtra("completion_date", dateFormat.format(completionCalendar.getTime()));
            resultIntent.putExtra("photo_uri", selectedPhotoUri != null ? selectedPhotoUri.toString() : null);
            resultIntent.putExtra("amount_formatted", formatNumber((long) amount));

            setResult(RESULT_OK, resultIntent);

            Toast.makeText(this, "Target added successfully: " + targetName, Toast.LENGTH_SHORT).show();
            finish();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
        }
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
        public long createdAt;

        public SavingsTarget(String name, String category, double targetAmount, long completionDate, String photoUri, long createdAt) {
            this.name = name;
            this.category = category;
            this.targetAmount = targetAmount;
            this.completionDate = completionDate;
            this.photoUri = photoUri;
            this.createdAt = createdAt;
        }
    }
}