package com.example.spendly.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
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
import com.example.spendly.adapter.SavingsAdapter;
import com.example.spendly.model.SavingsItem;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SavingFragment extends Fragment implements SavingsAdapter.OnSavingsItemClickListener, SavingsAdapter.OnSavingItemRemoveListener {

    private static final String TAG = "SavingFragment";
    private static final int REQUEST_CODE_ADD_SAVINGS = 1001;

    // View variables
    private View rootView;
    private ViewSwitcher viewSwitcher;
    private View emptyView;
    private View listView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    // UI components for savings list layout
    private MaterialButton btnAddNewTarget;
    private RecyclerView recyclerViewSavings;
    private SavingsAdapter savingsAdapter;
    private List<SavingsItem> savingsList;

    // UI components for empty state layout
    private Button btnAddFirstSaving;

    public SavingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize savings list
        savingsList = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Create the main container with a ViewSwitcher
        rootView = inflater.inflate(R.layout.fragment_saving, container, false);

        // Initialize views
        setupViews(inflater, container);

        // Check if data exists and show appropriate state
        checkForSavingsData();

        return rootView;
    }

    private void setupViews(LayoutInflater inflater, ViewGroup container) {
        // Set up the list view (recycler view with items)
        recyclerViewSavings = rootView.findViewById(R.id.recyclerView_savings);
        btnAddNewTarget = rootView.findViewById(R.id.btn_add_new_target);

        if (btnAddNewTarget != null) {
            btnAddNewTarget.setOnClickListener(v -> navigateToAddSavings());
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
        }
    }

    private void checkForSavingsData() {
        if (currentUser == null) return;

        db.collection("users")
                .document(currentUser.getUid())
                .collection("savings")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // No data, show empty state
                        Log.d(TAG, "No savings data found, showing empty state");
                        showEmptyState();
                    } else {
                        // Data exists, show list state and load all data
                        Log.d(TAG, "Savings data found, showing list view");
                        showListState();
                        loadSavingsData();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking for savings data", e);
                    // On error, show empty state as fallback
                    showEmptyState();
                });
    }

    private void showEmptyState() {
        if (recyclerViewSavings != null && emptyView != null) {
            recyclerViewSavings.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            if (btnAddNewTarget != null) {
                btnAddNewTarget.setVisibility(View.GONE);
            }
        }
    }

    private void showListState() {
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

        // Create and set adapter
        savingsAdapter = new SavingsAdapter(requireContext(), this, this);

        // Set layout manager and adapter
        recyclerViewSavings.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewSavings.setAdapter(savingsAdapter);

        // Debug log
        Log.d(TAG, "RecyclerView setup completed successfully.");
    }

    @Override
    public void onResume() {
        super.onResume();

        // Always check for data when fragment resumes
        if (currentUser != null) {
            checkForSavingsData();
        }
    }

    private void loadSavingsData() {
        if (currentUser == null || recyclerViewSavings == null) {
            Log.e(TAG, "Cannot load savings data - user is null or RecyclerView is null");
            return;
        }

        Log.d(TAG, "Loading savings data from Firestore...");

        db.collection("users")
                .document(currentUser.getUid())
                .collection("savings")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    savingsList.clear();

                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Data found in Firestore
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            SavingsItem item = document.toObject(SavingsItem.class);
                            if (item != null) {
                                item.setId(document.getId());
                                savingsList.add(item);
                                Log.d(TAG, "Added item: " + item.getName() + ", id: " + item.getId());
                            }
                        }

                        // Update adapter with the new data
                        if (savingsAdapter != null) {
                            savingsAdapter.setSavingsList(savingsList);
                            Log.d(TAG, "Updated adapter with " + savingsList.size() + " items");
                            showListState();
                        }
                    } else {
                        // No data found, show empty state
                        showEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading savings data", e);
                    Toast.makeText(getContext(), "Failed to load savings data", Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToAddSavings() {
        Intent intent = new Intent(getActivity(), AddSavingsActivity.class);
        startActivityForResult(intent, REQUEST_CODE_ADD_SAVINGS);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ADD_SAVINGS && resultCode == Activity.RESULT_OK && data != null) {
            // Extract data from result
            String targetName = data.getStringExtra("target_name");
            String targetCategory = data.getStringExtra("target_category");
            double targetAmount = data.getDoubleExtra("target_amount", 0);
            long completionDate = data.getLongExtra("completion_date", 0);
            String photoUri = data.getStringExtra("photo_uri");

            // Create new SavingsItem and save to Firebase
            saveSavingsToFirebase(targetName, targetCategory, targetAmount, completionDate, photoUri);

            // Add a temporary item immediately for better UX
            SavingsItem newItem = new SavingsItem();
            newItem.setName(targetName);
            newItem.setCategory(targetCategory);
            newItem.setTargetAmount(targetAmount);
            newItem.setCurrentAmount(0);
            newItem.setCompletionDate(completionDate);
            newItem.setPhotoUri(photoUri);
            newItem.setCreatedAt(System.currentTimeMillis());

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
    }

    private void saveSavingsToFirebase(String name, String category, double targetAmount,
                                      long completionDate, String photoUri) {
        if (currentUser == null) {
            Toast.makeText(getContext(), "You need to be logged in to save targets", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create document data
        Map<String, Object> savingData = new HashMap<>();
        savingData.put("name", name);
        savingData.put("category", category);
        savingData.put("targetAmount", targetAmount);
        savingData.put("currentAmount", 0);
        savingData.put("completionDate", completionDate);
        savingData.put("photoUri", photoUri);
        savingData.put("createdAt", System.currentTimeMillis());

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
                    loadSavingsData();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding savings target", e);
                    Toast.makeText(getContext(), "Failed to save target", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onSavingsItemClick(SavingsItem item) {
        // Handle click on savings item
        Toast.makeText(getContext(), "Selected: " + item.getName(), Toast.LENGTH_SHORT).show();
        // TODO: Navigate to savings detail screen
    }

    @Override
    public void onSavingItemRemove(SavingsItem item, int position) {
        showRemoveConfirmationDialog(item, position);
    }

    private void showRemoveConfirmationDialog(SavingsItem item, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Remove Savings Target");
        builder.setMessage("Are you sure you want to remove \"" + item.getName() + "\"?");

        builder.setPositiveButton("Yes", (dialog, which) -> {
            deleteSavingsFromFirestore(item.getId(), position);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

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

        db.collection("users")
                .document(currentUser.getUid())
                .collection("savings")
                .document(savingsId)
                .delete()
                .addOnSuccessListener(aVoid -> {
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
                    Toast.makeText(getContext(), "Failed to remove savings target", Toast.LENGTH_SHORT).show();
                });
    }
}

