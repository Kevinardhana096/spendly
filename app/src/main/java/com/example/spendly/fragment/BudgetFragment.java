package com.example.spendly.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spendly.R;
import com.example.spendly.activity.SetBudgetActivity;
import com.example.spendly.activity.SetTotalBudgetActivity;
import com.example.spendly.adapter.BudgetCategoryAdapter;
import com.example.spendly.model.BudgetCategory;
import com.example.spendly.repository.BudgetRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BudgetFragment extends Fragment implements BudgetCategoryAdapter.BudgetCategoryListener {

    private View budgetView;
    private View emptyStateContainer;
    private TextView tvMonthYear;
    private TextView tvRemainingBudget;
    private TextView tvUsedBudget;
    private TextView tvTotalBudget;
    private TextView tvBudgetStatus;
    private TextView btnEditBudget;
    private TextView btnAddCategory;
    private TextView tvNoCategories;
    private ProgressBar progressBudget;
    private ProgressBar loadingProgressBar;
    private RecyclerView rvBudgetCategories;
    private BudgetCategoryAdapter categoryAdapter;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private BudgetRepository budgetRepository;
    private boolean hasBudget = false;
    private boolean hasCategories = false;
    private double totalBudget = 0.0;
    private double usedBudget = 0.0;
    private double remainingBudget = 0.0;
    private boolean isOfflineMode = false;

    // Real-time listeners for budget updates
    private com.google.firebase.firestore.ListenerRegistration budgetListener;
    private com.google.firebase.firestore.ListenerRegistration categoriesListener;

    public BudgetFragment() {
        // Required empty public constructor
    }

    public static BudgetFragment newInstance() {
        return new BudgetFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (getActivity() != null) {
            budgetRepository = BudgetRepository.getInstance(getActivity());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the root container
        View rootView = inflater.inflate(R.layout.fragment_budget, container, false);

        // Initialize views
        initViews(rootView);
        setupRecyclerView();
        setClickListeners();

        return rootView;    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Hide both views initially until we check budget state
        emptyStateContainer.setVisibility(View.GONE);
        budgetView.setVisibility(View.GONE);
        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(View.VISIBLE);
        }
        
        // Setup real-time listeners for automatic updates
        setupRealtimeBudgetListeners();
    }

    // Add caching variables
    private boolean isDataLoaded = false;
    private long lastLoadTime = 0;
    private static final long CACHE_DURATION = 30000; // 30 seconds cache

    @Override
    public void onResume() {
        super.onResume();
        
        // Only reload if data is stale or not loaded yet
        long currentTime = System.currentTimeMillis();
        if (!isDataLoaded || (currentTime - lastLoadTime) > CACHE_DURATION) {
            checkBudgetState();
        }
    }

    private void initViews(View view) {
        emptyStateContainer = view.findViewById(R.id.empty_state_container);
        budgetView = view.findViewById(R.id.budget_content);
        tvMonthYear = view.findViewById(R.id.tv_month_year);
        tvRemainingBudget = view.findViewById(R.id.tv_remaining_budget);
        tvUsedBudget = view.findViewById(R.id.tv_used_amount);
        tvTotalBudget = view.findViewById(R.id.tv_total_budget);
        progressBudget = view.findViewById(R.id.progress_budget);
        tvBudgetStatus = view.findViewById(R.id.tv_budget_status);
        btnEditBudget = view.findViewById(R.id.btn_edit_budget);
        btnAddCategory = view.findViewById(R.id.btn_add_category);
        tvNoCategories = view.findViewById(R.id.tv_no_categories);
        rvBudgetCategories = view.findViewById(R.id.rv_budget_categories);
        loadingProgressBar = view.findViewById(R.id.loading_progress_bar);

        // Set current month and year
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(sdf.format(new Date()));
    }

    private void setupRecyclerView() {
        categoryAdapter = new BudgetCategoryAdapter(getContext(), this);
        rvBudgetCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        rvBudgetCategories.setAdapter(categoryAdapter);
    }

    private void setClickListeners() {
        btnEditBudget.setOnClickListener(v -> {
            // Direct to SetTotalBudgetActivity to edit the budget
            Intent intent = new Intent(getActivity(), SetTotalBudgetActivity.class);
            startActivity(intent);
        });

        btnAddCategory.setOnClickListener(v -> {
            // Direct to SetBudgetActivity to add a category
            Intent intent = new Intent(getActivity(), SetBudgetActivity.class);
            if (hasBudget) {
                intent.putExtra("monthly_budget", totalBudget);
                intent.putExtra("remaining_budget", remainingBudget);
            }
            startActivity(intent);
        });
    }

    private void checkBudgetState() {
        if (getActivity() == null || budgetRepository == null) return;

        if (loadingProgressBar != null) {
            loadingProgressBar.setVisibility(View.VISIBLE);
        }

        // Use the repository to check if budget exists, works offline
        budgetRepository.checkBudgetExists(new BudgetRepository.BudgetCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                boolean budgetExists = data.containsKey("exists") && (boolean) data.get("exists");
                isOfflineMode = data.containsKey("offline_only") && (boolean) data.get("offline_only");

                if (budgetExists) {
                    // Budget exists, get total budget data
                    getTotalBudgetData();
                } else {
                    // No budget found
                    if (loadingProgressBar != null) {
                        loadingProgressBar.setVisibility(View.GONE);
                    }
                    showEmptyState();
                }
            }            @Override
            public void onError(String error) {
                if (loadingProgressBar != null) {
                    loadingProgressBar.setVisibility(View.GONE);
                }

                showEmptyState();                if (getContext() != null) {                    Toast.makeText(getContext(), "Error checking budget: " + error,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Exception e) {

            }
        });
    }

    private void getTotalBudgetData() {
        budgetRepository.getTotalBudget(new BudgetRepository.BudgetCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                loadTotalBudgetData(data);
                hasBudget = true;

                // Check if categories exist
                checkBudgetCategories();
            }            @Override
            public void onError(String error) {
                if (loadingProgressBar != null) {
                    loadingProgressBar.setVisibility(View.GONE);
                }

                if (getContext() != null) {
                    Toast.makeText(getContext(),
                            "Error loading budget data: " + error,
                            Toast.LENGTH_SHORT).show();
                }

                showEmptyState();
            }

            @Override
            public void onError(Exception e) {

            }
        });
    }

    private void checkBudgetCategories() {
        budgetRepository.checkCategoriesExist(new BudgetRepository.BudgetCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                boolean categoriesExist = data.containsKey("exists") && (boolean) data.get("exists");
                hasCategories = categoriesExist;

                if (categoriesExist) {
                    // Load categories data
                    getBudgetCategories();
                } else {
                    if (loadingProgressBar != null) {
                        loadingProgressBar.setVisibility(View.GONE);
                    }

                    // Show budget view with no categories
                    showBudgetView();
                    showNoCategories();
                }
            }            @Override
            public void onError(String error) {
                if (loadingProgressBar != null) {
                    loadingProgressBar.setVisibility(View.GONE);
                }

                // Show budget view with error toast
                showBudgetView();
                showNoCategories();

                if (getContext() != null) {
                    Toast.makeText(getContext(),
                        "Failed to check budget categories. You can add them with the + button.",
                        Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Exception e) {

            }
        });
    }

    private void getBudgetCategories() {
        budgetRepository.getBudgetCategories(new BudgetRepository.BudgetCallback() {
            @Override
            public void onSuccess(Map<String, Object> data) {
                if (loadingProgressBar != null) {
                    loadingProgressBar.setVisibility(View.GONE);
                }

                loadBudgetCategories(data);
                showBudgetView();
                showCategoriesList();

                // If we're in offline mode, show notification
                if (isOfflineMode && getContext() != null) {
                    Toast.makeText(getContext(),
                            "Offline mode: Using locally saved budget data",
                            Toast.LENGTH_SHORT).show();
                }
            }            @Override
            public void onError(String error) {
                if (loadingProgressBar != null) {
                    loadingProgressBar.setVisibility(View.GONE);
                }

                showBudgetView();
                showNoCategories();

                if (getContext() != null) {
                    Toast.makeText(getContext(),
                            "Error loading categories: " + error,
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Exception e) {

            }
        });
    }

    private void loadTotalBudgetData(Map<String, Object> data) {
        // Extract budget data
        if (data.containsKey("monthly_budget")) {
            if (data.get("monthly_budget") instanceof Double) {
                totalBudget = (Double) data.get("monthly_budget");
            } else if (data.get("monthly_budget") instanceof Long) {
                totalBudget = ((Long) data.get("monthly_budget")).doubleValue();
            }
        }

        if (data.containsKey("remaining_budget")) {
            if (data.get("remaining_budget") instanceof Double) {
                remainingBudget = (Double) data.get("remaining_budget");
            } else if (data.get("remaining_budget") instanceof Long) {
                remainingBudget = ((Long) data.get("remaining_budget")).doubleValue();
            }
        }

        // Calculate used budget
        usedBudget = totalBudget - remainingBudget;

        // Update UI with budget amounts
        String formattedRemainingBudget = data.containsKey("remaining_formatted") ?
            data.get("remaining_formatted").toString() : formatNumber((int)remainingBudget);

        String formattedTotalBudget = data.containsKey("budget_formatted") ?
            data.get("budget_formatted").toString() : formatNumber((int)totalBudget);

        String formattedUsedBudget = formatNumber((int)usedBudget);

        tvRemainingBudget.setText("Rp" + formattedRemainingBudget);
        tvTotalBudget.setText("From Rp" + formattedTotalBudget);
        tvUsedBudget.setText("Used Rp" + formattedUsedBudget);

        // Update progress bar
        int progressPercentage = totalBudget > 0 ? (int)((usedBudget / totalBudget) * 100) : 0;
        progressBudget.setProgress(progressPercentage);

        // Update budget status message based on spending
        updateBudgetStatusMessage(progressPercentage);
    }

    private void updateBudgetStatusMessage(int progressPercentage) {
        if (progressPercentage < 50) {
            tvBudgetStatus.setText("You're on track! You can still spend Rp" + formatNumber((int)remainingBudget) + " for the rest of this month.");
        } else if (progressPercentage < 80) {
            tvBudgetStatus.setText("You've used " + progressPercentage + "% of your budget. Try to limit your spending for the rest of the month.");
        } else {
            tvBudgetStatus.setText("Warning! You've used " + progressPercentage + "% of your monthly budget. Be careful with your spending.");
        }
    }

    private void loadBudgetCategories(Map<String, Object> categoriesData) {
        List<BudgetCategory> categoryList = new ArrayList<>();

        for (Map.Entry<String, Object> entry : categoriesData.entrySet()) {
            String categoryName = entry.getKey();
            if (categoryName.equals("offline_only")) continue;

            @SuppressWarnings("unchecked")
            Map<String, Object> categoryData = (Map<String, Object>) entry.getValue();

            double amount = 0;
            double spent = 0;
            String formattedAmount = "0";
            String formattedSpent = "0";
            String dateAdded = new Date().toString();

            if (categoryData.containsKey("amount")) {
                if (categoryData.get("amount") instanceof Double) {
                    amount = (Double) categoryData.get("amount");
                } else if (categoryData.get("amount") instanceof Long) {
                    amount = ((Long) categoryData.get("amount")).doubleValue();
                }
            }

            if (categoryData.containsKey("spent")) {
                if (categoryData.get("spent") instanceof Double) {
                    spent = (Double) categoryData.get("spent");
                } else if (categoryData.get("spent") instanceof Long) {
                    spent = ((Long) categoryData.get("spent")).doubleValue();
                }
            }

            if (categoryData.containsKey("formatted_amount")) {
                formattedAmount = categoryData.get("formatted_amount").toString();
            }

            if (categoryData.containsKey("formatted_spent")) {
                formattedSpent = categoryData.get("formatted_spent").toString();
            }

            if (categoryData.containsKey("date_added")) {
                dateAdded = categoryData.get("date_added").toString();
            }

            BudgetCategory category = new BudgetCategory(
                categoryName,
                "", // We'll set icon based on name in the adapter
                amount,
                spent,
                formattedAmount,
                formattedSpent,
                dateAdded
            );

            categoryList.add(category);
        }

        // Update the adapter with the categories
        if (categoryAdapter != null) {
            categoryAdapter.setCategories(categoryList);
        }

        // Update UI state based on whether we have categories or not
        hasCategories = !categoryList.isEmpty();
        if (hasCategories) {
            showCategoriesList();
        } else {
            showNoCategories();
        }
    }

    private void showCategoriesList() {
        if (tvNoCategories != null) {
            tvNoCategories.setVisibility(View.GONE);
        }
        if (rvBudgetCategories != null) {
            rvBudgetCategories.setVisibility(View.VISIBLE);
        }
    }

    private void showNoCategories() {
        if (tvNoCategories != null) {
            tvNoCategories.setVisibility(View.VISIBLE);
        }
        if (rvBudgetCategories != null) {
            rvBudgetCategories.setVisibility(View.GONE);
        }
    }

    private void showBudgetView() {
        // Hide empty state and show budget content
        if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
            emptyStateContainer.setVisibility(View.GONE);
            budgetView.setVisibility(View.VISIBLE);
        }
    }

    private void showEmptyState() {
        // Show empty state fragment and hide budget content
        if (getActivity() != null && !getActivity().isFinishing() && isAdded()) {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.empty_state_container, new BudgetingEmptyStateFragment())
                    .commit();

            emptyStateContainer.setVisibility(View.VISIBLE);
            budgetView.setVisibility(View.GONE);
        }
    }

    private String formatNumber(int number) {
        return String.format(Locale.getDefault(), "%,d", number).replace(",", ".");
    }

    // Attempt to sync data when coming back online
    public void syncData() {
        if (budgetRepository != null && isOfflineMode) {
            budgetRepository.syncWithFirebase(new BudgetRepository.BudgetCallback() {
                @Override
                public void onSuccess(Map<String, Object> data) {
                    isOfflineMode = false;
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Budget data synchronized with cloud", Toast.LENGTH_SHORT).show();
                    }
                }                @Override
                public void onError(String error) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to sync: " + error, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(Exception e) {

                }
            });
        }
    }

    // BudgetCategoryAdapter.BudgetCategoryListener implementation
    @Override
    public void onCategoryClicked(int position) {
        List<BudgetCategory> categories = categoryAdapter.getCategories();
        if (position >= 0 && position < categories.size()) {
            BudgetCategory category = categories.get(position);

            // Toggle expanded state
            category.setExpanded(!category.isExpanded());
            categoryAdapter.updateCategory(position, category);
        }
    }

    @Override
    public void onEditCategoryClicked(int position) {
        List<BudgetCategory> categories = categoryAdapter.getCategories();
        if (position >= 0 && position < categories.size()) {
            BudgetCategory category = categories.get(position);

            // Send to SetBudgetActivity with category information
            Intent intent = new Intent(getActivity(), SetBudgetActivity.class);
            intent.putExtra("monthly_budget", totalBudget);
            intent.putExtra("remaining_budget", remainingBudget);
            intent.putExtra("edit_mode", true);
            intent.putExtra("category_name", category.getCategoryName());
            intent.putExtra("category_amount", category.getAmount());
            startActivity(intent);
        }
    }

    @Override
    public void onAddExpenseClicked(int position) {
        List<BudgetCategory> categories = categoryAdapter.getCategories();
        if (position >= 0 && position < categories.size()) {
            BudgetCategory category = categories.get(position);

            // TODO: Launch AddTransactionActivity with category pre-selected
            Toast.makeText(getContext(), "Add expense for " + category.getCategoryName(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Setup real-time Firestore listeners for budget data
     */
    private void setupRealtimeBudgetListeners() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        // Listen for total budget changes
        budgetListener = db.collection("users")
                .document(currentUser.getUid())
                .collection("budget")
                .document("total_budget")
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        if (data != null) {
                            loadTotalBudgetData(data);
                            updateBudgetUI();
                        }
                    }
                });

        // Listen for budget categories changes
        categoriesListener = db.collection("users")
                .document(currentUser.getUid())
                .collection("budget")
                .whereNotEqualTo("category", null)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        return;
                    }

                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        Map<String, Object> categoriesData = new HashMap<>();
                        categoriesData.put("categories", querySnapshot.getDocuments());
                        loadBudgetCategories(categoriesData);
                        updateCategoriesUI();
                    }
                });
    }

    /**
     * Update budget UI elements
     */
    private void updateBudgetUI() {
        if (!isAdded()) return;

        if (tvTotalBudget != null) {
            tvTotalBudget.setText(String.format("From Rp%,.0f", totalBudget));
        }
        if (tvRemainingBudget != null) {
            tvRemainingBudget.setText(String.format("Rp%,.0f", remainingBudget));
        }
        if (tvUsedBudget != null) {
            tvUsedBudget.setText(String.format("Used Rp%,.0f", usedBudget));
        }

        // Update progress bar
        if (progressBudget != null && totalBudget > 0) {
            int progressPercentage = (int) ((usedBudget / totalBudget) * 100);
            progressBudget.setProgress(Math.min(progressPercentage, 100));
        }

        // Update budget status
        if (tvBudgetStatus != null) {
            if (remainingBudget < 0) {
                tvBudgetStatus.setText("Over budget!");
                tvBudgetStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else if (remainingBudget < totalBudget * 0.1) {
                tvBudgetStatus.setText("Low budget remaining");
                tvBudgetStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            } else {
                tvBudgetStatus.setText("Budget on track");
                tvBudgetStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
        }
    }

    /**
     * Update categories UI
     */
    private void updateCategoriesUI() {
        if (!isAdded()) return;

        if (categoryAdapter != null) {
            categoryAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Clean up real-time listeners
        if (budgetListener != null) {
            budgetListener.remove();
        }
        if (categoriesListener != null) {
            categoriesListener.remove();
        }
    }
}
