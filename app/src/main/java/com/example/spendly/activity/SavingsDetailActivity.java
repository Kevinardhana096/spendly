package com.example.spendly.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.spendly.R;
import com.example.spendly.adapter.SavingHistoryAdapter;
import com.example.spendly.model.SavingHistoryItem;
import com.example.spendly.model.SavingsItem;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SavingsDetailActivity extends AppCompatActivity implements SavingHistoryAdapter.OnHistoryItemClickListener {

    private static final String TAG = "SavingsDetailActivity";
    private static final int REQUEST_VERIFY_PIN_ADD_MONEY = 1001;
    private static final int REQUEST_EDIT_SAVINGS = 1002;

    // Current context - Updated to 2025-06-21 19:30:30 UTC
    private static final String CURRENT_DATE_TIME = "2025-06-21 19:30:30";
    private static final String CURRENT_USER = "Kevin Ardhana";
    private static final long CURRENT_TIMESTAMP = 1719345030000L; // 2025-06-21 19:30:30 UTC

    // UI Components - Simplified (removed bottom navigation)
    private ImageView backButton;
    private ImageView carImage;
    private TextView carTitle;
    private TextView targetDate;
    private MaterialButton editButton;
    private TextView currentAmount;
    private TextView needsAmount;
    private TextView targetAmount;
    private MaterialButton addMoneyButton;
    private TextView addHistoryButton;
    private RecyclerView historyRecyclerView;
    private LinearLayout emptyHistoryLayout;    // Data
    private SavingsItem savingsItem;
    private String savingsId;
    private List<SavingHistoryItem> historyList;
    private SavingHistoryAdapter historyAdapter;
    
    // Temporary data for PIN verification
    private double pendingAddAmount = 0.0;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "=== SavingsDetailActivity Started ===");
        Log.d(TAG, "Current Date/Time: " + CURRENT_DATE_TIME + " UTC");
        Log.d(TAG, "Current User: " + CURRENT_USER);
        Log.d(TAG, "Current Timestamp: " + CURRENT_TIMESTAMP);
        Log.d(TAG, "Purpose: Display detailed savings information and transaction management");

        try {
            // Set content view (without bottom navigation)
            setContentView(R.layout.activity_savings_detail);
            Log.d(TAG, "Layout inflated successfully (clean detail view)");

            // Initialize Firebase
            initializeFirebase();

            // Initialize history list
            historyList = new ArrayList<>();

            // Initialize views
            initViews();

            // Get data from intent
            getSavingsDataFromIntent();

            // Setup click listeners
            setupClickListeners();

            // Setup RecyclerView
            setupHistoryRecyclerView();

            // Display savings data
            displaySavingsData();

            // Load saving history
            loadSavingHistory();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error loading savings detail: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeFirebase() {
        Log.d(TAG, "Initializing Firebase for user: " + CURRENT_USER);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            Log.d(TAG, "Firebase User authenticated: " + currentUser.getEmail());
            Log.d(TAG, "Firebase UID: " + currentUser.getUid());

            // Verify user matches expected user
            if (!CURRENT_USER.equals(currentUser.getEmail()) &&
                    !currentUser.getEmail().contains(CURRENT_USER)) {
                Log.w(TAG, "User mismatch - Expected: " + CURRENT_USER + ", Got: " + currentUser.getEmail());
            }
        } else {
            Log.e(TAG, "No Firebase user found!");
            Toast.makeText(this, "Please login to view savings details", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        Log.d(TAG, "Initializing views (detail activity - no bottom navigation)...");

        try {
            // Header views
            backButton = findViewById(R.id.backButton);

            // Content views
            carImage = findViewById(R.id.carImage);
            carTitle = findViewById(R.id.carTitle);
            targetDate = findViewById(R.id.targetDate);
            editButton = findViewById(R.id.editButton);
            currentAmount = findViewById(R.id.currentAmount);
            needsAmount = findViewById(R.id.needsAmount);
            targetAmount = findViewById(R.id.targetAmount);

            // Action views
            addMoneyButton = findViewById(R.id.addMoneyButton);
            addHistoryButton = findViewById(R.id.addHistoryButton);

            // History views
            historyRecyclerView = findViewById(R.id.historyRecyclerView);
            emptyHistoryLayout = findViewById(R.id.emptyHistoryLayout);

            // Log view initialization status
            Log.d(TAG, "View initialization status:");
            Log.d(TAG, "✓ backButton: " + (backButton != null));
            Log.d(TAG, "✓ carImage: " + (carImage != null));
            Log.d(TAG, "✓ carTitle: " + (carTitle != null));
            Log.d(TAG, "✓ currentAmount: " + (currentAmount != null));
            Log.d(TAG, "✓ addMoneyButton: " + (addMoneyButton != null));
            Log.d(TAG, "✓ historyRecyclerView: " + (historyRecyclerView != null));
            Log.d(TAG, "✓ emptyHistoryLayout: " + (emptyHistoryLayout != null));

            // Validate critical views
            if (backButton == null || carImage == null || carTitle == null ||
                    currentAmount == null || addMoneyButton == null || historyRecyclerView == null) {
                throw new RuntimeException("Critical views are missing! Check activity_savings_detail.xml");
            }

            Log.d(TAG, "All critical views initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            throw e;
        }
    }

    private void getSavingsDataFromIntent() {
        Log.d(TAG, "Getting savings data from intent...");

        Intent intent = getIntent();
        if (intent != null) {
            // Get savings ID
            savingsId = intent.getStringExtra("savings_id");
            Log.d(TAG, "Received savings ID: " + savingsId);

            // Get SavingsItem object
            if (intent.hasExtra("savings_item")) {
                savingsItem = (SavingsItem) intent.getSerializableExtra("savings_item");
                if (savingsItem != null) {
                    Log.d(TAG, "Received savings item: " + savingsItem.getName());
                    Log.d(TAG, "Current amount: Rp" + formatNumber(savingsItem.getCurrentAmount()));
                    Log.d(TAG, "Target amount: Rp" + formatNumber(savingsItem.getTargetAmount()));

                    if (savingsItem.getCompletionDate() > 0) {
                        Date targetDate = new Date(savingsItem.getCompletionDate());
                        Log.d(TAG, "Target date: " + formatDate(targetDate));

                        // Calculate days remaining from current date
                        long daysRemaining = calculateDaysFromCurrentDate(targetDate);
                        Log.d(TAG, "Days remaining: " + daysRemaining);
                    }
                } else {
                    Log.e(TAG, "Savings item is null!");
                }
            } else {
                Log.w(TAG, "No savings_item in intent");
            }

            // Debug info from intent
            String debugUser = intent.getStringExtra("debug_user");
            String debugSource = intent.getStringExtra("debug_source");
            long debugTimestamp = intent.getLongExtra("debug_timestamp", 0L);

            Log.d(TAG, "Debug info from intent:");
            Log.d(TAG, "- User: " + debugUser);
            Log.d(TAG, "- Source: " + debugSource);
            Log.d(TAG, "- Timestamp: " + debugTimestamp);

            // If no savings item but has ID, fetch from Firestore
            if (savingsItem == null && savingsId != null) {
                Log.d(TAG, "No savings item in intent, fetching from Firestore with ID: " + savingsId);
                fetchSavingsFromFirestore();
            }
        } else {
            Log.e(TAG, "Intent is null!");
        }

        if (savingsItem == null && savingsId == null) {
            Log.e(TAG, "No savings data provided in intent");
            Toast.makeText(this, "Error: No savings data found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void fetchSavingsFromFirestore() {
        if (currentUser == null || savingsId == null) {
            Log.e(TAG, "Cannot fetch from Firestore - user or ID is null");
            return;
        }

        Log.d(TAG, "Fetching savings from Firestore...");
        Log.d(TAG, "User ID: " + currentUser.getUid());
        Log.d(TAG, "Savings ID: " + savingsId);

        DocumentReference docRef = db.collection("users")
                .document(currentUser.getUid())
                .collection("savings")
                .document(savingsId);

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Log.d(TAG, "Document found in Firestore");
                savingsItem = documentSnapshot.toObject(SavingsItem.class);
                if (savingsItem != null) {
                    savingsItem.setId(documentSnapshot.getId());
                    Log.d(TAG, "Successfully fetched savings: " + savingsItem.getName());
                    displaySavingsData();
                    loadSavingHistory();
                } else {
                    Log.e(TAG, "Failed to convert document to SavingsItem");
                    Toast.makeText(this, "Error parsing savings data", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Log.e(TAG, "Savings document does not exist in Firestore");
                Toast.makeText(this, "Savings data not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching savings data from Firestore", e);
            Toast.makeText(this, "Error loading savings data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void setupClickListeners() {
        Log.d(TAG, "Setting up click listeners for detail activity...");

        // Back button - return to previous screen (main navigation)
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                Log.d(TAG, "Back button clicked - returning to savings list");
                Log.d(TAG, "User: " + CURRENT_USER + " finished viewing savings details");
                finish(); // Clean back navigation
            });
        }        // Edit button - edit savings details
        if (editButton != null) {
            editButton.setOnClickListener(v -> {
                Log.d(TAG, "Edit button clicked for savings: " +
                        (savingsItem != null ? savingsItem.getName() : "unknown"));
                Log.d(TAG, "User: " + CURRENT_USER + " wants to edit savings");

                // Navigate to edit savings activity
                Intent editIntent = new Intent(SavingsDetailActivity.this, AddSavingsActivity.class);
                editIntent.putExtra("edit_mode", true);
                editIntent.putExtra("savings_item", savingsItem);
                editIntent.putExtra("savings_id", savingsId);
                startActivityForResult(editIntent, 2002);
            });
        }

        // Add money button - primary action
        if (addMoneyButton != null) {
            addMoneyButton.setOnClickListener(v -> {
                Log.d(TAG, "Add money button clicked at " + CURRENT_DATE_TIME);
                Log.d(TAG, "User: " + CURRENT_USER + " wants to add money to savings");
                showAddMoneyDialog();
            });
        }

        // View all history button (optional)
        if (addHistoryButton != null) {
            addHistoryButton.setOnClickListener(v -> {
                Log.d(TAG, "View all history clicked");
                Log.d(TAG, "User: " + CURRENT_USER + " wants to view full history");

                // TODO: Navigate to full history view
                Toast.makeText(this, "Full history view coming soon", Toast.LENGTH_SHORT).show();
            });
        }

        Log.d(TAG, "All click listeners setup completed");
    }

    private void setupHistoryRecyclerView() {
        Log.d(TAG, "Setting up history RecyclerView...");

        if (historyRecyclerView != null) {
            historyAdapter = new SavingHistoryAdapter(historyList, this);
            historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            historyRecyclerView.setAdapter(historyAdapter);

            // Set nested scrolling to false for better performance in ScrollView
            historyRecyclerView.setNestedScrollingEnabled(false);

            Log.d(TAG, "History RecyclerView setup completed");
        } else {
            Log.e(TAG, "historyRecyclerView is null!");
        }
    }

    private void displaySavingsData() {
        if (savingsItem == null) {
            Log.e(TAG, "Cannot display savings data - savingsItem is null");
            return;
        }

        Log.d(TAG, "Displaying savings data for: " + savingsItem.getName());
        Log.d(TAG, "Current time: " + CURRENT_DATE_TIME);
        Log.d(TAG, "Current user: " + CURRENT_USER);

        // Set title
        if (carTitle != null) {
            carTitle.setText(savingsItem.getName());
            Log.d(TAG, "Set title: " + savingsItem.getName());
        }

        // Set target date with current date context
        if (targetDate != null && savingsItem.getCompletionDate() > 0) {
            Date targetDateObj = new Date(savingsItem.getCompletionDate());
            String formattedTargetDate = formatDate(targetDateObj);
            targetDate.setText("Target Goal " + formattedTargetDate);

            // Calculate days from current date (2025-06-21 19:30:30)
            long daysFromToday = calculateDaysFromCurrentDate(targetDateObj);
            Log.d(TAG, "Target date: " + formattedTargetDate + " (" + daysFromToday + " days from today)");
        } else {
            if (targetDate != null) {
                targetDate.setText("Target Goal: Not set");
            }
            Log.w(TAG, "No target date set for savings");
        }

        // Set amounts with detailed logging
        double current = savingsItem.getCurrentAmount();
        double target = savingsItem.getTargetAmount();
        double progress = target > 0 ? (current / target) * 100 : 0;

        Log.d(TAG, "Amount details:");
        Log.d(TAG, "- Current amount: Rp" + formatNumber(current));
        Log.d(TAG, "- Target amount: Rp" + formatNumber(target));
        Log.d(TAG, "- Progress: " + String.format("%.2f%%", progress));

        if (currentAmount != null) {
            currentAmount.setText(formatCurrency(current));
        }

        if (targetAmount != null) {
            targetAmount.setText(formatCurrency(target));
        }

        // Calculate needs amount
        double needs = target - current;
        if (needs < 0) needs = 0;

        if (needsAmount != null) {
            needsAmount.setText(formatCurrency(needs));
            Log.d(TAG, "Still needs: Rp" + formatNumber(needs));
        }

        // Load image with Glide
        loadSavingImage();

        Log.d(TAG, "Savings data displayed successfully");
    }

    private void loadSavingImage() {
        if (carImage == null) {
            Log.e(TAG, "carImage view is null");
            return;
        }

        if (savingsItem.getPhotoUri() != null && !savingsItem.getPhotoUri().isEmpty()) {
            Log.d(TAG, "Loading image from URI: " + savingsItem.getPhotoUri());

            try {
                Glide.with(this)
                        .load(savingsItem.getPhotoUri())
                        .placeholder(R.drawable.placeholder_green)
                        .error(R.drawable.placeholder_green)
                        .centerCrop()
                        .into(carImage);

                Log.d(TAG, "Image loaded successfully with Glide");
            } catch (Exception e) {
                Log.e(TAG, "Error loading image with Glide", e);
                carImage.setImageResource(R.drawable.placeholder_green);
            }
        } else {
            Log.d(TAG, "No photo URI provided, using placeholder");
            carImage.setImageResource(R.drawable.placeholder_green);
        }
    }

    private void loadSavingHistory() {
        if (currentUser == null || savingsId == null) {
            Log.e(TAG, "Cannot load history - user or savingsId is null");
            showEmptyHistoryState();
            return;
        }

        Log.d(TAG, "Loading saving history for savings ID: " + savingsId);
        Log.d(TAG, "User: " + CURRENT_USER + " at " + CURRENT_DATE_TIME);

        db.collection("users")
                .document(currentUser.getUid())
                .collection("savings")
                .document(savingsId)
                .collection("history")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    historyList.clear();

                    if (!queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " history items");

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            SavingHistoryItem historyItem = document.toObject(SavingHistoryItem.class);
                            if (historyItem != null) {
                                historyItem.setId(document.getId());
                                historyList.add(historyItem);

                                // Log each history item with current time context
                                Date historyDate = new Date(historyItem.getDate());
                                long minutesAgo = (CURRENT_TIMESTAMP - historyItem.getDate()) / (1000 * 60);

                                Log.d(TAG, "History item: " + historyItem.getFormattedAmount() +
                                        " on " + formatDateTime(historyDate) +
                                        " (" + minutesAgo + " minutes ago)");
                            }
                        }

                        // Show history RecyclerView, hide empty state
                        if (historyRecyclerView != null) {
                            historyRecyclerView.setVisibility(View.VISIBLE);
                        }
                        if (emptyHistoryLayout != null) {
                            emptyHistoryLayout.setVisibility(View.GONE);
                        }

                        // Update adapter
                        if (historyAdapter != null) {
                            historyAdapter.notifyDataSetChanged();
                        }

                        Log.d(TAG, "History data loaded and displayed successfully");
                    } else {
                        Log.d(TAG, "No history items found, showing empty state");
                        showEmptyHistoryState();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading saving history", e);
                    showEmptyHistoryState();
                });
    }

    private void showEmptyHistoryState() {
        if (historyRecyclerView != null) {
            historyRecyclerView.setVisibility(View.GONE);
        }
        if (emptyHistoryLayout != null) {
            emptyHistoryLayout.setVisibility(View.VISIBLE);
        }
        Log.d(TAG, "Showing empty history state for user: " + CURRENT_USER);
    }

    private void showAddMoneyDialog() {
        Log.d(TAG, "Showing add money dialog at " + CURRENT_DATE_TIME);
        Log.d(TAG, "User: " + CURRENT_USER + " wants to add money to: " +
                (savingsItem != null ? savingsItem.getName() : "unknown savings"));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Money to " + (savingsItem != null ? savingsItem.getName() : "Savings"));
        builder.setMessage("How much would you like to add to your savings?");

        // Create input field
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Enter amount (Rp)");

        // Set padding for better UX
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        builder.setView(input);        builder.setPositiveButton("Add Money", (dialog, which) -> {
            String amountStr = input.getText().toString().trim();
            Log.d(TAG, "User " + CURRENT_USER + " entered amount: " + amountStr);

            if (!amountStr.isEmpty()) {
                try {
                    double amount = Double.parseDouble(amountStr);
                    if (amount > 0) {
                        Log.d(TAG, "Valid amount entered: Rp" + formatNumber(amount));
                        // Store amount and verify PIN first
                        pendingAddAmount = amount;
                        verifyPinForAddMoney();
                    } else {
                        Log.w(TAG, "Invalid amount (not positive): " + amount);
                        Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid number format: " + amountStr, e);
                    Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.w(TAG, "Empty amount entered by user: " + CURRENT_USER);
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            Log.d(TAG, "Add money dialog cancelled by user: " + CURRENT_USER);
            dialog.cancel();
        });

        builder.show();
    }    private void addMoneyToSavings(double amount) {
        if (currentUser == null || savingsId == null) {
            Log.e(TAG, "Cannot add money - user or savingsId is null");
            return;
        }

        Log.d(TAG, "=== ADDING MONEY TO SAVINGS ===");
        Log.d(TAG, "User: " + CURRENT_USER);
        Log.d(TAG, "DateTime: " + CURRENT_DATE_TIME);
        Log.d(TAG, "Timestamp: " + CURRENT_TIMESTAMP);
        Log.d(TAG, "Amount to add: Rp" + formatNumber(amount));
        Log.d(TAG, "Savings ID: " + savingsId);
        Log.d(TAG, "Current amount before: Rp" + formatNumber(savingsItem.getCurrentAmount()));

        // Calculate new amount
        double newCurrentAmount = savingsItem.getCurrentAmount() + amount;
        Log.d(TAG, "New current amount will be: Rp" + formatNumber(newCurrentAmount));

        // Calculate new progress
        double newProgress = savingsItem.getTargetAmount() > 0 ?
                (newCurrentAmount / savingsItem.getTargetAmount()) * 100 : 0;
        Log.d(TAG, "New progress: " + String.format("%.2f%%", newProgress));

        // Create history entry with current timestamp
        Map<String, Object> historyData = new HashMap<>();
        historyData.put("amount", amount);
        historyData.put("date", CURRENT_TIMESTAMP);
        historyData.put("formattedAmount", formatCurrency(amount));
        historyData.put("type", "deposit");
        historyData.put("note", "Manual deposit by " + CURRENT_USER + " at " + CURRENT_DATE_TIME);
        historyData.put("userId", CURRENT_USER);
        historyData.put("savingsId", savingsId);

        // Create transaction for transaction history (savings as expense)
        Map<String, Object> transactionData = new HashMap<>();
        transactionData.put("amount", amount);
        transactionData.put("category", "Savings");
        transactionData.put("type", "expense"); // Savings is an expense from available balance
        transactionData.put("description", "Money transferred to savings: " + savingsItem.getName());
        transactionData.put("date", CURRENT_TIMESTAMP);
        transactionData.put("createdAt", CURRENT_TIMESTAMP);
        transactionData.put("userId", currentUser.getUid());
        transactionData.put("formattedAmount", formatCurrency(amount));

        // Update savings document
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("currentAmount", newCurrentAmount);

        Log.d(TAG, "Saving transaction to Firestore...");

        // First, check user's current balance
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Double currentBalance = documentSnapshot.getDouble("currentBalance");
                        if (currentBalance == null) currentBalance = 0.0;

                        Log.d(TAG, "Current user balance: Rp" + formatNumber(currentBalance));

                        if (currentBalance >= amount) {
                            // User has sufficient balance, proceed with transactions
                            double newBalance = currentBalance - amount;
                            Log.d(TAG, "New balance will be: Rp" + formatNumber(newBalance));

                            // Start transaction: Add to savings history
                            db.collection("users")
                                    .document(currentUser.getUid())
                                    .collection("savings")
                                    .document(savingsId)
                                    .collection("history")
                                    .add(historyData)
                                    .addOnSuccessListener(historyRef -> {
                                        Log.d(TAG, "Savings history added successfully");

                                        // Update savings amount
                                        db.collection("users")
                                                .document(currentUser.getUid())
                                                .collection("savings")
                                                .document(savingsId)
                                                .update(updateData)
                                                .addOnSuccessListener(aVoid -> {
                                                    Log.d(TAG, "Savings amount updated successfully");

                                                    // Add transaction to transaction history
                                                    db.collection("users")
                                                            .document(currentUser.getUid())
                                                            .collection("transactions")
                                                            .add(transactionData)
                                                            .addOnSuccessListener(transactionRef -> {
                                                                Log.d(TAG, "Transaction history added successfully");

                                                                // Update user balance
                                                                db.collection("users")
                                                                        .document(currentUser.getUid())
                                                                        .update("currentBalance", newBalance)
                                                                        .addOnSuccessListener(balanceUpdate -> {
                                                                            Log.d(TAG, "✅ ALL OPERATIONS COMPLETED SUCCESSFULLY");
                                                                            Log.d(TAG, "- Savings updated: " + formatCurrency(newCurrentAmount));
                                                                            Log.d(TAG, "- Balance reduced: " + formatCurrency(newBalance));
                                                                            Log.d(TAG, "- Transaction recorded");
                                                                            Log.d(TAG, "- Progress: " + String.format("%.1f%%", newProgress));

                                                                            // Update local data
                                                                            savingsItem.setCurrentAmount(newCurrentAmount);

                                                                            // Refresh UI
                                                                            displaySavingsData();
                                                                            loadSavingHistory();

                                                                            String successMessage = "Money added successfully!\n" +
                                                                                    "Added: " + formatCurrency(amount) + "\n" +
                                                                                    "New savings: " + formatCurrency(newCurrentAmount) + "\n" +
                                                                                    "Progress: " + String.format("%.1f%%", newProgress) + "\n" +
                                                                                    "Available balance: " + formatCurrency(newBalance);

                                                                            Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();

                                                                            // Set result to indicate changes were made
                                                                            setResult(RESULT_OK);
                                                                        })
                                                                        .addOnFailureListener(e -> {
                                                                            Log.e(TAG, "❌ Error updating user balance", e);
                                                                            Toast.makeText(this, "Failed to update balance: " + e.getMessage(),
                                                                                    Toast.LENGTH_SHORT).show();
                                                                        });
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                Log.e(TAG, "❌ Error adding transaction history", e);
                                                                Toast.makeText(this, "Failed to record transaction: " + e.getMessage(),
                                                                        Toast.LENGTH_SHORT).show();
                                                            });
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e(TAG, "❌ Error updating savings amount", e);
                                                    Toast.makeText(this, "Failed to update savings: " + e.getMessage(),
                                                            Toast.LENGTH_SHORT).show();
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "❌ Error adding savings history", e);
                                        Toast.makeText(this, "Failed to add money: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Log.w(TAG, "Insufficient balance. Required: " + formatNumber(amount) + 
                                    ", Available: " + formatNumber(currentBalance));
                            Toast.makeText(this, "Insufficient balance!\n" +
                                    "Required: " + formatCurrency(amount) + "\n" +
                                    "Available: " + formatCurrency(currentBalance), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.e(TAG, "User document not found");
                        Toast.makeText(this, "Error: User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking user balance", e);
                    Toast.makeText(this, "Error checking balance: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void verifyPinForAddMoney() {
        Log.d(TAG, "Starting PIN verification for add money: Rp" + formatNumber(pendingAddAmount));
        
        Intent intent = new Intent(this, VerifyPinActivity.class);
        intent.putExtra(VerifyPinActivity.EXTRA_VERIFICATION_TYPE, VerifyPinActivity.TYPE_ADD_MONEY);
        
        // Pass verification data
        Bundle verificationData = new Bundle();
        verificationData.putDouble("amount", pendingAddAmount);
        verificationData.putString("savings_name", savingsItem != null ? savingsItem.getName() : "Savings");
        intent.putExtra(VerifyPinActivity.EXTRA_VERIFICATION_DATA, verificationData);
        
        startActivityForResult(intent, REQUEST_VERIFY_PIN_ADD_MONEY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_VERIFY_PIN_ADD_MONEY) {
            if (resultCode == RESULT_OK) {
                // PIN verified successfully, proceed with add money
                Log.d(TAG, "PIN verified successfully, proceeding to add money: Rp" + formatNumber(pendingAddAmount));
                addMoneyToSavings(pendingAddAmount);
            } else {
                // PIN verification failed or cancelled
                Log.d(TAG, "PIN verification cancelled or failed");
                Toast.makeText(this, "PIN verification required to add money", Toast.LENGTH_SHORT).show();
            }
            // Reset pending amount
            pendingAddAmount = 0.0;
        } else if (requestCode == REQUEST_EDIT_SAVINGS) {            if (resultCode == RESULT_OK) {
                // Savings edited successfully, refresh data
                Log.d(TAG, "Savings edited successfully, refreshing data");
                displaySavingsData();
                loadSavingHistory();
            }
        }
    }

    // Helper methods
    private String formatCurrency(double amount) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return "Rp" + formatter.format(amount).replace(",", ".");
    }

    private String formatNumber(double amount) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(amount).replace(",", ".");
    }

    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return sdf.format(date);
    }

    private String formatDateTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    private long calculateDaysFromCurrentDate(Date targetDate) {
        // Calculate from current date: 2025-06-21 19:30:30
        Date currentDate = new Date(CURRENT_TIMESTAMP);

        long diffInMillies = targetDate.getTime() - currentDate.getTime();
        return TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onHistoryItemClick(SavingHistoryItem historyItem) {
        Log.d(TAG, "History item clicked by user: " + CURRENT_USER);
        Log.d(TAG, "Clicked item: " + historyItem.getFormattedAmount());

        Date historyDate = new Date(historyItem.getDate());
        long minutesAgo = (CURRENT_TIMESTAMP - historyItem.getDate()) / (1000 * 60);

        String message = "Transaction Details:\n" +
                "Amount: " + historyItem.getFormattedAmount() + "\n" +
                "Date: " + formatDateTime(historyDate) + "\n" +
                "(" + minutesAgo + " minutes ago)";

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "=== Activity Resumed ===");
        Log.d(TAG, "Current time: " + CURRENT_DATE_TIME);
        Log.d(TAG, "User: " + CURRENT_USER + " returned to savings detail");

        // Refresh data when returning to this activity
        if (savingsItem != null && savingsId != null) {
            Log.d(TAG, "Refreshing savings data for: " + savingsItem.getName());
            loadSavingHistory();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Activity paused - User: " + CURRENT_USER + " left savings detail");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "=== SavingsDetailActivity Destroyed ===");
        Log.d(TAG, "User: " + CURRENT_USER + " finished viewing savings details");
        Log.d(TAG, "Session ended at: " + CURRENT_DATE_TIME);
    }

}