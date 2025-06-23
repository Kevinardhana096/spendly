package com.example.spendly.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.example.spendly.R;
import com.example.spendly.activity.AddSavingsActivity;
import com.example.spendly.activity.SavingsDetailActivity;
import com.example.spendly.adapter.SavingsAdapter;
import com.example.spendly.model.SavingsItem;
import com.example.spendly.repository.RealtimeSavingsRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SavingFragment extends Fragment implements SavingsAdapter.OnSavingsItemClickListener, SavingsAdapter.OnSavingItemRemoveListener {

    private static final String TAG = "SavingFragment";
    private static final int REQUEST_CODE_ADD_SAVINGS = 1001;

    // Current context - Updated to 2025-06-21 19:03:35
    private static final String CURRENT_DATE_TIME = "2025-06-21 19:03:35";
    private static final String CURRENT_USER = "nowriafisda";
    private static final long CURRENT_TIMESTAMP = 1719343415000L; // 2025-06-21 19:03:35 UTC    // View variables
    private View rootView;
    private View emptyView;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    // Real-time repository
    private RealtimeSavingsRepository savingsRepository;

    // UI components for savings list layout
    private MaterialButton btnAddNewTarget;
    private RecyclerView recyclerViewSavings;
    private SavingsAdapter savingsAdapter;
    private List<SavingsItem> savingsList;

    // UI components for empty state layout
    private Button btnAddFirstSaving;

    public SavingFragment() {
        // Required empty public constructor
    }    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "=== SavingFragment Created with Real-time Repository ===");        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            Log.d(TAG, "Firebase user authenticated: " + currentUser.getEmail());
            
            // Initialize real-time repository
            savingsRepository = RealtimeSavingsRepository.getInstance(requireContext());
        } else {
            Log.e(TAG, "No Firebase user found!");
        }

        // Initialize savings list
        savingsList = new ArrayList<>();
    }    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");

        rootView = inflater.inflate(R.layout.fragment_saving, container, false);

        // Initialize views
        setupViews(inflater, container);

        // Setup real-time data observer
        setupRealtimeDataObserver();

        return rootView;
    }

    private void setupRealtimeDataObserver() {
        if (currentUser == null || savingsRepository == null) {
            Log.e(TAG, "Cannot setup observer - user or repository is null");
            return;
        }

        Log.d(TAG, "Setting up real-time data observer");

        // Observe savings data with real-time updates
        savingsRepository.getSavings(currentUser.getUid()).observe(getViewLifecycleOwner(), 
            new Observer<List<SavingsItem>>() {
                @Override
                public void onChanged(List<SavingsItem> savings) {
                    Log.d(TAG, "Real-time savings update received: " + 
                          (savings != null ? savings.size() : 0) + " items");
                    
                    if (savings != null) {
                        savingsList.clear();
                        savingsList.addAll(savings);
                        
                        if (savingsAdapter != null) {
                            savingsAdapter.setSavingsList(savingsList);
                        }
                        
                        // Update UI state
                        if (savings.isEmpty()) {
                            showEmptyState();
                        } else {
                            showListState();
                        }
                    }
                }
            });

        // Observe loading state
        savingsRepository.getLoadingState().observe(getViewLifecycleOwner(), 
            isLoading -> {
                // You can show/hide loading indicators here
                Log.d(TAG, "Loading state: " + isLoading);
            });

        // Observe error messages
        savingsRepository.getErrorMessage().observe(getViewLifecycleOwner(), 
            error -> {
                if (error != null && !error.isEmpty()) {
                    Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void setupViews(LayoutInflater inflater, ViewGroup container) {
        Log.d(TAG, "Setting up views...");

        // Set up the list view (recycler view with items)
        recyclerViewSavings = rootView.findViewById(R.id.recyclerView_savings);
        btnAddNewTarget = rootView.findViewById(R.id.btn_add_new_target);

        if (btnAddNewTarget != null) {
            btnAddNewTarget.setOnClickListener(v -> navigateToAddSavings());
            Log.d(TAG, "Add new target button listener set");
        } else {
            Log.e(TAG, "btnAddNewTarget is null!");
        }

        // Setup RecyclerView
        setupRecyclerView();

        // Create and add the empty view as needed
        ViewGroup parent = (ViewGroup) recyclerViewSavings.getParent();
        emptyView = inflater.inflate(R.layout.empty_savings_state, container, false);
        parent.addView(emptyView);
        emptyView.setVisibility(View.GONE);

        // Set up the button in empty view
        btnAddFirstSaving = emptyView.findViewById(R.id.btn_add_first_saving);
        if (btnAddFirstSaving != null) {
            btnAddFirstSaving.setOnClickListener(v -> navigateToAddSavings());
            Log.d(TAG, "Add first saving button listener set");
        }

        Log.d(TAG, "Views setup completed");
    }    private void checkForSavingsData() {
        // This method is now handled by the real-time repository observer
        // No manual data loading needed
        Log.d(TAG, "Data loading handled by real-time observer");
    }

    private void showEmptyState() {
        Log.d(TAG, "Showing empty state");
        if (recyclerViewSavings != null && emptyView != null) {
            recyclerViewSavings.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            if (btnAddNewTarget != null) {
                btnAddNewTarget.setVisibility(View.GONE);
            }
        }
    }

    private void showListState() {
        Log.d(TAG, "Showing list state");
        if (recyclerViewSavings != null && emptyView != null) {
            recyclerViewSavings.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            if (btnAddNewTarget != null) {
                btnAddNewTarget.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupRecyclerView() {
        if (recyclerViewSavings == null) {
            Log.e(TAG, "RecyclerView is null! Cannot setup RecyclerView.");
            return;
        }

        Log.d(TAG, "Setting up RecyclerView with SavingsAdapter...");

        // Create and set adapter with proper listeners
        savingsAdapter = new SavingsAdapter(requireContext(), this, this);

        // Set layout manager and adapter
        recyclerViewSavings.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewSavings.setAdapter(savingsAdapter);

        Log.d(TAG, "RecyclerView setup completed successfully.");
        Log.d(TAG, "SavingsAdapter created with click listeners");    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called - real-time updates active");
        // No need to manually reload data - real-time observer handles it
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up repository resources
        if (savingsRepository != null) {
            savingsRepository.cleanup();        }
        Log.d(TAG, "SavingFragment destroyed and cleaned up");
    }

    private void navigateToAddSavings() {
        Log.d(TAG, "Navigating to AddSavingsActivity...");
        Intent intent = new Intent(getActivity(), AddSavingsActivity.class);
        startActivityForResult(intent, REQUEST_CODE_ADD_SAVINGS);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ADD_SAVINGS && resultCode == Activity.RESULT_OK && data != null) {
            Log.d(TAG, "Received result from AddSavingsActivity");            // Extract data from result
            String targetName = data.getStringExtra("target_name");
            String targetCategory = data.getStringExtra("target_category");
            double targetAmount = data.getDoubleExtra("target_amount", 0);
            long completionDate = data.getLongExtra("completion_date", 0);
            String photoUri = data.getStringExtra("photo_uri");
            String photoBase64 = data.getStringExtra("photo_base64");

            // Create new SavingsItem and save to Firebase
            saveSavingsToFirebase(targetName, targetCategory, targetAmount, completionDate, photoUri, photoBase64);

            // Add a temporary item immediately for better UX
            SavingsItem newItem = new SavingsItem();
            newItem.setName(targetName);
            newItem.setCategory(targetCategory);
            newItem.setTargetAmount(targetAmount);
            newItem.setCurrentAmount(0);
            newItem.setCompletionDate(completionDate);
            newItem.setPhotoUri(photoUri);
            newItem.setPhotoBase64(photoBase64); // Set Base64 data
            newItem.setCreatedAt(CURRENT_TIMESTAMP);

            // Add to list and update adapter
            if (savingsList == null) {
                savingsList = new ArrayList<>();
            }
            savingsList.add(0, newItem);

            // Make sure we're showing list state now that we have data
            showListState();

            if (savingsAdapter != null) {
                savingsAdapter.setSavingsList(savingsList);
            }

            Toast.makeText(getContext(), "Savings target added successfully!", Toast.LENGTH_SHORT).show();
        }
    }    private void saveSavingsToFirebase(String name, String category, double targetAmount,
                                       long completionDate, String photoUri, String photoBase64) {
        if (currentUser == null) {
            Toast.makeText(getContext(), "You need to be logged in to save targets", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Saving new savings target to Firebase...");

        // Create document data
        Map<String, Object> savingData = new HashMap<>();
        savingData.put("name", name);
        savingData.put("category", category);
        savingData.put("targetAmount", targetAmount);
        savingData.put("currentAmount", 0);
        savingData.put("completionDate", completionDate);
        savingData.put("photoUri", photoUri);
        savingData.put("photoBase64", photoBase64); // Store Base64 data
        savingData.put("createdAt", CURRENT_TIMESTAMP);
        savingData.put("userId", CURRENT_USER);

        // Add to Firestore
        db.collection("users")
                .document(currentUser.getUid())
                .collection("savings")
                .add(savingData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Savings target added with ID: " + documentReference.getId());

                    // Make sure list is showing
                    showListState();

                    // Reload data to get the real ID
                    // Real-time repository will automatically update the UI
                    // loadSavingsData(); // No longer needed
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding savings target", e);
                    Toast.makeText(getContext(), "Failed to save target: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // *** CRITICAL NAVIGATION METHOD ***
    @Override
    public void onSavingsItemClick(SavingsItem item) {
        Log.d(TAG, "=== SAVINGS ITEM CLICKED ===");
        Log.d(TAG, "Clicked item: " + item.getName());
        Log.d(TAG, "Item ID: " + item.getId());
        Log.d(TAG, "Current amount: " + item.getCurrentAmount());
        Log.d(TAG, "Target amount: " + item.getTargetAmount());
        Log.d(TAG, "Photo URI: " + item.getPhotoUri());
        Log.d(TAG, "Timestamp: " + CURRENT_DATE_TIME);
        Log.d(TAG, "User: " + CURRENT_USER);

        // Validate item data
        if (item.getId() == null || item.getId().isEmpty()) {
            Log.e(TAG, "ERROR: Savings item ID is null or empty!");
            Toast.makeText(getContext(), "Error: Invalid savings data", Toast.LENGTH_SHORT).show();
            return;
        }

        if (getActivity() == null) {
            Log.e(TAG, "ERROR: Fragment activity is null!");
            return;
        }

        try {
            // Create intent to navigate to SavingsDetailActivity
            Intent intent = new Intent(getActivity(), SavingsDetailActivity.class);

            // Pass the savings item and ID
            intent.putExtra("savings_item", item);
            intent.putExtra("savings_id", item.getId());

            // Add debug extras
            intent.putExtra("debug_timestamp", CURRENT_TIMESTAMP);
            intent.putExtra("debug_user", CURRENT_USER);
            intent.putExtra("debug_source", "SavingFragment");

            Log.d(TAG, "Intent created successfully");
            Log.d(TAG, "Extras added: savings_item, savings_id=" + item.getId());

            // Start the detail activity
            startActivity(intent);

            Log.d(TAG, "Successfully navigated to SavingsDetailActivity");
            Log.d(TAG, "=== NAVIGATION COMPLETED ===");
        } catch (Exception e) {
            Log.e(TAG, "ERROR during navigation", e);
            Toast.makeText(getContext(), "Navigation error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSavingItemRemove(SavingsItem item, int position) {
        Log.d(TAG, "Remove requested for item: " + item.getName() + " at position: " + position);
        showRemoveConfirmationDialog(item, position);
    }

    private void showRemoveConfirmationDialog(SavingsItem item, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Remove Savings Target");
        builder.setMessage("Are you sure you want to remove \"" + item.getName() + "\"?");

        builder.setPositiveButton("Yes", (dialog, which) -> {
            Log.d(TAG, "User confirmed removal of: " + item.getName());
            deleteSavingsFromFirestore(item.getId(), position);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            Log.d(TAG, "User cancelled removal");
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteSavingsFromFirestore(String savingsId, int position) {
        if (currentUser == null) {
            Toast.makeText(getContext(), "You need to be logged in to perform this action", Toast.LENGTH_SHORT).show();
            return;
        }

        if (savingsId == null || savingsId.isEmpty()) {
            Toast.makeText(getContext(), "Cannot delete: missing savings ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Deleting savings from Firestore: " + savingsId);

        db.collection("users")
                .document(currentUser.getUid())
                .collection("savings")
                .document(savingsId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Successfully deleted savings: " + savingsId);

                    // Remove the item from the adapter
                    if (savingsAdapter != null) {
                        savingsAdapter.removeItem(position);
                    }

                    // Also remove from our list
                    if (position < savingsList.size()) {
                        savingsList.remove(position);
                    }

                    // Check if list is now empty
                    if (savingsList.isEmpty()) {
                        // Switch to empty state
                        showEmptyState();
                    }

                    Toast.makeText(getContext(), "Savings target removed successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error removing savings target", e);
                    Toast.makeText(getContext(), "Failed to remove savings target: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}